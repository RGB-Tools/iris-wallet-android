package com.iriswallet.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.iriswallet.R
import com.iriswallet.data.AppRepository
import com.iriswallet.databinding.FragmentMainBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppAuthenticationService
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppError
import com.iriswallet.utils.TAG

class MainFragment : MainBaseFragment<FragmentMainBinding>(FragmentMainBinding::inflate) {

    private lateinit var appAuthenticationService: AppAuthenticationService

    private lateinit var adapter: AssetListAdapter

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity.startConnectivityService()
        appAuthenticationService = AppAuthenticationService(this)
        activity
            ?.onBackPressedDispatcher
            ?.addCallback(
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

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.main, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.refreshMenu -> {
                            disableUI(showProgress = true)
                            viewModel.refreshAssets()
                            true
                        }
                        R.id.issueAssetMenu -> {
                            disableUI(showProgress = false)
                            findNavController()
                                .navigate(R.id.action_mainFragment_to_issueAssetFragment)
                            true
                        }
                        R.id.unspentListMenu -> {
                            disableUI(showProgress = false)
                            findNavController()
                                .navigate(R.id.action_mainFragment_to_bitcoinUnspentFragment)
                            true
                        }
                        R.id.helpPageMenu -> {
                            disableUI(showProgress = false)
                            findNavController()
                                .navigate(R.id.action_mainFragment_to_helpPageFragment)
                            true
                        }
                        R.id.settingsMenu -> {
                            disableUI(showProgress = false)
                            findNavController()
                                .navigate(R.id.action_mainFragment_to_settingsFragment)
                            true
                        }
                        else -> true
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        refreshListAdapter(viewModel.cachedAssets)
        if (viewModel.refreshingAssets) disableUI()

        mActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        mActivity.supportActionBar!!.show()
        binding.assetRV.layoutManager = LinearLayoutManager(activity)
        binding.mainReceiveBtn.setOnClickListener {
            disableUI(showProgress = false)
            findNavController().navigate(R.id.action_mainFragment_to_receiveAssetFragment)
        }
        binding.mainSwipeRefresh.setOnRefreshListener {
            disableUI()
            viewModel.refreshAssets()
        }
        viewModel.assets.observe(viewLifecycleOwner) {
            if (AppRepository.allowedFailure == null) {
                it.getContentIfNotHandled()?.let { response ->
                    if (!response.data.isNullOrEmpty()) refreshListAdapter(response.data)
                    else
                        handleError(response.error!!) {
                            toastError(R.string.err_refreshing_assets, response.error.message)
                        }
                }
            } else { // only at app start when allowFailures = true
                val error = AppError(AppRepository.allowedFailure!!)
                handleError(error) { toastError(R.string.err_refreshing_assets, error.message) }
                AppRepository.allowedFailure = null
            }
            if (!viewModel.refreshingAssets) enableUI()
            mActivity.hideSplashScreen = true
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

    override fun enableUI() {
        super.enableUI()
        adapter.isClickEnabled = true
        binding.mainReceiveBtn.isEnabled = true
        setLoader(false)
    }

    private fun disableUI(showProgress: Boolean = true) {
        mActivity.backEnabled = false
        if (showProgress) setLoader(true)
        adapter.isClickEnabled = false
    }

    private fun refreshListAdapter(assets: List<AppAsset>) {
        Log.d(TAG, "Refreshing asset list view...")
        adapter = AssetListAdapter(assets, viewModel, this)
        binding.assetRV.adapter = adapter
        adapter.notifyDataSetChanged()
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

    private fun setLoader(state: Boolean) {
        binding.mainSwipeRefresh.post { binding.mainSwipeRefresh.isRefreshing = state }
    }
}
