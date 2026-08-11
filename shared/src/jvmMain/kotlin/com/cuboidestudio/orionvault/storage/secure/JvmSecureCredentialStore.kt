package com.cuboidestudio.orionvault.storage.secure

import com.cuboidestudio.orionvault.crypto.CipherBlob
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

private val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("windows")

private fun secureStorageDir(): File {
    val dir = if (isWindows) {
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "OrionVault/secure")
    } else {
        File(System.getProperty("user.home"), ".orionvault/secure")
    }
    dir.mkdirs()
    return dir
}

private fun secretsFile(): File =
    File(secureStorageDir(), if (isWindows) "vault_secrets.dpapi" else "vault_secrets.enc")

private fun authSessionFile(): File =
    File(secureStorageDir(), if (isWindows) "auth_session.dpapi" else "auth_session.enc")

private fun biometricChoiceFile(): File =
    File(secureStorageDir(), if (isWindows) "biometric_choice.dpapi" else "biometric_choice.enc")

private fun biometricBlobFile(): File =
    File(secureStorageDir(), if (isWindows) "biometric_blob.dpapi" else "biometric_blob.enc")

private fun breachCheckFile(): File =
    File(secureStorageDir(), if (isWindows) "breach_check.dpapi" else "breach_check.enc")

/**
 * Implementação real para Windows via DPAPI (JNA `Crypt32Util`): os bytes só podem ser
 * descriptografados pelo mesmo usuário/máquina que os protegeu (design doc seção 5).
 *
 * TODO: em plataformas Desktop não-Windows (Linux/macOS), cai em um fallback apenas com
 * permissões de arquivo restritas ao dono, sem cifragem adicional do blob — endurecer
 * (ex.: libsecret/gnome-keyring no Linux, Keychain no macOS) antes de uso em produção
 * fora do Windows (ver seção 11, item 7 do design doc).
 */
class JvmSecureCredentialStore : SecureCredentialStore {
    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) {
        writeProtected(secretsFile(), VaultSecretsSerializer.serialize(secrets))
    }

    override suspend fun loadVaultSecrets(): StoredVaultSecrets? =
        readProtected(secretsFile())?.let { VaultSecretsSerializer.deserialize(it) }

    override suspend fun clear() {
        secretsFile().delete()
    }

    override suspend fun saveAuthSession(session: StoredAuthSession) {
        writeProtected(authSessionFile(), AuthSessionSerializer.serialize(session))
    }

    override suspend fun loadAuthSession(): StoredAuthSession? =
        readProtected(authSessionFile())?.let { AuthSessionSerializer.deserialize(it) }

    override suspend fun clearAuthSession() {
        authSessionFile().delete()
    }

    // Nunca acionados na prática: BiometricAuthenticator.isAvailable() é sempre falso no JVM/Desktop
    // (sem hardware biométrico de SO equivalente nesta plataforma).
    override suspend fun saveBiometricChoice(choice: BiometricUnlockChoice) {
        writeProtected(biometricChoiceFile(), choice.name)
    }

    override suspend fun loadBiometricChoice(): BiometricUnlockChoice =
        readProtected(biometricChoiceFile())?.let {
            runCatching { BiometricUnlockChoice.valueOf(it) }.getOrNull()
        } ?: BiometricUnlockChoice.UNDECIDED

    override suspend fun saveBiometricKeystoreBlob(blob: CipherBlob) {
        writeProtected(biometricBlobFile(), blob.toStorageString())
    }

    override suspend fun loadBiometricKeystoreBlob(): CipherBlob? =
        readProtected(biometricBlobFile())?.let { runCatching { CipherBlob.fromStorageString(it) }.getOrNull() }

    override suspend fun clearBiometricKeystoreBlob() {
        biometricBlobFile().delete()
    }

    override suspend fun saveBreachCheckEnabled(enabled: Boolean) {
        writeProtected(breachCheckFile(), enabled.toString())
    }

    override suspend fun loadBreachCheckEnabled(): Boolean =
        readProtected(breachCheckFile())?.toBooleanStrictOrNull() ?: false

    private fun writeProtected(file: File, content: String) {
        val serialized = content.toByteArray(StandardCharsets.UTF_8)
        val protected = if (isWindows) Crypt32Util.cryptProtectData(serialized) else serialized
        Files.write(file.toPath(), protected)
        if (!isWindows) {
            try {
                Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
            } catch (e: UnsupportedOperationException) {
                // Sistema de arquivos sem suporte a permissões POSIX (ex.: FAT) - segue sem restringir.
            }
        }
    }

    private fun readProtected(file: File): String? {
        if (!file.exists()) return null
        val stored = Files.readAllBytes(file.toPath())
        val plain = if (isWindows) Crypt32Util.cryptUnprotectData(stored) else stored
        return String(plain, StandardCharsets.UTF_8)
    }
}
