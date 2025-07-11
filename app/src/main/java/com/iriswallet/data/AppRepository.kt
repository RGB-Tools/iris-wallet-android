package com.iriswallet.data

import android.util.Log
import com.iriswallet.R
import com.iriswallet.data.db.HiddenAsset
import com.iriswallet.data.db.RgbPendingAsset
import com.iriswallet.data.retrofit.Distribution
import com.iriswallet.data.retrofit.DistributionMode
import com.iriswallet.data.retrofit.RetrofitModule.assetCertificationService
import com.iriswallet.data.retrofit.RgbAsset
import com.iriswallet.data.retrofit.RgbAssetGroup
import com.iriswallet.utils.AppAsset
import com.iriswallet.utils.AppAssetType
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppException
import com.iriswallet.utils.AppTransfer
import com.iriswallet.utils.Receiver
import com.iriswallet.utils.RgbFaucet
import com.iriswallet.utils.TAG
import com.iriswallet.utils.UTXO
import java.io.File
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import org.rgbtools.RgbLibException
import org.rgbtools.TransactionType

object AppRepository {

    internal var isCacheDirty: Boolean = false

    val appAssets: MutableList<AppAsset> = mutableListOf()

    private var rgbPendingAssetIDs: MutableList<String> = mutableListOf()

    fun getCachedFungibles(): List<AppAsset> {
        return appAssets.filter { listOf(AppAssetType.BITCOIN, AppAssetType.NIA).contains(it.type) }
    }

    fun getCachedCollectibles(): List<AppAsset> {
        return appAssets.filter { it.type == AppAssetType.CFA }
    }

    internal fun getCachedAsset(assetID: String): AppAsset? {
        return appAssets.find { it.id == assetID }
    }

    private fun updateBitcoinAsset(refresh: Boolean = true) {
        Log.d(TAG, "Updating bitcoin asset with refresh $refresh...")

        if (appAssets.isEmpty()) {
            appAssets.add(
                AppAsset(
                    AppAssetType.BITCOIN,
                    AppConstants.BITCOIN_ASSET_ID,
                    AppContainer.bitcoinAssetName,
                    false,
                    ticker = AppContainer.bitcoinAssetTicker,
                )
            )
        }
        val bitcoinAsset = appAssets[0]

        if (refresh) RgbRepository.sync()

        val transfers = RgbRepository.listVanillaTransfers().toMutableList()
        val autoTXs =
            RgbRepository.listTransactions(refresh)
                .filter { it.transactionType == TransactionType.CREATE_UTXOS }
                .map { tx -> tx.txid }
        transfers.filter { it.txid in autoTXs }.forEach { it.internal = true }
        bitcoinAsset.transfers = transfers

        val balance = RgbRepository.getVanillaBalance()
        Log.d(TAG, "Vanilla balance: $balance")
        bitcoinAsset.spendableBalance = balance.vanilla.spendable
        bitcoinAsset.settledBalance = balance.vanilla.settled
        bitcoinAsset.totalBalance = balance.vanilla.future
    }

    private fun updateRGBAsset(
        asset: AppAsset,
        refresh: Boolean = true,
        updateTransfers: Boolean = true,
        updateTransfersFilter: String? = null,
    ) {
        Log.d(TAG, "Updating RGB asset (${asset.id})...")

        var callListTransfers = updateTransfers
        if (refresh)
            runCatching {
                    callListTransfers = RgbRepository.refresh(asset) || callListTransfers
                    val balance = RgbRepository.getBalance(asset.id)
                    asset.spendableBalance = balance.spendable
                    asset.settledBalance = balance.settled
                    asset.totalBalance = balance.future
                }
                .onFailure {
                    if (rgbPendingAssetIDs.contains(asset.id)) {
                        return
                    }
                    throw it
                }

        if (callListTransfers) {
            if (updateTransfersFilter == asset.id || updateTransfersFilter == null)
                asset.transfers = RgbRepository.listTransfers(asset)
        }
    }

