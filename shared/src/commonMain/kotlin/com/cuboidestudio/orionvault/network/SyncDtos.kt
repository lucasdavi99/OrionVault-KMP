package com.cuboidestudio.orionvault.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Campo cifrado no formato do backend: duas colunas (`*_ciphertext` / `*_nonce`), ambas em
 * Base64. Localmente o mesmo dado vive como uma única string `Base64(nonce).Base64(ciphertext)`
 * (`crypto/CipherBlob.kt`), então a conversão é puro split/join — **nunca** há decifragem no
 * caminho de rede.
 */
internal object CipherFieldCodec {
    fun ciphertextOf(storage: String?): String? = storage?.substringAfter('.', "")?.takeIf { it.isNotEmpty() }
    fun nonceOf(storage: String?): String? = storage?.substringBefore('.', "")?.takeIf { it.isNotEmpty() }

    fun toStorage(ciphertext: String?, nonce: String?): String? {
        if (ciphertext.isNullOrEmpty() || nonce.isNullOrEmpty()) return null
        return "$nonce.$ciphertext"
    }
}

@Serializable
internal data class RemoteFolderDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    val name: String,
    val depth: Int = 1,
    val version: Long = 1,
    val deleted: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
internal data class RemoteFolderPatch(
    @SerialName("parent_id") val parentId: String? = null,
    val name: String? = null,
    val depth: Int? = null,
    val version: Long,
    val deleted: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
internal data class RemoteItemDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("folder_id") val folderId: String? = null,
    val title: String,
    @SerialName("username_ciphertext") val usernameCiphertext: String? = null,
    @SerialName("username_nonce") val usernameNonce: String? = null,
    @SerialName("email_ciphertext") val emailCiphertext: String? = null,
    @SerialName("email_nonce") val emailNonce: String? = null,
    @SerialName("password_ciphertext") val passwordCiphertext: String = "",
    @SerialName("password_nonce") val passwordNonce: String = "",
    @SerialName("url_ciphertext") val urlCiphertext: String? = null,
    @SerialName("url_nonce") val urlNonce: String? = null,
    @SerialName("notes_ciphertext") val notesCiphertext: String? = null,
    @SerialName("notes_nonce") val notesNonce: String? = null,
    val version: Long = 1,
    val deleted: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
internal data class RemoteItemPatch(
    @SerialName("folder_id") val folderId: String? = null,
    val title: String? = null,
    @SerialName("username_ciphertext") val usernameCiphertext: String? = null,
    @SerialName("username_nonce") val usernameNonce: String? = null,
    @SerialName("email_ciphertext") val emailCiphertext: String? = null,
    @SerialName("email_nonce") val emailNonce: String? = null,
    @SerialName("password_ciphertext") val passwordCiphertext: String? = null,
    @SerialName("password_nonce") val passwordNonce: String? = null,
    @SerialName("url_ciphertext") val urlCiphertext: String? = null,
    @SerialName("url_nonce") val urlNonce: String? = null,
    @SerialName("notes_ciphertext") val notesCiphertext: String? = null,
    @SerialName("notes_nonce") val notesNonce: String? = null,
    val version: Long,
    val deleted: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
internal data class RemoteItemVersionDto(
    @SerialName("item_id") val itemId: String,
    val version: Long,
    val title: String,
    @SerialName("username_ciphertext") val usernameCiphertext: String? = null,
    @SerialName("username_nonce") val usernameNonce: String? = null,
    @SerialName("email_ciphertext") val emailCiphertext: String? = null,
    @SerialName("email_nonce") val emailNonce: String? = null,
    @SerialName("password_ciphertext") val passwordCiphertext: String = "",
    @SerialName("password_nonce") val passwordNonce: String = "",
    @SerialName("url_ciphertext") val urlCiphertext: String? = null,
    @SerialName("url_nonce") val urlNonce: String? = null,
    @SerialName("notes_ciphertext") val notesCiphertext: String? = null,
    @SerialName("notes_nonce") val notesNonce: String? = null,
    @SerialName("device_id") val deviceId: String
)

/**
 * Parâmetros do Argon2id da conta (`vault_kdf`). Não são segredo: o salt existe para inviabilizar
 * rainbow tables, não para esconder informação. Publicá-los é o que permite a um segundo
 * dispositivo derivar exatamente a mesma chave do cofre a partir da Master Password + Secret Key
 * digitadas pelo usuário — nenhuma das quais trafega.
 */
@Serializable
internal data class RemoteVaultKdfDto(
    @SerialName("user_id") val userId: String,
    val version: Int,
    @SerialName("ops_limit") val opsLimit: Long,
    @SerialName("mem_limit_bytes") val memLimitBytes: Int,
    /** Base64 do salt, mesmo alfabeto usado por `crypto/CipherBlob.kt`. */
    val salt: String,
    @SerialName("created_at") val createdAt: String
)

/** Resultado de um push com guarda de concorrência otimista. */
sealed interface PushResult {
    /** O servidor aceitou a escrita; [version] é a nova versão autoritativa da linha. */
    data class Applied(val version: Long) : PushResult

    /** 0 linhas afetadas: outra origem alterou a linha desde o último sync. */
    data object Conflict : PushResult
}
