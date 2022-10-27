package com.iriswallet.data

import android.util.Log
import com.iriswallet.R
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppException
import com.iriswallet.utils.AppTransfer
import com.iriswallet.utils.TAG
import com.iriswallet.utils.UTXO
import org.bitcoindevkit.*

object BdkRepository {

    private val keys: DescriptorSecretKey by lazy {
        DescriptorSecretKey(
            AppContainer.bitcoinNetwork.toBdkNetwork(),
            AppContainer.bitcoinKeys.mnemonic,
            AppContainer.mnemonicPassword,
        )
    }

    private val vanillaWallet: Wallet by lazy { getWallet(keys) }

    private val blockchain: Blockchain by lazy {
        Blockchain(
            BlockchainConfig.Electrum(
                ElectrumConfig(
                    AppContainer.electrumURL,
                    null,
                    AppConstants.bdkRetry.toUByte(),
                    AppConstants.bdkTimeout.toUByte(),
                    AppConstants.bdkStopGap.toULong()
                )
            )
        )
    }

    private fun calculateDescriptor(keys: DescriptorSecretKey, change: Boolean): String {
        val changeNum = if (change) 1 else 0
        val path =
            DerivationPath(
                "m/84'/${AppContainer.bitcoinDerivationPathCoinType}'/${AppConstants.derivationAccountVanilla}'/$changeNum"
            )
        return "wpkh(${keys.extend(path).asString()})"
    }

    private fun getWallet(keys: DescriptorSecretKey): Wallet {
        val descriptor: String = calculateDescriptor(keys, false)
        val changeDescriptor: String = calculateDescriptor(keys, true)
        val dbPath = AppContainer.bdkDBVanillaPath
        return Wallet(
            descriptor,
            changeDescriptor,
            AppContainer.bitcoinNetwork.toBdkNetwork(),
            DatabaseConfig.Sqlite(SqliteDbConfiguration(dbPath.absolutePath)),
        )
    }

    fun getBalance(): Balance {
        return vanillaWallet.getBalance()
    }

    fun getNewAddress(): String {
        return vanillaWallet.getAddress(AddressIndex.NEW).address
    }

    fun listTransfers(): List<AppTransfer> {
        val transactions = vanillaWallet.listTransactions()
        return transactions
            .filter { it.confirmationTime != null }
            .sortedBy { it.confirmationTime!!.timestamp }
            .map { AppTransfer(it) } +
            transactions.filter { it.confirmationTime == null }.map { AppTransfer(it) }
    }

    fun listUnspent(): List<UTXO> {
        return vanillaWallet.listUnspent().map { UTXO(it) }
    }

    fun sendToAddress(address: String, amount: ULong): String {
        try {
            val psbt = TxBuilder().addRecipient(address, amount).finish(vanillaWallet)
            vanillaWallet.sign(psbt)
            blockchain.broadcast(psbt)
            return psbt.txid()
        } catch (e: BdkException.InsufficientFunds) {
            throw AppException(AppContainer.appContext.getString(R.string.insufficient_bitcoins))
        }
    }

    fun syncWithBlockchain() {
        Log.d(TAG, "Syncing vanilla wallet with blockchain...")
        vanillaWallet.sync(blockchain, null)
        Log.d(TAG, "Wallet synced!")
    }
}
