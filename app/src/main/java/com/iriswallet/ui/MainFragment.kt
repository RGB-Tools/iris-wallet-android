package com.iriswallet.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.iriswallet.R
import com.iriswallet.databinding.FragmentMainBinding
import com.iriswallet.utils.AppAuthenticationService
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.TAG

class MainFragment : MainBaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {

    private lateinit var appAuthenticationService: AppAuthenticationService

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getOfflineAssets()

        mActivity.startConnectivityService()
        appAuthenticationService = AppAuthenticationService(this)

        mActivity.onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!mActivity.backEnabled) return
                    if (doubleBackToExitPressedOnce) {
                        activity?.finish()
                        return
                    }
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(
                            activity,
                            getString(R.string.back_again_to_exit),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                    Handler(Looper.getMainLooper())
                        .postDelayed(
                            { doubleBackToExitPressedOnce = false },
                            AppConstants.waitDoubleBackTime
                        )
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val tabLayout = binding.mainTabLayout
        val viewPager = binding.mainViewPager
        viewPager.adapter = ChildFragmentStateAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                when (position) {
                    0 -> tab.text = getString(R.string.fungibles)
                    1 -> tab.text = getString(R.string.collectibles)
                }
            }
            .attach()

        viewModel.offlineAssets.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data.isNullOrEmpty())
                    handleError(response.error!!) {
                        showExitDialog(getString(R.string.err_getting_offline_assets))
                    }
                else {
                    mActivity.hideSplashScreen = true
                    viewModel.refreshAssets()
                }
            }
        }

        binding.mainReceiveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_receiveAssetFragment)
        }

        mActivity.services.observe(viewLifecycleOwner) {
            if (it.peekContent().containsValue(false)) showConnectionIssuesBanner()
            else binding.mainCooL.visibility = View.GONE
            it.getContentIfNotHandled()?.let { serviceMap ->
                if (serviceMap.containsValue(false)) showConnectionIssuesBanner()
                else binding.mainCooL.visibility = View.GONE
            }
        }
    }

    private fun showConnectionIssuesBanner() {
        Log.d(TAG, "Showing connection error banner...")
        (binding.mainCardView.layoutParams as CoordinatorLayout.LayoutParams).behavior = null
        binding.mainCardViewTV.text = getString(R.string.connection_err)
        binding.mainCardView.visibility = View.VISIBLE
        binding.mainCardView.isEnabled = true
        binding.mainCardView.isCheckable = true
        binding.mainCardView.toggle()
        (binding.mainCardView.layoutParams as CoordinatorLayout.LayoutParams).setMargins(0, 0, 0, 0)
        binding.mainCardView.alpha = 1.0f
        binding.mainCardView.requestLayout()
        binding.mainCooL.visibility = View.VISIBLE
    }
}

class ChildFragmentStateAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FungiblesFragment()
            else -> CollectiblesFragment()
        }
    }
}
