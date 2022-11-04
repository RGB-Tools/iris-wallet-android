package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentFirstRunBinding
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils

class FirstRunFragment :
    MainBaseFragment<FragmentFirstRunBinding>(FragmentFirstRunBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // needed until we find a way to stop rgb-lib services on clearAndShowExitDialog
        AppUtils.deleteAppData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        binding.firstRunTV.text =
            getString(R.string.first_run_disclaimer, AppContainer.bitcoinNetwork.capitalized)
        binding.firstRunCreateBtn.setOnClickListener {
            disableUI()
            AppContainer.bdkDir.mkdir()
            AppContainer.rgbDir.mkdir()
            viewModel.initNewApp()
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
                else
                    findNavController()
                        .navigate(R.id.action_firstRunFragment_to_termsAndConditionsFragment)
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
}