    private fun updateRGBAssets(
        refresh: Boolean = true,
        updateTransfers: Boolean = true,
        updateTransfersFilter: String? = null,
        firstAppRefresh: Boolean = false,
    ) {
        Log.d(TAG, "Updating RGB assets...")
        if (refresh && !firstAppRefresh) RgbRepository.refresh()
        val rgbAssets = RgbRepository.listAssets()
        for (rgbAsset in rgbAssets) {
            var nextUpdateTransfers = updateTransfers
            var assetToUpdate = getCachedAsset(rgbAsset.id)
            if (assetToUpdate == null) {
                assetToUpdate = rgbAsset
                appAssets.add(rgbAsset)
                nextUpdateTransfers = true
            } else if (rgbPendingAssetIDs.contains(assetToUpdate.id)) {
                appAssets.remove(assetToUpdate)
                removeRgbPendingAsset(assetToUpdate.id)
                assetToUpdate = rgbAsset
                appAssets.add(rgbAsset)
                nextUpdateTransfers = true
            } else {
                assetToUpdate.spendableBalance = rgbAsset.spendableBalance
                assetToUpdate.settledBalance = rgbAsset.settledBalance
                assetToUpdate.totalBalance = rgbAsset.totalBalance
            }

            updateRGBAsset(
                assetToUpdate,
                refresh = firstAppRefresh,
                updateTransfers = nextUpdateTransfers,
                updateTransfersFilter = updateTransfersFilter,
            )
        }
        if (firstAppRefresh) {
            val changed = RgbRepository.refresh()
            if (!changed) return
            val updatedRgbAssets = RgbRepository.listAssets()
            for (rgbAsset in updatedRgbAssets) {
                var assetToUpdate = getCachedAsset(rgbAsset.id)
                if (assetToUpdate == null) {
                    assetToUpdate = rgbAsset
                    appAssets.add(rgbAsset)
                } else if (rgbPendingAssetIDs.contains(assetToUpdate.id)) {
                    appAssets.remove(assetToUpdate)
                    removeRgbPendingAsset(assetToUpdate.id)
                    assetToUpdate = rgbAsset
                    appAssets.add(rgbAsset)
                } else {
                    continue
                }
                updateRGBAsset(
                    assetToUpdate,
                    refresh = false,
                    updateTransfers = true,
                    updateTransfersFilter = updateTransfersFilter,
                )
            }
        }
    }

    private fun removeRgbPendingAsset(rgbPendingAssetID: String) {
        AppContainer.db.rgbPendingAssetDao().deleteRgbPendingAsset(rgbPendingAssetID)
        rgbPendingAssetIDs.remove(rgbPendingAssetID)
    }

    private fun checkMaxAssets() {
        if (appAssets.size >= AppConstants.MAX_ASSETS)
            throw AppException(AppContainer.appContext.getString(R.string.reached_max_assets))
    }

    private fun createUTXOs(e: Exception) {
        Log.d(TAG, "Creating UTXOs because: ${e.message}")
        try {
            Log.d(TAG, "Calling create UTXOs...")
            val newUTXOs = RgbRepository.createUTXOs()
            Log.d(TAG, "Created $newUTXOs UTXOs")
        } catch (e: RgbLibException) {
            when (e) {
                is RgbLibException.InsufficientBitcoins -> {
                    throw AppException(
                        AppContainer.appContext.getString(
                            R.string.insufficient_bitcoins_for_rgb,
                            AppConstants.SATS_FOR_RGB.toString(),
                        )
                    )
                }
                is RgbLibException.MinFeeNotMet -> {
                    Log.d(TAG, "Insufficient fees: $e")
                    throw AppException(
                        AppContainer.appContext.getString(R.string.insufficient_fees)
                    )
                }
                else -> throw e
            }
        }
    }

    private fun <T> handleMissingFunds(callback: () -> T): T {
        return try {
            callback()
        } catch (e: RgbLibException) {
            when (e) {
                is RgbLibException.InsufficientBitcoins -> {
                    throw AppException(
                        AppContainer.appContext.getString(
                            R.string.insufficient_bitcoins_for_rgb,
                            AppConstants.SATS_FOR_RGB.toString(),
                        )
                    )
                }
                is RgbLibException.InsufficientAllocationSlots -> {
                    createUTXOs(e)
                    updateBitcoinAsset()
                    callback()
                }
                is RgbLibException.InvalidRecipientId,
                is RgbLibException.InvalidRecipientNetwork ->
                    throw AppException(
                        AppContainer.appContext.getString(R.string.invalid_recipient_id)
                    )
                is RgbLibException.RecipientIdAlreadyUsed ->
                    throw AppException(
                        AppContainer.appContext.getString(R.string.blinded_utxo_already_used)
                    )
                else -> throw e
            }
        }
    }

    private fun startRGBReceiving(
        asset: AppAsset?,
        blinded: Boolean = true,
        expirationSeconds: UInt? = null,
    ): Receiver {
        var updateTransfers = true
        if (asset == null) {
            checkMaxAssets()
            updateTransfers = false
        }
        val blindedData =
            RgbRepository.getReceiveData(
                asset?.id,
                expirationSeconds ?: AppConstants.RGB_BLIND_DURATION,
                blinded = blinded,
            )
        runCatching {
                updateRGBAssets(
                    refresh = false,
                    updateTransfers = updateTransfers,
                    updateTransfersFilter = asset?.id,
                )
            }
            .onFailure { isCacheDirty = true }
        return Receiver(blindedData.invoice, AppConstants.RGB_BLIND_DURATION, false)
    }

