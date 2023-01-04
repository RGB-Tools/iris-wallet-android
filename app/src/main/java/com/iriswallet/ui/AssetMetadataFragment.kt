package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentAssetMetadataBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppConstants
import java.text.SimpleDateFormat
import java.util.*

class AssetMetadataFragment :
    MainBaseFragment<FragmentAssetMetadataBinding>(FragmentAssetMetadataBinding::inflate) {

    lateinit var asset: AppAsset

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.metadataScrollView.visibility = View.GONE

        viewModel.metadata.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    enableUI()
                    showMetadata(response.data)
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_showing_metadata, response.error.message)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        asset = viewModel.viewingAsset!!
        viewModel.getAssetMetadata(asset)
    }

    override fun enableUI() {
        super.enableUI()
        binding.metadataPB.visibility = View.INVISIBLE
    }

    private fun showMetadata(metadata: org.rgbtools.Metadata) {
        binding.metadataTypeTV.text = metadata.assetType.toString()
        binding.metadataIssuedSupplyTV.text = metadata.issuedSupply.toString()
        binding.metadataDateTV.text =
            SimpleDateFormat(AppConstants.transferDateFmt, Locale.US)
                .format(metadata.timestamp * 1000)
        binding.metadataNameTV.text = metadata.name
        binding.metadataPrecisionTV.text = metadata.precision.toString()
        if (metadata.ticker.isNullOrBlank()) {
            binding.metadataTickerTV.visibility = View.GONE
            binding.metadataTickerLabelTV.visibility = View.GONE
        } else binding.metadataTickerTV.text = metadata.ticker
        if (metadata.description.isNullOrBlank()) {
            binding.metadataDescriptionTV.visibility = View.GONE
            binding.metadataDescriptionLabelTV.visibility = View.GONE
        } else binding.metadataDescriptionTV.text = metadata.description
        if (metadata.parentId.isNullOrBlank()) {
            binding.metadataParentIDTV.visibility = View.GONE
            binding.metadataParentIDLabelTV.visibility = View.GONE
        } else binding.metadataParentIDTV.text = metadata.parentId
        binding.metadataScrollView.visibility = View.VISIBLE
    }
}
