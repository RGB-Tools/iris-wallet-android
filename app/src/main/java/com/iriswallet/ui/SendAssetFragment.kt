package com.iriswallet.ui

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.zxing.client.android.Intents
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentSendAssetBinding
import com.iriswallet.utils.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

class SendAssetFragment :
    MainBaseFragment<FragmentSendAssetBinding>(FragmentSendAssetBinding::inflate),
    AppAuthenticationServiceListener {

    lateinit var asset: AppAsset

    private var isLoading = false

    private lateinit var appAuthenticationService: AppAuthenticationService

    private lateinit var editableFields: Array<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appAuthenticationService = AppAuthenticationService(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = viewModel.viewingAsset!!

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.send_asset, menu)
                    val scanItem = menu.findItem(R.id.scanMenu)
                    scanItem.isEnabled = !isLoading
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.scanMenu -> {
                            disableUI(false)
                            val options = ScanOptions()
                            options.captureActivity = ScanActivity::class.java
                            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            options.setOrientationLocked(false)
                            options.setBeepEnabled(false)
                            options.setCameraId(0)
                            barcodeLauncher.launch(options)
                            true
                        }
                        android.R.id.home -> {
                            mActivity.onSupportNavigateUp()
                            true
                        }
                        else -> true
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        editableFields = arrayOf(binding.sendPayToTV, binding.sendAmountTV)
        for (editText in editableFields) {
            editText.addTextChangedListener(
                object : TextWatcher {
                    override fun beforeTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {}
                    override fun onTextChanged(
                        charSequence: CharSequence,
                        i: Int,
                        i1: Int,
                        i2: Int
                    ) {}
                    override fun afterTextChanged(editable: Editable) {
                        val editType = editText.inputType
                        if (numberTypes.contains(editType))
                            fixETAmount(editText, editable.toString(), asset.settledBalance)
                        binding.sendSendBtn.isEnabled =
                            allETsFilled(editableFields) && isETPositive(binding.sendAmountTV)
                    }
                }
            )
        }
        binding.sendSendBtn.setOnClickListener {
            disableUI()
            if (SharedPreferencesManager.pinActionsConfigured) appAuthenticationService.auth()
            else authenticated()
        }

        viewModel.sent.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (!response.data.isNullOrBlank()) {
                    Toast.makeText(
                            activity,
                            getString(R.string.sent_txid, response.data),
                            Toast.LENGTH_LONG
                        )
                        .show()
                    viewModel.refreshAssetDetail(asset)
                    findNavController().popBackStack()
                } else
                    handleError(response.error!!) {
                        toastError(R.string.err_sending, response.error.message)
                        enableUI()
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableUI()
        val enable = allETsFilled(editableFields)
        binding.sendSendBtn.isEnabled = enable && isETPositive(binding.sendAmountTV)
        binding.sendBalanceLL.detailBalanceTV.text = asset.totalBalance.toString()
        if (asset.bitcoin()) {
            binding.sendBalanceLL.detailTickerTV.text = getString(R.string.bitcoin_unit)
            binding.sendPayToTV.hint = getString(R.string.address).lowercase()
            binding.sendAmountTV.inputType =
                InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            // decimals not yet supported for RGB amounts
            binding.sendAmountTV.hint = "0"
            binding.sendBalanceLL.detailTickerTV.text = asset.ticker
        }
    }

    override fun enableUI() {
        super.enableUI()
        binding.sendPB.visibility = View.INVISIBLE
        isLoading = false
        requireActivity().invalidateOptionsMenu()
        binding.sendSendBtn.isEnabled = true
    }

    private fun disableUI(showProgress: Boolean = true) {
        binding.sendSendBtn.isEnabled = false
        mActivity.backEnabled = false
        if (showProgress) binding.sendPB.visibility = View.VISIBLE
        isLoading = true
        requireActivity().invalidateOptionsMenu()
    }

    private val barcodeLauncher =
        registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
            if (result.contents == null) {
                val originalIntent = result.originalIntent
                if (
                    originalIntent != null &&
                        originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)
                )
                    Toast.makeText(
                            activity,
                            getString(R.string.cancelled_scan_permissions),
                            Toast.LENGTH_LONG
                        )
                        .show()
            } else binding.sendPayToTV.setText(result.contents)
        }

    companion object {
        val numberTypes =
            arrayOf(
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_NUMBER_FLAG_DECIMAL,
                InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL
            )
    }

    override fun authenticated(requestCode: String) {
        viewModel.sendAsset(
            asset,
            binding.sendPayToTV.text.toString(),
            binding.sendAmountTV.text.toString()
        )
    }

    override fun handleAuthError(requestCode: String, errorExtraInfo: String?, errCode: Int?) {
        toastError(R.string.err_sending, errorExtraInfo)
        enableUI()
    }
}
