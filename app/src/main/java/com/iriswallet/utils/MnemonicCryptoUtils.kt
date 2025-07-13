package com.iriswallet.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.iriswallet.data.SharedPreferencesManager
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object MnemonicCryptoUtils {
    private const val KEY_ALIAS = "mnemonic_aes_key"
    private const val KEY_SIZE = 256
    private const val KEY_STORE_TYPE = "AndroidKeyStore"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEY_STORE_TYPE).apply { load(null) }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
            ?: run {
                val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_STORE_TYPE)
                val keySpec =
                    KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(KEY_SIZE)
                        .build()
                keyGenerator.init(keySpec)
                keyGenerator.generateKey()
            }
    }

    fun encryptAndStoreMnemonic(mnemonic: String) {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(mnemonic.toByteArray(Charsets.UTF_8))
        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        SharedPreferencesManager.mnemonicIv = ivBase64
        SharedPreferencesManager.encryptedMnemonic = encryptedBase64
    }

    fun decryptMnemonic(): String? {
        val encryptedMnemonic = SharedPreferencesManager.encryptedMnemonic
        val ivBase64 = SharedPreferencesManager.mnemonicIv
        if (encryptedMnemonic.isNullOrBlank() || ivBase64.isNullOrBlank()) return null
        val encryptedBytes = Base64.decode(encryptedMnemonic, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
