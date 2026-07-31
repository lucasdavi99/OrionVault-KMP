package com.cuboidestudio.orionvault.crypto

import com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AeadCipherTest : LibsodiumTestBase() {

    @Test
    fun roundTripEncryptDecrypt() = runLibsodiumTest {
        val key = SecretKeyGenerator.generate() + SecretKeyGenerator.generate() // 32 bytes
        val plaintext = "hunter2".encodeToByteArray()

        val blob = AeadCipher.encrypt(plaintext, key)
        val decrypted = AeadCipher.decrypt(blob, key)

        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun nonceIsNotReusedAcrossCalls() = runLibsodiumTest {
        val key = SecretKeyGenerator.generate() + SecretKeyGenerator.generate()
        val plaintext = "hunter2".encodeToByteArray()

        val blob1 = AeadCipher.encrypt(plaintext, key)
        val blob2 = AeadCipher.encrypt(plaintext, key)

        assertNotEquals(blob1.nonce.toList(), blob2.nonce.toList())
    }

    @Test
    fun tamperedCiphertextFailsToDecrypt() = runLibsodiumTest {
        val key = SecretKeyGenerator.generate() + SecretKeyGenerator.generate()
        val plaintext = "hunter2".encodeToByteArray()

        val blob = AeadCipher.encrypt(plaintext, key)
        blob.ciphertext[0] = (blob.ciphertext[0] + 1).toByte()

        assertFailsWith<AeadCorrupedOrTamperedDataException> {
            AeadCipher.decrypt(blob, key)
        }
    }

    @Test
    fun storageRoundTripPreservesBlob() = runLibsodiumTest {
        val key = SecretKeyGenerator.generate() + SecretKeyGenerator.generate()
        val plaintext = "hunter2".encodeToByteArray()

        val blob = AeadCipher.encrypt(plaintext, key)
        val restored = CipherBlob.fromStorageString(blob.toStorageString())
        val decrypted = AeadCipher.decrypt(restored, key)

        assertContentEquals(plaintext, decrypted)
    }
}
