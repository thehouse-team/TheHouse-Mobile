package com.thehouse.app.data.auth.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.thehouse.app.data.preferences.PreferencesStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Almacén del refresh token cifrado.
 *
 * - Cifrado AES-GCM (IV aleatorio de 12 bytes precediendo al ciphertext).
 * - Clave AES-256 generada dentro de Android Keystore, no exportable.
 * - Persistencia en DataStore como Base64 (bloque de datos excluido de backup
 *   por `backup_rules.xml`/`data_extraction_rules.xml`).
 * - Implementación directa con Keystore: NO se usa `EncryptedSharedPreferences`
 *   (decisión de la spec NV-04) y no se agrega ninguna librería adicional.
 *
 * Si el Keystore no puede descifrar (hardware roto/app restaurada), se devuelve
 * null: la sesión local se considera inválida y la UI pedirá login de nuevo.
 */
class EncryptedRefreshTokenStorage(
    private val preferences: PreferencesStore,
) : SessionTokenStorage {

    override suspend fun readRefreshToken(): ByteArray? {
        val encoded = preferences.observeString(KEY_REFRESH_TOKEN).first() ?: return null
        if (encoded.isBlank()) return null
        return runCatching { decrypt(decode(encoded)) }.getOrNull()
    }

    override suspend fun writeRefreshToken(value: ByteArray) {
        val encrypted = encrypt(value)
        preferences.putString(KEY_REFRESH_TOKEN, encode(encrypted))
    }

    override suspend fun clear() {
        preferences.remove(KEY_REFRESH_TOKEN)
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return iv + ciphertext
    }

    private fun decrypt(data: ByteArray): ByteArray {
        if (data.size <= IV_SIZE) {
            throw IllegalArgumentException("Invalid token payload")
        }
        val iv = data.copyOfRange(0, IV_SIZE)
        val ciphertext = data.copyOfRange(IV_SIZE, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore
            .getInstance(ANDROID_KEYSTORE)
            .apply {
                @Suppress("UNCHECKED_CAST")
                load(null as java.io.InputStream?, null as CharArray?)
            }
        (keyStore.getEntry(KEY_ALIAS, null as KeyStore.ProtectionParameter?)
            as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(value: ByteArray): String =
        Base64.encodeToString(value, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "thehouse-refresh-token-key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE = 256
        const val IV_SIZE = 12
        const val TAG_BITS = 128
        const val KEY_REFRESH_TOKEN = "auth_refresh_token_encrypted"
    }
}
