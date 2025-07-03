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
    var certified: Boolean,
    val iface: AssetIface? = null,
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
        AppAssetType.RGB20,
        rgbAsset.assetId,
        rgbAsset.name,
        certified,
        iface = rgbAsset.assetIface,
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
        AppAssetType.RGB25,
        rgbAsset.assetId,
        rgbAsset.name,
        certified,
        iface = rgbAsset.assetIface,
        media = rgbAsset.media?.let { AppMedia(it) },
        spendableBalance = rgbAsset.balance.spendable,
        settledBalance = rgbAsset.balance.settled,
        totalBalance = rgbAsset.balance.future,
    )

    constructor(
        rgbPendingAsset: RgbPendingAsset
    ) : this(
        if (rgbPendingAsset.schema == AppAssetType.RGB20.schemaName()) AppAssetType.RGB20
        else AppAssetType.RGB25,
        rgbPendingAsset.assetID,
        rgbPendingAsset.name,
        rgbPendingAsset.certified,
        iface =
            if (rgbPendingAsset.schema == AppAssetType.RGB20.toString()) AssetIface.RGB20
            else AssetIface.RGB25,
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
    RGB25;

    fun schemaName(): String {
        return when (this) {
            BITCOIN -> ""
            RGB20 -> "NIA"
            RGB25 -> "CFA"
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
    val invoice: String,
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
data class AppTransferTransportEndpoint(
    val endpoint: String,
    val transportType: TransportType,
    val used: Boolean
) : Parcelable {
    constructor(
        transferTransportEndpoint: TransferTransportEndpoint
    ) : this(
        transferTransportEndpoint.endpoint,
        transferTransportEndpoint.transportType,
        transferTransportEndpoint.used
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
    val blindedUTXO: String? = null,
    val receiveUTXO: AppOutpoint? = null,
    val changeUTXO: AppOutpoint? = null,
    val transportEndpoints: List<AppTransferTransportEndpoint>? = null,
    var internal: Boolean = false,
    val idx: Int? = null,
    val batchTransferIdx: Int? = null,
) : Parcelable {
    constructor(
        bdkTransfer: TransactionDetails
    ) : this(
        if (bdkTransfer.confirmationTime == null) Date(System.currentTimeMillis())
        else Date(bdkTransfer.confirmationTime!!.timestamp.toLong() * 1000),
        if (bdkTransfer.confirmationTime == null) TransferStatus.WAITING_CONFIRMATIONS
        else TransferStatus.SETTLED,
        if (bdkTransfer.received > bdkTransfer.sent) AppTransferKind.RECEIVE
        else AppTransferKind.SEND,
        amount = AppUtils.uLongAbsDiff(bdkTransfer.received, bdkTransfer.sent),
        txid = bdkTransfer.txid,
    )

    constructor(
        transfer: Transfer
    ) : this(
        Date(transfer.updatedAt * 1000),
        transfer.status,
        AppTransferKind.fromRgbLibTransferKind(transfer.kind),
        amount = transfer.amount,
        expiration = transfer.expiration,
        txid = transfer.txid,
        blindedUTXO = transfer.recipientId,
        receiveUTXO = transfer.receiveUtxo?.let { AppOutpoint(it) },
        changeUTXO = transfer.changeUtxo?.let { AppOutpoint(it) },
        transportEndpoints = transfer.transportEndpoints.map { AppTransferTransportEndpoint(it) },
        idx = transfer.idx,
        batchTransferIdx = transfer.batchTransferIdx,
    )

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
