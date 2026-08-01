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

    /** Todas as pastas do cofre, sem respeitar hierarquia — usado pelo seletor de pasta do editor de itens. */
    suspend fun listAllFolders(): List<VaultFolder>
    suspend fun createFolder(parentId: String?, name: String): VaultFolder
    suspend fun renameFolder(id: String, newName: String)
    suspend fun deleteFolder(id: String)

    /** folderId nulo lista/cria contas avulsas (sem pasta). */
    suspend fun listItems(folderId: String?): List<VaultItem>

    suspend fun createItem(
        folderId: String?,
        title: String,
        username: String?,
        email: String?,
        password: String,
        url: String?,
        notes: String?
    ): VaultItem

    suspend fun updateItem(
        id: String,
        folderId: String?,
        title: String,
        username: String?,
        email: String?,
        password: String,
        url: String?,
        notes: String?
    )

    suspend fun deleteItem(id: String)
}
