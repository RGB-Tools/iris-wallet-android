package com.iriswallet

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.LogHelper
import com.iriswallet.utils.MnemonicCryptoUtils

class IrisWallet : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContainer.initObject(applicationContext)
        SharedPreferencesManager.initObject(
            getSharedPreferences(AppConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE),
            PreferenceManager.getDefaultSharedPreferences(this),
        )
        AppContainer.storedMnemonic = MnemonicCryptoUtils.decryptMnemonic().orEmpty()
        LogHelper.truncateLogFile()
    }
}
