@file:OptIn(ExperimentalTime::class, FlowPreview::class)

package com.cuboidestudio.orionvault.sync

import com.cuboidestudio.orionvault.domain.model.SyncFolder
import com.cuboidestudio.orionvault.domain.model.SyncItem
import com.cuboidestudio.orionvault.domain.model.SyncState
import com.cuboidestudio.orionvault.domain.repository.VaultRepository
import com.cuboidestudio.orionvault.network.PushResult
import com.cuboidestudio.orionvault.network.SyncApiClient
import com.cuboidestudio.orionvault.session.AccountSessionManager
import com.cuboidestudio.orionvault.session.AccountState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/**
 * Conflito detectado no push: o servidor recusou a escrita otimista porque a linha mudou desde
 * a última sincronização deste dispositivo. Cobre pastas e itens (o `isFolder` distingue).
 */
data class SyncConflict(
    val id: String,
    val title: String,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long,
    val isFolder: Boolean
)

/**
 * Motor de sincronização: push (com concorrência otimista) seguido de pull incremental.
 *
 * Zero-knowledge: só manipula [SyncFolder]/[SyncItem], que carregam ciphertext cru. A chave do
 * cofre não é acessível a partir daqui.
 */
class SyncEngine(
    private val vaultRepository: VaultRepository,
    private val accountSessionManager: AccountSessionManager,
    private val syncApiClient: SyncApiClient,
    private val scope: CoroutineScope
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt.asStateFlow()

    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()

    private val changeSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val syncMutex = Mutex()

    /** Chamado pelo `VaultRepositoryImpl` a cada escrita local (hook injetado, plano 4.5). */
    fun notifyLocalChange() {
        changeSignal.tryEmit(Unit)
    }

    init {
        scope.launch {
            changeSignal.debounce(DEBOUNCE_MILLIS).collect { runSyncIfLoggedIn() }
        }
        // Entrar numa conta é o momento de (a) publicar os parâmetros de KDF locais, para que um
        // futuro segundo dispositivo consiga restaurar este cofre, e (b) sincronizar na hora — sem
        // isso um cofre recém-restaurado só baixaria o conteúdo na próxima escrita local ou num
        // "Sincronizar agora" manual. StateFlow já deduplica valores iguais, então basta reagir.
        scope.launch {
            accountSessionManager.accountState.collect { state ->
                if (state is AccountState.LoggedIn) {
                    vaultRepository.getLocalKdfParams()?.let { params ->
                        // Falha aqui (offline, tabela ausente) não pode impedir o sync de rodar.
                        runCatching { syncApiClient.pushVaultKdfIfAbsent(params, state.userId) }
                    }
                    runSyncIfLoggedIn()
                }
            }
        }
    }

    /** Gatilho de app em foreground; ligado a partir de cada entry point de plataforma. */
    fun onAppForegrounded() {
        scope.launch { runSyncIfLoggedIn() }
    }

    /** Botão "Sincronizar agora" da SyncSettingsScreen. */
    fun syncNow() {
        scope.launch { runSyncIfLoggedIn() }
    }

    private suspend fun runSyncIfLoggedIn() {
        if (accountSessionManager.accountState.value !is AccountState.LoggedIn) return
        val userId = accountSessionManager.currentUserId ?: return
        syncMutex.withLock {
            _status.value = SyncStatus.Syncing
            try {
                pushLocalChanges(userId)
                pullRemoteChanges(userId)
                _lastSyncedAt.value = Clock.System.now().toEpochMilliseconds()
                _status.value = SyncStatus.Idle
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: "Falha na sincronização")
            }
        }
    }

    // --- Push ---

    private suspend fun pushLocalChanges(userId: String) {
        val deviceId = vaultRepository.getOrCreateDeviceId()

        // Pastas primeiro: itens referenciam folder_id, que precisa existir no servidor.
        for (folder in vaultRepository.listDirtyFolders()) {
            when (folder.syncState) {
                SyncState.DIRTY_NEW -> when (val result = syncApiClient.insertFolder(folder, userId)) {
                    is PushResult.Applied -> vaultRepository.markFolderSynced(folder.id, result.version)
                    PushResult.Conflict -> recordFolderConflict(folder)
                }

                SyncState.DIRTY_UPDATED ->
                    when (val result = syncApiClient.updateFolderIfVersion(folder, folder.syncedVersion)) {
                        is PushResult.Applied -> vaultRepository.markFolderSynced(folder.id, result.version)
                        PushResult.Conflict -> recordFolderConflict(folder)
                    }

                SyncState.TOMBSTONE ->
                    when (syncApiClient.deleteFolderIfVersion(folder, folder.syncedVersion)) {
                        is PushResult.Applied -> vaultRepository.purgeTombstone(folder.id, isFolder = true)
                        // Alguém já apagou/alterou remotamente: a linha local morre de todo jeito.
                        PushResult.Conflict -> vaultRepository.purgeTombstone(folder.id, isFolder = true)
                    }
            }
        }

        for (item in vaultRepository.listDirtyItems()) {
            when (item.syncState) {
                SyncState.DIRTY_NEW -> when (val result = syncApiClient.insertItem(item, userId)) {
                    is PushResult.Applied -> {
                        vaultRepository.markItemSynced(item.id, result.version)
                        syncApiClient.insertItemVersion(item, result.version, deviceId)
                    }

                    PushResult.Conflict -> recordItemConflict(item)
                }

                SyncState.DIRTY_UPDATED ->
                    when (val result = syncApiClient.updateItemIfVersion(item, item.syncedVersion)) {
                        is PushResult.Applied -> {
                            vaultRepository.markItemSynced(item.id, result.version)
                            syncApiClient.insertItemVersion(item, result.version, deviceId)
                        }

                        PushResult.Conflict -> recordItemConflict(item)
                    }

                SyncState.TOMBSTONE ->
                    when (syncApiClient.deleteItemIfVersion(item, item.syncedVersion)) {
                        is PushResult.Applied -> vaultRepository.purgeTombstone(item.id, isFolder = false)
                        PushResult.Conflict -> vaultRepository.purgeTombstone(item.id, isFolder = false)
                    }
            }
        }
    }

    private suspend fun recordFolderConflict(folder: SyncFolder) {
        val remote = syncApiClient.fetchFolder(folder.id)
        addConflict(
            SyncConflict(
                id = folder.id,
                title = folder.name,
                localUpdatedAt = folder.updatedAt,
                remoteUpdatedAt = remote?.updatedAt ?: 0L,
                isFolder = true
            )
        )
    }

    private suspend fun recordItemConflict(item: SyncItem) {
        val remote = syncApiClient.fetchItem(item.id)
        addConflict(
            SyncConflict(
                id = item.id,
                title = item.title,
                localUpdatedAt = item.updatedAt,
                remoteUpdatedAt = remote?.updatedAt ?: 0L,
                isFolder = false
            )
        )
    }

    private fun addConflict(conflict: SyncConflict) {
        _conflicts.value = _conflicts.value.filterNot { it.id == conflict.id } + conflict
    }

    // --- Pull ---

    private suspend fun pullRemoteChanges(userId: String) {
        val cursor = vaultRepository.getSyncCursor() ?: 0L
        var maxSeen = cursor

        for ((folder, deleted) in syncApiClient.fetchFoldersSince(cursor, userId)) {
            if (deleted) {
                deleteLocally(folder.id, isFolder = true)
            } else {
                vaultRepository.applyRemoteFolder(folder)
            }
            if (folder.updatedAt > maxSeen) maxSeen = folder.updatedAt
        }

        for ((item, deleted) in syncApiClient.fetchItemsSince(cursor, userId)) {
            if (deleted) {
                deleteLocally(item.id, isFolder = false)
            } else {
                vaultRepository.applyRemoteItem(item)
            }
            if (item.updatedAt > maxSeen) maxSeen = item.updatedAt
        }

        if (maxSeen > cursor) vaultRepository.setSyncCursor(maxSeen)
    }

    /**
     * O servidor já confirmou o delete, então não há tombstone a propagar de volta: remove a
     * linha local direto.
     */
    private suspend fun deleteLocally(id: String, isFolder: Boolean) {
        if (isFolder) vaultRepository.deleteFolder(id) else vaultRepository.deleteItem(id)
        vaultRepository.purgeTombstone(id, isFolder)
    }

    // --- Resolução de conflito (UI mínima, plano 5.5) ---

    /**
     * "Manter a versão deste dispositivo": rebaseia a escrita local sobre a versão atual do
     * servidor e re-empurra no próximo ciclo.
     */
    fun resolveKeepLocal(conflict: SyncConflict) {
        scope.launch {
            runCatching {
                if (conflict.isFolder) {
                    val remote = syncApiClient.fetchFolder(conflict.id) ?: return@runCatching
                    val local = vaultRepository.listDirtyFolders().firstOrNull { it.id == conflict.id }
                        ?: return@runCatching
                    when (val result = syncApiClient.updateFolderIfVersion(local, remote.version)) {
                        is PushResult.Applied -> vaultRepository.markFolderSynced(local.id, result.version)
                        PushResult.Conflict -> return@runCatching
                    }
                } else {
                    val remote = syncApiClient.fetchItem(conflict.id) ?: return@runCatching
                    val local = vaultRepository.listDirtyItems().firstOrNull { it.id == conflict.id }
                        ?: return@runCatching
                    when (val result = syncApiClient.updateItemIfVersion(local, remote.version)) {
                        is PushResult.Applied -> {
                            vaultRepository.markItemSynced(local.id, result.version)
                            syncApiClient.insertItemVersion(local, result.version, vaultRepository.getOrCreateDeviceId())
                        }

                        PushResult.Conflict -> return@runCatching
                    }
                }
                dismissConflict(conflict)
            }.onFailure { _status.value = SyncStatus.Error(it.message ?: "Falha ao resolver conflito") }
        }
    }

    /** "Manter a versão do outro dispositivo": descarta a edição local e aplica a do servidor. */
    fun resolveKeepRemote(conflict: SyncConflict) {
        scope.launch {
            runCatching {
                if (conflict.isFolder) {
                    syncApiClient.fetchFolder(conflict.id)?.let { vaultRepository.applyRemoteFolder(it) }
                } else {
                    syncApiClient.fetchItem(conflict.id)?.let { vaultRepository.applyRemoteItem(it) }
                }
                dismissConflict(conflict)
            }.onFailure { _status.value = SyncStatus.Error(it.message ?: "Falha ao resolver conflito") }
        }
    }

    private fun dismissConflict(conflict: SyncConflict) {
        _conflicts.value = _conflicts.value.filterNot { it.id == conflict.id }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 3_000L
    }
}
