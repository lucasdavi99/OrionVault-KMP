package com.cuboidestudio.orionvault.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "orionvault_biometric_unlock_key"

/**
 * Chave AES do Android Keystore vinculada à autenticação do aparelho: só pode ser usada logo após
 * uma confirmação bem-sucedida de biometria OU do bloqueio de tela (PIN/padrão/senha), qualquer um
 * que o usuário tenha configurado. Nunca sai do hardware seguro do aparelho.
 */
internal object BiometricKeystoreKey {
    fun getOrCreate(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            // Invalida a chave se o usuário recadastrar biometria — força reconfirmar a Master
            // Password antes de reativar (ver tratamento de KeyPermanentlyInvalidatedException).
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 0 = precisa autenticar a cada uso (sem janela de tolerância). BIOMETRIC_STRONG or
            // DEVICE_CREDENTIAL cobre digital/rosto classe 3 OU PIN/padrão/senha — qualquer método
            // de bloqueio que o aparelho já tenha configurado.
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    /** Remove a chave do Keystore — usado ao desativar o desbloqueio biométrico ou após invalidação. */
    fun delete() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }
}
