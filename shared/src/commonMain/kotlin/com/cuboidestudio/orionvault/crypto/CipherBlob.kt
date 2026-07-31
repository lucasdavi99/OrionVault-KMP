package com.cuboidestudio.orionvault.crypto

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Nonce + ciphertext (com tag de autenticação já embutida) de um blob cifrado com
 * XChaCha20-Poly1305. Serializado como `Base64(nonce).Base64(ciphertext)` para armazenamento
 * em colunas de texto.
 */
class CipherBlob(val nonce: ByteArray, val ciphertext: ByteArray) {
    @OptIn(ExperimentalEncodingApi::class)
    fun toStorageString(): String = "${Base64.encode(nonce)}.${Base64.encode(ciphertext)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CipherBlob) return false
        return nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()

    companion object {
        @OptIn(ExperimentalEncodingApi::class)
        fun fromStorageString(value: String): CipherBlob {
            val parts = value.split(".")
            require(parts.size == 2) { "Formato de CipherBlob inválido" }
            return CipherBlob(Base64.decode(parts[0]), Base64.decode(parts[1]))
        }
    }
}
