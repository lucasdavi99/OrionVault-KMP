package com.cuboidestudio.orionvault.domain.repository

import com.cuboidestudio.orionvault.crypto.KdfParams
import com.cuboidestudio.orionvault.domain.model.SyncFolder
import com.cuboidestudio.orionvault.domain.model.SyncItem
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

    /**
     * Recria localmente um cofre que já existe em outro dispositivo (design doc seção 3.4).
     * Diferente de [createVault], nada é sorteado aqui: a Secret Key vem do que o usuário digitou
     * e os [params] vêm do servidor (`vault_kdf`), para que a chave derivada seja **idêntica** à
     * do dispositivo original e o conteúdo baixado pelo sync decifre.
     */
    suspend fun restoreVault(masterPassword: CharArray, secretKey: ByteArray, params: KdfParams)

    /**
     * Deriva a chave a partir de [masterPassword]/[secretKey]/[params] (sem persistir nada) e
     * tenta decifrar [sampleCipher] — uma coluna cifrada real de um item já sincronizado nessa
     * conta. Usado no fluxo de restauração para detectar Master Password ou Secret Key erradas
     * *antes* de [restoreVault], já que um valor errado hoje não falharia sozinho: produziria um
     * cofre local "válido" cuja chave simplesmente não bate com a do dispositivo original, e os
     * itens baixados pelo sync sumiriam silenciosamente da lista (mesmo comportamento defensivo
     * de [listItems] para itens corrompidos/adulterados).
     */
    suspend fun verifyKeyAgainstCiphertext(masterPassword: CharArray, secretKey: ByteArray, params: KdfParams, sampleCipher: String): Boolean

    /** Tenta desbloquear o cofre; retorna false se a Master Password estiver incorreta. */
    suspend fun unlock(masterPassword: CharArray): Boolean

    /**
     * Desbloqueia com uma chave já derivada (ex.: recuperada via desbloqueio biométrico secundário).
     * Revalida contra o canary antes de aceitar — nunca confia cegamente na origem da chave.
     */
    suspend fun unlockWithKey(key: ByteArray): Boolean

    /**
     * Chave de sessão atual, para uso exclusivo por quem ativa o desbloqueio biométrico logo após
     * um unlock por senha bem-sucedido. Retorna null se o cofre estiver bloqueado.
     */
    fun currentSessionKeyOrNull(): ByteArray?
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

    // --- Superfície voltada ao motor de sync (nada aqui decifra nada) ---

    /** Pastas com `syncState != 0`, incluindo tombstones, na ordem de push. */
    suspend fun listDirtyFolders(): List<SyncFolder>

    /** Itens com `syncState != 0`, incluindo tombstones, na ordem de push. */
    suspend fun listDirtyItems(): List<SyncItem>

    /** Marca a linha como limpa e registra a versão confirmada pelo servidor. */
    suspend fun markFolderSynced(id: String, newVersion: Long)
    suspend fun markItemSynced(id: String, newVersion: Long)

    /** Remove fisicamente uma linha em tombstone (delete já confirmado no servidor). */
    suspend fun purgeTombstone(id: String, isFolder: Boolean)

    /** Aplica localmente uma linha vinda do servidor (upsert por id, deixando-a limpa). */
    suspend fun applyRemoteFolder(remote: SyncFolder)
    suspend fun applyRemoteItem(remote: SyncItem)

    suspend fun getSyncCursor(): Long?
    suspend fun setSyncCursor(timestamp: Long)

    /** Identificador aleatório e persistente deste dispositivo, usado em `item_versions`. Não é segredo. */
    suspend fun getOrCreateDeviceId(): String

    /**
     * Parâmetros de KDF deste cofre (salt + custo), ou nulo se ainda não há cofre. Não são segredo
     * e são o que o motor de sync publica em `vault_kdf` para viabilizar a restauração em outro
     * dispositivo — expostos aqui para que o `SyncEngine` não precise falar com o secure store.
     */
    suspend fun getLocalKdfParams(): KdfParams?
}
