package com.cuboidestudio.orionvault.storage.secure

import com.cuboidestudio.orionvault.crypto.CipherBlob
import platform.Foundation.NSUserDefaults

/**
 * TODO: placeholder usando `NSUserDefaults` — migrar para Keychain antes de qualquer uso
 * em produção (ver seção 11, item 7 e seção 12.1 do design doc).
 */
class IosSecureCredentialStore : SecureCredentialStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) {
        defaults.setObject(VaultSecretsSerializer.serialize(secrets), KEY)
    }

    override suspend fun loadVaultSecrets(): StoredVaultSecrets? {
        val raw = defaults.stringForKey(KEY) ?: return null
        return VaultSecretsSerializer.deserialize(raw)
    }

    override suspend fun clear() {
        defaults.removeObjectForKey(KEY)
    }

    override suspend fun saveAuthSession(session: StoredAuthSession) {
        defaults.setObject(AuthSessionSerializer.serialize(session), AUTH_KEY)
    }

    override suspend fun loadAuthSession(): StoredAuthSession? {
        val raw = defaults.stringForKey(AUTH_KEY) ?: return null
        return AuthSessionSerializer.deserialize(raw)
    }

    override suspend fun clearAuthSession() {
        defaults.removeObjectForKey(AUTH_KEY)
    }

    // Nunca acionados na prática: BiometricAuthenticator.isAvailable() é sempre falso no iOS
    // (TODO: LocalAuthentication/Face ID/Touch ID, fora do escopo desta rodada).
    override suspend fun saveBiometricChoice(choice: BiometricUnlockChoice) {
        defaults.setObject(choice.name, BIOMETRIC_CHOICE_KEY)
    }

    override suspend fun loadBiometricChoice(): BiometricUnlockChoice =
        defaults.stringForKey(BIOMETRIC_CHOICE_KEY)?.let {
            runCatching { BiometricUnlockChoice.valueOf(it) }.getOrNull()
        } ?: BiometricUnlockChoice.UNDECIDED

    override suspend fun saveBiometricKeystoreBlob(blob: CipherBlob) {
        defaults.setObject(blob.toStorageString(), BIOMETRIC_BLOB_KEY)
    }

    override suspend fun loadBiometricKeystoreBlob(): CipherBlob? {
        val raw = defaults.stringForKey(BIOMETRIC_BLOB_KEY) ?: return null
        return runCatching { CipherBlob.fromStorageString(raw) }.getOrNull()
    }

    override suspend fun clearBiometricKeystoreBlob() {
        defaults.removeObjectForKey(BIOMETRIC_BLOB_KEY)
    }

    companion object {
        private const val KEY = "vault_secrets"
        private const val AUTH_KEY = "cloud_auth_session"
        private const val BIOMETRIC_CHOICE_KEY = "biometric_unlock_choice"
        private const val BIOMETRIC_BLOB_KEY = "biometric_unlock_blob"
    }
}
