package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentReceiveAssetBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.Receiver

class ReceiveAssetFragment :
    MainBaseFragment<FragmentReceiveAssetBinding>(FragmentReceiveAssetBinding::inflate) {

    private var receiveData: Receiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableUI()
        viewModel.genReceiveData(viewModel.viewingAsset)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.receiveInfoLL.visibility = View.GONE

        binding.receiveCopyBtn.setOnClickListener {
            toClipboard(AppConstants.receiveDataClipLabel, receiveData!!.recipient)
        }

        viewModel.recipient.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    receiveData = response.data
                    showReceiveData(response.data)
                    enableUI()
                } else {
                    handleError(response.error!!) {
                        toastError(R.string.recipient_error, response.error.message)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (viewModel.viewingAsset == null) viewModel.refreshAssets()
        else viewModel.refreshAssetDetail(viewModel.viewingAsset!!)
    }

    override fun enableUI() {
        super.enableUI()
        binding.receiveCopyBtn.isEnabled = true
        binding.receiveCopyBtn.visibility = View.VISIBLE
    }

    private fun disableUI() {
        mActivity.backEnabled = false
    }

    private fun showReceiveData(receiveData: Receiver) {
        val labelString = if (receiveData.bitcoin) R.string.address else R.string.blinded_utxo_cap
        binding.receiveLabelTV.text = getString(labelString)
        binding.receiveDataTV.text = receiveData.recipient
        val bitmap = AppUtils.getQRCodeBitmap(receiveData.recipient, mActivity.windowManager)
        binding.receiveQRCodeImg.setImageBitmap(bitmap)
        binding.receiveLoader.visibility = View.GONE
        binding.receiveQRCodeImg.visibility = View.VISIBLE
        binding.receiveInfoLL.visibility = View.VISIBLE
        binding.receiveExpiryTV.visibility =
            if (receiveData.expirationSeconds == null) View.GONE else View.VISIBLE
    }
}
