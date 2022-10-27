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
        return coloredWallet.createUtxos(online, false, null)
    }

    fun deleteTransfer(transfer: AppTransfer) {
        if (transfer.status != TransferStatus.FAILED)
            coloredWallet.failTransfers(online, transfer.recipient, null)
        coloredWallet.deleteTransfers(transfer.recipient, null)
    }

    fun getAddress(): String {
        return coloredWallet.getAddress()
    }

    fun getBalance(assetID: String): Balance {
        return coloredWallet.getAssetBalance(assetID)
    }

    fun getBlindedUTXO(assetID: String? = null, expirationSeconds: UInt): BlindData {
        return coloredWallet.blind(assetID, expirationSeconds)
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
        val assetsRgb21 = assets.rgb21!!.toList()
        Log.d(TAG, "RGB 21 assets: $assetsRgb21")
        return assetsRgb20.map { AppAsset(it) } + assetsRgb21.map { AppAsset(it) }
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

    fun refresh(asset: AppAsset? = null) {
        return coloredWallet.refresh(online, asset?.id)
    }

    fun send(asset: AppAsset, blindedUTXO: String, amount: ULong): String {
        return coloredWallet.send(
            online,
            mapOf(asset.id to listOf(Recipient(blindedUTXO, amount))),
            false
        )
    }
}
