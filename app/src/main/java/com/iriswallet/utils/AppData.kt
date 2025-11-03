package com.iriswallet.utils

import android.os.Parcelable
import android.util.Log
import com.iriswallet.data.db.RgbPendingAsset
import com.iriswallet.data.retrofit.FaucetConfig
import com.iriswallet.data.retrofit.RgbAssetGroup
import java.util.*
import kotlinx.parcelize.Parcelize
import org.rgbtools.*
import org.rgbtools.BitcoinNetwork

@Parcelize
data class AppAsset(
    val type: AppAssetType,
    val id: String,
    var name: String,
    var certified: Boolean,
    val schema: AssetSchema? = null,
    val ticker: String? = null,
    val media: AppMedia? = null,
    val fromFaucet: Boolean = false,
    var spendableBalance: ULong = 0UL,
    var settledBalance: ULong = 0UL,
    var totalBalance: ULong = 0UL,
    var transfers: List<AppTransfer> = listOf(),
    var hidden: Boolean = false,
) : Parcelable {
    constructor(
        rgbAsset: AssetNia,
        certified: Boolean,
    ) : this(
        AppAssetType.NIA,
        rgbAsset.assetId,
        rgbAsset.name,
        certified,
        schema = AssetSchema.NIA,
        ticker = rgbAsset.ticker,
        media = rgbAsset.media?.let { AppMedia(it) },
        spendableBalance = rgbAsset.balance.spendable,
        settledBalance = rgbAsset.balance.settled,
        totalBalance = rgbAsset.balance.future,
    )

    constructor(
        rgbAsset: AssetCfa,
        certified: Boolean,
    ) : this(
        AppAssetType.CFA,
        rgbAsset.assetId,
        rgbAsset.name,
        certified,
        schema = AssetSchema.CFA,
        media = rgbAsset.media?.let { AppMedia(it) },
        spendableBalance = rgbAsset.balance.spendable,
        settledBalance = rgbAsset.balance.settled,
        totalBalance = rgbAsset.balance.future,
    )

    constructor(
        rgbPendingAsset: RgbPendingAsset
    ) : this(
        if (rgbPendingAsset.schema == AppAssetType.NIA.schemaName()) AppAssetType.NIA
        else AppAssetType.CFA,
        rgbPendingAsset.assetID,
        rgbPendingAsset.name,
        rgbPendingAsset.certified,
        schema =
            if (rgbPendingAsset.schema == AppAssetType.NIA.toString()) AssetSchema.NIA
            else AssetSchema.CFA,
        ticker = rgbPendingAsset.ticker,
        media = null,
        fromFaucet = true,
        totalBalance = rgbPendingAsset.amount.toULong(),
        transfers =
            listOf(
                AppTransfer(
                    Date(rgbPendingAsset.timestamp),
                    TransferStatus.WAITING_COUNTERPARTY,
                    AppTransferKind.RECEIVE,
                    amount = rgbPendingAsset.amount.toULong(),
                )
            ),
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
data class AppMedia(val filePath: String, val mime: MimeType, val mimeString: String) : Parcelable {
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
    NIA,
    CFA;

    fun schemaName(): String {
        return when (this) {
            BITCOIN -> ""
            NIA -> "NIA"
            CFA -> "CFA"
        }
    }
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
    OTHER,
}

data class RgbFaucet(
    val faucetName: String,
    val groups: HashMap<String, RgbAssetGroup>,
    val url: String,
) {
    constructor(
        faucetConfig: FaucetConfig,
        url: String,
    ) : this(faucetConfig.name, faucetConfig.groups, url)
}

data class Receiver(val invoice: String, val expirationSeconds: UInt? = null, val bitcoin: Boolean)

data class RgbUnspent(
    val assetID: String?,
    val tickerOrName: String?,
    val amount: ULong,
    val settled: Boolean,
) {
    companion object {
        fun fromAllocation(rgbAllocation: RgbAllocation, tickerOrName: String?): RgbUnspent {
            val amount = rgbAllocation.assignment.getAmountULong()
            return RgbUnspent(
                assetID = rgbAllocation.assetId,
                tickerOrName = tickerOrName,
                amount = amount,
                settled = rgbAllocation.settled,
            )
        }
    }
}

@Parcelize
data class AppOutpoint(val txid: String, val vout: UInt) : Parcelable {
    constructor(outpoint: Outpoint) : this(outpoint.txid, outpoint.vout)

    fun outpointStr(): String {
        return "$txid:$vout"
    }
}

@Parcelize
data class AppTransferTransportEndpoint(
    val endpoint: String,
    val transportType: TransportType,
    val used: Boolean,
) : Parcelable {
    constructor(
        transferTransportEndpoint: TransferTransportEndpoint
    ) : this(
        transferTransportEndpoint.endpoint,
        transferTransportEndpoint.transportType,
        transferTransportEndpoint.used,
    )
}

enum class AppTransferKind {
    ISSUANCE,
    RECEIVE,
    SEND;

    companion object {
        fun fromRgbLibTransferKind(transferKind: TransferKind): AppTransferKind {
            return when (transferKind) {
                TransferKind.ISSUANCE -> ISSUANCE
                TransferKind.RECEIVE_BLIND -> RECEIVE
                TransferKind.RECEIVE_WITNESS -> RECEIVE
                TransferKind.SEND -> SEND
            }
        }
    }
}

@Parcelize
data class AppTransfer(
    val date: Date,
    val status: TransferStatus,
    val kind: AppTransferKind,
    val amount: ULong? = null,
    val expiration: Long? = null,
    val txid: String? = null,
    val recipientId: String? = null,
    val receiveUTXO: AppOutpoint? = null,
    val changeUTXO: AppOutpoint? = null,
    val transportEndpoints: List<AppTransferTransportEndpoint>? = null,
    var internal: Boolean = false,
    val idx: Int? = null,
    val batchTransferIdx: Int? = null,
    val invoiceString: String? = null,
    val consignmentPath: String? = null,
) : Parcelable {
    constructor(
        transaction: Transaction
    ) : this(
        transaction.confirmationTime?.let { Date(it.timestamp.toLong().times(1000)) }
            ?: Date(System.currentTimeMillis()),
        if (transaction.confirmationTime == null) TransferStatus.WAITING_CONFIRMATIONS
        else TransferStatus.SETTLED,
        if (transaction.received > transaction.sent) AppTransferKind.RECEIVE
        else AppTransferKind.SEND,
        amount = AppUtils.uLongAbsDiff(transaction.received, transaction.sent),
        txid = transaction.txid,
    )

    companion object {
        fun fromTransfer(transfer: Transfer): AppTransfer {
            val amount =
                if (transfer.kind == TransferKind.SEND) {
                    transfer.requestedAssignment!!.getAmountULong()
                } else {
                    transfer.assignments.sumOf { assignment -> assignment.getAmountULong() }
                }
            return AppTransfer(
                date = Date(transfer.updatedAt * 1000),
                status = transfer.status,
                kind = AppTransferKind.fromRgbLibTransferKind(transfer.kind),
                amount = amount,
                expiration = transfer.expiration,
                txid = transfer.txid,
                recipientId = transfer.recipientId,
                receiveUTXO = transfer.receiveUtxo?.let { AppOutpoint(it) },
                changeUTXO = transfer.changeUtxo?.let { AppOutpoint(it) },
                transportEndpoints =
                    transfer.transportEndpoints.map { AppTransferTransportEndpoint(it) },
                idx = transfer.idx,
                batchTransferIdx = transfer.batchTransferIdx,
                invoiceString = transfer.invoiceString,
                consignmentPath = transfer.consignmentPath,
            )
        }
    }

    fun deletable(): Boolean {
        return status in listOf(TransferStatus.WAITING_COUNTERPARTY, TransferStatus.FAILED)
    }

    fun incoming(): Boolean {
        return listOf(AppTransferKind.RECEIVE, AppTransferKind.ISSUANCE).contains(kind)
    }
}

data class UTXO(
    val txid: String,
    val vout: UInt,
    val satAmount: ULong,
    var walletName: String,
    var rgbUnspents: List<RgbUnspent>,
) {
    constructor(
        unspent: Unspent,
        rgbUnspents: List<RgbUnspent>,
    ) : this(
        unspent.utxo.outpoint.txid,
        unspent.utxo.outpoint.vout,
        unspent.utxo.btcAmount,
        if (unspent.utxo.colorable) AppConstants.COLORED_WALLET else AppConstants.VANILLA_WALLET,
        rgbUnspents,
    )

    fun outpoint(): AppOutpoint {
        return AppOutpoint(txid, vout)
    }
}

fun Assignment.getAmountULong(): ULong {
    return when (this) {
        is Assignment.Fungible -> this.amount
        is Assignment.NonFungible -> 1UL
        else -> 0UL
    }
}
