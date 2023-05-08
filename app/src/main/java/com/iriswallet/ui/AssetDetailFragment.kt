package com.iriswallet.ui

import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.databinding.FragmentAssetDetailBinding
import com.iriswallet.utils.*
import java.io.File

class AssetDetailFragment :
    MainBaseFragment<FragmentAssetDetailBinding>(FragmentAssetDetailBinding::inflate),
    TextureView.SurfaceTextureListener {

    private lateinit var adapter: TransferListAdapter

    lateinit var asset: AppAsset

    private var refreshing: Boolean = false

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var textureView: TextureView

    private var requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showDownloadedNotification()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.asset_detail, menu)

                    val hideAssetMenuItem = menu.findItem(R.id.hideAssetMenu)
                    val title = if (asset.hidden) R.string.unhide_asset else R.string.hide_asset
                    hideAssetMenuItem.title = getString(title)

                    if (asset.bitcoin()) menu.findItem(R.id.assetMetadataMenu).isVisible = false

                    menu.findItem(R.id.downloadMediaMenu).isVisible = asset.media != null
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.refreshAssetMenu -> {
                            refreshAsset()
                            true
                        }
                        R.id.assetMetadataMenu -> {
                            findNavController()
                                .navigate(R.id.action_assetDetailFragment_to_assetMetadataFragment)
                            true
                        }
                        R.id.downloadMediaMenu -> {
                            val mediaFile = File(asset.media!!.filePath).toURI().toString()
                            AppUtils.saveFileToDownloads(
                                requireContext(),
                                mediaFile,
                                AppConstants.rgbDownloadMediaFileName.format(asset.id),
                                asset.media!!.mimeString
                            )
                            showDownloadedNotification()
                            Toast.makeText(
                                    activity,
                                    getString(R.string.downloaded_media),
                                    Toast.LENGTH_LONG
                                )
                                .show()
                            true
                        }
                        R.id.hideAssetMenu -> {
                            val msg =
                                if (asset.hidden) R.string.confirm_unhide_asset
                                else R.string.confirm_hide_asset
                            val posBtn = if (asset.hidden) R.string.unhide else R.string.hide
                            AlertDialog.Builder(requireContext())
                                .setMessage(getString(msg))
                                .setPositiveButton(getString(posBtn)) { _, _ ->
                                    disableUI(disableBack = true)
                                    viewModel.handleAssetVisibility(asset.id)
                                }
                                .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
                                .create()
                                .show()
                            true
                        }
                        android.R.id.home -> {
                            mActivity.onSupportNavigateUp()
                            true
                        }
                        else -> true
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        binding.detailTransferRV.layoutManager = LinearLayoutManager(mActivity)

        binding.detailReceiveBtn.setOnClickListener {
            findNavController().navigate(R.id.action_assetDetailFragment_to_receiveAssetFragment)
        }
        binding.detailSendBtn.setOnClickListener {
            if (asset.spendableBalance == 0UL)
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.no_spendable_balance))
                    .setPositiveButton(getString(R.string.OK)) { _, _ -> }
                    .create()
                    .show()
            else findNavController().navigate(R.id.action_assetDetailFragment_to_sendAssetFragment)
        }

        binding.detailCopyAssetIdBtn.setOnClickListener {
            toClipboard(AppConstants.assetIdClipLabel, asset.id)
        }

        binding.detailSwipeRefresh.setOnRefreshListener { refreshAsset() }

        viewModel.asset.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                if (response.requestID != asset.id) return@let
                if (response.data != null) {
                    asset = response.data
                    redrawAssetDetails()
                } else {
                    handleError(response.error!!) {
                        toastMsg(R.string.err_refreshing_asset_details, response.error.message)
                    }
                }
                refreshing = false
                enableUI()
            }
        }

        viewModel.hidden.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { response ->
                enableUI()
                if (response.data != null) {
                    if (response.data && !SharedPreferencesManager.showHiddenAssets)
                        findNavController().popBackStack()
                    else requireActivity().invalidateOptionsMenu()
                } else {
                    val msg =
                        if (asset.hidden) R.string.err_unhiding_asset else R.string.err_hiding_asset
                    handleError(response.error!!) { toastMsg(msg, response.error.message) }
                }
            }
        }

        mActivity.services.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let {
                if (!refreshing) binding.detailSendBtn.isEnabled = enableSendBtn()
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        asset = viewModel.viewingAsset!!
        adapter = TransferListAdapter(ArrayList(asset.transfers.reversed()), viewModel, this)
        binding.detailTransferRV.adapter = adapter
        setHeader()
        redrawAssetDetails()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.refreshingAsset) {
            refreshing = true
            disableUI()
        }
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

    private fun disableUI(disableBack: Boolean = false) {
        if (disableBack) mActivity.backEnabled = false
        setLoader(true)
        binding.detailReceiveBtn.isEnabled = false
        binding.detailSendBtn.isEnabled = false
    }

    private fun refreshAsset() {
        refreshing = true
        disableUI()
        viewModel.refreshAssetDetail(asset)
    }

    private fun enableSendBtn(): Boolean {
        if (mActivity.serviceMap != null) {
            val enable = mActivity.serviceMap!![AppContainer.electrumURL] == true
            if (enable && !asset.bitcoin())
                return mActivity.serviceMap!![AppContainer.proxyURL] == true
            return enable
        }
        return true
    }

    private fun redrawAssetDetails() {
        binding.detailBalanceLL.detailBalanceTV.text = asset.totalBalance.toString()
        adapter.updateData(ArrayList(asset.transfers.reversed()))
    }

    private fun setHeader() {
        val media = asset.media
        var showMedia = false
        if (asset.type == AppAssetType.RGB121) {
            if (media != null) {
                showMedia = true
                when (media.mime) {
                    MimeType.IMAGE -> {
                        val collectibleImg = Drawable.createFromPath(media.filePath)
                        binding.detailCollectibleImg.setImageDrawable(collectibleImg)
                        binding.detailCollectibleImg.visibility = View.VISIBLE
                        binding.detailAssetFileCard.visibility = View.GONE
                    }
                    MimeType.VIDEO -> {
                        binding.detailAssetFileCard.visibility = View.GONE
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
                    MimeType.OTHER -> {
                        Log.i(TAG, "Media file without preview")
                        binding.detailCollectibleImg.visibility = View.GONE
                        binding.detailAssetFileCard.visibility = View.VISIBLE
                        showMedia = false
                    }
                }
            } else {
                binding.detailAssetMediaInfoTV.text = getString(R.string.asset_without_media)
            }
        } else {
            binding.detailAssetFileCard.visibility = View.GONE
        }
        binding.detailCollectibleCard.visibility = if (showMedia) View.VISIBLE else View.GONE

        if (asset.bitcoin()) {
            binding.detailAssetIdBox.visibility = View.GONE
            binding.detailIDTV.visibility = View.GONE
            binding.detailBalanceLL.detailTickerTV.text = getString(R.string.bitcoin_unit)
        } else {
            binding.detailIDTV.text = asset.id
            binding.detailBalanceLL.detailTickerTV.text = asset.ticker
        }
    }

    private fun setLoader(state: Boolean) {
        binding.detailSwipeRefresh.post { binding.detailSwipeRefresh.isRefreshing = state }
    }

    private fun showDownloadedNotification() {
        AppUtils.showDownloadedNotification(
            requireContext(),
            AppConstants.DOWNLOAD_MEDIA_NOTIFICATION_CHANNEL,
            AppConstants.DOWNLOAD_MEDIA_NOTIFICATION_ID,
            R.string.download_media_notification_channel_name,
            R.string.download_media_notification_channel_description,
            R.string.download_media_notification_title,
            R.string.download_media_notification_text,
            requestPermissionLauncher,
        )
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
