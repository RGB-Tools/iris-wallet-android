package com.iriswallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.iriswallet.R
import com.iriswallet.databinding.FungibleItemBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils

class FungiblesAdapter(
    private val dataSet: List<AppAsset>,
    private val viewModel: MainViewModel,
    private val fragment: FungiblesFragment,
    private val fungibleSize: Int,
) : RecyclerView.Adapter<FungiblesAdapter.ViewHolder>() {
    private var isClickEnabled = true

    class ViewHolder(val binding: FungibleItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val bind =
            FungibleItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(bind)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.itemView.context
        val asset = dataSet[position]

        viewHolder.binding.assetNameTV.text = asset.name
        if (asset.bitcoin()) {
            viewHolder.binding.assetBalanceTV.text = asset.totalBalance.toString()
            viewHolder.binding.assetTickerTV.text = context.getString(R.string.bitcoin_unit)
            viewHolder.binding.assetIDTV.text = asset.ticker
            viewHolder.binding.fungibleImg.setImageDrawable(
                AppCompatResources.getDrawable(context, AppContainer.bitcoinLogoID)
            )
        } else {
            viewHolder.binding.assetTickerTV.text = asset.ticker
            viewHolder.binding.assetBalanceTV.text = asset.totalBalance.toString()
            viewHolder.binding.assetIDTV.text = asset.id
            viewHolder.binding.fungibleImg.setImageDrawable(
                AppUtils.getAssetIdIdenticon(asset.id, fungibleSize, fungibleSize)
            )
        }
        viewHolder.itemView.setOnClickListener {
            if (asset.fromFaucet) {
                Toast.makeText(
                        fragment.activity,
                        fragment.getString(R.string.pending_asset_faucet),
                        Toast.LENGTH_LONG
                    )
                    .show()
            } else if (isClickEnabled) {
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
