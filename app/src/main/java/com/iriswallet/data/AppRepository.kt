package com.iriswallet.data

import android.util.Log
import com.iriswallet.R
import com.iriswallet.data.db.AutomaticTransaction
import com.iriswallet.data.db.RgbPendingAsset
import com.iriswallet.data.retrofit.RgbAsset
import com.iriswallet.utils.*
import java.util.concurrent.TimeUnit
import org.rgbtools.RgbLibException

object AppRepository {

    internal var isCacheDirty: Boolean = false

    var allowedFailure: Throwable? = null

    private val bitcoinAsset: AppAsset by lazy {
        AppAsset(
            AppAssetType.BITCOIN,
            AppConstants.bitcoinAssetID,
            AppContainer.bitcoinAssetName,
            ticker = AppContainer.bitcoinAssetTicker,
        )
    }
    private val appAssets: MutableList<AppAsset> by lazy { mutableListOf(bitcoinAsset) }
    private var rgbPendingAssetIDs: MutableList<String> = mutableListOf()

    fun getCachedFungibles(): List<AppAsset> {
        return appAssets.filter {
            listOf(AppAssetType.BITCOIN, AppAssetType.RGB20).contains(it.type)
        }
    }

    fun getCachedCollectibles(): List<AppAsset> {
        return appAssets.filter { it.type == AppAssetType.RGB21 }
    }

    private fun handleFailure(throwable: Throwable, allowFailures: Boolean) {
        if (allowFailures) {
            if (allowedFailure == null) allowedFailure = throwable
        } else throw throwable
    }

    private fun updateBitcoinAsset(allowFailures: Boolean = false, refresh: Boolean = true) {
        Log.d(TAG, "Updating bitcoin asset...")

        if (refresh)
            runCatching { BdkRepository.syncWithBlockchain() }
                .onFailure { handleFailure(it, allowFailures) }

        val transfers = BdkRepository.listTransfers().toMutableList()
        val autoTXs = AppContainer.db.automaticTransactionDao().getAutomaticTransactions()
        transfers
            .filter { it.txid in autoTXs.map { tx -> tx.txid } }
            .forEach { it.automatic = true }
        bitcoinAsset.transfers = transfers

        val balance = BdkRepository.getBalance()
        bitcoinAsset.settledBalance = balance.total
        bitcoinAsset.totalBalance = balance.total
    }

    private fun updateRGBAsset(
        asset: AppAsset,
        allowFailures: Boolean = false,
        refresh: Boolean = true
    ) {
        Log.d(TAG, "Updating RGB asset (${asset.id})...")

        if (refresh)
            runCatching { RgbRepository.refresh(asset) }
                .onFailure {
                    if (rgbPendingAssetIDs.contains(asset.id)) {
                        return
                    }
                    handleFailure(it, allowFailures)
                }

        asset.transfers = RgbRepository.listTransfers(asset)

        val balance = RgbRepository.getBalance(asset.id)
        asset.settledBalance = balance.settled
        asset.totalBalance = balance.future
    }

    private fun updateRGBAssets(allowFailures: Boolean = false, refresh: Boolean = true) {
        Log.d(TAG, "Updating RGB assets...")
        if (refresh) RgbRepository.refresh()
        val rgbAssets = RgbRepository.listAssets()
        for (rgbAsset in rgbAssets) {
            var assetToUpdate = appAssets.find { it.id == rgbAsset.id }
            if (assetToUpdate == null) {
                assetToUpdate = rgbAsset
                appAssets.add(assetToUpdate)
            } else if (rgbPendingAssetIDs.contains(assetToUpdate.id)) {
                appAssets.remove(assetToUpdate)
                removeRgbPendingAsset(assetToUpdate.id)
                assetToUpdate = rgbAsset
                appAssets.add(assetToUpdate)
            }
            updateRGBAsset(assetToUpdate, allowFailures, refresh = false)
        }
    }

