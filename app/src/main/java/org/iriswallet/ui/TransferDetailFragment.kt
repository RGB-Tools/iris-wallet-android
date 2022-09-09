package org.iriswallet.ui

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.*
import org.iriswallet.R
import org.iriswallet.databinding.FragmentTransferDetailBinding
import org.iriswallet.utils.AppConstants
import org.iriswallet.utils.AppContainer
import org.iriswallet.utils.Transfer
import org.iriswallet.utils.UTXO

class TransferDetailFragment :
    MainBaseFragment<FragmentTransferDetailBinding>(FragmentTransferDetailBinding::inflate) {

    private lateinit var transfer: Transfer

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
                                    viewModel.deleteTransfer(
                                        viewModel.viewingAsset!!,
                                        transfer.recipient!!
                                    )
                                }
                                .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                                .create()
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

        transfer = viewModel.viewingTransfer!!

        if (transfer.automatic) {
            binding.transferInfo.visibility = View.VISIBLE
            binding.transferInfo.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.auto_text_utxo))
                    .setPositiveButton(getString(R.string.OK)) { _, _ -> }
                    .create()
                    .show()
            }
        }

        if (transfer.deletable())
            viewModel.asset.observe(viewLifecycleOwner) {
                it.getContentIfNotHandled()?.let {
                    enableUI()
                    findNavController().popBackStack()
                }
            }

        val amountStr =
            if (transfer.amount == null) getString(R.string.not_available)
            else transfer.amount.toString()
        binding.transferAmountTV.text = amountStr
        if (transfer.incoming) {
            binding.transferAmountTV.setTextColor(
                ContextCompat.getColor(view.context, R.color.color_green)
            )
            binding.transferAmountTV.text = getString(R.string.positive_amount, amountStr)
        } else {
            binding.transferAmountTV.setTextColor(
                ContextCompat.getColor(view.context, R.color.color_red)
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
            val params = binding.transferTXIDLabelTV.layoutParams as ConstraintLayout.LayoutParams
            params.topToBottom = binding.transferAmountTV.id
        }

        binding.transferDateTV.text =
            SimpleDateFormat(AppConstants.transferFullDateFmt, Locale.US).format(transfer.date)

        if (viewModel.viewingAsset!!.bitcoin() || transfer.recipient.isNullOrBlank()) {
            binding.transferRecipientLabelTV.visibility = View.GONE
            binding.transferRecipientTV.visibility = View.GONE
        }
        binding.transferRecipientTV.text = transfer.recipient

        // unblinded UTXO
        val unblindedOutpoint = transfer.unblindedUTXO
        if (!transfer.incoming or (unblindedOutpoint == null)) {
            binding.transferUnblindedUTXOLabelTV.visibility = View.GONE
            binding.transferUnblindedUTXOTV.visibility = View.GONE
        } else {
            binding.transferUnblindedUTXOTV.text = UTXO(unblindedOutpoint!!).outpointStr()
            binding.transferUnblindedUTXOTV.paintFlags =
                binding.transferUnblindedUTXOTV.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.transferUnblindedUTXOTV.visibility = View.VISIBLE
            binding.transferUnblindedUTXOLabelTV.visibility = View.VISIBLE
            binding.transferUnblindedUTXOTV.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val link = AppContainer.explorerURL + transfer.unblindedUTXO
                intent.data = Uri.parse(link)
                startActivity(intent)
            }
        }

        // change UTXO
        if (transfer.changeUTXO == null) {
            binding.transferChangeUTXOLabelTV.visibility = View.GONE
            binding.transferChangeUTXOTV.visibility = View.GONE
        } else {
            binding.transferChangeUTXOTV.text = UTXO(transfer.changeUTXO!!).outpointStr()
            binding.transferChangeUTXOTV.paintFlags =
                binding.transferChangeUTXOTV.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            binding.transferChangeUTXOTV.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                val link = AppContainer.explorerURL + transfer.changeUTXO
                intent.data = Uri.parse(link)
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.viewingTransfer = null
    }

    private fun disableUI() {
        mActivity.backEnabled = false
        binding.transferPB.visibility = View.VISIBLE
    }
}
