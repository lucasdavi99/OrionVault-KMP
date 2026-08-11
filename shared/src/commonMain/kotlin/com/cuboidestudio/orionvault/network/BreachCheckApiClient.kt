package com.cuboidestudio.orionvault.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

internal data class HibpRangeEntry(val suffix: String, val count: Int)

/**
 * Consulta a API k-anonymity do Have I Been Pwned: só um prefixo de 5 caracteres do hash SHA-1
 * da senha sai do aparelho, nunca a senha nem o hash completo (ver `Sha1.kt`). Deliberadamente
 * não reaproveita o `json`/ContentNegotiation usado pelos clients do Supabase — a resposta é
 * texto puro, e este host é de terceiro, então nenhum header de auth do Supabase é enviado aqui.
 */
internal class BreachCheckApiClient(private val httpClient: HttpClient) {
    suspend fun checkRange(prefixHex5: String): Result<List<HibpRangeEntry>> = runCatching {
        val response = httpClient.get("$BASE_URL$prefixHex5") {
            header("Add-Padding", "true")
        }
        val body = requireSuccess(response)
        parseHibpRange(body)
    }

    private suspend fun requireSuccess(response: HttpResponse): String {
        val text = response.bodyAsText()
        if (response.status.isSuccess()) return text
        error("Falha ao consultar HIBP (HTTP ${response.status.value})")
    }

    private companion object {
        const val BASE_URL = "https://api.pwnedpasswords.com/range/"
    }
}

/** Extraído como função pura para ser testável sem precisar mockar HTTP. */
internal fun parseHibpRange(body: String): List<HibpRangeEntry> =
    body.lineSequence().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@mapNotNull null
        val parts = trimmed.split(":")
        if (parts.size != 2) return@mapNotNull null
        val count = parts[1].toIntOrNull() ?: return@mapNotNull null
        HibpRangeEntry(suffix = parts[0], count = count)
    }.toList()
