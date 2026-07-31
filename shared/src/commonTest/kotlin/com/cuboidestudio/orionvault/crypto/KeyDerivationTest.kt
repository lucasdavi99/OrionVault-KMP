package com.cuboidestudio.orionvault.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

class KeyDerivationTest : LibsodiumTestBase() {

    @Test
    fun sameInputsProduceSameKey() = runLibsodiumTest {
        val secretKey = SecretKeyGenerator.generate()
        val params = KdfParamsV1.newParams()

        val key1 = KeyDerivation.deriveVaultKey("correct horse".toCharArray(), secretKey, params)
        val key2 = KeyDerivation.deriveVaultKey("correct horse".toCharArray(), secretKey, params)

        assertContentEquals(key1, key2)
    }

    @Test
    fun differentPasswordProducesDifferentKey() = runLibsodiumTest {
        val secretKey = SecretKeyGenerator.generate()
        val params = KdfParamsV1.newParams()

        val key1 = KeyDerivation.deriveVaultKey("correct horse".toCharArray(), secretKey, params)
        val key2 = KeyDerivation.deriveVaultKey("wrong horse".toCharArray(), secretKey, params)

        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun differentSecretKeyProducesDifferentKey() = runLibsodiumTest {
        val params = KdfParamsV1.newParams()

        val key1 = KeyDerivation.deriveVaultKey("correct horse".toCharArray(), SecretKeyGenerator.generate(), params)
        val key2 = KeyDerivation.deriveVaultKey("correct horse".toCharArray(), SecretKeyGenerator.generate(), params)

        assertFalse(key1.contentEquals(key2))
    }
}
