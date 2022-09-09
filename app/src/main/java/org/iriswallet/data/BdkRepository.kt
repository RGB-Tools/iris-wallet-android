package org.iriswallet.data

import android.util.Log
import org.bitcoindevkit.*
import org.iriswallet.R
import org.iriswallet.utils.AppConstants
import org.iriswallet.utils.AppContainer
import org.iriswallet.utils.AppException
import org.iriswallet.utils.TAG
import org.iriswallet.utils.Transfer
import org.iriswallet.utils.UTXO

object BdkRepository {

    private val keys: ExtendedKeyInfo by lazy {
        restoreExtendedKey(
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

    private fun calculateDescriptor(keys: ExtendedKeyInfo, change: Boolean): String {
        val changeNum = if (change) 1 else 0
        return "wpkh(${keys.xprv}/84'/1'/${AppConstants.derivationAccountVanilla}'/$changeNum/*)"
    }

    private fun getWallet(keys: ExtendedKeyInfo): Wallet {
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

    fun getBalance(): ULong {
        return vanillaWallet.getBalance()
    }

    fun getNewAddress(): String {
        return vanillaWallet.getAddress(AddressIndex.NEW).address
    }

    fun listTransfers(): List<Transfer> {
        val transactions = vanillaWallet.getTransactions()
        return transactions
            .filterIsInstance<Transaction.Confirmed>()
            .sortedBy { it.confirmation.timestamp }
            .map { Transfer(it) } +
            transactions.filterIsInstance<Transaction.Unconfirmed>().map { Transfer(it) }
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
