package com.iriswallet.ui

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.iriswallet.R
import com.iriswallet.databinding.CollectibleItemBinding
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.MimeType
import com.iriswallet.utils.TAG

class CollectiblesAdapter(
    private val dataSet: List<AppAsset>,
    private val viewModel: MainViewModel,
    private val fragment: CollectiblesFragment
) : RecyclerView.Adapter<CollectiblesAdapter.ViewHolder>() {
    private var isClickEnabled = true

    class ViewHolder(val binding: CollectibleItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val bind =
            CollectibleItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(bind)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val asset = dataSet[position]
        viewHolder.binding.collectibleName.text = asset.name

        val media = asset.media
        var showIdenticon = true
        if (media != null) {
            showIdenticon = false
            when (media.mime) {
                MimeType.IMAGE -> {
                    val collectibleImg = Drawable.createFromPath(media.filePath)
                    viewHolder.binding.collectibleImg.setImageDrawable(collectibleImg)
                }
                MimeType.VIDEO -> {
                    val collectibleVideoThumbnail =
                        AppUtils.getVideoThumbnail(media.filePath, 120, 120)
                    if (collectibleVideoThumbnail == null) Log.e(TAG, "Unhandled error")
                    else viewHolder.binding.collectibleImg.setImageBitmap(collectibleVideoThumbnail)
                }
                MimeType.UNSUPPORTED -> {
                    Log.i(TAG, "Unsupported media file")
                    showIdenticon = true
                }
            }
        }

        if (showIdenticon) {
            val collectibleMedia =
                AppUtils.getAssetIdIdenticon(asset.id, fragment.mActivity.windowManager)
            viewHolder.binding.collectibleImg.setImageDrawable(collectibleMedia)
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
