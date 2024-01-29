package com.iriswallet.data

import android.util.Log
import com.iriswallet.R
import com.iriswallet.utils.*
import com.iriswallet.utils.AppTransfer
import java.io.File
import org.rgbtools.*

object RgbRepository {

    private val coloredWallet: Wallet by lazy {
        Wallet(
            WalletData(
                AppContainer.rgbDir.absolutePath,
                AppContainer.bitcoinNetwork.toRgbLibNetwork(),
                DatabaseType.SQLITE,
                1u,
                AppContainer.bitcoinKeys.accountXpub,
                AppContainer.bitcoinKeys.mnemonic,
                AppConstants.derivationChangeVanilla.toUByte(),
            )
        )
    }

    private var online: Online by LazyMutable { goOnline(SharedPreferencesManager.electrumURL) }

    fun backupDo(backupPath: File, mnemonic: String) {
        coloredWallet.backup(backupPath.absolutePath, mnemonic)
    }

    fun backupRestore(backupPath: File, mnemonic: String, dataDir: File) {
        restoreBackup(backupPath.absolutePath, mnemonic, dataDir.absolutePath)
    }

    fun createUTXOs(): UByte {
        return coloredWallet.createUtxos(
            online,
            false,
            null,
            null,
            SharedPreferencesManager.feeRate.toFloat(),
            false,
        )
    }

    fun deleteTransfer(transfer: AppTransfer) {
        if (transfer.status != TransferStatus.FAILED)
            coloredWallet.failTransfers(online, transfer.batchTransferIdx, false, false)
        coloredWallet.deleteTransfers(transfer.batchTransferIdx, false)
    }

    fun failAndDeleteOldTransfers(): Boolean {
        var changed = coloredWallet.failTransfers(online, null, true, false)
        val deleted = coloredWallet.deleteTransfers(null, true)
        if (deleted) changed = true
        return changed
    }

    fun getBalance(assetID: String): Balance {
        return coloredWallet.getAssetBalance(assetID)
    }

    fun getReceiveData(
        assetID: String? = null,
        expirationSeconds: UInt,
        blinded: Boolean = true
    ): ReceiveData {
        val minConfirmations = 1.toUByte()
        val amount = null
        val transportEndpoints = listOf(SharedPreferencesManager.proxyTransportEndpoint)
        return if (blinded) {
            coloredWallet.blindReceive(
                assetID,
                amount,
                expirationSeconds,
                transportEndpoints,
                minConfirmations,
            )
        } else {
            coloredWallet.witnessReceive(
                assetID,
                amount,
                expirationSeconds,
                transportEndpoints,
                minConfirmations,
            )
        }
    }

    fun getMetadata(assetID: String): Metadata {
        return coloredWallet.getAssetMetadata(assetID)
    }

    private fun goOnline(electrumURL: String): Online {
        return coloredWallet.goOnline(true, electrumURL)
    }

    fun goOnlineAgain(electrumURL: String) {
        val newOnline = goOnline(electrumURL)
        online = newOnline
    }

    fun isBackupRequired(): Boolean {
        return coloredWallet.backupInfo()
    }

    fun issueAssetRgb20(ticker: String, name: String, amounts: List<ULong>): AssetNia {
        return coloredWallet.issueAssetNia(
            online,
            ticker,
            name,
            AppConstants.rgbDefaultPrecision,
            amounts
        )
    }

    fun issueAssetRgb25(
        name: String,
        amounts: List<ULong>,
        description: String?,
        filePath: String?
    ): AssetCfa {
        val desc = if (description.isNullOrBlank()) null else description
        return coloredWallet.issueAssetCfa(
            online,
            name,
            desc,
            AppConstants.rgbDefaultPrecision,
            amounts,
            filePath
        )
    }

    fun listAssets(): List<AppAsset> {
        val assets = coloredWallet.listAssets(listOf())
        val assetsRgb20 = assets.nia!!.sortedBy { assetNia -> assetNia.addedAt }
        Log.d(TAG, "RGB 20 assets: $assetsRgb20")
        val assetsRgb25 = assets.cfa!!.sortedBy { assetCfa -> assetCfa.addedAt }
        Log.d(TAG, "RGB 25 assets: $assetsRgb25")
        return assetsRgb20.map {
            val isSavedAsCertified =
                AppContainer.db.rgbCertifiedAssetDao().getRgbCertifiedAsset(it.assetId) != null
            AppAsset(it, isSavedAsCertified)
        } +
            assetsRgb25.map {
                val isSavedAsCertified =
                    AppContainer.db.rgbCertifiedAssetDao().getRgbCertifiedAsset(it.assetId) != null
                AppAsset(it, isSavedAsCertified)
            }
    }

    fun listTransactions(sync: Boolean): List<Transaction> {
        val onlineOpt = if (sync) online else null
        return coloredWallet.listTransactions(onlineOpt, !sync)
    }

    fun listTransfers(asset: AppAsset): List<AppTransfer> {
        return coloredWallet.listTransfers(asset.id).map { AppTransfer(it) }
    }

    fun listUnspent(assetsInfoMap: Map<String, String>): List<UTXO> {
        val unspents = coloredWallet.listUnspents(online, false, false)
        return unspents.map { unspent ->
            val rgbUnspents =
                unspent.rgbAllocations.map { RgbUnspent(it, assetsInfoMap[it.assetId]) }
            UTXO(unspent, rgbUnspents)
        }
    }

    fun refresh(asset: AppAsset? = null, light: Boolean = false): Boolean {
        val filter =
            if (light)
                listOf(
                    RefreshFilter(RefreshTransferStatus.WAITING_COUNTERPARTY, true),
                    RefreshFilter(RefreshTransferStatus.WAITING_COUNTERPARTY, false)
                )
            else listOf()
        return coloredWallet.refresh(online, asset?.id, filter, false).values.any {
            it.updatedStatus != null
        }
    }

    fun send(
        asset: AppAsset,
        blindedUTXO: String,
        amount: ULong,
        transportEndpoints: List<String>,
        feeRate: Float,
    ): SendResult {
        try {
            return coloredWallet.send(
                online,
                mapOf(asset.id to listOf(Recipient(blindedUTXO, null, amount, transportEndpoints))),
                false,
                feeRate,
                1u,
                false,
            )
        } catch (e: RgbLibException.InvalidTransportEndpoints) {
            throw AppException(
                AppContainer.appContext.getString(R.string.invalid_transport_endpoints)
            )
        }
    }
}
