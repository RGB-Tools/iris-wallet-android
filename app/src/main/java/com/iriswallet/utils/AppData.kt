package com.iriswallet.utils

import android.os.Parcelable
import android.util.Log
import com.iriswallet.data.db.RgbPendingAsset
import com.iriswallet.data.retrofit.FaucetConfig
import com.iriswallet.data.retrofit.RgbAssetGroup
import java.util.*
import kotlinx.parcelize.Parcelize
import org.bitcoindevkit.LocalUtxo
import org.bitcoindevkit.Network
import org.bitcoindevkit.TransactionDetails
import org.rgbtools.*
import org.rgbtools.BitcoinNetwork

@Parcelize
data class AppAsset(
    val type: AppAssetType,
    val id: String,
    var name: String,
    val ticker: String? = null,
    var media: AppMedia? = null,
    val fromFaucet: Boolean = false,
    var spendableBalance: ULong = 0UL,
    var settledBalance: ULong = 0UL,
    var totalBalance: ULong = 0UL,
    var transfers: List<AppTransfer> = listOf(),
    var hidden: Boolean = false,
) : Parcelable {
    constructor(
        rgbAsset: AssetRgb20
    ) : this(
        AppAssetType.RGB20,
        rgbAsset.assetId,
        rgbAsset.name,
        ticker = rgbAsset.ticker,
        spendableBalance = rgbAsset.balance.spendable,
        settledBalance = rgbAsset.balance.settled,
        totalBalance = rgbAsset.balance.future,
    )

    constructor(
        rgbAsset: AssetRgb121
    ) : this(
        AppAssetType.RGB121,
        rgbAsset.assetId,
        rgbAsset.name,
        media = rgbAsset.dataPaths.getOrNull(0)?.let { AppMedia(it) },
        spendableBalance = rgbAsset.balance.spendable,
        settledBalance = rgbAsset.balance.settled,
        totalBalance = rgbAsset.balance.future,
    )

    constructor(
        rgbPendingAsset: RgbPendingAsset
    ) : this(
        if (rgbPendingAsset.schema == AppAssetType.RGB20.toString()) AppAssetType.RGB20
        else AppAssetType.RGB121,
        rgbPendingAsset.assetID,
        rgbPendingAsset.name,
        ticker = rgbPendingAsset.ticker,
        media = null,
        fromFaucet = true,
        totalBalance = rgbPendingAsset.amount.toULong(),
        transfers =
            listOf(
                AppTransfer(
                    Date(rgbPendingAsset.timestamp),
                    TransferStatus.WAITING_COUNTERPARTY,
                    TransferKind.RECEIVE,
                    amount = rgbPendingAsset.amount.toULong(),
                )
            )
    )

    fun bitcoin(): Boolean {
        return type == AppAssetType.BITCOIN
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

@Parcelize
data class AppMedia(
    val filePath: String,
    val mime: MimeType,
    val mimeString: String,
) : Parcelable {
    constructor(
        media: Media
    ) : this(
        media.filePath,
        when (media.mime.split("/").getOrNull(0)?.uppercase()) {
            MimeType.IMAGE.toString() -> MimeType.IMAGE
            MimeType.VIDEO.toString() -> MimeType.VIDEO
            else -> MimeType.OTHER
        },
        media.mime,
    )
}

data class AppResponse<T>(
    val requestID: String? = null,
    val data: T? = null,
    val error: AppError? = null,
)

enum class BitcoinNetwork {
    SIGNET,
    TESTNET,
    MAINNET;

    fun toBdkNetwork(): Network {
        return when (this) {
            SIGNET -> Network.SIGNET
            TESTNET -> Network.TESTNET
            MAINNET -> Network.BITCOIN
        }
    }

    fun toRgbLibNetwork(): BitcoinNetwork {
        return when (this) {
            SIGNET -> BitcoinNetwork.SIGNET
            TESTNET -> BitcoinNetwork.TESTNET
            MAINNET -> BitcoinNetwork.MAINNET
        }
    }

    val capitalized by lazy { this.toString().lowercase().replaceFirstChar(Char::titlecase) }
}

enum class AppAssetType {
    BITCOIN,
    RGB20,
    RGB121
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

enum class MimeType {
    IMAGE,
    VIDEO,
    OTHER
}

data class RgbFaucet(
    val faucetName: String,
    val groups: HashMap<String, RgbAssetGroup>,
    val url: String,
) {
    constructor(
        faucetConfig: FaucetConfig,
        url: String
    ) : this(
        faucetConfig.name,
        faucetConfig.groups,
        url,
    )
}

data class Receiver(
    val recipient: String,
    val expirationSeconds: UInt? = null,
    val bitcoin: Boolean,
)

data class RgbUnspent(
    val assetID: String?,
    val tickerOrName: String?,
    val amount: ULong,
    val settled: Boolean,
) {
    constructor(
        rgbAllocation: RgbAllocation,
        tickerOrName: String?,
    ) : this(
        rgbAllocation.assetId,
        tickerOrName,
        rgbAllocation.amount,
        rgbAllocation.settled,
    )
}

@Parcelize
data class AppOutpoint(val txid: String, val vout: UInt) : Parcelable {
    constructor(outpoint: Outpoint) : this(outpoint.txid, outpoint.vout)

    fun outpointStr(): String {
        return "$txid:$vout"
    }
}

@Parcelize
data class AppTransferConsignmentEndpoint(
    val endpoint: String,
    val protocol: ConsignmentEndpointProtocol,
    val used: Boolean
) : Parcelable {
    constructor(
        transferConsignmentEndpoint: TransferConsignmentEndpoint
    ) : this(
        transferConsignmentEndpoint.endpoint,
        transferConsignmentEndpoint.protocol,
        transferConsignmentEndpoint.used
    )
}

@Parcelize
data class AppTransfer(
    val date: Date,
    val status: TransferStatus,
    val kind: TransferKind,
    val amount: ULong? = null,
    val expiration: Long? = null,
    val txid: String? = null,
    val blindedUTXO: String? = null,
    val unblindedUTXO: AppOutpoint? = null,
    val changeUTXO: AppOutpoint? = null,
    val consignmentEndpoints: List<AppTransferConsignmentEndpoint>? = null,
    var internal: Boolean = false,
) : Parcelable {
    constructor(
        bdkTransfer: TransactionDetails
    ) : this(
        if (bdkTransfer.confirmationTime == null) Date(System.currentTimeMillis())
        else Date(bdkTransfer.confirmationTime!!.timestamp.toLong() * 1000),
        if (bdkTransfer.confirmationTime == null) TransferStatus.WAITING_CONFIRMATIONS
        else TransferStatus.SETTLED,
        if (bdkTransfer.received > bdkTransfer.sent) TransferKind.RECEIVE else TransferKind.SEND,
        amount = AppUtils.uLongAbsDiff(bdkTransfer.received, bdkTransfer.sent),
        txid = bdkTransfer.txid,
    )

    constructor(
        transfer: Transfer
    ) : this(
        Date(transfer.updatedAt * 1000),
        transfer.status,
        transfer.kind,
        amount = transfer.amount,
        expiration = transfer.expiration,
        txid = transfer.txid,
        blindedUTXO = transfer.blindedUtxo,
        unblindedUTXO = transfer.unblindedUtxo?.let { AppOutpoint(it) },
        changeUTXO = transfer.changeUtxo?.let { AppOutpoint(it) },
        consignmentEndpoints =
            transfer.consignmentEndpoints.map { AppTransferConsignmentEndpoint(it) }
    )

    fun deletable(): Boolean {
        return status in listOf(TransferStatus.WAITING_COUNTERPARTY, TransferStatus.FAILED)
    }

    fun incoming(): Boolean {
        return listOf(TransferKind.RECEIVE, TransferKind.ISSUANCE).contains(kind)
    }
}

data class UTXO(
    val txid: String,
    val vout: UInt,
    val satAmount: ULong,
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
        unspent: Unspent,
        rgbUnspents: List<RgbUnspent>,
    ) : this(
        unspent.utxo.outpoint.txid,
        unspent.utxo.outpoint.vout,
        unspent.utxo.btcAmount,
        AppConstants.coloredWallet,
        rgbUnspents
    )

    constructor(
        outpoint: AppOutpoint
    ) : this(
        outpoint.txid,
        outpoint.vout,
        0UL,
        null,
        listOf(),
    )

    fun outpoint(): AppOutpoint {
        return AppOutpoint(txid, vout)
    }
}
