package com.iriswallet.data

import android.util.Log
import com.iriswallet.utils.*
import com.iriswallet.utils.AppTransfer
import org.rgbtools.*

object RgbRepository {

    private val coloredWallet: Wallet by lazy {
        Wallet(
            WalletData(
                AppContainer.rgbDir.absolutePath,
                AppContainer.bitcoinNetwork.toRgbLibNetwork(),
                DatabaseType.SQLITE,
                AppContainer.bitcoinKeys.xpub,
                AppContainer.bitcoinKeys.mnemonic,
            )
        )
    }

    private val online: Online by lazy {
        coloredWallet.goOnline(true, AppContainer.electrumURL, AppContainer.proxyURL)
    }

    fun createUTXOs(): UByte {
        return coloredWallet.createUtxos(online, false, null, null)
    }

    fun deleteTransfer(transfer: AppTransfer) {
        if (transfer.status != TransferStatus.FAILED)
            coloredWallet.failTransfers(online, transfer.blindedUTXO, null, false)
        coloredWallet.deleteTransfers(transfer.blindedUTXO, null, false)
    }

    fun failAndDeleteOldTransfers(): Boolean {
        var changed = coloredWallet.failTransfers(online, null, null, true)
        val deleted = coloredWallet.deleteTransfers(null, null, true)
        if (deleted) changed = true
        return changed
    }

    fun getAddress(): String {
        return coloredWallet.getAddress()
    }

    fun getBalance(assetID: String): Balance {
        return coloredWallet.getAssetBalance(assetID)
    }

    fun getBlindedUTXO(assetID: String? = null, expirationSeconds: UInt): BlindData {
        return coloredWallet.blind(assetID, null, expirationSeconds)
    }

    fun getMetadata(assetID: String): Metadata {
        return coloredWallet.getAssetMetadata(online, assetID)
    }

    fun issueAssetRgb20(ticker: String, name: String, amounts: List<ULong>): AssetRgb20 {
        return coloredWallet.issueAssetRgb20(
            online,
            ticker,
            name,
            AppConstants.rgbDefaultPrecision,
            amounts
        )
    }

    fun listAssets(): List<AppAsset> {
        val assets = coloredWallet.listAssets(listOf())
        val assetsRgb20 = assets.rgb20!!.toList()
        Log.d(TAG, "RGB 20 assets: $assetsRgb20")
        val assetsRgb121 = assets.rgb121!!.toList()
        Log.d(TAG, "RGB 121 assets: $assetsRgb121")
        return assetsRgb20.map { AppAsset(it) } + assetsRgb121.map { AppAsset(it) }
    }

    fun listTransfers(asset: AppAsset): List<AppTransfer> {
        return coloredWallet.listTransfers(asset.id).map { AppTransfer(it) }
    }

    fun listUnspent(assetsInfoMap: Map<String, String>): List<UTXO> {
        val unspents = coloredWallet.listUnspents(false)
        return unspents.map { unspent ->
            val rgbUnspents =
                unspent.rgbAllocations.map { RgbUnspent(it, assetsInfoMap[it.assetId]) }
            UTXO(unspent, rgbUnspents)
        }
    }

    fun refresh(asset: AppAsset? = null): Boolean {
        return coloredWallet.refresh(online, asset?.id, listOf())
    }

    fun send(asset: AppAsset, blindedUTXO: String, amount: ULong): String {
        return coloredWallet.send(
            online,
            mapOf(asset.id to listOf(Recipient(blindedUTXO, amount))),
            false
        )
    }
}
