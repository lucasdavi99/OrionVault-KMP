package com.cuboidestudio.orionvault.domain.repository

import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem

class FolderDepthExceededException(message: String) : RuntimeException(message)

/**
 * Ponto único de acesso ao cofre local: ciclo de vida (criação/unlock/lock) e CRUD de
 * pastas/itens, já cifrando/decifrando os campos sensíveis (design doc seções 3, 4, 5).
 */
interface VaultRepository {
    suspend fun isVaultInitialized(): Boolean

    /** Cria o cofre e retorna a Secret Key formatada para exibição única no onboarding. */
    suspend fun createVault(masterPassword: CharArray): String

    /** Tenta desbloquear o cofre; retorna false se a Master Password estiver incorreta. */
    suspend fun unlock(masterPassword: CharArray): Boolean
    fun lock()
    fun isUnlocked(): Boolean

    suspend fun listFolders(parentId: String?): List<VaultFolder>
    suspend fun createFolder(parentId: String?, name: String): VaultFolder
    suspend fun renameFolder(id: String, newName: String)
    suspend fun deleteFolder(id: String)

    suspend fun listItems(folderId: String): List<VaultItem>
    suspend fun createItem(
        folderId: String,
        title: String,
        username: String?,
        password: String,
        url: String?,
        notes: String?
    ): VaultItem

    suspend fun updateItem(
        id: String,
        title: String,
        username: String?,
        password: String,
        url: String?,
        notes: String?
    )

    suspend fun deleteItem(id: String)
}
