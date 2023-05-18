package com.iriswallet.ui

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iriswallet.R
import com.iriswallet.databinding.FragmentBitcoinUnspentBinding

class BitcoinUnspentFragment :
    MainBaseFragment<FragmentBitcoinUnspentBinding>(FragmentBitcoinUnspentBinding::inflate) {

    private lateinit var adapter: BitcoinUnspentAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLoader(true)
        binding.unspentRV.layoutManager = LinearLayoutManager(view.context)
        binding.unspentSwipeRefresh.setOnRefreshListener {
            disableUI()
            viewModel.getBitcoinUnspents()
        }
        if (!::adapter.isInitialized) adapter = BitcoinUnspentAdapter(listOf())
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.unspentRV.adapter = adapter

        viewModel.unspents.observe(viewLifecycleOwner) {
            val prevResponse = it.peekContent()
            if (it.hasBeenHandled && prevResponse.data?.isNotEmpty() == true) {
                adapter = BitcoinUnspentAdapter(prevResponse.data)
                binding.unspentRV.adapter = adapter
            }
            it.getContentIfNotHandled()?.let { response ->
                if (response.data != null) { // empty list allowed (no unspents)
                    adapter = BitcoinUnspentAdapter(response.data)
                    binding.unspentRV.adapter = adapter
                    enableUI()
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_listing_unspents, response.error.message)
                        enableUI()
                        findNavController().popBackStack()
                    }
                }
            }
            setLoader(false)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.getBitcoinUnspents()
    }

    override fun enableUI() {
        super.enableUI()
        setLoader(false)
    }

    private fun disableUI() {
        setLoader(true)
    }

    private fun setLoader(state: Boolean) {
        binding.unspentSwipeRefresh.post { binding.unspentSwipeRefresh.isRefreshing = state }
    }
}
