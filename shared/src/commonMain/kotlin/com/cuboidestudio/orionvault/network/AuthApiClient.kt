@file:OptIn(ExperimentalTime::class)

package com.cuboidestudio.orionvault.network

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Wrapper fino sobre os endpoints REST do Supabase Auth (GoTrue).
 *
 * Zero-knowledge: aqui trafega apenas a senha **da conta na nuvem**, que é uma credencial
 * totalmente distinta da Master Password do cofre e nunca participa de nenhuma derivação de
 * chave. As senhas chegam como [CharArray] (mesma convenção de `VaultRepository.createVault`);
 * a conversão para `String` acontece no último instante possível, só para montar o corpo JSON —
 * desvio de higiene documentado e inevitável com serialização JSON.
 */
class AuthApiClient(
    private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun signUp(email: String, password: CharArray): Result<SignUpResult> = runCatching {
        val response = postJson("${SupabaseConfig.authBaseUrl}/signup", EmailPasswordRequest(email, password.concatToString()))
        val body = requireSuccess(response)
        val parsed = json.decodeFromString(AuthTokenResponse.serializer(), body)
        val session = parsed.toAuthSession(fallbackEmail = email)
        if (session == null) SignUpResult.ConfirmationRequired else SignUpResult.SignedIn(session)
    }

    suspend fun login(email: String, password: CharArray): Result<AuthSession> = runCatching {
        val response = postJson(
            "${SupabaseConfig.authBaseUrl}/token?grant_type=password",
            EmailPasswordRequest(email, password.concatToString())
        )
        val body = requireSuccess(response)
        json.decodeFromString(AuthTokenResponse.serializer(), body).toAuthSession(fallbackEmail = email)
            ?: error("O servidor não devolveu uma sessão válida")
    }

    suspend fun refresh(refreshToken: String): Result<AuthSession> = runCatching {
        val response = postJson(
            "${SupabaseConfig.authBaseUrl}/token?grant_type=refresh_token",
            RefreshTokenRequest(refreshToken)
        )
        val body = requireSuccess(response)
        json.decodeFromString(AuthTokenResponse.serializer(), body).toAuthSession(fallbackEmail = "")
            ?: error("O servidor não devolveu uma sessão válida")
    }

    suspend fun logout(accessToken: String): Result<Unit> = runCatching {
        val response = httpClient.post("${SupabaseConfig.authBaseUrl}/logout") {
            header("apikey", SupabaseConfig.anonKey)
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
        }
        requireSuccess(response)
    }.map { }

    private suspend inline fun <reified T : Any> postJson(url: String, body: T): HttpResponse =
        httpClient.post(url) {
            header("apikey", SupabaseConfig.anonKey)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun requireSuccess(response: HttpResponse): String {
        val text = response.bodyAsText()
        if (response.status.isSuccess()) return text
        val message = runCatching {
            json.decodeFromString(SupabaseErrorResponse.serializer(), text).bestMessage()
        }.getOrNull()
        error(message ?: "Falha na autenticação (HTTP ${response.status.value})")
    }

    private fun AuthTokenResponse.toAuthSession(fallbackEmail: String): AuthSession? {
        val access = accessToken ?: return null
        val refresh = refreshToken ?: return null
        val expiresAt = Clock.System.now().toEpochMilliseconds() + (expiresIn ?: 3600L) * 1000L
        return AuthSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochMillis = expiresAt,
            userId = user?.id ?: id ?: "",
            email = user?.email ?: email ?: fallbackEmail
        )
    }
}
