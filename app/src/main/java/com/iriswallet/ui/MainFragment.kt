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
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.tabs.TabLayoutMediator
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentMainBinding
import com.iriswallet.utils.AppAuthenticationService
import com.iriswallet.utils.AppAuthenticationServiceListener
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.TAG

class MainFragment :
    MainBaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate),
    AppAuthenticationServiceListener {

    private lateinit var appAuthenticationService: AppAuthenticationService

    private var doubleBackToExitPressedOnce = false

    private var backupSnackbarDismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appAuthenticationService = AppAuthenticationService(this)

        mActivity.startConnectivityService()

        mActivity.onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!mActivity.backEnabled) {
                        Toast.makeText(
                                activity,
                                getString(R.string.back_disabled),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                        return
                    }
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

        mActivity.binding.navView.menu.findItem(R.id.backupFragment).setOnMenuItemClickListener {
            launchBackupFragment()
            true
        }
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

        viewModel.offlineAssets.observe(viewLifecycleOwner) { mActivity.hideSplashScreen = true }

        binding.mainReceiveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_receiveAssetFragment)
        }

        handleBackupBannerVisibility()
        mActivity.services.observe(viewLifecycleOwner) {
            handleConnectionBannerVisibility(it.peekContent())
            it.getContentIfNotHandled()?.let { serviceMap ->
                handleConnectionBannerVisibility(serviceMap)
            }
        }
    }

    private fun handleBackupBannerVisibility() {
        if (!SharedPreferencesManager.backupConfigured && !backupSnackbarDismissed) {
            Log.d(TAG, "Showing backup banner...")
            binding.mainBackupConfigureBtn.visibility = View.VISIBLE
            binding.mainBackupConfigureBtn.isEnabled = true
            binding.mainBackupConfigureBtn.setOnClickListener { launchBackupFragment() }
            val swipeDismissBehavior = SwipeDismissBehavior<View>()
            swipeDismissBehavior.setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
            swipeDismissBehavior.listener =
                object : SwipeDismissBehavior.OnDismissListener {
                    override fun onDismiss(view: View?) {
                        backupSnackbarDismissed = true
                    }

                    override fun onDragStateChanged(state: Int) {
                        when (state) {
                            SwipeDismissBehavior.STATE_DRAGGING -> {
                                binding.mainBackupCardView.isDragged = true
                            }
                            SwipeDismissBehavior.STATE_SETTLING -> {
                                binding.mainBackupCardView.isDragged = true
                            }
                            SwipeDismissBehavior.STATE_IDLE -> {
                                binding.mainBackupCardView.isDragged = false
                                binding.mainBackupCooL.visibility = View.GONE
                            }
                            else -> {}
                        }
                    }
                }
            (binding.mainBackupCardView.layoutParams as CoordinatorLayout.LayoutParams).behavior =
                swipeDismissBehavior
            binding.mainBackupCooL.visibility = View.VISIBLE
        } else binding.mainBackupCooL.visibility = View.GONE
    }

    private fun handleConnectionBannerVisibility(serviceMap: Map<String, Boolean>) {
        if (serviceMap.containsValue(false)) {
            Log.d(TAG, "Showing connection error banner...")
            (binding.mainConnectionCardView.layoutParams as CoordinatorLayout.LayoutParams)
                .behavior = null
            binding.mainConnectionCardView.isEnabled = true
            binding.mainConnectionCardView.isCheckable = true
            binding.mainConnectionCardView.toggle()
            (binding.mainConnectionCardView.layoutParams as CoordinatorLayout.LayoutParams)
                .setMargins(0, 0, 0, 0)
            binding.mainConnectionCardView.alpha = 1.0f
            binding.mainConnectionCardView.requestLayout()
            binding.mainConnectionCooL.visibility = View.VISIBLE
        } else binding.mainConnectionCooL.visibility = View.GONE
    }

    override fun authenticated(requestCode: String) {
        mActivity.binding.drawerLayout.close()
        findNavController().navigate(R.id.action_mainFragment_to_backupFragment)
    }

    override fun handleAuthError(requestCode: String, errorExtraInfo: String?, errCode: Int?) {
        toastMsg(R.string.err_accessing_backup_page, errorExtraInfo)
    }

    private fun launchBackupFragment() {
        if (SharedPreferencesManager.pinActionsConfigured) appAuthenticationService.auth()
        else authenticated()
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
