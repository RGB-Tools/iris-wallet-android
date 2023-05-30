package com.iriswallet.ui

import android.app.Dialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import androidx.preference.PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback
import com.google.android.material.textview.MaterialTextView
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.data.SharedPreferencesManager.PREFS_ELECTRUM_URL
import com.iriswallet.data.SharedPreferencesManager.PREFS_FEE_RATE
import com.iriswallet.data.SharedPreferencesManager.PREFS_HIDE_EXHAUSTED_ASSETS
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_ACTIONS_CONFIGURED
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_LOGIN_CONFIGURED
import com.iriswallet.data.SharedPreferencesManager.PREFS_PROXY_CONSIGNMENT_ENDPOINT
import com.iriswallet.data.SharedPreferencesManager.PREFS_SHOW_HIDDEN_ASSETS
import com.iriswallet.utils.*
import org.rgbtools.ConsignmentEndpoint
import org.rgbtools.ConsignmentTransport
import org.rgbtools.RgbLibException

class SettingsFragment :
    PreferenceBaseFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    AppAuthenticationServiceListener,
    OnPreferenceDisplayDialogCallback {

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

        // set EditTexts to shared pref values (otherwise a new wallet would see an empty ET)
        val electrumET = findPreference<EditTextPreference>(PREFS_ELECTRUM_URL)
        electrumET!!.text = AppContainer.electrumURL
        val proxyET = findPreference<EditTextPreference>(PREFS_PROXY_CONSIGNMENT_ENDPOINT)
        proxyET!!.text = AppContainer.proxyConsignmentEndpoint

        val showHiddenAssets = findPreference<SwitchPreferenceCompat>(PREFS_SHOW_HIDDEN_ASSETS)!!
        setHideExhaustedAssets(showHiddenAssets)

        val feeRatePref = findPreference<EditTextPreference>(PREFS_FEE_RATE)!!
        feeRatePref.text = SharedPreferencesManager.feeRate
        feeRatePref.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.selectAll()
            it.filters +=
                DecimalsInputFilter(
                    AppConstants.feeRateIntegerPlaces,
                    AppConstants.feeRateDecimalPlaces,
                    minValue = AppConstants.minFeeRate
                )
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceDisplayDialog(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        if (pref is EditTextPreference && pref.key == PREFS_PROXY_CONSIGNMENT_ENDPOINT) {
            val f = ConsignmentEndpointEditTextPreferenceFragment.newInstance(pref.key)
            // see https://issuetracker.google.com/issues/181793702
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
            return true
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (val preference = findPreference<Preference>(key.toString())) {
            is SwitchPreferenceCompat -> {
                when (key) {
                    PREFS_PIN_ACTIONS_CONFIGURED,
                    PREFS_PIN_LOGIN_CONFIGURED -> {
                        if (handlingPinError) handlingPinError = false
                        else appAuthenticationService.auth(key)
                    }
                    PREFS_SHOW_HIDDEN_ASSETS -> setHideExhaustedAssets(preference)
                }
            }
            is EditTextPreference -> {
                when (key) {
                    PREFS_ELECTRUM_URL -> {
                        preference.text =
                            preference.text!!.ifBlank { AppContainer.electrumURLDefault }
                    }
                    PREFS_PROXY_CONSIGNMENT_ENDPOINT -> {
                        preference.text =
                            preference.text!!.ifBlank {
                                AppContainer.proxyConsignmentEndpointDefault
                            }
                    }
                    PREFS_FEE_RATE -> {
                        SharedPreferencesManager.feeRate = preference.text!!
                    }
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
                Toast.makeText(activity, R.string.no_auth_available, Toast.LENGTH_LONG).show()
            }
            AppAuthenticationService.FAILED_AUTH -> handlingPinError = false
            else -> {
                toastError(R.string.err_configuring_app_auth, errorExtraInfo)
                preference.isChecked = !preference.isChecked
            }
        }
    }

    private fun setHideExhaustedAssets(showHiddenAssets: SwitchPreferenceCompat) {
        val hideExhaustedAssets =
            findPreference<Preference>(PREFS_HIDE_EXHAUSTED_ASSETS) as SwitchPreferenceCompat
        hideExhaustedAssets.isEnabled = !showHiddenAssets.isChecked
    }
}

class ConsignmentEndpointEditTextPreferenceFragment : EditTextPreferenceDialogFragmentCompat() {

    private lateinit var consignmentEndpointDialogMessageTV: MaterialTextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val consignmentEndpointET = dialog.findViewById<EditText>(android.R.id.edit)
                var err = false
                if (consignmentEndpointET?.text!!.isNotBlank()) {
                    try {
                        val consignmentEndpoint =
                            ConsignmentEndpoint(consignmentEndpointET.text.toString())
                        if (
                            consignmentEndpoint.protocol() !=
                                ConsignmentTransport.RGB_HTTP_JSON_RPC
                        )
                            err = true
                    } catch (e: RgbLibException) {
                        err = true
                    }
                }
                if (!err) {
                    val updatedEndpoint = consignmentEndpointET.text.toString()
                    if (preference.callChangeListener(updatedEndpoint))
                        (preference as EditTextPreference).text = updatedEndpoint
                    dialog.dismiss()
                } else {
                    val existingMessage = preference.dialogMessage.toString()
                    val updatedMessage =
                        "$existingMessage\n\n${getString(R.string.invalid_consignment_endpoint)}"
                    val spannable = SpannableString(updatedMessage)
                    spannable.setSpan(
                        ForegroundColorSpan(Color.RED),
                        existingMessage.length,
                        updatedMessage.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        existingMessage.length,
                        updatedMessage.length,
                        0
                    )
                    consignmentEndpointDialogMessageTV.text = spannable
                }
            }
        }
        return dialog
    }

    override fun onBindDialogView(view: View) {
        val dialogMessageView = view.findViewById<MaterialTextView>(android.R.id.message)
        consignmentEndpointDialogMessageTV = dialogMessageView
        super.onBindDialogView(view)
    }

    companion object {
        fun newInstance(key: String): ConsignmentEndpointEditTextPreferenceFragment {
            val fragment = ConsignmentEndpointEditTextPreferenceFragment()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}
