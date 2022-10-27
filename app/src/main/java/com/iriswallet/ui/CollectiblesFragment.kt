package com.iriswallet.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.iriswallet.R
import com.iriswallet.databinding.FragmentCollectiblesBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppAssetType
import com.iriswallet.utils.TAG

class CollectiblesFragment :
    MainBaseFragment<FragmentCollectiblesBinding>(FragmentCollectiblesBinding::inflate) {
    private lateinit var adapter: CollectiblesAdapter

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
                            if (viewModel.refreshingAssets) return true
                            disableUI()
                            viewModel.refreshAssets()
                            true
                        }
                        else -> true
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        refreshListAdapter(viewModel.cachedCollectibles)

        binding.collectiblesRV.layoutManager = GridLayoutManager(activity, 2)

        binding.collectiblesSwipeRefresh.setOnRefreshListener {
            disableUI()
            viewModel.refreshAssets()
        }
        viewModel.refreshedCollectibles.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) refreshListAdapter(response.data)
            }
            if (!viewModel.refreshingAssets) enableUI()
        }
        viewModel.refreshedAssets.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.error != null || response.data.isNullOrEmpty()) {
                    handleError(response.error!!) {
                        toastError(R.string.err_refreshing_assets, response.error.message)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.refreshingAssets) disableUI()
    }

    override fun onPause() {
        super.onPause()
        runCatching { setLoader(false) }
    }

    override fun enableUI() {
        super.enableUI()
        setLoader(false)
    }

    private fun disableUI(showProgress: Boolean = true) {
        mActivity.backEnabled = false
        binding.collectiblesSwipeRefresh.isEnabled = false
        if (showProgress) setLoader(true)
    }

    private fun refreshListAdapter(assets: List<AppAsset>) {
        val collectibles = assets.filter { it.type == AppAssetType.RGB21 }
        Log.d(TAG, "Refreshing collectibles view with ${collectibles.size} assets...")
        adapter = CollectiblesAdapter(collectibles, viewModel, this)
        binding.collectiblesRV.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun setLoader(state: Boolean) {
        binding.collectiblesSwipeRefresh.post {
            runCatching { binding.collectiblesSwipeRefresh.isRefreshing = state }
        }
    }
}
