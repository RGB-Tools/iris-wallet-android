package com.iriswallet.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_ACTIONS_CONFIGURED
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_LOGIN_CONFIGURED
import com.iriswallet.utils.AppAuthenticationService
import com.iriswallet.utils.AppAuthenticationServiceListener
import com.iriswallet.utils.TAG

class SettingsFragment :
    PreferenceBaseFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    AppAuthenticationServiceListener {

    private lateinit var appAuthenticationService: AppAuthenticationService

    private var sharedPreferences: SharedPreferences? = null

    private var handlingPinError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appAuthenticationService = AppAuthenticationService(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (findPreference<Preference>(key.toString())) {
            is SwitchPreferenceCompat -> {
                if (key in listOf(PREFS_PIN_ACTIONS_CONFIGURED, PREFS_PIN_LOGIN_CONFIGURED)) {
                    if (handlingPinError) handlingPinError = false
                    else appAuthenticationService.auth(key!!)
                }
            }
        }
    }

    override fun authenticated(requestCode: String) {
        Log.d(TAG, "Successfully changed PIN $requestCode preference")
    }

    override fun handleAuthError(requestCode: String, errorExtraInfo: String?, errCode: Int?) {
        handlingPinError = true
        val preference = findPreference<Preference>(requestCode) as SwitchPreferenceCompat

        when (errCode) {
            AppAuthenticationService.USER_DISABLED_AUTH -> {
                preference.isChecked = false
                val otherPref =
                    findPreference<Preference>(
                        if (requestCode == PREFS_PIN_LOGIN_CONFIGURED) PREFS_PIN_ACTIONS_CONFIGURED
                        else PREFS_PIN_LOGIN_CONFIGURED
                    )
                        as SwitchPreferenceCompat
                otherPref.isChecked = false
                handlingPinError = false
                Toast.makeText(context, R.string.no_auth_available, Toast.LENGTH_LONG).show()
            }
            AppAuthenticationService.FAILED_AUTH -> handlingPinError = false
            else -> {
                toastError(R.string.err_configuring_app_auth, errorExtraInfo)
                preference.isChecked = !preference.isChecked
            }
        }
    }
}
