package com.iriswallet.ui

import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import com.iriswallet.BuildConfig
import com.iriswallet.R
import com.iriswallet.databinding.FragmentAboutPageBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import java.util.regex.Pattern

class AboutPageFragment :
    MainBaseFragment<FragmentAboutPageBinding>(FragmentAboutPageBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.aboutVersionTV.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)

        Linkify.addLinks(
            binding.aboutPrivacyPolicyTV,
            Pattern.compile(getString(R.string.privacy_policy_link)),
            null,
            null
        ) { _, _ -> AppConstants.privacyPolicyURL }

        Linkify.addLinks(
            binding.aboutTermsOfServiceTV,
            Pattern.compile(getString(R.string.terms_of_service_link)),
            null,
            null
        ) { _, _ -> AppContainer.termsAndConditionsURL }
    }
}
