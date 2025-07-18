package com.iriswallet.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer

object SharedPreferencesManager {

    private const val PREFS_MNEMONIC_IV = "mnemonic_iv"
    private const val PREFS_ENCRYPTED_MNEMONIC = "encrypted_mnemonic"
    private const val PREFS_TERMS_ACCEPTED = "terms_accepted"
    const val PREFS_PIN_ACTIONS_CONFIGURED = "pin_actions_configured"
    const val PREFS_PIN_LOGIN_CONFIGURED = "pin_login_configured"
    const val PREFS_ELECTRUM_URL = "electrum_url_pref"
    const val PREFS_PROXY_CONSIGNMENT_ENDPOINT = "proxy_consignment_endpoint_pref"
    const val PREFS_SHOW_HIDDEN_ASSETS = "show_hidden_assets"
    const val PREFS_HIDE_EXHAUSTED_ASSETS = "hide_exhausted_assets"
    const val PREFS_FEE_RATE = "fee_rate"
    private const val PREFS_BACKUP_GOOGLE_ACCOUNT = "backup_google_account"
    private const val PREFS_BACKUP_LAST_TIME = "backup_last_time"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsSharedPreferences: SharedPreferences

    fun initObject(
        sharedPreferences: SharedPreferences,
        settingsSharedPreferences: SharedPreferences,
    ) {
        this.sharedPreferences = sharedPreferences
        this.settingsSharedPreferences = settingsSharedPreferences
    }

    fun clearAll() {
        sharedPreferences.edit { clear() }
        settingsSharedPreferences.edit { clear() }
    }

    var backupGoogleAccount: String?
        get() = sharedPreferences.getString(PREFS_BACKUP_GOOGLE_ACCOUNT, null)
        set(value) {
            sharedPreferences.edit { putString(PREFS_BACKUP_GOOGLE_ACCOUNT, value)?.apply() }
        }

    var backupLastTime: Long
        get() = sharedPreferences.getLong(PREFS_BACKUP_LAST_TIME, 0)
        set(value) {
            sharedPreferences.edit { putLong(PREFS_BACKUP_LAST_TIME, value)?.apply() }
        }

    var electrumURL: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_ELECTRUM_URL,
                AppContainer.electrumURLDefault,
            )!!
        set(value) {
            settingsSharedPreferences.edit { putString(PREFS_ELECTRUM_URL, value)?.apply() }
        }

    var feeRate: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_FEE_RATE,
                AppConstants.DEFAULT_FEE_RATE.toString(),
            )!!
        set(value) {
            settingsSharedPreferences.edit { putString(PREFS_FEE_RATE, value)?.apply() }
        }

    var mnemonicIv: String?
        get() = sharedPreferences.getString(PREFS_MNEMONIC_IV, "")
        set(value) {
            sharedPreferences.edit { putString(PREFS_MNEMONIC_IV, value)?.apply() }
        }

    var encryptedMnemonic: String?
        get() = sharedPreferences.getString(PREFS_ENCRYPTED_MNEMONIC, "")
        set(value) {
            sharedPreferences.edit { putString(PREFS_ENCRYPTED_MNEMONIC, value)?.apply() }
        }

    var termsAccepted: Boolean
        get() = sharedPreferences.getBoolean(PREFS_TERMS_ACCEPTED, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREFS_TERMS_ACCEPTED, value)?.apply() }
        }

    var pinActionsConfigured: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_PIN_ACTIONS_CONFIGURED, false)
        set(value) {
            settingsSharedPreferences.edit {
                putBoolean(PREFS_PIN_ACTIONS_CONFIGURED, value)?.apply()
            }
        }

    var pinLoginConfigured: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_PIN_LOGIN_CONFIGURED, false)
        set(value) {
            settingsSharedPreferences.edit {
                putBoolean(PREFS_PIN_LOGIN_CONFIGURED, value)?.apply()
            }
        }

    var proxyTransportEndpoint: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_PROXY_CONSIGNMENT_ENDPOINT,
                AppContainer.proxyTransportEndpointDefault,
            )!!
        set(value) {
            settingsSharedPreferences.edit {
                putString(PREFS_PROXY_CONSIGNMENT_ENDPOINT, value)?.apply()
            }
        }

    var showHiddenAssets: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_SHOW_HIDDEN_ASSETS, false)
        set(value) {
            settingsSharedPreferences.edit { putBoolean(PREFS_SHOW_HIDDEN_ASSETS, value)?.apply() }
        }

    var hideExhaustedAssets: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_HIDE_EXHAUSTED_ASSETS, false)
        set(value) {
            settingsSharedPreferences.edit {
                putBoolean(PREFS_HIDE_EXHAUSTED_ASSETS, value)?.apply()
            }
        }
}
