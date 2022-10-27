package com.iriswallet.ui

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.iriswallet.R
import com.iriswallet.databinding.FragmentHelpPageBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.BitcoinNetwork

class HelpPageFragment :
    MainBaseFragment<FragmentHelpPageBinding>(FragmentHelpPageBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.helpFaucetLabelTV.text =
            getString(R.string.help_faucet_label, AppContainer.bitcoinNetwork)
        val faucets =
            when (AppContainer.bitcoinNetwork) {
                BitcoinNetwork.SIGNET -> AppConstants.signetFaucets
                BitcoinNetwork.TESTNET -> AppConstants.testnetFaucets
            }
        faucets.forEach { faucet ->
            val textView = TextView(view.context)
            textView.text = faucet
            textView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.text_medium)
            )
            textView.setTextColor(view.context.getColor(R.color.color_link))
            textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            textView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(faucet)
                startActivity(intent)
            }
            binding.helpFaucetLL.addView(textView)
        }
    }
}
