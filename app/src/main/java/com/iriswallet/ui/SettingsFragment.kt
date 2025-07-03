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
import com.iriswallet.data.RgbRepository
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.data.SharedPreferencesManager.PREFS_ELECTRUM_URL
import com.iriswallet.data.SharedPreferencesManager.PREFS_FEE_RATE
import com.iriswallet.data.SharedPreferencesManager.PREFS_HIDE_EXHAUSTED_ASSETS
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_ACTIONS_CONFIGURED
import com.iriswallet.data.SharedPreferencesManager.PREFS_PIN_LOGIN_CONFIGURED
import com.iriswallet.data.SharedPreferencesManager.PREFS_PROXY_CONSIGNMENT_ENDPOINT
import com.iriswallet.data.SharedPreferencesManager.PREFS_SHOW_HIDDEN_ASSETS
import com.iriswallet.utils.*
import org.rgbtools.RgbLibException
import org.rgbtools.TransportEndpoint
import org.rgbtools.TransportType

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
        electrumET!!.text = SharedPreferencesManager.electrumURL
        val proxyET = findPreference<EditTextPreference>(PREFS_PROXY_CONSIGNMENT_ENDPOINT)
        proxyET!!.text = SharedPreferencesManager.proxyTransportEndpoint

        val showHiddenAssets = findPreference<SwitchPreferenceCompat>(PREFS_SHOW_HIDDEN_ASSETS)!!
        setHideExhaustedAssets(showHiddenAssets)

        val feeRatePref = findPreference<EditTextPreference>(PREFS_FEE_RATE)!!
        feeRatePref.text = SharedPreferencesManager.feeRate
        feeRatePref.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.selectAll()
            it.filters +=
                IntegerInputFilter(
                    AppConstants.feeRateIntegerPlaces,
                    AppConstants.minFeeRate,
                    AppConstants.maxFeeRate,
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
        pref: Preference,
    ): Boolean {
        if (pref is EditTextPreference && pref.key == PREFS_PROXY_CONSIGNMENT_ENDPOINT) {
            showCustomEditTextDialog(pref.key, TransportEndpointEditTextPreferenceFragment)
            return true
        } else if (pref is EditTextPreference && pref.key == PREFS_ELECTRUM_URL) {
            showCustomEditTextDialog(pref.key, ElectrumURLEditTextPreferenceFragment)
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
                            preference.text!!.ifBlank { AppContainer.proxyTransportEndpointDefault }
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

    private fun <T> showCustomEditTextDialog(
        prefKey: String,
        customClass: CustomEditTextPreferenceDialogFragmentCompat<T>,
    ) {
        val f = customClass.newInstance(prefKey) as EditTextPreferenceDialogFragmentCompat
        // see https://issuetracker.google.com/issues/181793702
        f.setTargetFragment(this, 0)
        f.show(parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
    }
}

interface CustomEditTextPreferenceDialogFragmentCompat<T> {
    fun newInstance(key: String): T
}

class TransportEndpointEditTextPreferenceFragment : EditTextPreferenceDialogFragmentCompat() {

    private lateinit var dialogMessageTV: MaterialTextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val editText = dialog.findViewById<EditText>(android.R.id.edit)
                var err = false
                if (editText?.text!!.isNotBlank()) {
                    try {
                        val consignmentEndpoint = TransportEndpoint(editText.text.toString())
                        if (consignmentEndpoint.transportType() != TransportType.JSON_RPC)
                            err = true
                    } catch (e: RgbLibException) {
                        err = true
                    }
                }
                if (!err) {
                    val updatedPref = editText.text.toString()
                    if (preference.callChangeListener(updatedPref))
                        (preference as EditTextPreference).text = updatedPref
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
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    spannable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        existingMessage.length,
                        updatedMessage.length,
                        0,
                    )
                    dialogMessageTV.text = spannable
                }
            }
        }
        return dialog
    }

    override fun onBindDialogView(view: View) {
        val dialogMessageView = view.findViewById<MaterialTextView>(android.R.id.message)
        dialogMessageTV = dialogMessageView
        super.onBindDialogView(view)
    }

    companion object :
        CustomEditTextPreferenceDialogFragmentCompat<TransportEndpointEditTextPreferenceFragment> {
        override fun newInstance(key: String): TransportEndpointEditTextPreferenceFragment {
            val fragment = TransportEndpointEditTextPreferenceFragment()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}

class ElectrumURLEditTextPreferenceFragment : EditTextPreferenceDialogFragmentCompat() {

    private lateinit var dialogMessageTV: MaterialTextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            val positiveBtn = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val editText = dialog.findViewById<EditText>(android.R.id.edit)
                var err = false
                if (editText?.text!!.isNotBlank()) {
                    try {
                        RgbRepository.goOnlineAgain(editText.text.toString())
                    } catch (e: RgbLibException) {
                        err = true
                    }
                }
                if (!err) {
                    val updatedPref = editText.text.toString()
                    if (preference.callChangeListener(updatedPref))
                        (preference as EditTextPreference).text = updatedPref
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
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    spannable.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        existingMessage.length,
                        updatedMessage.length,
                        0,
                    )
                    dialogMessageTV.text = spannable
                }
            }
        }
        return dialog
    }

    override fun onBindDialogView(view: View) {
        val dialogMessageView = view.findViewById<MaterialTextView>(android.R.id.message)
        dialogMessageTV = dialogMessageView
        super.onBindDialogView(view)
    }

    companion object :
        CustomEditTextPreferenceDialogFragmentCompat<ElectrumURLEditTextPreferenceFragment> {
        override fun newInstance(key: String): ElectrumURLEditTextPreferenceFragment {
            val fragment = ElectrumURLEditTextPreferenceFragment()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }
    }
}
