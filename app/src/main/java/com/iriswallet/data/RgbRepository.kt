package com.iriswallet.data

import android.util.Log
import com.iriswallet.R
import com.iriswallet.utils.*
import com.iriswallet.utils.AppTransfer
import java.io.File
import org.rgbtools.*

object RgbRepository {

    private val wallet: Wallet by lazy {
        Wallet(
            WalletData(
                AppContainer.rgbDir.absolutePath,
                AppContainer.bitcoinNetwork.toRgbLibNetwork(),
                DatabaseType.SQLITE,
                1u,
                AppContainer.bitcoinKeys.accountXpubVanilla,
                AppContainer.bitcoinKeys.accountXpubColored,
                AppContainer.bitcoinKeys.mnemonic,
                AppContainer.bitcoinKeys.masterFingerprint,
                AppConstants.derivationChangeVanilla.toUByte(),
                AppConstants.supportedSchemas,
            )
        )
    }

    private var online: Online by LazyMutable { goOnline(SharedPreferencesManager.electrumURL) }

    fun backupDo(backupPath: File, mnemonic: String) {
        wallet.backup(backupPath.absolutePath, mnemonic)
    }

    fun backupRestore(backupPath: File, mnemonic: String, dataDir: File) {
        restoreBackup(backupPath.absolutePath, mnemonic, dataDir.absolutePath)
    }

    fun createUTXOs(): UByte {
        return wallet.createUtxos(
            online,
            false,
            null,
            null,
            SharedPreferencesManager.feeRate.toULong(),
            false,
        )
    }

    fun deleteTransfer(transfer: AppTransfer) {
        if (transfer.status != TransferStatus.FAILED)
            wallet.failTransfers(
                online,
                transfer.batchTransferIdx,
                noAssetOnly = false,
                skipSync = false,
            )
        wallet.deleteTransfers(transfer.batchTransferIdx, false)
    }

    fun failAndDeleteOldTransfers(): Boolean {
        var changed = wallet.failTransfers(online, null, noAssetOnly = true, skipSync = false)
        val deleted = wallet.deleteTransfers(null, true)
        if (deleted) changed = true
        return changed
    }

    fun getBalance(assetID: String): Balance {
        return wallet.getAssetBalance(assetID)
    }

    fun getNewAddress(): String {
        return wallet.getAddress()
    }

    fun getVanillaBalance(): BtcBalance {
        return wallet.getBtcBalance(online, skipSync = false)
    }

    fun getReceiveData(
        assetID: String? = null,
        expirationSeconds: UInt,
        blinded: Boolean = true,
    ): ReceiveData {
        val minConfirmations = 1.toUByte()
        val transportEndpoints = listOf(SharedPreferencesManager.proxyTransportEndpoint)
        return if (blinded) {
            wallet.blindReceive(
                assetID,
                Assignment.Any,
                expirationSeconds,
                transportEndpoints,
                minConfirmations,
            )
        } else {
            wallet.witnessReceive(
                assetID,
                Assignment.Any,
                expirationSeconds,
                transportEndpoints,
                minConfirmations,
            )
        }
    }

    fun getMetadata(assetID: String): Metadata {
        return wallet.getAssetMetadata(assetID)
    }

    private fun goOnline(electrumURL: String): Online {
        return wallet.goOnline(true, electrumURL)
    }

    fun goOnlineAgain(electrumURL: String) {
        val newOnline = goOnline(electrumURL)
        online = newOnline
    }

    fun isBackupRequired(): Boolean {
        return wallet.backupInfo()
    }

    fun issueAssetRgb20(ticker: String, name: String, amounts: List<ULong>): AssetNia {
        return wallet.issueAssetNia(ticker, name, AppConstants.rgbDefaultPrecision, amounts)
    }

