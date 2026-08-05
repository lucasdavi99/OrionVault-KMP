package com.cuboidestudio.orionvault.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EmailPasswordRequest(
    val email: String,
    val password: String
)

@Serializable
internal data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
internal data class SupabaseUser(
    val id: String,
    val email: String? = null
)

@Serializable
internal data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: SupabaseUser? = null,
    // Resposta de /signup com "Confirm email" ligado: vem só o usuário, sem tokens, e o
    // objeto raiz é o próprio usuário (id/email no topo).
    val id: String? = null,
    val email: String? = null
)

@Serializable
internal data class SupabaseErrorResponse(
    val message: String? = null,
    val msg: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
) {
    fun bestMessage(): String? = message ?: msg ?: errorDescription ?: error
}
