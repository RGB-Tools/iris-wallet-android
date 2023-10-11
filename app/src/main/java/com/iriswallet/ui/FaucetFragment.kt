package com.iriswallet.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.iriswallet.R
import com.iriswallet.data.retrofit.DistributionMode
import com.iriswallet.databinding.FragmentFaucetBinding
import com.iriswallet.utils.RgbFaucet
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class FaucetFragment : MainBaseFragment<FragmentFaucetBinding>(FragmentFaucetBinding::inflate) {

    private val activeButtonTags = mutableListOf<String>()
    private lateinit var requestedAssetGroup: Pair<String, Int>
    private var excludeTags: MutableList<String> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.rgbFaucets.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (!response.data.isNullOrEmpty()) {
                    enableUI()
                    showFaucetGroups(response.data)
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_getting_faucet_data, response.error.message)
                        enableUI()
                    }
                }
            }
        }

        viewModel.rgbFaucetResponse.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    if (requestedAssetGroup.second == 1) excludeTags.add(requestedAssetGroup.first)
                    val (asset, distribution) = response.data
                    enableUI()
                    val msg =
                        when (distribution.modeEnum()) {
                            DistributionMode.STANDARD ->
                                getString(R.string.faucet_standard_req_succeeded, asset.name)
                            DistributionMode.RANDOM -> {
                                if (distribution.randomParams == null) {
                                    getString(R.string.faucet_unexpected_res)
                                } else {
                                    val date =
                                        ZonedDateTime.parse(
                                            distribution.randomParams.requestWindowClose,
                                            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC)
                                        ).withZoneSameInstant(ZoneId.systemDefault())
                                    getString(
                                        R.string.faucet_random_req_succeeded,
                                        date.format(DateTimeFormatter.ofPattern("HH:mm, MMM dd"))
                                    )
                                }
                            }
                        }
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_receiving_from_faucet, response.error.message)
                        enableUI()
                    }
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.getFaucetAssetGroups()
    }

    override fun enableUI() {
        super.enableUI()
        binding.faucetPB.visibility = View.INVISIBLE
        for (tag in activeButtonTags) {
            if (excludeTags.contains(tag)) continue
            val btn = binding.faucetAssetGroupsLL.findViewWithTag<MaterialButton>(tag)
            btn.isEnabled = true
        }
    }

    private fun disableUI() {
        binding.faucetPB.visibility = View.VISIBLE
        for (id in activeButtonTags) {
            val btn = binding.faucetAssetGroupsLL.findViewWithTag<MaterialButton>(id)
            btn.isEnabled = false
        }
    }

    private fun showFaucetGroups(faucets: List<RgbFaucet>) {
        binding.faucetAssetGroupsLL.removeAllViews()
        activeButtonTags.clear()

        for (faucet in faucets) {
            for (assetGroup in faucet.groups) {
                val assetGroupLL: LinearLayout =
                    LayoutInflater.from(context)
                        .inflate(R.layout.asset_group, binding.faucetAssetGroupsLL, false)
                        as LinearLayout
                val assetGroupName = assetGroupLL.findViewById<TextView>(R.id.assetGroupName)
                assetGroupName.text = assetGroup.value.name
                val requestBtn = assetGroupLL.findViewById<MaterialButton>(R.id.faucetRequestBtn)
                val tag = "${faucet.url} ${assetGroup.key}"
                requestBtn.tag = tag
                if (assetGroup.value.requestsLeft == 0) {
                    requestBtn.isEnabled = false
                    excludeTags.add(tag)
                } else {
                    activeButtonTags.add(tag)
                    requestBtn.setOnClickListener {
                        disableUI()
                        requestedAssetGroup = Pair(tag, assetGroup.value.requestsLeft)
                        viewModel.receiveFromRgbFaucet(faucet.url, assetGroup.key)
                    }
                }
                val faucetName = assetGroupLL.findViewById<TextView>(R.id.faucetName)
                faucetName.text = faucet.faucetName
                binding.faucetAssetGroupsLL.addView(assetGroupLL)
            }
        }
    }
}