    fun issueAssetRgb25(
        name: String,
        amounts: List<ULong>,
        description: String?,
        filePath: String?,
    ): AssetCfa {
        val desc = if (description.isNullOrBlank()) null else description
        return wallet.issueAssetCfa(name, desc, AppConstants.rgbDefaultPrecision, amounts, filePath)
    }

    fun listAssets(): List<AppAsset> {
        val assets = wallet.listAssets(listOf())
        val assetsNia = assets.nia!!.sortedBy { assetNia -> assetNia.addedAt }
        Log.d(TAG, "NIA assets: $assetsNia")
        val assetsCfa = assets.cfa!!.sortedBy { assetCfa -> assetCfa.addedAt }
        Log.d(TAG, "CFA assets: $assetsCfa")
        return assetsNia.map {
            val isSavedAsCertified =
                AppContainer.db.rgbCertifiedAssetDao().getRgbCertifiedAsset(it.assetId) != null
            AppAsset(it, isSavedAsCertified)
        } +
            assetsCfa.map {
                val isSavedAsCertified =
                    AppContainer.db.rgbCertifiedAssetDao().getRgbCertifiedAsset(it.assetId) != null
                AppAsset(it, isSavedAsCertified)
            }
    }

    fun listTransactions(sync: Boolean): List<Transaction> {
        val onlineOpt = if (sync) online else null
        return wallet.listTransactions(onlineOpt, !sync)
    }

    fun listTransfers(asset: AppAsset): List<AppTransfer> {
        return wallet.listTransfers(asset.id).map { AppTransfer.fromTransfer(it) }
    }

    fun listUnspent(assetsInfoMap: Map<String, String>): List<UTXO> {
        val unspents = wallet.listUnspents(online, settledOnly = false, skipSync = false)
        return unspents.map { unspent ->
            val rgbUnspents =
                unspent.rgbAllocations.map {
                    RgbUnspent.fromAllocation(it, assetsInfoMap[it.assetId])
                }
            UTXO(unspent, rgbUnspents)
        }
    }

    fun listVanillaTransfers(): List<AppTransfer> {
        val transactions =
            wallet.listTransactions(online, skipSync = false).filter {
                it.transactionType != TransactionType.RGB_SEND
            }
        return transactions
            .filter { it.confirmationTime != null }
            .sortedBy { it.confirmationTime!!.timestamp }
            .map { AppTransfer(it) } +
            transactions.filter { it.confirmationTime == null }.map { AppTransfer(it) }
    }

    fun refresh(asset: AppAsset? = null, light: Boolean = false): Boolean {
        val filter =
            if (light)
                listOf(
                    RefreshFilter(RefreshTransferStatus.WAITING_COUNTERPARTY, true),
                    RefreshFilter(RefreshTransferStatus.WAITING_COUNTERPARTY, false),
                )
            else listOf()
        return wallet.refresh(online, asset?.id, filter, false).values.any {
            it.updatedStatus != null
        }
    }

    fun send(
        asset: AppAsset,
        blindedUTXO: String,
        amount: ULong,
        transportEndpoints: List<String>,
        feeRate: ULong,
    ): SendResult {
        try {
            val assignment =
                if (asset.schema == AssetSchema.UDA) Assignment.NonFungible
                else Assignment.Fungible(amount)
            return wallet.send(
                online,
                mapOf(
                    asset.id to listOf(Recipient(blindedUTXO, null, assignment, transportEndpoints))
                ),
                false,
                feeRate,
                1u,
                false,
            )
        } catch (_: RgbLibException.InvalidTransportEndpoints) {
            throw AppException(
                AppContainer.appContext.getString(R.string.invalid_transport_endpoints)
            )
        }
    }

    fun sendToAddress(address: String, amount: ULong, feeRate: ULong): String {
        try {
            return wallet.sendBtc(online, address, amount, feeRate, skipSync = false)
        } catch (_: RgbLibException) {
            throw AppException(AppContainer.appContext.getString(R.string.insufficient_bitcoins))
        }
    }

    fun sync() {
        wallet.sync(online)
    }
}
