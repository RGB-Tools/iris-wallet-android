package com.iriswallet.ui

import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.iriswallet.R
import com.iriswallet.databinding.FragmentAssetDetailBinding
import com.iriswallet.utils.*
import org.rgbtools.TransferStatus

class AssetDetailFragment :
    MainBaseFragment<FragmentAssetDetailBinding>(FragmentAssetDetailBinding::inflate),
    TextureView.SurfaceTextureListener {

    private lateinit var adapter: TransferListAdapter

    lateinit var asset: AppAsset

    private var refreshing: Boolean = false

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var textureView: TextureView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asset = viewModel.viewingAsset!!
        setHeader()

        binding.detailReceiveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_assetDetailFragment_to_receiveAssetFragment)
        }
        binding.detailSendBtn.setOnClickListener {
            findNavController().navigate(R.id.action_assetDetailFragment_to_sendAssetFragment)
        }

        binding.detailCopyAssetIdBtn.setOnClickListener {
            toClipboard(AppConstants.assetIdClipLabel, asset.id)
        }

        binding.detailTransferRV.layoutManager = LinearLayoutManager(mActivity)

        adapter = TransferListAdapter(ArrayList(asset.transfers.reversed()), viewModel, this)
        binding.detailTransferRV.adapter = adapter

        binding.detailSwipeRefresh.setOnRefreshListener { refreshAsset() }

        viewModel.asset.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (!refreshing) return@let
                if (response.data != null) {
                    asset = response.data
                    redrawAssetDetails()
                } else {
                    handleError(response.error!!) {
                        toastError(R.string.err_refreshing_asset_details, response.error.message)
                    }
                }
                refreshing = false
                enableUI()
            }
        }
        mActivity.services.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { if (!refreshing) enableUI() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (refreshing) disableUI() else enableUI()
        redrawAssetDetails()
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer != null) {
            runCatching {
                mediaPlayer!!.stop()
                mediaPlayer!!.release()
            }
        }
    }

    override fun onDestroy() {
        viewModel.viewingAsset = null
        super.onDestroy()
    }

    override fun enableUI() {
        super.enableUI()
        setLoader(false)
        binding.detailSendBtn.isEnabled = enableSendBtn()
        binding.detailReceiveBtn.isEnabled = true
    }

    private fun disableUI() {
        setLoader(true)
    }

    private fun refreshAsset() {
        refreshing = true
        disableUI()
        viewModel.refreshAssetDetail(asset)
    }

    private fun enableSendBtn(): Boolean {
        if (
            asset.settledBalance == 0UL ||
                (!asset.bitcoin() &&
                    asset.transfers.size == 1 &&
                    asset.transfers[0].status != TransferStatus.SETTLED)
        ) {
            return false
        }
        if (mActivity.serviceMap != null) {
            val enable = mActivity.serviceMap!![AppContainer.electrumURL] == true
            if (enable && !asset.bitcoin())
                return mActivity.serviceMap!![AppContainer.proxyURL] == true
            return enable
        }
        return true
    }

    private fun redrawAssetDetails() {
        binding.detailSendBtn.isEnabled = enableSendBtn()

        if (asset.bitcoin())
            binding.detailBalanceLL.detailTickerTV.text = getString(R.string.bitcoin_unit)
        else binding.detailBalanceLL.detailTickerTV.text = asset.ticker
        binding.detailBalanceLL.detailBalanceTV.text = asset.totalBalance.toString()

        adapter.updateData(ArrayList(asset.transfers.reversed()))
    }

    private fun setHeader() {
        val media = asset.media
        var showMedia = false
        if (asset.type == AppAssetType.RGB21 && media != null) {
            showMedia = true
            when (media.mime) {
                MimeType.IMAGE -> {
                    val collectibleImg = Drawable.createFromPath(media.filePath)
                    binding.detailCollectibleImg.setImageDrawable(collectibleImg)
                    binding.detailCollectibleImg.visibility = View.VISIBLE
                }
                MimeType.VIDEO -> {
                    binding.detailCollectibleCard.visibility = View.VISIBLE
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(media.filePath)
                    val videoWidth =
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toInt()
                            ?: 0
                    val videoHeight =
                        retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toInt()
                            ?: 0
                    retriever.release()
                    val collectibleVideoThumbnail =
                        AppUtils.getVideoThumbnail(media.filePath, videoWidth, videoHeight)
                    binding.detailCollectibleImg.setImageBitmap(collectibleVideoThumbnail)
                    textureView =
                        LayoutInflater.from(context)
                            .inflate(R.layout.video, binding.detailCollectibleCard, false)
                            as TextureView
                    textureView.surfaceTextureListener = this
                    binding.detailCollectibleCard.addView(textureView)
                }
                MimeType.UNSUPPORTED -> {
                    binding.detailCollectibleImg.visibility = View.GONE
                    showMedia = false
                    Log.i(TAG, "Unsupported media file")
                }
            }
        }
        binding.detailCollectibleCard.visibility = if (showMedia) View.VISIBLE else View.GONE

        if (asset.bitcoin()) {
            binding.detailAssetIdBox.visibility = View.GONE
            binding.detailIDTV.visibility = View.GONE
        } else {
            binding.detailIDTV.text = asset.id
        }
    }

    private fun setLoader(state: Boolean) {
        binding.detailSwipeRefresh.post { binding.detailSwipeRefresh.isRefreshing = state }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(asset.media!!.filePath)
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setSurface(Surface(surface))
            mediaPlayer?.isLooping = true
            mediaPlayer?.setOnPreparedListener { player -> player?.start() }
            mediaPlayer?.setOnErrorListener { _, _, _ -> false }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}
