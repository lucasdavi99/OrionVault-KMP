package com.cuboidestudio.orionvault.storage.secure

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Serialização em texto (uma linha por campo) de [StoredAuthSession], no mesmo espírito de
 * [VaultSecretsSerializer]. Os campos vão em Base64 para que nenhum token com caractere
 * inesperado quebre o formato por linhas.
 */
@OptIn(ExperimentalEncodingApi::class)
object AuthSessionSerializer {
    private const val FORMAT_VERSION = 1

    fun serialize(session: StoredAuthSession): String = buildString {
        appendLine(FORMAT_VERSION)
        appendLine(Base64.encode(session.accessToken.encodeToByteArray()))
        appendLine(Base64.encode(session.refreshToken.encodeToByteArray()))
        appendLine(session.expiresAtEpochMillis)
        appendLine(Base64.encode(session.userId.encodeToByteArray()))
        append(Base64.encode(session.email.encodeToByteArray()))
    }

    fun deserialize(raw: String): StoredAuthSession? {
        val lines = raw.split("\n")
        if (lines.size < 6) return null
        if (lines[0].trim().toIntOrNull() != FORMAT_VERSION) return null
        return runCatching {
            StoredAuthSession(
                accessToken = Base64.decode(lines[1].trim()).decodeToString(),
                refreshToken = Base64.decode(lines[2].trim()).decodeToString(),
                expiresAtEpochMillis = lines[3].trim().toLong(),
                userId = Base64.decode(lines[4].trim()).decodeToString(),
                email = Base64.decode(lines[5].trim()).decodeToString()
            )
        }.getOrNull()
    }
}
