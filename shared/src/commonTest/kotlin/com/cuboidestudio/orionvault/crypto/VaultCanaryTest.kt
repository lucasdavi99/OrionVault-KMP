package com.cuboidestudio.orionvault.crypto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultCanaryTest : LibsodiumTestBase() {

    @Test
    fun verifySucceedsWithCorrectKey() = runLibsodiumTest {
        val key = SecretKeyGenerator.generate() + SecretKeyGenerator.generate()
        val canary = VaultCanary.create(key)

        assertTrue(VaultCanary.verify(canary, key))
    }

    @Test
    fun verifyFailsWithWrongKey() = runLibsodiumTest {
        val key = SecretKeyGenerator.generate() + SecretKeyGenerator.generate()
        val wrongKey = SecretKeyGenerator.generate() + SecretKeyGenerator.generate()
        val canary = VaultCanary.create(key)

        assertFalse(VaultCanary.verify(canary, wrongKey))
    }
}
