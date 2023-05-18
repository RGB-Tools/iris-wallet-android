package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentAfterUpdateBinding
import com.iriswallet.utils.AppContainer

class AfterUpdateFragment :
    MainBaseFragment<FragmentAfterUpdateBinding>(FragmentAfterUpdateBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        mActivity.hideSplashScreen = true

        binding.auUnderstandBtn.setOnClickListener {
            SharedPreferencesManager.proxyTransportEndpoint =
                AppContainer.proxyTransportEndpointDefault
            SharedPreferencesManager.electrumURL = AppContainer.electrumURLDefault
            SharedPreferencesManager.removeOldKeys()
            SharedPreferencesManager.updatedToRgb010 = true
            findNavController().navigate(R.id.action_afterUpdateFragment_to_mainFragment)
        }
    }
}
