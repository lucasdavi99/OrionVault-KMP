package com.cuboidestudio.orionvault.security

import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore

/**
 * TODO: sem implementação real ainda — integrar com `LocalAuthentication`/`LAContext`
 * (Face ID/Touch ID/senha do dispositivo) numa rodada futura. Por ora sempre indisponível,
 * então o convite de ativação nunca aparece e a tela de Segurança mostra "Indisponível".
 */
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
