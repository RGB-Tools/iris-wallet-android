package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.iriswallet.R
import com.iriswallet.databinding.FragmentBackupBinding
import com.iriswallet.utils.AppContainer

class BackupFragment : MainBaseFragment<FragmentBackupBinding>(FragmentBackupBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backupShowHideMnemonicBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(AppContainer.storedMnemonic)
                .setPositiveButton(getString(R.string.hide_mnemonic)) { _, _ -> }
                .create()
                .show()
        }
    }
}
