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
import androidx.recyclerview.widget.RecyclerView
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentCollectiblesBinding
import com.iriswallet.utils.TAG

class CollectiblesFragment :
    MainBaseFragment<FragmentCollectiblesBinding>(FragmentCollectiblesBinding::inflate) {
    private lateinit var adapter: CollectiblesAdapter

    private var collectibleSize: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectibleSize = resources.getDimensionPixelSize(R.dimen.collectible_size)
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

        binding.collectiblesRV.layoutManager = GridLayoutManager(activity, 2)

        binding.collectiblesSwipeRefresh.setOnRefreshListener {
            disableUI(swipeRefreshHandledAutomatically = true)
            viewModel.refreshAssets()
        }
        viewModel.refreshedCollectibles.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                enableUI()
                if (response.error == null) refreshListAdapter()
            }
        }
        viewModel.refreshedAssets.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.error != null || response.data.isNullOrEmpty()) {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_refreshing_assets, response.error.message)
                        enableUI()
                    }
                }
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        refreshListAdapter()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.refreshingAssets) disableUI()
    }

    override fun onPause() {
        super.onPause()
        runCatching { binding.collectiblesSwipeRefresh.isRefreshing = false }
    }

    override fun enableUI() {
        super.enableUI()
        binding.collectiblesSwipeRefresh.isEnabled = true
        binding.collectiblesSwipeRefresh.isRefreshing = false
    }

    private fun disableUI(swipeRefreshHandledAutomatically: Boolean = false) {
        mActivity.backEnabled = false
        if (!swipeRefreshHandledAutomatically) {
            binding.collectiblesSwipeRefresh.isEnabled = false
            binding.collectiblesSwipeRefresh.isRefreshing = true
        }
    }

    private fun refreshListAdapter() {
        val assets = viewModel.cachedCollectibles
        val visibleAssets =
            if (!SharedPreferencesManager.showHiddenAssets) {
                val assetsToShow = assets.filter { !it.hidden }
                if (SharedPreferencesManager.hideExhaustedAssets) {
                    assetsToShow.filter { it.totalBalance > 0UL }
                } else {
                    assetsToShow
                }
            } else assets
        Log.d(
            TAG,
            "Refreshing collectibles view with ${visibleAssets.size} (out of ${assets.size}) assets..."
        )
        adapter = CollectiblesAdapter(visibleAssets, viewModel, this, collectibleSize!!)
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.collectiblesRV.adapter = adapter
        adapter.notifyDataSetChanged()
    }
}
