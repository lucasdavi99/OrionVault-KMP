@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.cuboidestudio.orionvault.domain.repository

import com.cuboidestudio.orionvault.crypto.AeadCipher
import com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException
import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.cuboidestudio.orionvault.crypto.KdfParams
import com.cuboidestudio.orionvault.crypto.KdfParamsV1
import com.cuboidestudio.orionvault.crypto.KeyDerivation
import com.cuboidestudio.orionvault.crypto.SecretKeyGenerator
import com.cuboidestudio.orionvault.crypto.VaultCanary
import com.cuboidestudio.orionvault.crypto.ensureLibsodiumInitialized
import com.cuboidestudio.orionvault.domain.model.SyncFolder
import com.cuboidestudio.orionvault.domain.model.SyncItem
import com.cuboidestudio.orionvault.domain.model.SyncState
import com.cuboidestudio.orionvault.domain.model.VaultConstants
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import com.cuboidestudio.orionvault.session.AccountSessionManager
import com.cuboidestudio.orionvault.session.AccountState
import com.cuboidestudio.orionvault.storage.db.Folder
import com.cuboidestudio.orionvault.storage.db.Item
import com.cuboidestudio.orionvault.storage.db.OrionVaultDatabase
import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
import com.cuboidestudio.orionvault.storage.secure.StoredVaultSecrets
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class VaultRepositoryImpl(
    private val database: OrionVaultDatabase,
    private val secureStore: SecureCredentialStore,
    /**
     * Usado apenas para decidir se um tombstone precisa mesmo esperar confirmação do servidor.
     * Nulo em testes e em qualquer cenário sem camada de nuvem.
     */
    private val accountSessionManager: AccountSessionManager? = null,
    /**
     * Hook de notificação de mudança local (fase 4.5 do plano). Callback simples em vez de um
     * Flow: o único consumidor é o `SyncEngine`, que converte isso num SharedFlow internamente.
     */
    private val onLocalChange: () -> Unit = {}
) : VaultRepository {
    private var sessionKey: ByteArray? = null

    override suspend fun isVaultInitialized(): Boolean = secureStore.loadVaultSecrets() != null

    override suspend fun createVault(masterPassword: CharArray): String {
        ensureLibsodiumInitialized()
        val secretKey = SecretKeyGenerator.generate()
        val params = KdfParamsV1.newParams()
        val key = KeyDerivation.deriveVaultKey(masterPassword, secretKey, params)
        val canary = VaultCanary.create(key)
        secureStore.saveVaultSecrets(StoredVaultSecrets(secretKey, params, canary))
        sessionKey = key
        return SecretKeyGenerator.formatForDisplay(secretKey)
    }

    override suspend fun restoreVault(masterPassword: CharArray, secretKey: ByteArray, params: KdfParams) {
        ensureLibsodiumInitialized()
        // Nada é sorteado: a mesma Master Password + Secret Key + salt do dispositivo de origem
        // produzem, por definição, a mesma chave — é isso que faz o conteúdo baixado decifrar.
        val key = KeyDerivation.deriveVaultKey(masterPassword, secretKey, params)
        val canary = VaultCanary.create(key)
        secureStore.saveVaultSecrets(StoredVaultSecrets(secretKey, params, canary))
        sessionKey = key
    }

    override suspend fun verifyKeyAgainstCiphertext(
        masterPassword: CharArray,
        secretKey: ByteArray,
        params: KdfParams,
        sampleCipher: String
    ): Boolean {
        ensureLibsodiumInitialized()
        val key = KeyDerivation.deriveVaultKey(masterPassword, secretKey, params)
        return try {
            decryptField(sampleCipher, key)
            true
        } catch (e: AeadCorrupedOrTamperedDataException) {
            false
        }
    }

    override suspend fun unlock(masterPassword: CharArray): Boolean {
        ensureLibsodiumInitialized()
        val stored = secureStore.loadVaultSecrets() ?: return false
        val key = KeyDerivation.deriveVaultKey(masterPassword, stored.secretKey, stored.kdfParams)
        if (!VaultCanary.verify(stored.canary, key)) return false
        sessionKey = key
        return true
    }

    override suspend fun unlockWithKey(key: ByteArray): Boolean {
        ensureLibsodiumInitialized()
        val stored = secureStore.loadVaultSecrets() ?: return false
        if (!VaultCanary.verify(stored.canary, key)) return false
        // Cópia própria: quem chama (ex.: desbloqueio biométrico) costuma zerar o array recebido
        // logo depois, por higiene de memória — sessionKey não pode compartilhar essa referência.
        sessionKey = key.copyOf()
        return true
    }

    override fun currentSessionKeyOrNull(): ByteArray? = sessionKey

    override fun lock() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    override fun isUnlocked(): Boolean = sessionKey != null

    private fun requireKey(): ByteArray = sessionKey ?: error("Vault está bloqueado")

    override suspend fun listFolders(parentId: String?): List<VaultFolder> =
        database.vaultQueries.selectFoldersByParent(parentId).executeAsList().map { it.toVaultFolder() }

    override suspend fun listAllFolders(): List<VaultFolder> =
        database.vaultQueries.selectAllFolders().executeAsList().map { it.toVaultFolder() }

    private fun Folder.toVaultFolder() =
        VaultFolder(id, parentId, name, createdAt, updatedAt, version.toInt())

    override suspend fun createFolder(parentId: String?, name: String): VaultFolder {
        val newDepth = depthOf(parentId) + 1
        if (newDepth > VaultConstants.MAX_FOLDER_DEPTH) {
            throw FolderDepthExceededException(
                "Profundidade máxima de pastas (${VaultConstants.MAX_FOLDER_DEPTH}) excedida"
            )
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val id = newId()
        database.vaultQueries.insertFolder(id, parentId, name, now, now, 1L, SyncState.DIRTY_NEW, 0L)
        onLocalChange()
        return VaultFolder(id, parentId, name, now, now, 1)
    }

    override suspend fun renameFolder(id: String, newName: String) {
        val current = database.vaultQueries.selectFolderById(id).executeAsOne()
        database.vaultQueries.renameFolder(
            newName,
            Clock.System.now().toEpochMilliseconds(),
            current.version + 1,
            nextDirtyState(current.syncState),
            id
        )
        onLocalChange()
    }

    override suspend fun deleteFolder(id: String) {
        deleteFolderRecursive(id)
        onLocalChange()
    }

    /**
     * Exclusão de pasta é em cascata: subpastas e itens dentro delas não podem sobrar
     * apontando para um `parentId`/`folderId` de uma pasta já tombstoneada.
     */
    private fun deleteFolderRecursive(id: String) {
        database.vaultQueries.selectFolderIdsByParent(id).executeAsList().forEach { childId ->
            deleteFolderRecursive(childId)
        }
        database.vaultQueries.selectItemIdsByFolder(id).executeAsList().forEach { itemId ->
            tombstoneItemRow(itemId)
        }
        tombstoneFolderRow(id)
    }

    private fun tombstoneFolderRow(id: String) {
        val current = database.vaultQueries.selectFolderById(id).executeAsOneOrNull() ?: return
        database.vaultQueries.tombstoneFolder(Clock.System.now().toEpochMilliseconds(), id)
        if (shouldPurgeImmediately(current.syncState)) {
            database.vaultQueries.purgeTombstoneFolder(id)
        }
    }

    private fun tombstoneItemRow(id: String) {
        val current = database.vaultQueries.selectItemById(id).executeAsOneOrNull() ?: return
        database.vaultQueries.tombstoneItem(Clock.System.now().toEpochMilliseconds(), id)
        if (shouldPurgeImmediately(current.syncState)) {
            database.vaultQueries.purgeTombstoneItem(id)
        }
    }

    override suspend fun listItems(folderId: String?): List<VaultItem> {
        ensureLibsodiumInitialized()
        val key = requireKey()
        // Um item com tag AEAD inválida (corrompido, adulterado, ou cifrado sob uma chave
        // diferente da sessão atual) não deve derrubar a listagem inteira - o cofre precisa
        // continuar navegável para que o usuário veja e trate o item afetado isoladamente.
        return database.vaultQueries.selectItemsByFolder(folderId).executeAsList().mapNotNull {
            try {
                toVaultItem(it, key)
            } catch (e: AeadCorrupedOrTamperedDataException) {
                null
            }
        }
    }

    private fun toVaultItem(row: Item, key: ByteArray): VaultItem = VaultItem(
        id = row.id,
        folderId = row.folderId,
        title = row.title,
        username = row.usernameCipher?.let { decryptField(it, key) },
        email = row.emailCipher?.let { decryptField(it, key) },
        password = decryptField(row.passwordCipher, key),
        url = row.urlCipher?.let { decryptField(it, key) },
        notes = row.notesCipher?.let { decryptField(it, key) },
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        version = row.version.toInt()
    )

    override suspend fun createItem(
        folderId: String?,
        title: String,
        username: String?,
        email: String?,
        password: String,
        url: String?,
        notes: String?
    ): VaultItem {
        ensureLibsodiumInitialized()
        val key = requireKey()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = newId()
        database.vaultQueries.insertItem(
            id, folderId, title,
            username?.let { encryptField(it, key) },
            email?.let { encryptField(it, key) },
            encryptField(password, key),
            url?.let { encryptField(it, key) },
            notes?.let { encryptField(it, key) },
            now, now, 1L, SyncState.DIRTY_NEW, 0L
        )
        onLocalChange()
        return VaultItem(id, folderId, title, username, email, password, url, notes, now, now, 1)
    }

    override suspend fun updateItem(
        id: String,
        folderId: String?,
        title: String,
        username: String?,
        email: String?,
        password: String,
        url: String?,
        notes: String?
    ) {
        ensureLibsodiumInitialized()
        val key = requireKey()
        val current = database.vaultQueries.selectItemById(id).executeAsOne()
        val now = Clock.System.now().toEpochMilliseconds()
        database.vaultQueries.updateItem(
            folderId,
            title,
            username?.let { encryptField(it, key) },
            email?.let { encryptField(it, key) },
            encryptField(password, key),
            url?.let { encryptField(it, key) },
            notes?.let { encryptField(it, key) },
            now,
            current.version + 1,
            nextDirtyState(current.syncState),
            id
        )
        onLocalChange()
    }

    override suspend fun deleteItem(id: String) {
        tombstoneItemRow(id)
        onLocalChange()
    }

    // --- Superfície de sync ---

    override suspend fun listDirtyFolders(): List<SyncFolder> =
        database.vaultQueries.selectDirtyFolders().executeAsList().map { row ->
            SyncFolder(
                id = row.id,
                parentId = row.parentId,
                name = row.name,
                depth = depthOf(row.parentId) + 1,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                version = row.version,
                syncState = row.syncState,
                syncedVersion = row.syncedVersion
            )
        }

    override suspend fun listDirtyItems(): List<SyncItem> =
        database.vaultQueries.selectDirtyItems().executeAsList().map { row ->
            SyncItem(
                id = row.id,
                folderId = row.folderId,
                title = row.title,
                usernameCipher = row.usernameCipher,
                emailCipher = row.emailCipher,
                passwordCipher = row.passwordCipher,
                urlCipher = row.urlCipher,
                notesCipher = row.notesCipher,
                createdAt = row.createdAt,
                updatedAt = row.updatedAt,
                version = row.version,
                syncState = row.syncState,
                syncedVersion = row.syncedVersion
            )
        }

    override suspend fun markFolderSynced(id: String, newVersion: Long) {
        database.vaultQueries.markFolderSynced(newVersion, id)
    }

    override suspend fun markItemSynced(id: String, newVersion: Long) {
        database.vaultQueries.markItemSynced(newVersion, id)
    }

    override suspend fun purgeTombstone(id: String, isFolder: Boolean) {
        if (isFolder) {
            database.vaultQueries.purgeTombstoneFolder(id)
        } else {
            database.vaultQueries.purgeTombstoneItem(id)
        }
    }

    override suspend fun applyRemoteFolder(remote: SyncFolder) {
        database.vaultQueries.upsertRemoteFolder(
            remote.id,
            remote.parentId,
            remote.name,
            remote.createdAt,
            remote.updatedAt,
            remote.version,
            remote.version
        )
    }

    override suspend fun applyRemoteItem(remote: SyncItem) {
        database.vaultQueries.upsertRemoteItem(
            remote.id,
            remote.folderId,
            remote.title,
            remote.usernameCipher,
            remote.emailCipher,
            remote.passwordCipher,
            remote.urlCipher,
            remote.notesCipher,
            remote.createdAt,
            remote.updatedAt,
            remote.version,
            remote.version
        )
    }

    override suspend fun getSyncCursor(): Long? =
        database.vaultQueries.selectSyncMeta().executeAsOneOrNull()?.lastPulledAt

    override suspend fun setSyncCursor(timestamp: Long) {
        database.vaultQueries.ensureSyncMetaRow()
        database.vaultQueries.updateSyncCursor(timestamp)
    }

    override suspend fun getOrCreateDeviceId(): String {
        val existing = database.vaultQueries.selectSyncMeta().executeAsOneOrNull()?.deviceId
        if (existing != null) return existing
        val generated = newId()
        database.vaultQueries.ensureSyncMetaRow()
        database.vaultQueries.updateDeviceId(generated)
        return generated
    }

    override suspend fun getLocalKdfParams(): KdfParams? = secureStore.loadVaultSecrets()?.kdfParams

    /**
     * 0 (limpo) vira 2 (sujo, já enviado antes). 1 e 2 já significam "precisa push", então
     * ficam como estão — coalescing automático de múltiplas edições entre dois syncs.
     */
    private fun nextDirtyState(currentState: Long): Long =
        if (currentState == SyncState.CLEAN) SyncState.DIRTY_UPDATED else currentState

    /**
     * Um tombstone só precisa sobreviver até o servidor confirmar o delete. Se a linha nunca
     * foi enviada, ou se não há conta logada (e portanto nunca haverá push), a linha some na hora.
     */
    private fun shouldPurgeImmediately(currentState: Long): Boolean =
        currentState == SyncState.DIRTY_NEW ||
            accountSessionManager?.accountState?.value !is AccountState.LoggedIn

    private fun depthOf(folderId: String?): Int {
        var depth = 0
        var current = folderId
        while (current != null) {
            depth++
            current = database.vaultQueries.selectFolderById(current).executeAsOneOrNull()?.parentId
        }
        return depth
    }

    private fun newId(): String = Uuid.random().toString()

    private fun encryptField(plaintext: String, key: ByteArray): String =
        AeadCipher.encrypt(plaintext.encodeToByteArray(), key).toStorageString()

    private fun decryptField(stored: String, key: ByteArray): String =
        AeadCipher.decrypt(CipherBlob.fromStorageString(stored), key).decodeToString()
}
