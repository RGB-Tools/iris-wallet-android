package com.iriswallet.data

import android.util.Log
import java.util.*
import com.iriswallet.R
import com.iriswallet.data.db.AutomaticTransaction
import com.iriswallet.utils.*
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import org.rgbtools.*

object AppRepository {

    internal var isCacheDirty: Boolean = false

    var allowedFailure: Throwable? = null

    private val bitcoinAsset: AppAsset by lazy {
        AppAsset(
            AppConstants.bitcoinAssetID,
            AppContainer.bitcoinAssetTicker,
            AppContainer.bitcoinAssetName,
        )
    }
    private val appAssets: MutableList<AppAsset> by lazy { mutableListOf(bitcoinAsset) }

    private fun handleFailure(throwable: Throwable, allowFailures: Boolean) {
        if (allowFailures) {
            if (allowedFailure == null) allowedFailure = throwable
        } else throw throwable
    }

    private fun updateBitcoinAsset(allowFailures: Boolean = false) {
        Log.d(TAG, "Updating bitcoin asset...")

        runCatching { BdkRepository.syncWithBlockchain() }
            .onFailure { handleFailure(it, allowFailures) }

        val transfers = BdkRepository.listTransfers().toMutableList()
        val autoTXs = AppContainer.db.automaticTransactionDao().getAutomaticTransactions()
        transfers
            .filter { it.txid in autoTXs.map { tx -> tx.txid } }
            .forEach { it.automatic = true }
        bitcoinAsset.transfers = transfers

        bitcoinAsset.totalBalance = BdkRepository.getBalance()
    }

    private fun updateRGBAsset(asset: AppAsset, allowFailures: Boolean = false) {
        Log.d(TAG, "Updating RGB asset (${asset.id})...")

        var assetToUpdate = appAssets.find { it.id == asset.id }
        if (assetToUpdate == null) {
            assetToUpdate = asset
            appAssets.add(assetToUpdate)
        }

        runCatching { RgbRepository.refresh(asset) }.onFailure { handleFailure(it, allowFailures) }

        assetToUpdate.transfers = RgbRepository.listTransfers(asset)

        assetToUpdate.totalBalance = RgbRepository.getBalance(asset.id).future
    }

    private fun updateRGBAssets(allowFailures: Boolean = false) {
        Log.d(TAG, "Updating RGB assets...")
        RgbRepository.refresh()
        val rgbAssets = RgbRepository.listAssets()
        for (rgbAsset in rgbAssets) {
            updateRGBAsset(rgbAsset, allowFailures)
        }
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
        var newUTXOs = 0UL
        while (newUTXOs == 0UL && attempts > 0) {
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
                else -> throw e
            }
        }
    }

    private fun startRGBReceiving(asset: AppAsset?): Receiver {
        if (asset == null) checkMaxAssets()
        val blindedData = RgbRepository.getBlindedUTXO(asset?.id)
        val timestamp = System.currentTimeMillis()
        if (asset != null) runCatching { updateRGBAsset(asset) }.onFailure { isCacheDirty = true }
        return Receiver(blindedData.blindedUtxo, if (asset == null) Date(timestamp) else null)
    }

    private fun initiateRgbTransfer(asset: AppAsset, blindedUTXO: String, amount: ULong): String {
        Log.d(TAG, "Initiating transfer for blinded UTXO: $blindedUTXO")
        val txid = RgbRepository.send(asset, blindedUTXO, amount)
        runCatching { updateRGBAsset(asset) }.onFailure { isCacheDirty = true }
        return txid
    }

    private fun refreshAssets(allowFailures: Boolean = false) {
        updateBitcoinAsset(allowFailures)
        updateRGBAssets(allowFailures)
    }

    fun issueRGBAsset(ticker: String, name: String, amount: ULong): AppAsset {
        checkMaxAssets()
        val contract = handleMissingFunds { RgbRepository.issueAsset(ticker, name, amount) }
        val asset = AppAsset(contract.assetId, ticker, name, totalBalance = amount)
        updateRGBAsset(asset)
        return asset
    }

    fun deleteRGBTransfer(recipient: String) {
        Log.d(TAG, "Removing transfer '$recipient'")
        RgbRepository.deleteTransfer(recipient)
    }

    fun genReceiveData(asset: AppAsset?): Receiver {
        return if (asset != null && asset.bitcoin()) Receiver(BdkRepository.getNewAddress(), null)
        else handleMissingFunds { startRGBReceiving(asset) }
    }

    fun sendAsset(asset: AppAsset, recipient: String, amount: ULong): String {
        return if (asset.bitcoin()) BdkRepository.sendToAddress(recipient, amount)
        else handleMissingFunds { initiateRgbTransfer(asset, recipient, amount) }
    }

    fun getRefreshedAssets(allowFailures: Boolean = false): List<AppAsset> {
        refreshAssets(allowFailures)
        Log.d(TAG, "Updated APP assets: $appAssets")
        return appAssets
    }

    fun refreshAssetDetail(asset: AppAsset, allowFailures: Boolean = false): AppAsset {
        if (asset.bitcoin()) updateBitcoinAsset(allowFailures)
        else updateRGBAsset(asset, allowFailures)
        return appAssets.find { it.id == asset.id }!!
    }

    fun getBitcoinUnspents(): List<UTXO> {
        val unspentList = BdkRepository.listUnspent() + RgbRepository.listUnspent()
        Log.d(TAG, "Unspent list: $unspentList")
        return unspentList
    }

    fun getTickerForId(id: String): String {
        val asset = appAssets.find { it.id == id }
        return asset?.ticker ?: "Not found"
    }
}
