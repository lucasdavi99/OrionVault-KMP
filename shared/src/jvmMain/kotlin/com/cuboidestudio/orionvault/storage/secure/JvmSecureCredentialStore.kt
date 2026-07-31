package com.cuboidestudio.orionvault.storage.secure

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
        val serialized = VaultSecretsSerializer.serialize(secrets).toByteArray(StandardCharsets.UTF_8)
        val protected = if (isWindows) Crypt32Util.cryptProtectData(serialized) else serialized
        val file = secretsFile()
        Files.write(file.toPath(), protected)
        if (!isWindows) {
            try {
                Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------"))
            } catch (e: UnsupportedOperationException) {
                // Sistema de arquivos sem suporte a permissões POSIX (ex.: FAT) - segue sem restringir.
            }
        }
    }

    override suspend fun loadVaultSecrets(): StoredVaultSecrets? {
        val file = secretsFile()
        if (!file.exists()) return null
        val stored = Files.readAllBytes(file.toPath())
        val plain = if (isWindows) Crypt32Util.cryptUnprotectData(stored) else stored
        return VaultSecretsSerializer.deserialize(String(plain, StandardCharsets.UTF_8))
    }

    override suspend fun clear() {
        secretsFile().delete()
    }
}
