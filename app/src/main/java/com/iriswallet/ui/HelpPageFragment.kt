package com.iriswallet.ui

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.iriswallet.R
import com.iriswallet.databinding.FragmentHelpPageBinding
import com.iriswallet.utils.AppContainer

class HelpPageFragment :
    MainBaseFragment<FragmentHelpPageBinding>(FragmentHelpPageBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Linkify.addLinks(binding.helpLearnMoreTV, Linkify.WEB_URLS)
        Linkify.addLinks(binding.helpFeedbackTV, Linkify.WEB_URLS)

        binding.helpFaucetLabelTV.text =
            getString(R.string.help_faucet_label, AppContainer.bitcoinNetwork)
        if (AppContainer.btcHelpFaucetURLS.isEmpty()) {
            binding.helpFaucetLabelTV.visibility = View.GONE
            binding.helpFaucetTV.visibility = View.GONE
        }
        AppContainer.btcHelpFaucetURLS.forEach { faucet ->
            val linkTV: TextView =
                LayoutInflater.from(context)
                    .inflate(R.layout.help_link, binding.helpFaucetLL, false) as TextView
            linkTV.text = faucet
            linkTV.paintFlags = linkTV.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            linkTV.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(faucet)
                startActivity(intent)
            }
            binding.helpFaucetLL.addView(linkTV)
        }
    }
}
