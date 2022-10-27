package com.iriswallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.iriswallet.R
import com.iriswallet.databinding.AssetListItemBinding
import com.iriswallet.utils.AppAsset

class AssetListAdapter(
    private val dataSet: List<AppAsset>,
    private val viewModel: MainViewModel,
    private val fragment: MainFragment
) : RecyclerView.Adapter<AssetListAdapter.ViewHolder>() {
    var isClickEnabled = true

    class ViewHolder(val binding: AssetListItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val bind =
            AssetListItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(bind)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val asset = dataSet[position]
        viewHolder.binding.assetNameTV.text = asset.name
        if (asset.bitcoin()) {
            viewHolder.binding.assetBalanceTV.text = asset.totalBalance.toString()
            viewHolder.binding.assetTickerTV.text =
                viewHolder.itemView.context.getString(R.string.bitcoin_unit)
            viewHolder.binding.assetIDTV.text = asset.ticker
        } else {
            viewHolder.binding.assetTickerTV.text = asset.ticker
            viewHolder.binding.assetBalanceTV.text = asset.totalBalance.toString()
            viewHolder.binding.assetIDTV.text = asset.id
        }
        viewHolder.itemView.setOnClickListener {
            if (isClickEnabled) {
                viewModel.viewingAsset = asset
                fragment
                    .findNavController()
                    .navigate(
                        MainFragmentDirections.actionMainFragmentToAssetDetailFragment(asset.name)
                    )
            }
        }
    }

    override fun getItemCount() = dataSet.size
}
