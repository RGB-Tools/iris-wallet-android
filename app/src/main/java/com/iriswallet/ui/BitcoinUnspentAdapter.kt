package com.iriswallet.ui

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iriswallet.R
import com.iriswallet.databinding.UnspentListItemBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.RgbUnspent
import com.iriswallet.utils.UTXO

class BitcoinUnspentAdapter(private val dataSet: List<UTXO>) :
    RecyclerView.Adapter<BitcoinUnspentAdapter.ViewHolder>() {

    class ViewHolder(val binding: UnspentListItemBinding, val AppContainer: AppContainer) :
        RecyclerView.ViewHolder(binding.root) {
        val notAvailable: String = binding.root.context.getString(R.string.not_available)
        val bitcoinLogo: Drawable =
            AppCompatResources.getDrawable(itemView.context, AppContainer.bitcoinLogoID)!!
        val rgbLogo: Drawable =
            AppCompatResources.getDrawable(itemView.context, R.drawable.rgb_logo_round)!!
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val bind =
            UnspentListItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(bind, AppContainer)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val unspent = dataSet[position]

        viewHolder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            val link = AppContainer.explorerURL + unspent.outpointStr()
            intent.data = Uri.parse(link)
            viewHolder.itemView.context.startActivity(intent)
        }

        if (unspent.walletName == AppConstants.vanillaWallet)
            viewHolder.binding.unspentWalletImg.setImageDrawable(viewHolder.bitcoinLogo)
        else viewHolder.binding.unspentWalletImg.setImageDrawable(viewHolder.rgbLogo)

        viewHolder.binding.unspentOutpointTV.text = unspent.outpointStr()
        viewHolder.binding.unspentBTCAmountTV.text = unspent.btcAmount.toString()

        // dynamically handle unspentRGBCL contents
        emptyRGBData(viewHolder)
        fillRGBData(viewHolder, unspent.rgbUnspents)
    }

    override fun getItemCount() = dataSet.size

    private fun emptyRGBData(viewHolder: ViewHolder) {
        viewHolder.binding.unspentRGBCL.removeAllViews()
    }

    private fun addRgbBalanceTV(viewHolder: ViewHolder, balance: String, id: Int, top: Int) {
        val context = viewHolder.itemView.context
        val textView = TextView(context)
        textView.id = id
        textView.text = balance
        textView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            viewHolder.itemView.resources.getDimension(R.dimen.text_view)
        )
        textView.setTextColor(context.getColor(R.color.color_accent))
        textView.typeface = Typeface.DEFAULT_BOLD
        viewHolder.binding.unspentRGBCL.addView(textView)
        val params = textView.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        if (top == ConstraintLayout.LayoutParams.PARENT_ID) params.topToTop = top
        else params.topToBottom = top
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
    }

    private fun addRgbTickerTV(viewHolder: ViewHolder, ticker: String, id: Int) {
        val context = viewHolder.itemView.context
        val resources = viewHolder.itemView.resources
        val textView = TextView(context)
        textView.id = id
        textView.text = ticker
        textView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            viewHolder.itemView.resources.getDimension(R.dimen.text_small)
        )
        textView.setTextColor(context.getColor(R.color.color_accent))
        viewHolder.binding.unspentRGBCL.addView(textView)
        val params = textView.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
        params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        params.topToTop = id - 1
        params.bottomToBottom = id - 1
        params.endToStart = id - 1
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.small_size)
    }

    private fun addRgbIdTV(viewHolder: ViewHolder, assetId: String, id: Int) {
        val context = viewHolder.itemView.context
        val resources = viewHolder.itemView.resources
        val textView = TextView(context)
        textView.id = id
        textView.text = assetId
        textView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            viewHolder.itemView.resources.getDimension(R.dimen.text_small)
        )
        textView.setTextColor(context.getColor(R.color.color_accent))
        textView.isSingleLine = true
        textView.ellipsize = TextUtils.TruncateAt.MIDDLE
        viewHolder.binding.unspentRGBCL.addView(textView)
        val params = textView.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        params.topToTop = id - 1
        params.bottomToBottom = id - 1
        params.endToStart = id - 1
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.small_size)
    }

    private fun fillRGBData(viewHolder: ViewHolder, unspents: List<RgbUnspent>) {
        var topToTopOf = ConstraintLayout.LayoutParams.PARENT_ID
        var lastBalanceId = 9000
        var lastTickerId = lastBalanceId + 1
        var lastIdId = lastBalanceId + 2
        var id: Int
        for (unspent in unspents) {
            if (!unspent.settled && unspent.amount > 0UL) continue

            // add balance TextView
            id = lastBalanceId + 3
            val balance =
                if (unspent.amount > 0UL) unspent.amount.toString() else viewHolder.notAvailable
            addRgbBalanceTV(viewHolder, balance, id, topToTopOf)
            lastBalanceId = id
            topToTopOf = id
            // add ticker TextView
            id = lastTickerId + 3
            addRgbTickerTV(viewHolder, unspent.ticker ?: viewHolder.notAvailable, id)
            lastTickerId = id
            // add id TextView
            id = lastIdId + 3
            addRgbIdTV(viewHolder, unspent.asset_id ?: viewHolder.notAvailable, id)
            lastIdId = id
        }
    }
}
