package org.iriswallet.data

import android.content.SharedPreferences

object SharedPreferencesManager {

    private const val PREFS_MNEMONIC = "mnemonic"
    const val PREFS_PIN_ACTIONS_CONFIGURED = "pin_actions_configured"
    const val PREFS_PIN_LOGIN_CONFIGURED = "pin_login_configured"

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
}
