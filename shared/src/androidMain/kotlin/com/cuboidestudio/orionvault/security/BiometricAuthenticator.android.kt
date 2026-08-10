package com.cuboidestudio.orionvault.security

import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.cuboidestudio.orionvault.storage.secure.BiometricUnlockChoice
import com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
private const val GCM_TAG_LENGTH_BITS = 128

actual class PlatformBiometricContext(val activity: FragmentActivity)

actual fun createBiometricAuthenticator(
    context: PlatformBiometricContext,
    secureStore: SecureCredentialStore
): BiometricAuthenticator = AndroidBiometricAuthenticator(context.activity, secureStore)

private class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity,
    private val secureStore: SecureCredentialStore
) : BiometricAuthenticator {

    override fun availability(): BiometricAvailability {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    override suspend fun enroll(vaultKey: ByteArray): Boolean {
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, BiometricKeystoreKey.getOrCreate())
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            resetInvalidatedState()
            return false
        }

        val authedCipher = showPrompt(cipher, "Ativar desbloqueio biométrico") ?: return false
        val ciphertext = authedCipher.doFinal(vaultKey)
        val blob = CipherBlob(authedCipher.iv, ciphertext)
        secureStore.saveBiometricKeystoreBlob(blob)
        secureStore.saveBiometricChoice(BiometricUnlockChoice.ENABLED)
        return true
    }

    override suspend fun promptUnlock(): ByteArray? {
        val blob = secureStore.loadBiometricKeystoreBlob() ?: return null
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, BiometricKeystoreKey.getOrCreate(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, blob.nonce))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            resetInvalidatedState()
            return null
        }

        val authedCipher = showPrompt(cipher, "Desbloquear cofre") ?: return null
        return try {
            authedCipher.doFinal(blob.ciphertext)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun disable() {
        BiometricKeystoreKey.delete()
        secureStore.clearBiometricKeystoreBlob()
    }

    /** Recadastro de biometria mudou desde que a chave foi criada: descarta tudo e pede reconfirmação. */
    private suspend fun resetInvalidatedState() {
        BiometricKeystoreKey.delete()
        secureStore.clearBiometricKeystoreBlob()
        secureStore.saveBiometricChoice(BiometricUnlockChoice.UNDECIDED)
    }

    private suspend fun showPrompt(cipher: Cipher, title: String): Cipher? =
        suspendCancellableCoroutine { continuation ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(result.cryptoObject?.cipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onAuthenticationFailed() {
                    // Tentativa individual falhou (ex.: dedo errado) — o prompt do sistema continua
                    // aberto e deixa o usuário tentar de novo, então não resolve a continuation aqui.
                }
            }

            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Confirme para continuar")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
}
