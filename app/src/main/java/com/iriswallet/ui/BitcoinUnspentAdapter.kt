package com.iriswallet.ui

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
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
            val link = AppContainer.explorerURL + unspent.outpoint().outpointStr()
            intent.data = Uri.parse(link)
            viewHolder.itemView.context.startActivity(intent)
        }

        if (unspent.walletName == AppConstants.vanillaWallet)
            viewHolder.binding.unspentWalletImg.setImageDrawable(viewHolder.bitcoinLogo)
        else viewHolder.binding.unspentWalletImg.setImageDrawable(viewHolder.rgbLogo)

        viewHolder.binding.unspentOutpointTV.text = unspent.outpoint().outpointStr()
        viewHolder.binding.unspentBTCAmountTV.text =
            viewHolder.itemView.context.getString(R.string.sat_amount, unspent.satAmount.toString())

        // dynamically handle unspentRGBLL contents
        fillRGBData(viewHolder, unspent.rgbUnspents)
    }

    override fun getItemCount() = dataSet.size

    private fun fillRGBData(viewHolder: ViewHolder, unspents: List<RgbUnspent>) {
        viewHolder.binding.unspentsRGBLL.removeAllViews()
        val context = viewHolder.itemView.context
        for (unspent in unspents) {
            val horizontalLL: LinearLayout =
                LayoutInflater.from(context)
                    .inflate(R.layout.unspent_rgb, viewHolder.binding.unspentsRGBLL, false)
                    as LinearLayout
            val assetIDTV = horizontalLL.findViewById<TextView>(R.id.rgbAssetId)
            assetIDTV.text = unspent.assetID ?: viewHolder.notAvailable
            val balanceTV = horizontalLL.findViewById<TextView>(R.id.rgbAmount)
            val balance =
                if (unspent.amount > 0UL) unspent.amount.toString() else viewHolder.notAvailable
            val ticker = unspent.tickerOrName ?: viewHolder.notAvailable
            balanceTV.text = context.getString(R.string.rgb_amount, ticker, balance)
            viewHolder.binding.unspentsRGBLL.addView(horizontalLL)
            if (!unspent.settled) {
                val futureColor = ContextCompat.getColor(context, R.color.roman_silver_op6)
                assetIDTV.setTextColor(futureColor)
                balanceTV.setTextColor(futureColor)
                assetIDTV.setTypeface(null, Typeface.ITALIC)
                balanceTV.setTypeface(null, Typeface.ITALIC)
            }
        }
    }
}
