@file:OptIn(ExperimentalUnsignedTypes::class)

package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
import com.ionspin.kotlin.crypto.util.LibsodiumRandom

/**
 * Cifragem simétrica AEAD dos campos sensíveis do cofre, usando XChaCha20-Poly1305
 * (decisão registrada nas seções 3.3/12 do design doc). Nonce de 24 bytes gerado
 * aleatoriamente a cada chamada — seguro para geração aleatória sem gestão de contador.
 *
 * A decriptação lança [com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException]
 * quando a tag de autenticação não bate (dado corrompido, adulterado ou chave errada).
 */
object AeadCipher {
    private val EMPTY_AAD = UByteArray(0)

    fun encrypt(plaintext: ByteArray, key: ByteArray, aad: ByteArray? = null): CipherBlob {
        val nonce = LibsodiumRandom.buf(crypto_aead_xchacha20poly1305_ietf_NPUBBYTES)
        val ciphertext = AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfEncrypt(
            message = plaintext.asUByteArray(),
            associatedData = aad?.asUByteArray() ?: EMPTY_AAD,
            nonce = nonce,
            key = key.asUByteArray()
        )
        return CipherBlob(nonce.asByteArray(), ciphertext.asByteArray())
    }

    fun decrypt(blob: CipherBlob, key: ByteArray, aad: ByteArray? = null): ByteArray {
        val plaintext = AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfDecrypt(
            ciphertextAndTag = blob.ciphertext.asUByteArray(),
            associatedData = aad?.asUByteArray() ?: EMPTY_AAD,
            nonce = blob.nonce.asUByteArray(),
            key = key.asUByteArray()
        )
        return plaintext.asByteArray()
    }
}
