package org.iriswallet.utils

import android.content.ClipboardManager
import android.content.Context
import androidx.room.Room
import java.io.File
import org.iriswallet.BuildConfig
import org.iriswallet.R
import org.iriswallet.data.db.AppDatabase
import org.iriswallet.utils.AppUtils.Companion.getRgbDir
import org.rgbtools.Keys
import org.rgbtools.generateKeys
import org.rgbtools.restoreKeys

// Container of objects shared across the whole app
object AppContainer {
    lateinit var appContext: Context

    fun initObject(context: Context) {
        appContext = context
    }

    val bitcoinNetwork =
        when (BuildConfig.FLAVOR) {
            "bitcoinSignet" -> BitcoinNetwork.SIGNET
            "bitcoinTestnet" -> BitcoinNetwork.TESTNET
            else -> throw RuntimeException("Unknown product flavor")
        }

    var storedMnemonic = ""
    val mnemonicPassword = null
    val bitcoinKeys: Keys by lazy {
        if (storedMnemonic.isBlank()) generateKeys(bitcoinNetwork.toRgbLibNetwork())
        else restoreKeys(bitcoinNetwork.toRgbLibNetwork(), storedMnemonic)
    }

    var canUseBiometric = false

    val rgbDir: File by lazy { getRgbDir(appContext.filesDir) }
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

    val confirmationsExplorerURL: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.signetConfirmationsExplorerURL
            BitcoinNetwork.TESTNET -> AppConstants.testnetConfirmationsExplorerURL
        }
    }

    val electrumURL: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.signetElectrumURL
            BitcoinNetwork.TESTNET -> AppConstants.testnetElectrumURL
        }
    }

    val explorerURL: String by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> AppConstants.signetExplorerURL
            BitcoinNetwork.TESTNET -> AppConstants.testnetExplorerURL
        }
    }

    val bitcoinAssetTicker: String by lazy {
        val prefix =
            when (bitcoinNetwork) {
                BitcoinNetwork.SIGNET -> "s"
                BitcoinNetwork.TESTNET -> "t"
            }
        prefix + AppConstants.bitcoinAssetID
    }

    val bitcoinAssetName: String by lazy {
        "${AppConstants.bitcoinAssetName} (${bitcoinNetwork.toString().lowercase()})"
    }

    val bitcoinLogoID: Int by lazy {
        when (bitcoinNetwork) {
            BitcoinNetwork.SIGNET -> R.drawable.bitcoin_signet
            BitcoinNetwork.TESTNET -> R.drawable.bitcoin_testnet
        }
    }
}
