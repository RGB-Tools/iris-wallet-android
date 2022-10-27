package com.iriswallet.ui

import android.content.ClipData
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentReceiveAssetBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.Receiver

class ReceiveAssetFragment :
    MainBaseFragment<FragmentReceiveAssetBinding>(FragmentReceiveAssetBinding::inflate) {

    private var receiveData: Receiver? = null

    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.genReceiveData(viewModel.viewingAsset)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.receive_asset, menu)
                    val copyDataItem = menu.findItem(R.id.copyDataMenu)
                    copyDataItem.isEnabled = !isLoading
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.copyDataMenu -> {
                            val clip =
                                ClipData.newPlainText(
                                    AppConstants.receiveDataClipLabel,
                                    receiveData!!.recipient
                                )
                            AppContainer.clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                    activity,
                                    getString(R.string.clipboard_filled),
                                    Toast.LENGTH_LONG
                                )
                                .show()
                            true
                        }
                        android.R.id.home -> {
                            mActivity.onBackPressed()
                            true
                        }
                        else -> true
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        viewModel.recipient.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) {
                    receiveData = response.data
                    binding.receiveDataTV.text = response.data.recipient
                    val bitmap =
                        AppUtils.getQRCodeBitmap(response.data.recipient, mActivity.windowManager)
                    binding.receiveQRCodeImg.setImageBitmap(bitmap)
                    showQRCode()
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

    override fun onPause() {
        if (viewModel.viewingAsset == null) viewModel.refreshAssets()
        else viewModel.refreshAssetDetail(viewModel.viewingAsset!!)
        super.onPause()
    }

    override fun enableUI() {
        super.enableUI()
        isLoading = false
        requireActivity().invalidateOptionsMenu()
    }

    private fun showQRCode() {
        binding.receiveLoader.visibility = View.GONE
        binding.receiveQRCodeImg.visibility = View.VISIBLE
        if (receiveData!!.expiration_time != null) binding.receiveExpiryTV.visibility = View.VISIBLE
        else binding.receiveExpiryTV.visibility = View.INVISIBLE
    }
}
