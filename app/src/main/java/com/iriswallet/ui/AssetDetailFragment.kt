package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.iriswallet.R
import com.iriswallet.databinding.FragmentAssetDetailBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer

class AssetDetailFragment :
    MainBaseFragment<FragmentAssetDetailBinding>(FragmentAssetDetailBinding::inflate) {

    private lateinit var adapter: TransferListAdapter

    lateinit var asset: AppAsset

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = viewModel.viewingAsset!!
        setHeader(asset)

        binding.detailReceiveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_assetDetailFragment_to_receiveAssetFragment)
        }
        binding.detailSendBtn.setOnClickListener {
            findNavController().navigate(R.id.action_assetDetailFragment_to_sendAssetFragment)
        }

        binding.detailTransferRV.layoutManager = LinearLayoutManager(mActivity)

        adapter = TransferListAdapter(ArrayList(asset.transfers.reversed()), viewModel, this)
        binding.detailTransferRV.adapter = adapter

        binding.detailSwipeRefresh.setOnRefreshListener { refreshAsset() }

        if (viewModel.refreshingAsset) disableUI() else enableUI()

        viewModel.asset.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    asset = response.data
                    redrawAssetDetails()
                } else {
                    handleError(response.error!!) {
                        toastError(R.string.err_refreshing_asset_details, response.error.message)
                    }
                }
                enableUI()
            }
        }
        mActivity.services.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { if (!viewModel.refreshingAsset) enableUI() }
        }
    }

    override fun onResume() {
        super.onResume()
        redrawAssetDetails()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.viewingAsset = null
    }

    override fun enableUI() {
        super.enableUI()
        adapter.isClickEnabled = true
        setLoader(false)
        if (mActivity.serviceMap != null) {
            binding.detailSendBtn.isEnabled =
                mActivity.serviceMap!![AppContainer.electrumURL] == true
            if (!asset.bitcoin())
                binding.detailSendBtn.isEnabled =
                    mActivity.serviceMap!![AppConstants.consignmentProxyURL] == true
        } else {
            binding.detailSendBtn.isEnabled = true
        }
        binding.detailReceiveBtn.isEnabled = true
    }

    private fun disableUI() {
        setLoader(true)
        adapter.isClickEnabled = false
    }

    private fun refreshAsset() {
        disableUI()
        viewModel.refreshAssetDetail(asset)
    }

    private fun redrawAssetDetails() {
        setHeader(asset)
        adapter.updateData(ArrayList(asset.transfers.reversed()))
    }

    private fun setHeader(asset: AppAsset) {
        if (asset.bitcoin()) binding.detailTickerTV.text = getString(R.string.bitcoin_unit)
        else binding.detailTickerTV.text = asset.ticker

        binding.detailQuantityTV.text = asset.totalBalance.toString()
        if (asset.bitcoin()) {
            binding.detailIDLabelTV.visibility = View.GONE
            binding.detailIDTV.visibility = View.GONE
        } else {
            binding.detailIDTV.text = asset.id
        }
    }

    private fun setLoader(state: Boolean) {
        binding.detailSwipeRefresh.post { binding.detailSwipeRefresh.isRefreshing = state }
    }
}
