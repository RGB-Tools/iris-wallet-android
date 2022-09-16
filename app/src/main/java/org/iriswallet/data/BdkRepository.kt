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
        val path = DerivationPath("m/84'/1'/${AppConstants.derivationAccountVanilla}'/$changeNum")
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

    fun getBalance(): ULong {
        return vanillaWallet.getBalance().total
    }

    fun getNewAddress(): String {
        return vanillaWallet.getAddress(AddressIndex.NEW).address
    }

    fun listTransfers(): List<Transfer> {
        val transactions = vanillaWallet.listTransactions()
        return transactions
            .filter { it.confirmationTime != null }
            .sortedBy { it.confirmationTime!!.timestamp }
            .map { Transfer(it) } +
            transactions.filter { it.confirmationTime == null }.map { Transfer(it) }
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
