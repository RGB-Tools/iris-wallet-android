package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentReceiveAssetBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.Receiver

class ReceiveAssetFragment :
    MainBaseFragment<FragmentReceiveAssetBinding>(FragmentReceiveAssetBinding::inflate) {

    var asset: AppAsset? = null

    private var receiveData: Receiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.receiveInfoLL.visibility = View.GONE

        binding.receiveCopyBtn.setOnClickListener {
            toClipboard(AppConstants.RECEIVE_DATA_CLIP_LABEL, receiveData!!.invoice)
        }

        binding.receiveDataTV.setOnClickListener {
            toClipboard(AppConstants.RECEIVE_DATA_CLIP_LABEL, receiveData!!.invoice)
        }

        viewModel.recipient.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    receiveData = response.data
                    showReceiveData(response.data)
                    enableUI()
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.recipient_error, response.error.message)
                        enableUI()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        asset = viewModel.viewingAsset
        viewModel.genReceiveData(asset)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (asset == null) viewModel.refreshAssets() else viewModel.refreshAssetDetail(asset!!)
    }

    private fun disableUI() {
        mActivity.backEnabled = false
    }

    private fun showReceiveData(receiveData: Receiver) {
        val labelString = if (receiveData.bitcoin) R.string.address else R.string.invoice
        binding.receiveLabelTV.text = getString(labelString)
        binding.receiveDataTV.text = receiveData.invoice
        val bitmap = AppUtils.getQRCodeBitmap(receiveData.invoice, mActivity.windowManager)
        binding.receiveQRCodeImg.setImageBitmap(bitmap)
        if (asset != null)
            binding.receiveExtraInfoTV.text =
                getString(R.string.blinded_utxo_asset, getString(R.string.blinded_utxo_expiry))

        binding.receiveLoader.visibility = View.GONE
        binding.receiveQRCodeImg.visibility = View.VISIBLE
        binding.receiveInfoLL.visibility = View.VISIBLE
        binding.receiveExtraInfoTV.visibility =
            if (receiveData.expirationSeconds == null) View.GONE else View.VISIBLE
        binding.receiveCopyBtn.visibility = View.VISIBLE
    }
}
