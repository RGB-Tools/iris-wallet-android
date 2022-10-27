package com.iriswallet.utils

import android.util.Log
import java.util.*
import org.bitcoindevkit.LocalUtxo
import org.bitcoindevkit.Network
import org.bitcoindevkit.TransactionDetails
import com.iriswallet.data.AppRepository
import org.rgbtools.*
import org.rgbtools.BitcoinNetwork

data class AppAsset(
    val id: String,
    val ticker: String,
    var name: String,
    var totalBalance: ULong = 0UL,
    var transfers: List<Transfer> = listOf(),
) {
    constructor(
        rgbAsset: Asset
    ) : this(
        rgbAsset.assetId,
        rgbAsset.ticker,
        rgbAsset.name,
        totalBalance = rgbAsset.balance.future,
    )

    fun bitcoin(): Boolean {
        return id == AppConstants.bitcoinAssetID
    }
}

enum class AppErrorType {
    APP_EXCEPTION,
    TIMEOUT_EXCEPTION,
    UNEXPECTED_EXCEPTION,
}

data class AppError(
    var type: AppErrorType = AppErrorType.UNEXPECTED_EXCEPTION,
    var message: String? = null,
) {
    constructor(throwable: Throwable) : this() {
        if (throwable.javaClass == AppException::class.java) {
            this.message = throwable.message
            this.type = AppErrorType.APP_EXCEPTION
        } else {
            Log.e(TAG, "Unexpected error: ${throwable.message}")
            throwable.printStackTrace()
        }
    }
}

class AppException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

data class AppResponse<T>(
    val data: T? = null,
    val error: AppError? = null,
)

enum class BitcoinNetwork {
    SIGNET,
    TESTNET;

    fun toBdkNetwork(): Network {
        return when (this) {
            SIGNET -> Network.SIGNET
            TESTNET -> Network.TESTNET
        }
    }

    fun toRgbLibNetwork(): BitcoinNetwork {
        return when (this) {
            SIGNET -> BitcoinNetwork.SIGNET
            TESTNET -> BitcoinNetwork.TESTNET
        }
    }

    val capitalized by lazy { this.toString().lowercase().replaceFirstChar(Char::titlecase) }
}

open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /** Returns the content and prevents its use again. */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /** Returns the content, even if it's already been handled. */
    fun peekContent(): T = content
}

data class Receiver(
    val recipient: String,
    val expiration_time: Date? = null,
)

data class RgbUnspent(
    val asset_id: String?,
    val ticker: String?,
    val amount: ULong,
    val settled: Boolean,
) {
    constructor(
        rgbAllocation: RgbAllocation
    ) : this(
        rgbAllocation.assetId,
        if (rgbAllocation.assetId != null) AppRepository.getTickerForId(rgbAllocation.assetId!!)
        else null,
        rgbAllocation.amount,
        rgbAllocation.settled,
    )
}

data class Transfer(
    val date: Date,
    val status: TransferStatus,
    val incoming: Boolean,
    val recipient: String? = null,
    val amount: ULong? = null,
    val txid: String? = null,
    val unblindedUTXO: Outpoint? = null,
    val changeUTXO: Outpoint? = null,
    var automatic: Boolean = false,
) {

    constructor(
        bdkTransfer: TransactionDetails
    ) : this(
        if (bdkTransfer.confirmationTime == null) Date(System.currentTimeMillis())
        else Date(bdkTransfer.confirmationTime!!.timestamp.toLong() * 1000),
        if (bdkTransfer.confirmationTime == null) TransferStatus.WAITING_CONFIRMATIONS
        else TransferStatus.SETTLED,
        bdkTransfer.received > bdkTransfer.sent,
        amount = AppUtils.uLongAbsDiff(bdkTransfer.received, bdkTransfer.sent),
        txid = bdkTransfer.txid,
    )

    constructor(
        transfer: org.rgbtools.Transfer
    ) : this(
        Date(transfer.updatedAt * 1000),
        transfer.status,
        transfer.received > transfer.sent,
        recipient = transfer.blindedUtxo,
        amount = AppUtils.uLongAbsDiff(transfer.received, transfer.sent),
        txid = transfer.txid,
        unblindedUTXO = transfer.unblindedUtxo,
        changeUTXO = transfer.changeUtxo,
    )

    fun deletable(): Boolean {
        return status in listOf(TransferStatus.WAITING_COUNTERPARTY, TransferStatus.FAILED)
    }
}

data class UTXO(
    val txid: String,
    val vout: UInt,
    val btcAmount: ULong,
    var walletName: String? = null,
    var rgbUnspents: List<RgbUnspent>
) {
    constructor(
        unspent: LocalUtxo
    ) : this(
        unspent.outpoint.txid,
        unspent.outpoint.vout,
        unspent.txout.value,
        AppConstants.vanillaWallet,
        listOf(),
    )

    constructor(
        unspent: Unspent
    ) : this(
        unspent.utxo.outpoint.txid,
        unspent.utxo.outpoint.vout,
        unspent.utxo.btcAmount,
        AppConstants.coloredWallet,
        unspent.rgbAllocations.map { RgbUnspent(it) },
    )

    constructor(
        outpoint: Outpoint
    ) : this(
        outpoint.txid,
        outpoint.vout,
        0UL,
        null,
        listOf(),
    )

    fun outpointStr(): String {
        return "$txid:$vout"
    }
}
