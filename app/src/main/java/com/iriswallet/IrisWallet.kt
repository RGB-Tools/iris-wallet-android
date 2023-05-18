package com.iriswallet

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer

class IrisWallet : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.initObject(applicationContext)
        SharedPreferencesManager.initObject(
            getSharedPreferences(AppConstants.sharedPreferencesName, Context.MODE_PRIVATE),
            EncryptedSharedPreferences.create(
                this,
                AppConstants.encryptedSharedPreferencesName,
                MasterKey.Builder(this, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ),
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        AppContainer.storedMnemonic = SharedPreferencesManager.mnemonic
    }
}
