package com.iriswallet.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentBackupBinding
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.GoogleDriveAuthHelper
import com.iriswallet.utils.GoogleDriveAuthListener

class BackupFragment :
    MainBaseFragment<FragmentBackupBinding>(FragmentBackupBinding::inflate),
    GoogleDriveAuthListener {

    private var isMnemonicHidden = true

    private lateinit var backupGoogleAccount: String
    private lateinit var driveAuthHelper: GoogleDriveAuthHelper

    private val authorizeLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            driveAuthHelper.handleAuthorizationResult(result.resultCode)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        driveAuthHelper = GoogleDriveAuthHelper(this, authorizeLauncher, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.backup.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data == true) {
                    Toast.makeText(
                            activity,
                            getString(R.string.backup_completed),
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    enableUI()
                    findNavController().popBackStack()
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_doing_backup, response.error.message)
                        enableUI()
                    }
                }
            }
        }

        binding.backupShowHideMnemonicBtn.setOnClickListener {
            if (isMnemonicHidden) {
                binding.backupShowHideMnemonicBtn.text = getString(R.string.hide_mnemonic)
                binding.backupShowHideMnemonicBtn.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_hide,
                    0,
                    0,
                    0,
                )
                val mnemonicWords =
                    AppContainer.storedMnemonic.split(' ').mapIndexed { index, s ->
                        "%02d. %s".format(index + 1, s)
                    }
                binding.backupMnemonic1.text =
                    mnemonicWords.subList(0, 6).joinToString(separator = "\n")
                binding.backupMnemonic2.text =
                    mnemonicWords.subList(6, 12).joinToString(separator = "\n")
                binding.backupMnemonicCardView.visibility = View.VISIBLE
            } else {
                binding.backupShowHideMnemonicBtn.text = getString(R.string.show_mnemonic)
                binding.backupShowHideMnemonicBtn.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_show,
                    0,
                    0,
                    0,
                )
                binding.backupMnemonicCardView.visibility = View.GONE
            }
            isMnemonicHidden = !isMnemonicHidden
        }

        if (!SharedPreferencesManager.backupGoogleAccount.isNullOrBlank()) {
            binding.backupConfigureBackupBtn.visibility = View.GONE
            val gAccountEmail = SharedPreferencesManager.backupGoogleAccount
            val message = getString(R.string.backup_configured, gAccountEmail)
            val spannable = SpannableString(message)
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                message.length - gAccountEmail!!.length,
                message.length,
                0,
            )
            binding.backupDataTV.text = spannable
        } else {
            binding.backupConfigureBackupBtn.setOnClickListener {
                disableUI()
                driveAuthHelper.initiateSignInAndRequestDriveAccess()
            }
        }
    }

    override fun enableUI() {
        super.enableUI()
        binding.backupPB.visibility = View.INVISIBLE
        binding.backupConfigureBackupBtn.isEnabled = true
        binding.backupShowHideMnemonicBtn.isEnabled = true
    }

    private fun disableUI() {
        binding.backupPB.visibility = View.VISIBLE
        binding.backupShowHideMnemonicBtn.isEnabled = false
        binding.backupConfigureBackupBtn.isEnabled = false
        mActivity.backEnabled = false
    }

    override fun onGoogleSignInSuccess(email: String) {
        this.backupGoogleAccount = email
    }

    override fun onDriveAccessTokenReceived(accessToken: String) {
        viewModel.startBackup(accessToken, backupGoogleAccount)
    }

    override fun onGoogleSignInError(errorExtraInfo: String?) {
        toastMsg(R.string.err_google_sign_in, errorExtraInfo)
        enableUI()
    }

    override fun onDriveAuthorizationError(errorExtraInfo: String?) {
        toastMsg(R.string.err_drive_authorization, errorExtraInfo)
        enableUI()
    }
}
