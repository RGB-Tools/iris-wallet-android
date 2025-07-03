package com.iriswallet.data

import android.content.SharedPreferences
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer

object SharedPreferencesManager {

    private const val PREFS_MNEMONIC = "mnemonic"
    const val PREFS_PIN_ACTIONS_CONFIGURED = "pin_actions_configured"
    const val PREFS_PIN_LOGIN_CONFIGURED = "pin_login_configured"
    const val PREFS_ELECTRUM_URL = "electrum_url_pref"
    const val PREFS_PROXY_CONSIGNMENT_ENDPOINT = "proxy_consignment_endpoint_pref"
    private const val OLD_PREFS_PROXY_URL = "proxy_url_pref"
    const val PREFS_SHOW_HIDDEN_ASSETS = "show_hidden_assets"
    const val PREFS_HIDE_EXHAUSTED_ASSETS = "hide_exhausted_assets"
    const val PREFS_FEE_RATE = "fee_rate"
    private const val PREFS_BACKUP_CONFIGURED = "backup_configured"
    private const val PREFS_BACKUP_REQUIRED = "backup_required"
    private const val PREFS_UPDATED_TO_RGB_0_10 = "updated_to_rgb_0_10"
    private const val PREFS_RECOVERED_FUNDS = "recovered_funds"

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var encryptedSharedPreferences: SharedPreferences
    private lateinit var settingsSharedPreferences: SharedPreferences

    fun initObject(
        sharedPreferences: SharedPreferences,
        encryptedSharedPreferences: SharedPreferences,
        settingsSharedPreferences: SharedPreferences,
    ) {
        this.sharedPreferences = sharedPreferences
        this.encryptedSharedPreferences = encryptedSharedPreferences
        this.settingsSharedPreferences = settingsSharedPreferences
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        encryptedSharedPreferences.edit().clear().apply()
        settingsSharedPreferences.edit().clear().apply()
    }

    fun removeOldKeys() {
        settingsSharedPreferences.edit().remove(OLD_PREFS_PROXY_URL).apply()
    }

    var backupConfigured: Boolean
        get() = sharedPreferences.getBoolean(PREFS_BACKUP_CONFIGURED, false)
        set(value) {
            sharedPreferences.edit()?.putBoolean(PREFS_BACKUP_CONFIGURED, value)?.apply()
        }

    var electrumURL: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_ELECTRUM_URL,
                AppContainer.electrumURLDefault,
            )!!
        set(value) {
            settingsSharedPreferences.edit()?.putString(PREFS_ELECTRUM_URL, value)?.apply()
        }

    var feeRate: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_FEE_RATE,
                AppConstants.defaultFeeRate.toString(),
            )!!
        set(value) {
            settingsSharedPreferences.edit()?.putString(PREFS_FEE_RATE, value)?.apply()
        }

    var mnemonic: String
        get() = encryptedSharedPreferences.getString(PREFS_MNEMONIC, "") ?: ""
        set(value) {
            encryptedSharedPreferences.edit()?.putString(PREFS_MNEMONIC, value)?.apply()
        }

    var pinActionsConfigured: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_PIN_ACTIONS_CONFIGURED, false)
        set(value) {
            settingsSharedPreferences
                .edit()
                ?.putBoolean(PREFS_PIN_ACTIONS_CONFIGURED, value)
                ?.apply()
        }

    var pinLoginConfigured: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_PIN_LOGIN_CONFIGURED, false)
        set(value) {
            settingsSharedPreferences.edit()?.putBoolean(PREFS_PIN_LOGIN_CONFIGURED, value)?.apply()
        }

    var proxyTransportEndpoint: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_PROXY_CONSIGNMENT_ENDPOINT,
                AppContainer.proxyTransportEndpointDefault,
            )!!
        set(value) {
            settingsSharedPreferences
                .edit()
                ?.putString(PREFS_PROXY_CONSIGNMENT_ENDPOINT, value)
                ?.apply()
        }

    var showHiddenAssets: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_SHOW_HIDDEN_ASSETS, false)
        set(value) {
            settingsSharedPreferences.edit()?.putBoolean(PREFS_SHOW_HIDDEN_ASSETS, value)?.apply()
        }

    var hideExhaustedAssets: Boolean
        get() = settingsSharedPreferences.getBoolean(PREFS_HIDE_EXHAUSTED_ASSETS, false)
        set(value) {
            settingsSharedPreferences
                .edit()
                ?.putBoolean(PREFS_HIDE_EXHAUSTED_ASSETS, value)
                ?.apply()
        }

    var updatedToRgb010: Boolean
        get() = sharedPreferences.getBoolean(PREFS_UPDATED_TO_RGB_0_10, false)
        set(value) {
            sharedPreferences.edit()?.putBoolean(PREFS_UPDATED_TO_RGB_0_10, value)?.apply()
        }

    var recoveredFunds: Boolean
        get() = sharedPreferences.getBoolean(PREFS_RECOVERED_FUNDS, false)
        set(value) {
            sharedPreferences.edit()?.putBoolean(PREFS_RECOVERED_FUNDS, value)?.apply()
        }
}
