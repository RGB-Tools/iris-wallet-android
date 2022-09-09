package org.iriswallet.utils

// Constants shared across the whole app
object AppConstants {
    const val appDBName = "iris_wallet_db"
    const val bdkDirName = ".bdk"
    const val rgbDirName = ".rgb"
    const val sharedPreferencesName = "shared_prefs"
    const val encryptedSharedPreferencesName = "secret_shared_prefs"

    const val maxAssets = 50
    const val satsForRgb = 11000UL
    const val rgbBlindDuration = 86400U
    const val rgbDefaultPrecision: UByte = 0U

    const val coloredWallet = "colored"
    const val vanillaWallet = "vanilla"

    const val derivationAccountVanilla = 0

    const val bitcoinAssetID = "BTC"
    const val bitcoinAssetName = "bitcoin"

    const val consignmentProxyURL = "http://proxy.rgbtools.org"

    const val signetElectrumURL = "tcp://pandora.network:60601"
    const val testnetElectrumURL = "tcp://pandora.network:60001"

    const val signetExplorerURL = "https://mempool.space/signet/tx/"
    const val testnetExplorerURL = "https://mempool.space/testnet/tx/"

    const val signetConfirmationsExplorerURL = "https://signet.bitcoinexplorer.org"
    const val testnetConfirmationsExplorerURL = "https://testnet.bitcoinexplorer.org"

    val signetFaucets = listOf("https://signetfaucet.com/", "https://alt.signetfaucet.com/")
    val testnetFaucets =
        listOf(
            "https://testnet-faucet.mempool.co/",
            "https://bitcoinfaucet.uo1.net/",
            "https://coinfaucet.eu/en/btc-testnet/",
            "https://testnet-faucet.com/btc-testnet/"
        )

    const val bdkTimeout = 5
    const val bdkRetry = 3
    const val bdkStopGap = 10
    const val bdkDBName = "bdk_db_%s"

    const val receiveDataClipLabel = "rgb_receive_data"

    const val transferDateFmt = "yyyy-MM-dd"
    const val transferTimeFmt = "HH:mm:ss"
    const val transferFullDateFmt = "$transferDateFmt $transferTimeFmt"

    const val waitDoubleBackTime = 2000L

    const val longTimeout = 30000L
    const val shortTimeout = 20000L
}
