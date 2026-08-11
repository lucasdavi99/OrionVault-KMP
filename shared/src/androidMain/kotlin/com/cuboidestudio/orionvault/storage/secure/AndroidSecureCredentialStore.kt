package com.cuboidestudio.orionvault.storage.secure

import android.content.Context
import com.cuboidestudio.orionvault.crypto.CipherBlob

/**
 * TODO: placeholder usando `SharedPreferences` comum — migrar para Android Keystore
 * (ex.: `EncryptedSharedPreferences` ou Tink) antes de qualquer uso em produção
 * (ver seção 11, item 7 e seção 12.1 do design doc).
 */
class AndroidSecureCredentialStore(private val context: Context) : SecureCredentialStore {
    private val prefs by lazy { context.getSharedPreferences("orionvault_secure", Context.MODE_PRIVATE) }

    override suspend fun saveVaultSecrets(secrets: StoredVaultSecrets) {
        prefs.edit().putString(KEY, VaultSecretsSerializer.serialize(secrets)).apply()
    }

    override suspend fun loadVaultSecrets(): StoredVaultSecrets? {
        val raw = prefs.getString(KEY, null) ?: return null
        return VaultSecretsSerializer.deserialize(raw)
    }

    override suspend fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    override suspend fun saveAuthSession(session: StoredAuthSession) {
        prefs.edit().putString(AUTH_KEY, AuthSessionSerializer.serialize(session)).apply()
    }

    override suspend fun loadAuthSession(): StoredAuthSession? {
        val raw = prefs.getString(AUTH_KEY, null) ?: return null
        return AuthSessionSerializer.deserialize(raw)
    }

    override suspend fun clearAuthSession() {
        prefs.edit().remove(AUTH_KEY).apply()
    }

    override suspend fun saveBiometricChoice(choice: BiometricUnlockChoice) {
        prefs.edit().putString(BIOMETRIC_CHOICE_KEY, choice.name).apply()
    }

    override suspend fun loadBiometricChoice(): BiometricUnlockChoice =
        prefs.getString(BIOMETRIC_CHOICE_KEY, null)?.let {
            runCatching { BiometricUnlockChoice.valueOf(it) }.getOrNull()
        } ?: BiometricUnlockChoice.UNDECIDED

    override suspend fun saveBiometricKeystoreBlob(blob: CipherBlob) {
        prefs.edit().putString(BIOMETRIC_BLOB_KEY, blob.toStorageString()).apply()
    }

    override suspend fun loadBiometricKeystoreBlob(): CipherBlob? {
        val raw = prefs.getString(BIOMETRIC_BLOB_KEY, null) ?: return null
        return runCatching { CipherBlob.fromStorageString(raw) }.getOrNull()
    }

    override suspend fun clearBiometricKeystoreBlob() {
        prefs.edit().remove(BIOMETRIC_BLOB_KEY).apply()
    }

    override suspend fun saveBreachCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(BREACH_CHECK_KEY, enabled).apply()
    }

    override suspend fun loadBreachCheckEnabled(): Boolean =
        prefs.getBoolean(BREACH_CHECK_KEY, false)

    companion object {
        private const val KEY = "vault_secrets"
        private const val AUTH_KEY = "cloud_auth_session"
        private const val BIOMETRIC_CHOICE_KEY = "biometric_unlock_choice"
        private const val BIOMETRIC_BLOB_KEY = "biometric_unlock_blob"
        private const val BREACH_CHECK_KEY = "breach_check_enabled"
    }
}