    private fun removeRgbPendingAsset(rgbPendingAssetID: String) {
        AppContainer.db.rgbPendingAssetDao().deleteRgbPendingAsset(rgbPendingAssetID)
        rgbPendingAssetIDs.remove(rgbPendingAssetID)
    }

    private fun checkMaxAssets() {
        if (appAssets.size >= AppConstants.maxAssets)
            throw AppException(AppContainer.appContext.getString(R.string.reached_max_assets))
    }

    private fun createUTXOs(e: Exception) {
        Log.d(TAG, "Creating UTXOs because: ${e.message}")
        if (e is RgbLibException.InsufficientFunds) {
            Log.d(TAG, "Sending funds to RGB wallet...")
            val txid =
                BdkRepository.sendToAddress(RgbRepository.getAddress(), AppConstants.satsForRgb)
            AppContainer.db
                .automaticTransactionDao()
                .insertAutomaticTransactions(AutomaticTransaction(txid))
        }
        var attempts = 3
        var newUTXOs: UByte = 0u
        while (newUTXOs == 0u.toUByte() && attempts > 0) {
            try {
                Log.d(TAG, "Calling create UTXOs...")
                newUTXOs = RgbRepository.createUTXOs()
            } catch (_: RgbLibException.InsufficientFunds) {}
            attempts--
        }
    }

    private fun <T> handleMissingFunds(callback: () -> T): T {
        return try {
            callback()
        } catch (e: RgbLibException) {
            when (e) {
                is RgbLibException.InsufficientFunds,
                is RgbLibException.InsufficientAllocationSlots -> {
                    createUTXOs(e)
                    updateBitcoinAsset()
                    callback()
                }
                is RgbLibException.InvalidBlindedUtxo ->
                    throw AppException(
                        AppContainer.appContext.getString(R.string.invalid_blinded_utxo)
                    )
                is RgbLibException.BlindedUtxoAlreadyUsed ->
                    throw AppException(
                        AppContainer.appContext.getString(R.string.blinded_utxo_already_used)
                    )
                else -> throw e
            }
        }
    }

    private fun startRGBReceiving(asset: AppAsset?): Receiver {
        if (asset == null) checkMaxAssets()
        val blindedData = RgbRepository.getBlindedUTXO(asset?.id, AppConstants.rgbBlindDuration)
        if (asset != null) runCatching { updateRGBAsset(asset) }.onFailure { isCacheDirty = true }
        return Receiver(blindedData.blindedUtxo, AppConstants.rgbBlindDuration, false)
    }

    private fun initiateRgbTransfer(asset: AppAsset, blindedUTXO: String, amount: ULong): String {
        Log.d(TAG, "Initiating transfer for blinded UTXO: $blindedUTXO")
        val txid = RgbRepository.send(asset, blindedUTXO, amount)
        runCatching { updateRGBAsset(asset) }.onFailure { isCacheDirty = true }
        return txid
    }

    fun issueRGBAsset(ticker: String, name: String, amounts: List<ULong>): AppAsset {
        checkMaxAssets()
        val contract = handleMissingFunds { RgbRepository.issueAssetRgb20(ticker, name, amounts) }
        val balance = amounts.sum()
        val asset =
            AppAsset(
                AppAssetType.RGB20,
                contract.assetId,
                name,
                ticker = ticker,
                settledBalance = balance,
                totalBalance = balance,
            )
        appAssets.add(asset)
        updateRGBAsset(asset)
        return asset
    }

    fun deleteRGBTransfer(transfer: AppTransfer) {
        Log.d(TAG, "Removing transfer '$transfer'")
        RgbRepository.deleteTransfer(transfer)
    }

    fun genReceiveData(asset: AppAsset?): Receiver {
        return if (asset != null && asset.bitcoin())
            Receiver(BdkRepository.getNewAddress(), null, true)
        else handleMissingFunds { startRGBReceiving(asset) }
    }