    private fun initiateRgbTransfer(
        asset: AppAsset,
        blindedUTXO: String,
        amount: ULong,
        transportEndpoints: List<String>,
        feeRate: ULong,
    ): String {
        Log.d(TAG, "Initiating transfer for blinded UTXO: $blindedUTXO")
        val refreshResult =
            RgbRepository.send(asset, blindedUTXO, amount, transportEndpoints, feeRate)
        runCatching {
                updateRGBAssets(
                    refresh = false,
                    updateTransfers = true,
                    updateTransfersFilter = asset.id,
                )
            }
            .onFailure { isCacheDirty = true }
        return refreshResult.txid
    }

    fun issueRgb20Asset(ticker: String, name: String, amounts: List<ULong>): AppAsset {
        checkMaxAssets()
        val contract = handleMissingFunds { RgbRepository.issueAssetRgb20(ticker, name, amounts) }
        val asset = AppAsset(contract, false)
        appAssets.add(asset)
        updateRGBAsset(asset)
        return asset
    }

    fun issueRgb25Asset(
        name: String,
        amounts: List<ULong>,
        description: String?,
        fileStream: InputStream?,
    ): AppAsset {
        checkMaxAssets()
        var filePath: String? = null
        var file: File? = null
        if (fileStream != null) {
            file = File.createTempFile("tmp", null, AppContainer.appContext.cacheDir)
            file.writeBytes(fileStream.readBytes())
            fileStream.close()
            filePath = file.absolutePath
        }
        val contract = handleMissingFunds {
            RgbRepository.issueAssetRgb25(name, amounts, description, filePath)
        }
        file?.delete()
        val asset = AppAsset(contract, false)
        appAssets.add(asset)
        updateRGBAsset(asset)
        return asset
    }

    fun deleteRGBTransfer(asset: AppAsset, transfer: AppTransfer): AppAsset {
        Log.d(TAG, "Removing transfer '$transfer'")
        RgbRepository.deleteTransfer(transfer)
        runCatching {
                updateRGBAssets(
                    refresh = false,
                    updateTransfers = true,
                    updateTransfersFilter = asset.id,
                )
            }
            .onFailure { isCacheDirty = true }
        return asset
    }

    fun handleAssetVisibility(assetID: String): Boolean {
        val asset = getCachedAsset(assetID)!!
        if (asset.hidden) {
            AppContainer.db.hiddenAssetDao().deleteHiddenAsset(asset.id)
            asset.hidden = false
        } else {
            AppContainer.db.hiddenAssetDao().insertHiddenAsset(HiddenAsset(id = asset.id))
            asset.hidden = true
        }
        return asset.hidden
    }

    fun getAssetMetadata(asset: AppAsset): org.rgbtools.Metadata {
        return RgbRepository.getMetadata(asset.id)
    }

    fun genReceiveData(
        asset: AppAsset?,
        blinded: Boolean = true,
        expirationSeconds: UInt? = null,
    ): Receiver {
        return if (asset != null && asset.bitcoin())
            Receiver(RgbRepository.getNewAddress(), null, true)
        else
            handleMissingFunds {
                startRGBReceiving(asset, blinded = blinded, expirationSeconds = expirationSeconds)
            }
    }

    fun sendAsset(
        asset: AppAsset,
        recipient: String,
        amount: ULong,
        transportEndpoints: List<String>,
        feeRate: ULong,
    ): String {
        return if (asset.bitcoin()) RgbRepository.sendToAddress(recipient, amount, feeRate)
        else
            handleMissingFunds {
                initiateRgbTransfer(asset, recipient, amount, transportEndpoints, feeRate)
            }
    }

    fun getAssets(): List<AppAsset> {
        updateBitcoinAsset(refresh = false)
        updateRGBAssets(refresh = false)

        val pendingAssets = AppContainer.db.rgbPendingAssetDao().getRgbPendingAssets()
        rgbPendingAssetIDs = pendingAssets.map { it.assetID }.toMutableList()
        for (rgbPendingAsset in pendingAssets) {
            val updatedAsset = getCachedAsset(rgbPendingAsset.assetID)
            if (
                updatedAsset != null ||
                    rgbPendingAsset.timestamp + TimeUnit.DAYS.toMillis(1) <
                        System.currentTimeMillis()
            )
                removeRgbPendingAsset(rgbPendingAsset.assetID)
            else appAssets.add(AppAsset(rgbPendingAsset))
        }

        val hiddenAssetsIds = AppContainer.db.hiddenAssetDao().getHiddenAssets().map { it.id }
        appAssets.filter { hiddenAssetsIds.contains(it.id) }.forEach { it.hidden = true }

        Log.d(TAG, "Offline APP assets: $appAssets")
        return appAssets
    }

