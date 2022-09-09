package org.iriswallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import org.iriswallet.R
import org.iriswallet.databinding.TransferListItemBinding
import org.iriswallet.utils.AppConstants
import org.iriswallet.utils.Transfer
import org.rgbtools.TransferStatus

class TransferListAdapter(
    private val dataSet: ArrayList<Transfer>,
    private val viewModel: MainViewModel,
    private val fragment: AssetDetailFragment
) : RecyclerView.Adapter<TransferListAdapter.ViewHolder>() {
    var isClickEnabled = true

    class ViewHolder(val binding: TransferListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val bind =
            TransferListItemBinding.inflate(
                LayoutInflater.from(viewGroup.context),
                viewGroup,
                false
            )
        return ViewHolder(bind)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val transaction = dataSet[position]

        if (transaction.automatic) viewHolder.binding.transferAutoTV.visibility = View.VISIBLE
        else viewHolder.binding.transferAutoTV.visibility = View.INVISIBLE

        var amount = transaction.amount!!.toString()
        if (transaction.incoming) {
            viewHolder.binding.transferItemAmountTV.setTextColor(
                ContextCompat.getColor(
                    viewHolder.binding.transferItemAmountTV.context,
                    R.color.color_green
                )
            )
            amount = viewHolder.itemView.context.getString(R.string.positive_amount, amount)
        } else {
            viewHolder.binding.transferItemAmountTV.setTextColor(
                ContextCompat.getColor(
                    viewHolder.binding.transferItemAmountTV.context,
                    R.color.color_red
                )
            )
            amount = viewHolder.itemView.context.getString(R.string.negative_amount, amount)
        }
        viewHolder.binding.transferItemAmountTV.text = amount
        viewHolder.binding.transferItemAmountTV.visibility = View.VISIBLE

        when (transaction.status) {
            TransferStatus.WAITING_COUNTERPARTY,
            TransferStatus.WAITING_CONFIRMATIONS,
            TransferStatus.FAILED -> handleIncompleteTransfer(viewHolder, transaction)
            TransferStatus.SETTLED -> {
                viewHolder.binding.transferItemDateTV.text =
                    SimpleDateFormat(AppConstants.transferDateFmt, Locale.US)
                        .format(transaction.date)
                viewHolder.binding.transferItemTimeTV.text =
                    SimpleDateFormat(AppConstants.transferTimeFmt, Locale.US)
                        .format(transaction.date)
            }
        }
        viewHolder.itemView.setOnClickListener {
            if (isClickEnabled) {
                viewModel.viewingTransfer = dataSet[position]
                fragment
                    .findNavController()
                    .navigate(R.id.action_assetDetailFragment_to_transferDetailFragment)
            }
        }
    }

    override fun getItemCount() = dataSet.size

    fun updateData(updatedData: ArrayList<Transfer>) {
        dataSet.clear()
        dataSet.addAll(updatedData)
        notifyDataSetChanged()
    }

    private fun handleIncompleteTransfer(
        viewHolder: ViewHolder,
        transfer: Transfer,
    ) {
        viewHolder.binding.transferItemAmountTV.setTextColor(
            ContextCompat.getColor(
                viewHolder.binding.transferItemAmountTV.context,
                R.color.color_gray
            )
        )
        viewHolder.binding.transferItemDateTV.text =
            if (transfer.status == TransferStatus.FAILED)
                viewHolder.itemView.context.getString(R.string.failed_transfer)
            else viewHolder.itemView.context.getString(R.string.ongoing_transfer)
        viewHolder.binding.transferItemTimeTV.text = transfer.status.name
        if (transfer.amount == 0UL) viewHolder.binding.transferItemAmountTV.visibility = View.GONE
    }
}
