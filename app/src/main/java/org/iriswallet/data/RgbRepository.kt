package org.iriswallet.data

import android.util.Log
import org.iriswallet.utils.*
import org.iriswallet.utils.Transfer
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

    private val online: Online by lazy { coloredWallet.goOnline(AppContainer.electrumURL, true) }

    fun createUTXOs(): ULong {
        return coloredWallet.createUtxos(online)
    }

    fun deleteTransfer(recipient: String) {
        coloredWallet.failTransfers(online, recipient)
        coloredWallet.deleteTransfers(recipient)
    }

    fun getAddress(): String {
        return coloredWallet.getAddress()
    }

    fun getBalance(assetID: String): Balance {
        return coloredWallet.getAssetBalance(assetID)
    }

    fun getBlindedUTXO(assetID: String? = null): BlindData {
        return coloredWallet.blind(assetID, AppConstants.rgbBlindDuration)
    }

    fun issueAsset(ticker: String, name: String, amount: ULong): Asset {
        return coloredWallet.issueAsset(
            online,
            ticker,
            name,
            AppConstants.rgbDefaultPrecision,
            amount
        )
    }

    fun listAssets(): List<AppAsset> {
        val assets = coloredWallet.listAssets().toList().sortedBy { it.ticker }
        Log.d(TAG, "RGB assets: $assets")
        return assets.map { AppAsset(it) }
    }

    fun listTransfers(asset: AppAsset): List<Transfer> {
        return coloredWallet.listTransfers(asset.id).map { Transfer(it) }
    }

    fun listUnspent(): List<UTXO> {
        return coloredWallet.listUnspents(false).map { UTXO(it) }
    }

    fun refresh(asset: AppAsset? = null) {
        return coloredWallet.refresh(online, asset?.id)
    }

    fun send(asset: AppAsset, blindedUTXO: String, amount: ULong): String {
        return coloredWallet.send(online, asset.id, blindedUTXO, amount)
    }
}
