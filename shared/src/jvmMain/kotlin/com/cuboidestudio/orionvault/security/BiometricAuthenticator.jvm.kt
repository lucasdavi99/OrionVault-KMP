package com.cuboidestudio.orionvault.security

import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore

/** Desktop/JVM não tem um equivalente de biometria/PIN de SO integrável aqui — sempre indisponível. */
actual class PlatformBiometricContext

actual fun createBiometricAuthenticator(
    context: PlatformBiometricContext,
    secureStore: SecureCredentialStore
): BiometricAuthenticator = object : BiometricAuthenticator {
    override fun availability(): BiometricAvailability = BiometricAvailability.UNAVAILABLE
    override suspend fun enroll(vaultKey: ByteArray): Boolean = false
    override suspend fun promptUnlock(): ByteArray? = null
    override suspend fun disable() {}
}