    fun sendAsset(asset: AppAsset, recipient: String, amount: ULong): String {
        return if (asset.bitcoin()) BdkRepository.sendToAddress(recipient, amount)
        else handleMissingFunds { initiateRgbTransfer(asset, recipient, amount) }
    }

    fun getAssets(): List<AppAsset> {
        updateBitcoinAsset(refresh = false)
        updateRGBAssets(refresh = false)
        val pendingAssets = AppContainer.db.rgbPendingAssetDao().getRgbPendingAssets()
        rgbPendingAssetIDs = pendingAssets.map { it.assetID }.toMutableList()
        for (rgbPendingAsset in pendingAssets) {
            val updatedAsset = appAssets.find { it.id == rgbPendingAsset.assetID }
            if (
                updatedAsset != null ||
                    rgbPendingAsset.timestamp + TimeUnit.DAYS.toMillis(1) <
                        System.currentTimeMillis()
            )
                removeRgbPendingAsset(rgbPendingAsset.assetID)
            else appAssets.add(AppAsset(rgbPendingAsset))
        }
        Log.d(TAG, "Offline APP assets: $appAssets")
        return appAssets
    }

    fun getRefreshedAssets(allowFailures: Boolean = false): List<AppAsset> {
        updateBitcoinAsset(allowFailures)
        updateRGBAssets(allowFailures)
        Log.d(TAG, "Updated APP assets: ${appAssets.map{it.id}}")
        return appAssets
    }

    fun refreshAssetDetail(asset: AppAsset, allowFailures: Boolean = false): AppAsset {
        if (asset.bitcoin()) updateBitcoinAsset(allowFailures)
        else updateRGBAsset(asset, allowFailures)
        return appAssets.find { it.id == asset.id }!!
    }

    fun getBitcoinUnspents(): List<UTXO> {
        val assetsInfoMap =
            appAssets.associate { it.id to if (it.ticker.isNullOrBlank()) it.name else it.ticker }
        val unspentList = BdkRepository.listUnspent() + RgbRepository.listUnspent(assetsInfoMap)
        Log.d(TAG, "Unspent list: $unspentList")
        return unspentList
    }

    suspend fun receiveFromBitcoinFaucet(): String {
        val address = BdkRepository.getNewAddress()
        Log.d(TAG, "Requesting bitcoins from faucet to address '$address'")
        return BtcFaucetRepository.receiveBitcoins(address)
    }

    suspend fun getRgbFaucetAssetGroups(): List<RgbFaucet> {
        val assetGroups = mutableListOf<RgbFaucet>()
        for (url in AppContainer.rgbFaucetURLS) {
            RgbFaucetRepository.getConfig(url, AppContainer.bitcoinKeys.xpub)?.let {
                assetGroups.add(RgbFaucet(it, url))
            }
        }
        if (assetGroups.isEmpty())
            throw AppException(AppContainer.appContext.getString(R.string.faucet_no_assets))
        return assetGroups
    }

    suspend fun receiveFromRgbFaucet(url: String, group: String): RgbAsset {
        val blindedUtxo = genReceiveData(null).recipient
        Log.d(TAG, "Requesting RGB asset from faucet '$url' and group '$group'")
        val asset =
            RgbFaucetRepository.receiveRgbAsset(
                url,
                AppContainer.bitcoinKeys.xpub,
                blindedUtxo,
                group
            )
        Log.d(TAG, "Will receive an RGB asset with ID '${asset.assetID}'")
        val existingAsset = appAssets.find { it.id == asset.assetID }
        if (existingAsset == null) {
            val rgbPendingAsset = RgbPendingAsset(asset)
            AppContainer.db.rgbPendingAssetDao().insertRgbPendingAsset(rgbPendingAsset)
            rgbPendingAssetIDs.add(rgbPendingAsset.assetID)
            appAssets.add(AppAsset(rgbPendingAsset))
        }
        return asset
    }
}
