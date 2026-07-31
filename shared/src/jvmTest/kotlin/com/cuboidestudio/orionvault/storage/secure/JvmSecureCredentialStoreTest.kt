package com.cuboidestudio.orionvault.storage.secure

import com.cuboidestudio.orionvault.crypto.KdfParamsV1
import com.cuboidestudio.orionvault.crypto.SecretKeyGenerator
import com.cuboidestudio.orionvault.crypto.VaultCanary
import com.cuboidestudio.orionvault.crypto.ensureLibsodiumInitialized
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmSecureCredentialStoreTest {

    @Test
    fun saveThenLoadReturnsSameSecrets() = runTest {
        ensureLibsodiumInitialized()
        val store = JvmSecureCredentialStore()
        store.clear()

        val secretKey = SecretKeyGenerator.generate()
        val params = KdfParamsV1.newParams()
        val canary = VaultCanary.create(secretKey + secretKey) // apenas para ter 32 bytes de chave neste teste
        val secrets = StoredVaultSecrets(secretKey, params, canary)

        store.saveVaultSecrets(secrets)
        val loaded = store.loadVaultSecrets()

        checkNotNull(loaded)
        assertContentEquals(secrets.secretKey, loaded.secretKey)
        assertEquals(secrets.kdfParams, loaded.kdfParams)
        assertEquals(secrets.canary, loaded.canary)

        store.clear()
        assertNull(store.loadVaultSecrets())
    }
}
