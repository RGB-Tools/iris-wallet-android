package com.iriswallet.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentFirstRunBinding
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.GoogleSignInService
import com.iriswallet.utils.GoogleSignInServiceListener
import com.iriswallet.utils.TAG
import java.util.Locale

class FirstRunFragment :
    MainBaseFragment<FragmentFirstRunBinding>(FragmentFirstRunBinding::inflate),
    GoogleSignInServiceListener {

    private lateinit var googleDriveService: GoogleSignInService

    private lateinit var restoredMnemonic: String

    private var creatingNewWallet: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // prevent mixing creation/restore with possible stale data
        AppUtils.deleteAppData()
        SharedPreferencesManager.updatedToRgb010 = true
        googleDriveService = GoogleSignInService(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        binding.firstRunTV.text =
            getString(R.string.first_run_disclaimer, AppContainer.bitcoinNetwork.capitalized)
        binding.firstRunCreateBtn.setOnClickListener {
            disableUI()
            creatingNewWallet = true
            viewModel.initNewApp()
        }
        binding.firstRunRestoreBtn.setOnClickListener {
            disableUI()
            AlertDialog.Builder(mActivity)
                .setTitle(getString(R.string.restore_info_title))
                .setMessage(getString(R.string.restore_info_message))
                .setPositiveButton(getString(R.string.restore_info_positive)) { _, _ ->
                    restoreDialog()
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ -> enableUI() }
                .setCancelable(false)
                .show()
        }
        mActivity.hideSplashScreen = true

        viewModel.offlineAssets.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data.isNullOrEmpty())
                    handleError(response.error!!) {
                        clearAndShowExitDialog(
                            getErrMsg(R.string.err_creating_wallet, response.error.message)
                        )
                    }
                else if (creatingNewWallet) {
                    Log.d(TAG, "Creating wallet...")
                    findNavController()
                        .navigate(R.id.action_firstRunFragment_to_termsAndConditionsFragment)
                } else { // restoring
                    Log.d(TAG, "Restoring wallet...")
                    viewModel.refreshAssets(firstAppRefresh = true)
                    findNavController().navigate(R.id.action_firstRunFragment_to_mainFragment)
                }
            }
        }
        viewModel.restore.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data == true) {
                    viewModel.getOfflineAssets()
                } else {
                    handleError(response.error!!) {
                        clearAndShowExitDialog(
                            getErrMsg(R.string.err_restoring_app, response.error.message)
                        )
                    }
                }
            }
        }
    }

    override fun enableUI() {
        super.enableUI()
        binding.firstRunCreateBtn.isEnabled = true
        binding.firstRunRestoreBtn.isEnabled = true
        binding.firstRunPB.visibility = View.INVISIBLE
    }

    private fun disableUI() {
        binding.firstRunCreateBtn.isEnabled = false
        binding.firstRunRestoreBtn.isEnabled = false
        binding.firstRunPB.visibility = View.VISIBLE
    }

    private fun restoreDialog() {
        val container = LinearLayout(mActivity)
        container.orientation = LinearLayout.VERTICAL
        val lp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        val margin =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    resources.getDimension(R.dimen.small_size),
                    resources.displayMetrics,
                )
                .toInt()
        lp.setMargins(margin, 0, margin, 0)
        val editText = EditText(mActivity)
        editText.layoutParams = lp
        editText.gravity = Gravity.TOP or Gravity.LEFT
        editText.inputType =
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        editText.setLines(1)
        editText.maxLines = 1
        editText.filters =
            arrayOf<InputFilter>(
                object : InputFilter.AllCaps() {
                    override fun filter(
                        source: CharSequence,
                        start: Int,
                        end: Int,
                        dest: Spanned,
                        dstart: Int,
                        dend: Int,
                    ): CharSequence {
                        return source.toString().lowercase(Locale.getDefault())
                    }
                }
            )
        container.addView(editText, lp)
        AlertDialog.Builder(mActivity)
            .setTitle(getString(R.string.mnemonic_restore_title))
            .setMessage(getString(R.string.mnemonic_restore_message))
            .setView(container)
            .setPositiveButton(getString(R.string.alert_continue)) { _, _ ->
                restoredMnemonic = editText.text.toString()
                googleDriveService.signInGoogle()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> enableUI() }
            .setCancelable(false)
            .show()
    }

    override fun loggedIn(gAccount: GoogleSignInAccount) {
        viewModel.restoreBackup(gAccount, restoredMnemonic)
    }

    override fun handleLoginError(errorExtraInfo: String?) {
        toastMsg(R.string.err_google_sign_in, errorExtraInfo)
        enableUI()
    }
}
