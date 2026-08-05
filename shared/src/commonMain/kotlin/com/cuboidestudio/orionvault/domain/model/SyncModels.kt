package com.cuboidestudio.orionvault.domain.model

/** Valores da coluna `syncState` de `Folder`/`Item` (ver Vault.sq). */
object SyncState {
    const val CLEAN = 0L
    const val DIRTY_NEW = 1L
    const val DIRTY_UPDATED = 2L
    const val TOMBSTONE = 3L
}

/**
 * Visão "crua" de uma pasta para o motor de sync: nada é decifrado aqui — `name` já era texto
 * claro localmente por design, e nenhuma outra coluna de pasta é sensível.
 */
data class SyncFolder(
    val id: String,
    val parentId: String?,
    val name: String,
    val depth: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val syncState: Long,
    val syncedVersion: Long
)

/**
 * Visão "crua" de um item para o motor de sync. Os campos `*Cipher` são exatamente as strings
 * `Base64(nonce).Base64(ciphertext)` guardadas localmente — o sync **nunca** chama
 * `AeadCipher.decrypt`, e a chave do cofre nunca chega a esta camada.
 */
data class SyncItem(
    val id: String,
    val folderId: String?,
    val title: String,
    val usernameCipher: String?,
    val emailCipher: String?,
    val passwordCipher: String,
    val urlCipher: String?,
    val notesCipher: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val syncState: Long,
    val syncedVersion: Long
)
