package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.iriswallet.R
import com.iriswallet.databinding.FragmentStartBinding
import com.iriswallet.utils.AppUtils

class StartFragment : MainBaseFragment<FragmentStartBinding>(FragmentStartBinding::inflate) {

    private var requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startApp()
            else {
                mActivity.hideSplashScreen = true
                showExitDialog(getString(R.string.err_notification_permission))
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!AppUtils.askForNotificationsPermission(requireContext(), requestPermissionLauncher))
            startApp()
    }

    private fun startApp() {
        findNavController().navigate(R.id.action_startFragment_to_routingFragment)
    }
}
