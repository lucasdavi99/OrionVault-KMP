@file:OptIn(ExperimentalUnsignedTypes::class)

package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.util.LibsodiumRandom

/**
 * Gera a Secret Key do cofre (design doc seção 3.1): alta entropia, gerada no dispositivo,
 * nunca enviada ao servidor. Exibida uma única vez no onboarding para o usuário anotar/salvar.
 */
object SecretKeyGenerator {
    const val SECRET_KEY_BYTES = 16 // 128 bits

    fun generate(): ByteArray = LibsodiumRandom.buf(SECRET_KEY_BYTES).asByteArray()

    /** Formata a Secret Key em blocos hex de 4 caracteres (ex.: A3F97C1D-...) para exibição/anotação. */
    fun formatForDisplay(secretKey: ByteArray): String =
        secretKey.toHex().chunked(4).joinToString("-")

    /** Reverte [formatForDisplay], aceitando o valor digitado/anotado pelo usuário. */
    fun parseFromDisplay(formatted: String): ByteArray {
        val hex = formatted.replace("-", "").trim()
        require(hex.length % 2 == 0) { "Secret Key inválida: tamanho ímpar" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}

private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

private fun ByteArray.toHex(): String {
    val result = StringBuilder(size * 2)
    for (b in this) {
        val i = b.toInt() and 0xFF
        result.append(HEX_CHARS[i shr 4])
        result.append(HEX_CHARS[i and 0x0F])
    }
    return result.toString()
}
