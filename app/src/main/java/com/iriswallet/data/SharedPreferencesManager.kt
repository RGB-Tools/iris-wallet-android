package com.iriswallet.data

import android.content.SharedPreferences
import com.iriswallet.utils.AppContainer

object SharedPreferencesManager {

    private const val PREFS_MNEMONIC = "mnemonic"
    const val PREFS_PIN_ACTIONS_CONFIGURED = "pin_actions_configured"
    const val PREFS_PIN_LOGIN_CONFIGURED = "pin_login_configured"
    const val PREFS_ELECTRUM_URL = "electrum_url_pref"
    const val PREFS_PROXY_URL = "proxy_url_pref"

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

    var electrumURL: String
        get() =
            settingsSharedPreferences.getString(
                PREFS_ELECTRUM_URL,
                AppContainer.electrumURLDefault
            )!!
        set(value) {
            settingsSharedPreferences.edit()?.putString(PREFS_ELECTRUM_URL, value)?.apply()
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

    var proxyURL: String
        get() = settingsSharedPreferences.getString(PREFS_PROXY_URL, AppContainer.proxyURLDefault)!!
        set(value) {
            settingsSharedPreferences.edit()?.putString(PREFS_PROXY_URL, value)?.apply()
        }
}
