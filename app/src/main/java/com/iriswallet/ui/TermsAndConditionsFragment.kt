package com.iriswallet.ui

import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentTermsAndConditionsBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import java.util.regex.Pattern

class TermsAndConditionsFragment :
    MainBaseFragment<FragmentTermsAndConditionsBinding>(
        FragmentTermsAndConditionsBinding::inflate
    ) {

    private lateinit var scrollViewListener: ViewTreeObserver.OnScrollChangedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity.onBackPressedDispatcher.addCallback(this) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        resources.openRawResource(AppContainer.termsAndConditionsID).bufferedReader().use {
            binding.tacTV.text = it.readText()
            Linkify.addLinks(
                binding.tacTV,
                Pattern.compile(getString(R.string.privacy_policy_link)),
                null,
                null
            ) { _, _ ->
                AppConstants.privacyPolicyURL
            }
        }
        scrollViewListener =
            ViewTreeObserver.OnScrollChangedListener {
                if (!binding.tacScrollView.canScrollVertically(1)) {
                    binding.tacAcceptBtn.isEnabled = true
                    binding.tacScrollView.viewTreeObserver.removeOnScrollChangedListener(
                        scrollViewListener
                    )
                }
            }
        binding.tacScrollView.viewTreeObserver.addOnScrollChangedListener(scrollViewListener)
        binding.tacAcceptBtn.setOnClickListener {
            SharedPreferencesManager.mnemonic = AppContainer.bitcoinKeys.mnemonic
            findNavController().navigate(R.id.action_termsAndConditionsFragment_to_mainFragment)
        }
        binding.tacDoNotAcceptBtn.setOnClickListener {
            clearAndShowExitDialog(getString(R.string.terms_not_accepted))
        }
    }
}
