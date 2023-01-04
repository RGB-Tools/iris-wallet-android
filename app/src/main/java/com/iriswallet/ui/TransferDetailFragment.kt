package com.iriswallet.ui

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentTransferDetailBinding
import com.iriswallet.utils.*
import com.iriswallet.utils.AppUtils.Companion.toBulletedList
import java.text.SimpleDateFormat
import java.util.*
import org.rgbtools.*

class TransferDetailFragment :
    MainBaseFragment<FragmentTransferDetailBinding>(FragmentTransferDetailBinding::inflate) {

    private lateinit var transfer: AppTransfer
    lateinit var asset: AppAsset

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.transfer_detail, menu)
                    val deleteTransferItem = menu.findItem(R.id.deleteTransferMenu)
                    deleteTransferItem.isVisible = transfer.deletable()
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.deleteTransferMenu -> {
                            AlertDialog.Builder(requireContext())
                                .setMessage(getString(R.string.confirm_delete_transfer))
                                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                                    disableUI()
                                    viewModel.deleteTransfer(asset, transfer)
                                }
                                .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                                .create()
                                .show()
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
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        asset = viewModel.viewingAsset!!
        transfer = viewModel.viewingTransfer!!
        drawTransferDetails()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.viewingTransfer = null
    }

    override fun enableUI() {
        super.enableUI()
        binding.transferPB.visibility = View.INVISIBLE
    }

    private fun disableUI() {
        mActivity.backEnabled = false
        binding.transferPB.visibility = View.VISIBLE
    }

    private fun drawTransferDetails() {
        binding.transferInternalTV.visibility =
            if (transfer.internal) {
                binding.transferInternalTV.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.auto_text_utxo))
                        .setPositiveButton(getString(R.string.OK)) { _, _ -> }
                        .create()
                        .show()
                }
                View.VISIBLE
            } else View.GONE

        if (transfer.deletable())
            viewModel.asset.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let { response ->
                    if (response.requestID != asset.id) return@let
                    enableUI()
                    if (response.data != null) {
                        findNavController().popBackStack()
                    } else {
                        handleError(response.error!!) {
                            toastMsg(R.string.err_deleting_transfer, response.error.message)
                        }
                    }
                }
            }

        val amountStr =
            if (transfer.amount == null) getString(R.string.not_available)
            else transfer.amount.toString()
        binding.transferAmountTV.text = amountStr
        if (transfer.incoming()) {
            binding.transferAmountTV.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_green)
            )
            binding.transferAmountTV.text = getString(R.string.positive_amount, amountStr)
        } else {
            binding.transferAmountTV.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.color_red)
            )
            binding.transferAmountTV.text = getString(R.string.negative_amount, amountStr)
        }

        if (!transfer.txid.isNullOrBlank()) {
            binding.transferTXIDTV.text = transfer.txid
            binding.transferTXIDTV.paintFlags =
                binding.transferTXIDTV.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.transferTXIDTV.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val link = AppContainer.explorerURL + transfer.txid
                intent.data = Uri.parse(link)
                startActivity(intent)
            }
        } else {
            binding.transferTXIDLabelTV.visibility = View.GONE
            binding.transferTXIDTV.visibility = View.GONE
        }

        binding.transferDateTV.text =
            SimpleDateFormat(AppConstants.transferFullDateFmt, Locale.US).format(transfer.date)

        binding.transferInvoiceLabelTV.visibility = View.GONE
        binding.transferInvoiceTV.visibility = View.GONE

        if (asset.bitcoin() || transfer.blindedUTXO.isNullOrBlank()) {
            binding.transferBlindedUtxoLabelTV.visibility = View.GONE
            binding.transferBlindedUtxoTV.visibility = View.GONE
        } else {
            binding.transferBlindedUtxoTV.text = transfer.blindedUTXO
            if (
                transfer.status == TransferStatus.WAITING_COUNTERPARTY &&
                    transfer.kind == TransferKind.RECEIVE
            ) {
                val invoiceData =
                    InvoiceData(
                        transfer.blindedUTXO!!,
                        amount = null,
                        assetId = asset.id,
                        expirationTimestamp = transfer.expiration,
                        consignmentEndpoints =
                            transfer.consignmentEndpoints.orEmpty().map {
                                val uri =
                                    if (
                                        it.protocol == ConsignmentEndpointProtocol.RGB_HTTP_JSON_RPC
                                    )
                                        AppConstants.rgbHttpJsonRpcProtocol
                                    else AppConstants.stormProtocol
                                uri + it.endpoint
                            },
                    )
                val invoice = Invoice.fromInvoiceData(invoiceData)
                binding.transferInvoiceTV.text = invoice.bech32Invoice()
                binding.transferInvoiceLabelTV.visibility = View.VISIBLE
                binding.transferInvoiceTV.visibility = View.VISIBLE
            }
        }

        // unblinded UTXO
        if (asset.bitcoin() || transfer.kind != TransferKind.RECEIVE) {
            binding.transferUnblindedUTXOLabelTV.visibility = View.GONE
            binding.transferUnblindedUTXOTV.visibility = View.GONE
        } else {
            val outpoint = transfer.unblindedUTXO!!.outpointStr()
            binding.transferUnblindedUTXOTV.text = outpoint
            binding.transferUnblindedUTXOTV.paintFlags =
                binding.transferUnblindedUTXOTV.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.transferUnblindedUTXOTV.visibility = View.VISIBLE
            binding.transferUnblindedUTXOLabelTV.visibility = View.VISIBLE
            binding.transferUnblindedUTXOTV.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val link = AppContainer.explorerURL + outpoint
                intent.data = Uri.parse(link)
                startActivity(intent)
            }
        }

        // change UTXO
        if (asset.bitcoin() || transfer.changeUTXO == null) {
            binding.transferChangeUTXOLabelTV.visibility = View.GONE
            binding.transferChangeUTXOTV.visibility = View.GONE
        } else {
            val outpoint = transfer.changeUTXO!!.outpointStr()
            binding.transferChangeUTXOTV.text = outpoint
            binding.transferChangeUTXOTV.paintFlags =
                binding.transferChangeUTXOTV.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.transferChangeUTXOTV.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val link = AppContainer.explorerURL + outpoint
                intent.data = Uri.parse(link)
                startActivity(intent)
            }
        }

        // consignment endpoints
        if (asset.bitcoin()) {
            binding.transferEndpointsTV.visibility = View.GONE
            binding.transferEndpointsLabelTV.visibility = View.GONE
        } else if (transfer.consignmentEndpoints.isNullOrEmpty()) {
            binding.transferEndpointsTV.text = getString(R.string.not_available)
        } else {
            val bulletedList =
                transfer.consignmentEndpoints!!
                    .map { it.endpoint }
                    .toBulletedList(
                        color = ContextCompat.getColor(requireContext(), R.color.caribbean_green)
                    )
            binding.transferEndpointsTV.text = bulletedList
        }
    }
}
