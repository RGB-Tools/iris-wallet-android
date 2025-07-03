package com.iriswallet.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_ACTIONS_CONFIGURED

interface AppAuthenticationServiceListener {
    fun authenticated(requestCode: String = PREFS_PIN_ACTIONS_CONFIGURED)

    fun handleAuthError(requestCode: String, errorExtraInfo: String? = null, errCode: Int? = null)
}

class AppAuthenticationService(private val fragment: Fragment) {
    private lateinit var appAuthenticationServiceListener: AppAuthenticationServiceListener
    private lateinit var biometricManager: BiometricManager

    fun auth(requestCode: String = PREFS_PIN_ACTIONS_CONFIGURED) {
        appAuthenticationServiceListener = fragment as AppAuthenticationServiceListener
        val executor = ContextCompat.getMainExecutor(fragment.requireActivity().applicationContext)
        val biometricPrompt =
            BiometricPrompt(
                fragment,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        appAuthenticationServiceListener.handleAuthError(
                            requestCode,
                            errString.toString().replaceFirstChar(Char::lowercase),
                            errorCode,
                        )
                    }

                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(result)
                        appAuthenticationServiceListener.authenticated(requestCode)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        appAuthenticationServiceListener.handleAuthError(
                            requestCode,
                            fragment
                                .getString(R.string.auth_failed)
                                .replaceFirstChar(Char::lowercase),
                            FAILED_AUTH,
                        )
                    }
                },
            )
        biometricManager = BiometricManager.from(fragment.requireActivity().applicationContext)

        val canUseBiometric = canUseBiometric()
        AppContainer.canUseBiometric = canUseBiometric
        Log.d(TAG, "SDK version ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Can use biometric: $canUseBiometric")
        if (!canUseBiometric) {
            val keyguardManager =
                fragment
                    .requireActivity()
                    .applicationContext
                    .getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            Log.d(TAG, "Device secure: ${keyguardManager.isDeviceSecure}")
            if (!keyguardManager.isDeviceSecure) {
                appAuthenticationServiceListener.handleAuthError(
                    requestCode,
                    fragment.getString(R.string.auth_failed).replaceFirstChar(Char::lowercase),
                    USER_DISABLED_AUTH,
                )
                return
            }
        }

        val basePromptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(fragment.getString(R.string.authentication_prompt_title))
                .setSubtitle(fragment.getString(R.string.authentication_prompt_subtitle))
        val promptInfo =
            if (canUseBiometric) {
                basePromptInfo.setNegativeButtonText(fragment.getString(R.string.cancel)).build()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    basePromptInfo.setAllowedAuthenticators(DEVICE_CREDENTIAL).build()
                else basePromptInfo.setDeviceCredentialAllowed(true).build()
            }
        biometricPrompt.authenticate(promptInfo)
    }

    private fun canUseBiometric(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        } else {
            biometricManager.canAuthenticate()
        } == BiometricManager.BIOMETRIC_SUCCESS
    }

    companion object {
        internal const val USER_DISABLED_AUTH = 666
        internal const val FAILED_AUTH = 777
    }
}