    fun getRefreshedAssets(firstAppRefresh: Boolean): List<AppAsset> {
        var updateTransfers = true
        if (firstAppRefresh) {
            updateTransfers = RgbRepository.failAndDeleteOldTransfers()
        }
        updateBitcoinAsset()
        updateRGBAssets(updateTransfers = updateTransfers, firstAppRefresh = firstAppRefresh)
        Log.d(TAG, "Updated APP assets: ${appAssets.map{it.id}}")
        return appAssets
    }

    fun refreshAssetDetail(asset: AppAsset): AppAsset {
        if (asset.bitcoin()) updateBitcoinAsset() else updateRGBAsset(asset)
        return asset
    }

    fun getBitcoinUnspents(): List<UTXO> {
        val assetsInfoMap =
            appAssets.associate { it.id to if (it.ticker.isNullOrBlank()) it.name else it.ticker }
        val unspentList = RgbRepository.listUnspent(assetsInfoMap)
        Log.d(TAG, "Unspent list: $unspentList")
        return unspentList
    }

    suspend fun receiveFromBitcoinFaucet(): String {
        val address = RgbRepository.getNewAddress()
        Log.d(TAG, "Requesting bitcoins from faucet to address '$address'")
        return BtcFaucetRepository.receiveBitcoins(address)
    }

    suspend fun getRgbFaucetAssetGroups(): List<RgbFaucet> {
        val assetGroups = mutableListOf<RgbFaucet>()
        for (url in AppContainer.rgbFaucetURLS) {
            RgbFaucetRepository.getConfig(url, AppContainer.walletIdentifier)?.let {
                assetGroups.add(RgbFaucet(it, url))
            }
        }
        if (assetGroups.isEmpty())
            throw AppException(AppContainer.appContext.getString(R.string.faucet_no_assets))
        return assetGroups
    }

    suspend fun receiveFromRgbFaucet(
        url: String,
        group: Map.Entry<String, RgbAssetGroup>,
    ): Pair<RgbAsset, Distribution> {
        val configDistribution = group.value.distribution!!
        val expirationSeconds =
            when (configDistribution.modeEnum()) {
                DistributionMode.STANDARD -> null
                DistributionMode.RANDOM -> {
                    val now = Instant.now().atZone(ZoneId.of("UTC"))
                    val windowClose =
                        ZonedDateTime.parse(
                            configDistribution.randomParams!!.requestWindowClose,
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC),
                        )
                    // add 3 hours to give faucet time to send asset
                    Duration.between(windowClose, now).seconds.toUInt() + 10800U
                }
            }
        val witnessInvoice =
            genReceiveData(null, blinded = false, expirationSeconds = expirationSeconds).invoice
        val groupKey = group.key
        Log.d(TAG, "Requesting RGB asset from faucet '$url' and group '$groupKey'")
        val (asset, distribution) =
            RgbFaucetRepository.receiveRgbAsset(
                url,
                AppContainer.walletIdentifier,
                witnessInvoice,
                groupKey,
            )
        when (distribution.modeEnum()) {
            DistributionMode.STANDARD -> {
                Log.d(TAG, "Will receive an RGB asset with ID '${asset.assetID}'")
                val existingAsset = getCachedAsset(asset.assetID)
                if (existingAsset == null) {
                    Log.d(TAG, "Asset from faucet is unknown")
                    val certified =
                        try {
                            assetCertificationService.isAssetCertified(asset.assetID).code() == 200
                        } catch (_: Exception) {
                            false
                        }
                    val rgbPendingAsset = RgbPendingAsset(asset, certified)
                    AppContainer.db.rgbPendingAssetDao().insertRgbPendingAsset(rgbPendingAsset)
                    rgbPendingAssetIDs.add(rgbPendingAsset.assetID)
                    appAssets.add(AppAsset(rgbPendingAsset))
                }
            }
            DistributionMode.RANDOM -> {
                Log.d(TAG, "Will probably receive an RGB asset with ID '${asset.assetID}'")
            }
        }
        return Pair(asset, distribution)
    }
}
