package com.iriswallet.ui

import android.net.Uri
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
import java.math.BigDecimal
import org.bitcoindevkit.Address
import org.bitcoindevkit.BdkException
import org.rgbtools.*

class SendAssetFragment :
    MainBaseFragment<FragmentSendAssetBinding>(FragmentSendAssetBinding::inflate),
    AppAuthenticationServiceListener {

    lateinit var asset: AppAsset
    private var consignmentEndpoints: List<String> = listOf()

    private var isLoading = false

    private lateinit var appAuthenticationService: AppAuthenticationService

    private lateinit var editableFields: Array<EditText>

    private var insertedFromClipboard = false

    private var validData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appAuthenticationService = AppAuthenticationService(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sendBalanceSpendableLL.detailBalanceLabelTV.text =
            getString(R.string.spendable_balance)

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

        editableFields = arrayOf(binding.sendPayToET, binding.sendAmountET, binding.sendFeeRateET)
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
                        if (editText.hashCode() == binding.sendAmountET.hashCode())
                            fixETAmount(editText, editable.toString())
                        binding.sendSendBtn.isEnabled = enableSendBtn()
                    }
                }
            )
        }

        binding.sendAmountET.setOnEditorActionListener(onKeyboardDoneListener)
        binding.sendFeeRateET.setOnEditorActionListener(onKeyboardDoneListener)

        binding.sendPayToET.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && !insertedFromClipboard) {
                val clipboardData =
                    AppContainer.clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (clipboardData?.isNotBlank() == true) {
                    if (detectContent(clipboardData)) {
                        insertedFromClipboard = true
                        v.clearFocus()
                    }
                }
            }
        }

        binding.sendFeeRateET.hint = AppConstants.defaultFeeRate.toString()
        binding.sendFeeRateET.setText(SharedPreferencesManager.feeRate)
        binding.sendFeeRateET.filters +=
            DecimalsInputFilter(
                AppConstants.feeRateIntegerPlaces,
                AppConstants.feeRateDecimalPlaces,
                minValue = AppConstants.minFeeRate
            )
        binding.sendFeeRateET.setSelectAllOnFocus(true)

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
                        toastMsg(R.string.err_sending, response.error.message)
                        enableUI()
                    }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        asset = viewModel.viewingAsset!!
    }

    override fun onResume() {
        super.onResume()
        enableUI()

        binding.sendSendBtn.isEnabled = enableSendBtn()
        binding.sendBalanceTotalLL.detailBalanceTV.text = asset.totalBalance.toString()
        binding.sendBalanceSpendableLL.detailBalanceTV.text = asset.spendableBalance.toString()
        if (asset.bitcoin()) {
            val ticker = getString(R.string.bitcoin_unit)
            binding.sendBalanceTotalLL.detailTickerTV.text = ticker
            binding.sendBalanceSpendableLL.detailTickerTV.text = ticker
            binding.sendPayToET.hint = getString(R.string.address).lowercase()
            binding.sendAmountET.inputType =
                InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            // decimals not yet supported for RGB amounts
            binding.sendAmountET.hint = "0"
            binding.sendBalanceTotalLL.detailTickerTV.text = asset.ticker
            binding.sendBalanceSpendableLL.detailTickerTV.text = asset.ticker
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

    private fun enableSendBtn(): Boolean {
        return allETsFilled(editableFields) &&
            isETPositive(binding.sendAmountET) &&
            binding.sendAmountET.text.toString().toULong() <= asset.spendableBalance
    }

    private fun detectContent(content: String, toastErr: Boolean = false): Boolean {
        val (payTo, amount) =
            if (asset.bitcoin()) {
                try {
                    Address(content)
                    Pair(content, null)
                } catch (e: BdkException) {
                    try {
                        val bitcoinInvoice = Uri.parse(content)
                        val address = bitcoinInvoice.schemeSpecificPart.split("?")[0]
                        if (bitcoinInvoice.scheme == "bitcoin") {
                            Address(address)
                            val queryParams = bitcoinInvoice.query?.split("&")
                            var amount: String? = null
                            if (queryParams != null) {
                                for (param in queryParams) {
                                    val parts = param.split("=")
                                    if (parts[0] == "amount") {
                                        amount = BigDecimal(parts[1]).movePointRight(8).toString()
                                        break
                                    }
                                }
                            }
                            Pair(address, amount)
                        } else {
                            throw RuntimeException("invalid bitcoin invoice")
                        }
                    } catch (e: Exception) {
                        if (toastErr) toastMsg(R.string.scanned_invalid_btc)
                        return false
                    }
                }
            } else {
                try {
                    val invoiceData = Invoice(content).invoiceData()
                    if (invoiceData.assetId != null && invoiceData.assetId != asset.id) {
                        if (toastErr)
                            toastError(
                                getString(R.string.scanned_invalid_asset, invoiceData.assetId)
                            )
                        return false
                    }
                    val amount =
                        if (invoiceData.amount != null) invoiceData.amount.toString() else null
                    if (
                        invoiceData.expirationTimestamp != null &&
                            invoiceData.expirationTimestamp!! * 1000L <= System.currentTimeMillis()
                    ) {
                        if (toastErr) toastMsg(R.string.scanned_expired_invoice)
                        return false
                    }
                    if (invoiceData.consignmentEndpoints.isNotEmpty()) {
                        consignmentEndpoints =
                            invoiceData.consignmentEndpoints.filter {
                                ConsignmentEndpoint(it).protocol() ==
                                    ConsignmentEndpointProtocol.RGB_HTTP_JSON_RPC
                            }
                        consignmentEndpoints.take(3)
                    }
                    Pair(invoiceData.blindedUtxo, amount)
                } catch (_: RgbLibException) {
                    try {
                        BlindedUtxo(content)
                        Pair(content, null)
                    } catch (_: RgbLibException) {
                        if (toastErr) toastMsg(R.string.scanned_invalid_rgb)
                        return false
                    }
                }
            }
        validData = payTo

        binding.sendPayToET.setText(payTo)
        if (amount != null) binding.sendAmountET.setText(amount)
        return true
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
            } else detectContent(result.contents, toastErr = true)
        }

    override fun authenticated(requestCode: String) {
        val payTo = binding.sendPayToET.text.toString()
        if (payTo != validData) {
            if (!detectContent(payTo, true)) return
        }
        if (consignmentEndpoints.isEmpty() && !asset.bitcoin()) {
            consignmentEndpoints = listOf(AppContainer.proxyConsignmentEndpointDefault)
            toastMsg(R.string.using_default_consignment_endpoint)
        }
        viewModel.sendAsset(
            asset,
            payTo,
            binding.sendAmountET.text.toString(),
            consignmentEndpoints,
            binding.sendFeeRateET.text.toString().toFloat(),
        )
    }

    override fun handleAuthError(requestCode: String, errorExtraInfo: String?, errCode: Int?) {
        toastMsg(R.string.err_sending, errorExtraInfo)
        enableUI()
    }
}
