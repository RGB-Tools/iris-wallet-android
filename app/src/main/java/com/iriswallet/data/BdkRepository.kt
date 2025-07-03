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
            Mnemonic.fromString(AppContainer.bitcoinKeys.mnemonic),
            AppContainer.mnemonicPassword,
        )
    }

    private val vanillaWallet: Wallet by lazy { getWallet(keys) }

    private val blockchain: Blockchain by lazy {
        Blockchain(
            BlockchainConfig.Electrum(
                ElectrumConfig(
                    SharedPreferencesManager.electrumURL,
                    null,
                    AppConstants.bdkRetry.toUByte(),
                    AppConstants.bdkTimeout.toUByte(),
                    AppConstants.bdkStopGap.toULong(),
                    true,
                )
            )
        )
    }

    private fun calculateDescriptor(
        keys: DescriptorSecretKey,
        changeNum: Int,
        derivationAccount: Int,
    ): String {
        return "tr(${keys.extend(DerivationPath(
            "m/86'/${AppContainer.bitcoinDerivationPathCoinType}'/${derivationAccount}'/$changeNum"
        )).asString()})"
    }

    private fun getWallet(keys: DescriptorSecretKey): Wallet {
        val descriptor: String =
            calculateDescriptor(
                keys,
                AppConstants.derivationChangeVanilla,
                AppConstants.derivationAccountVanilla,
            )
        return getWalletFromDescriptors(
            DatabaseConfig.Sqlite(
                SqliteDbConfiguration(AppContainer.bdkDBVanillaPath.absolutePath)
            ),
            descriptor,
            null,
        )
    }

    private fun getWalletFromDescriptors(
        dbConfig: DatabaseConfig,
        descriptor: String,
        changeDescriptor: String?,
    ): Wallet {
        val bdkNetwork = AppContainer.bitcoinNetwork.toBdkNetwork()
        val changeDesc = changeDescriptor?.let { Descriptor(it, bdkNetwork) }
        return Wallet(Descriptor(descriptor, bdkNetwork), changeDesc, bdkNetwork, dbConfig)
    }

    fun getBalance(): Balance {
        return vanillaWallet.getBalance()
    }

    fun getNewAddress(): String {
        return vanillaWallet.getAddress(AddressIndex.New).address.asString()
    }

    fun listTransfers(): List<AppTransfer> {
        val transactions = vanillaWallet.listTransactions(false)
        return transactions
            .filter { it.confirmationTime != null }
            .sortedBy { it.confirmationTime!!.timestamp }
            .map { AppTransfer(it) } +
            transactions.filter { it.confirmationTime == null }.map { AppTransfer(it) }
    }

    fun listUnspent(): List<UTXO> {
        val unspents = vanillaWallet.listUnspent()
        Log.d(TAG, "BDK unspents: $unspents")
        return unspents.map { UTXO(it) }
    }

    fun recoverFundsFromDerivationPath(derivationAccount: Int, change: Boolean) {
        Log.i(TAG, "Recovering funds from derivation account: $derivationAccount")
        val descriptor: String = calculateDescriptor(keys, 0, derivationAccount)
        val changeDescriptor = if (change) calculateDescriptor(keys, 1, derivationAccount) else null
        val wallet = getWalletFromDescriptors(DatabaseConfig.Memory, descriptor, changeDescriptor)
        wallet.sync(blockchain, null)
        if (wallet.getBalance().total == 0UL) {
            Log.w(TAG, "Skipping funds recovering because there's no total balance")
            return
        }
        val psbt =
            try {
                TxBuilder()
                    .drainWallet()
                    .drainTo(Address(getNewAddress()).scriptPubkey())
                    .finish(wallet)
                    .psbt
            } catch (e: BdkException.InsufficientFunds) {
                Log.w(TAG, "Skipping funds recovering because there's no sufficient balance")
                return
            }
        wallet.sign(psbt, null)
        blockchain.broadcast(psbt.extractTx())
        Log.i(TAG, "Funds recovered successfully")
    }

    fun sendToAddress(address: String, amount: ULong, feeRate: Float): String {
        try {
            val psbt =
                TxBuilder()
                    .addRecipient(Address(address).scriptPubkey(), amount)
                    .feeRate(feeRate)
                    .finish(vanillaWallet)
                    .psbt
            vanillaWallet.sign(psbt, null)
            blockchain.broadcast(psbt.extractTx())
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
