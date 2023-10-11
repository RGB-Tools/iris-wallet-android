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
            AppConstants.backupServerClientIDDebug
        } else {
            AppConstants.backupServerClientIDRelease
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
    val mnemonicPassword = null
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
    private val rgbWalletDir: File by lazy { File(rgbDir, bitcoinKeys.xpubFingerprint) }
    val rgbLogsFile: File by lazy { File(rgbWalletDir, "log") }
    internal val dbPath: File by lazy { appContext.getDatabasePath(AppConstants.appDBName)!! }
    val bdkDir: File by lazy { File(appContext.filesDir, AppConstants.bdkDirName) }
    val bdkDBVanillaPath: File by lazy {
        File(bdkDir, AppConstants.bdkDBName.format(AppConstants.vanillaWallet))
    }

    val clipboard: ClipboardManager by lazy {
        appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val db by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppConstants.appDBName).build()
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
            BitcoinNetwork.TESTNET -> AppConstants.btcTestnetFaucetURL
            BitcoinNetwork.MAINNET -> null
        }
    }

    val explorerURL: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.signetExplorerURL
            BitcoinNetwork.TESTNET -> AppConstants.testnetExplorerURL
            BitcoinNetwork.MAINNET -> AppConstants.mainnetExplorerURL
        }
    }

    val electrumURLDefault: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.signetElectrumURL
            BitcoinNetwork.TESTNET -> AppConstants.testnetElectrumURL
            BitcoinNetwork.MAINNET -> AppConstants.mainnetElectrumURL
        }
    }

    val rgbFaucetURLS: List<String> by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> listOf()
            BitcoinNetwork.TESTNET -> AppConstants.rgbTestnetFaucetURLs
            BitcoinNetwork.MAINNET -> AppConstants.rgbMainnetFaucetURLs
        }
    }

    val proxyURL: String
        get() {
            val endpoint = SharedPreferencesManager.proxyTransportEndpoint
            return if (endpoint.startsWith(AppConstants.rgbTLSRpcURI)) {
                "https://" + endpoint.removePrefix(AppConstants.rgbTLSRpcURI)
            } else {
                "http://" + endpoint.removePrefix(AppConstants.rgbRpcURI)
            }
        }

    val proxyTransportEndpointDefault: String by lazy { AppConstants.proxyTransportEndpoint }

    val bitcoinAssetTicker: String by lazy {
        val prefix =
            when (bitcoinNetwork) {
                BitcoinNetwork.SIGNET -> "s"
                BitcoinNetwork.TESTNET -> "t"
                BitcoinNetwork.MAINNET -> ""
            }
        prefix + AppConstants.bitcoinAssetID
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
        "${AppConstants.bitcoinAssetName}$network"
    }

    val bitcoinDerivationPathCoinType: Int by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> 1
            BitcoinNetwork.TESTNET -> 1
            BitcoinNetwork.MAINNET -> 0
        }
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
            BitcoinNetwork.SIGNET -> AppConstants.testnetTermsOfServiceURL
            BitcoinNetwork.TESTNET -> AppConstants.testnetTermsOfServiceURL
            BitcoinNetwork.MAINNET -> AppConstants.mainnetTermsOfServiceURL
        }
    }
}
