package com.cuboidestudio.orionvault.security

/** Por que a biometria/PIN do aparelho não pode ser usada como desbloqueio secundário agora. */
enum class BiometricAvailability { AVAILABLE, NO_HARDWARE, NOT_ENROLLED, UNAVAILABLE }

/**
 * Ponte para o bloqueio de tela do aparelho (digital, rosto, PIN, padrão ou senha — qualquer método
 * já configurado pelo usuário) usado como desbloqueio secundário do cofre.
 *
 * A chave do cofre nunca é derivada aqui: [enroll] apenas cifra uma cópia da chave já derivada por
 * senha (design doc do cofre) usando uma chave do Keystore/Keychain vinculada à autenticação do
 * aparelho, e [promptUnlock] a decifra de volta após reautenticação bem-sucedida.
 */
interface BiometricAuthenticator {
    fun availability(): BiometricAvailability
    fun isAvailable(): Boolean = availability() == BiometricAvailability.AVAILABLE

    /** Mostra o prompt do sistema, cifra [vaultKey] e persiste o resultado. Retorna false se cancelado/falhou. */
    suspend fun enroll(vaultKey: ByteArray): Boolean

    /** Mostra o prompt do sistema e decifra a chave do cofre salva; null se cancelado/indisponível/erro. */
    suspend fun promptUnlock(): ByteArray?

    /** Remove a chave protegida e o material salvo — usado ao desativar em Configurações de Segurança. */
    suspend fun disable()
}

/** Contexto opaco necessário por algumas plataformas (ex.: a `FragmentActivity` no Android) para exibir o prompt do sistema. */
expect class PlatformBiometricContext

expect fun createBiometricAuthenticator(
    context: PlatformBiometricContext,
    secureStore: com.cuboidestudio.orionvault.storage.secure.SecureCredentialStore
): BiometricAuthenticator
