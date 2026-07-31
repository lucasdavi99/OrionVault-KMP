package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException

/**
 * Blob cifrado fixo usado para validar a Master Password no unlock sem precisar
 * decriptar itens reais do cofre.
 */
object VaultCanary {
    private const val PLAINTEXT = "orionvault-canary-v1"

    fun create(key: ByteArray): CipherBlob = AeadCipher.encrypt(PLAINTEXT.encodeToByteArray(), key)

    fun verify(blob: CipherBlob, key: ByteArray): Boolean {
        return try {
            AeadCipher.decrypt(blob, key).decodeToString() == PLAINTEXT
        } catch (e: AeadCorrupedOrTamperedDataException) {
            false
        }
    }
}
