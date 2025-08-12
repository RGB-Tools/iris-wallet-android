package com.iriswallet.utils

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import com.iriswallet.BuildConfig
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.data.db.AppDatabase
import com.iriswallet.utils.AppUtils.Companion.getRgbDir
import java.io.File
import org.rgbtools.Keys
import org.rgbtools.generateKeys
import org.rgbtools.restoreKeys

// Container of objects shared across the whole app
object AppContainer {
    lateinit var appContext: Context

    fun initObject(context: Context) {
        appContext = context
    }

    val backupServerClientID: String by lazy {
        if (BuildConfig.DEBUG) {
            AppConstants.BACKUP_SERVER_CLIENT_ID_DEBUG
        } else {
            AppConstants.BACKUP_SERVER_CLIENT_ID_RELEASE
        }
    }

    val bitcoinNetwork =
        when (BuildConfig.FLAVOR) {
            "bitcoinSignet" -> BitcoinNetwork.SIGNET
            "bitcoinTestnet" -> BitcoinNetwork.TESTNET
            "bitcoinMainnet" -> BitcoinNetwork.MAINNET
            else -> throw RuntimeException("Unknown product flavor")
        }

    var storedMnemonic = ""
    val bitcoinKeys: Keys by lazy {
        if (storedMnemonic.isBlank()) {
            val keys = generateKeys(bitcoinNetwork.toRgbLibNetwork())
            storedMnemonic = keys.mnemonic
            keys
        } else restoreKeys(bitcoinNetwork.toRgbLibNetwork(), storedMnemonic)
    }

    var canUseBiometric = false

    val walletIdentifier: String by lazy { bitcoinKeys.xpub.getSha256() }

    val rgbDir: File by lazy { getRgbDir(appContext.filesDir) }
    private val rgbWalletDir: File by lazy { File(rgbDir, bitcoinKeys.masterFingerprint) }
    val rgbLogsFile: File by lazy { File(rgbWalletDir, "log") }
    val rgbRuntimeLockFile: File by lazy { File(rgbWalletDir, "rgb_runtime.lock") }
    internal val dbPath: File by lazy { appContext.getDatabasePath(AppConstants.APP_DB_NAME)!! }

    val clipboard: ClipboardManager by lazy {
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val db by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppConstants.APP_DB_NAME).build()
    }

    val btcHelpFaucetURLS: List<String> by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.signetHelpFaucets
            BitcoinNetwork.TESTNET -> AppConstants.testnetHelpFaucets
            BitcoinNetwork.MAINNET -> listOf()
        }
    }

    val btcFaucetURL: String? by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> null
            BitcoinNetwork.TESTNET -> AppConstants.BTC_TESTNET_FAUCET_URL
            BitcoinNetwork.MAINNET -> null
        }
    }

    val explorerURL: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.SIGNET_EXPLORER_URL
            BitcoinNetwork.TESTNET -> AppConstants.TESTNET_EXPLORER_URL
            BitcoinNetwork.MAINNET -> AppConstants.MAINNET_EXPLORER_URL
        }
    }

    val electrumURLDefault: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.SIGNET_ELECTRUM_URL
            BitcoinNetwork.TESTNET -> AppConstants.TESTNET_ELECTRUM_URL
            BitcoinNetwork.MAINNET -> AppConstants.MAINNET_ELECTRUM_URL
        }
    }

    val rgbFaucetURLS: List<String> by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> listOf()
            BitcoinNetwork.TESTNET -> listOf()
            BitcoinNetwork.MAINNET -> listOf()
        }
    }

    val proxyURL: String
        get() {
            val endpoint = SharedPreferencesManager.proxyTransportEndpoint
            return if (endpoint.startsWith(AppConstants.RGB_TLS_RPC_URI)) {
                "https://" + endpoint.removePrefix(AppConstants.RGB_TLS_RPC_URI)
            } else {
                "http://" + endpoint.removePrefix(AppConstants.RGB_RPC_URI)
            }
        }

    val proxyTransportEndpointDefault: String by lazy { AppConstants.PROXY_TRANSPORT_ENDPOINT }

    val bitcoinAssetTicker: String by lazy {
        val prefix =
            when (bitcoinNetwork) {
                BitcoinNetwork.SIGNET -> "s"
                BitcoinNetwork.TESTNET -> "t"
                BitcoinNetwork.MAINNET -> ""
            }
        prefix + AppConstants.BITCOIN_ASSET_ID
    }

    val bitcoinAssetName: String by lazy {
        val network =
            when (bitcoinNetwork) {
                BitcoinNetwork.SIGNET,
                BitcoinNetwork.TESTNET ->
                    " (${bitcoinNetwork.toString()
                .lowercase()})"
                BitcoinNetwork.MAINNET -> ""
            }
        "${AppConstants.BITCOIN_ASSET_NAME}$network"
    }

    val bitcoinLogoID: Int by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> R.drawable.bitcoin_signet
            BitcoinNetwork.TESTNET -> R.drawable.bitcoin_testnet
            BitcoinNetwork.MAINNET -> R.drawable.bitcoin_mainnet
        }
    }

    val termsAndConditionsID: Int by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> R.raw.terms_of_service_testnet
            BitcoinNetwork.TESTNET -> R.raw.terms_of_service_testnet
            BitcoinNetwork.MAINNET -> R.raw.terms_of_service_mainnet
        }
    }

    val termsAndConditionsURL: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.TESTNET_TERMS_OF_SERVICE_URL
            BitcoinNetwork.TESTNET -> AppConstants.TESTNET_TERMS_OF_SERVICE_URL
            BitcoinNetwork.MAINNET -> AppConstants.MAINNET_TERMS_OF_SERVICE_URL
        }
    }
}
