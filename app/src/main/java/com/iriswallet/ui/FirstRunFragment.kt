package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentFirstRunBinding
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils

class FirstRunFragment :
    MainBaseFragment<FragmentFirstRunBinding>(FragmentFirstRunBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar!!.hide()
        binding.firstRunTV.text =
            getString(R.string.first_run_disclaimer, AppContainer.bitcoinNetwork.capitalized)
        binding.firstRunCreateBtn.setOnClickListener {
            disableUI()
            AppContainer.bdkDir.mkdir()
            AppContainer.rgbDir.mkdir()
            viewModel.refreshAssets()
        }
        mActivity.hideSplashScreen = true

        viewModel.assets.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.error != null) {
                    handleError(response.error) {
                        clearAndShowExitDialog(R.string.err_creating_wallet, response.error.message)
                    }
                } else {
                    SharedPreferencesManager.mnemonic = AppContainer.bitcoinKeys.mnemonic
                    findNavController().navigate(R.id.action_firstRunFragment_to_mainFragment)
                }
            }
        }
    }

    override fun enableUI() {
        super.enableUI()
        binding.firstRunCreateBtn.isEnabled = true
        binding.firstRunPB.visibility = View.INVISIBLE
    }

    private fun disableUI() {
        binding.firstRunCreateBtn.isEnabled = false
        binding.firstRunPB.visibility = View.VISIBLE
    }

    private fun clearAndShowExitDialog(baseMsgID: Int, extraMsg: String?) {
        AppUtils.deleteAppData()
        AlertDialog.Builder(mActivity)
            .setMessage(getErrMsg(baseMsgID, extraMsg))
            .setPositiveButton(getString(R.string.exit)) { _, _ -> mActivity.finish() }
            .setCancelable(false)
            .create()
            .show()
    }
}
