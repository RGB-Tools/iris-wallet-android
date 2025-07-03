package com.iriswallet.utils

// Constants shared across the whole app
object AppConstants {
    const val appDBName = "iris_wallet_db"
    const val rgbDirName = ".rgb"
    const val rgbDownloadLogsFileName = "iris-logs-%s-%s.txt"
    const val rgbDownloadMediaFileName = "media_%s"
    const val sharedPreferencesName = "shared_prefs"
    const val encryptedSharedPreferencesName = "secret_shared_prefs"

    const val backupName = "%s.rgb_backup"
    const val backupServerClientIDDebug =
        "115452963739-i8bn94t1imp1svaulc0o2osctn0nbpqt.apps.googleusercontent.com"
    const val backupServerClientIDRelease =
        "527013939550-i6gdjjv727eqct5v53j899jimff13pjq.apps.googleusercontent.com"
    const val backupRestoreTimeout = 120000L

    const val maxAssets = 50
    const val satsForRgb = 9000UL
    const val rgbBlindDuration = 86400U
    const val rgbDefaultPrecision: UByte = 0U
    const val uLongMaxAmount: ULong = 18446744073709551615UL
    const val issueMaxAmount = uLongMaxAmount
    const val maxMediaBytes = 5 * 1024 * 1024
    const val defaultFeeRate = 1.5F
    const val minFeeRate = 1.0
    const val feeRateIntegerPlaces = 3
    const val feeRateDecimalPlaces = 2

    const val coloredWallet = "colored"
    const val vanillaWallet = "vanilla"

    const val derivationChangeVanilla = 1

    const val bitcoinAssetID = "BTC"
    const val bitcoinAssetName = "bitcoin"

    const val rgbRpcURI = "rpc://"
    const val rgbTLSRpcURI = "rpcs://"
    private const val proxyBaseURL = "proxy.iriswallet.com/0.2/json-rpc"
    const val proxyTransportEndpoint = rgbTLSRpcURI + proxyBaseURL

    const val signetElectrumURL = "ssl://electrum.iriswallet.com:50033"
    const val testnetElectrumURL = "ssl://electrum.iriswallet.com:50013"
    const val mainnetElectrumURL = "ssl://electrum.iriswallet.com:50003"

    const val signetExplorerURL = "https://mempool.space/signet/tx/"
    const val testnetExplorerURL = "https://mempool.space/testnet/tx/"
    const val mainnetExplorerURL = "https://mempool.space/tx/"

    val signetHelpFaucets = listOf("https://signetfaucet.com/", "https://alt.signetfaucet.com/")
    val testnetHelpFaucets =
        listOf(
            "https://testnet-faucet.mempool.co/",
            "https://bitcoinfaucet.uo1.net/",
            "https://coinfaucet.eu/en/btc-testnet/",
            "https://testnet-faucet.com/btc-testnet/",
        )

    const val btcTestnetFaucetURL = "https://btc-faucet.iriswallet.com"

    val rgbTestnetFaucetURLs =
        listOf(
            "https://rgb-faucet.iriswallet.com/testnet-planb2023/",
            "https://rgb-faucet.iriswallet.com/testnet-random2023/",
        )
    val rgbMainnetFaucetURLs = listOf("https://rgb-faucet.iriswallet.com/mainnet-random2023/")

    const val assetCertificationServerURL = "https://iriswallet.com"

    const val privacyPolicyURL = "https://iriswallet.com/privacy_policy.html"

    const val testnetTermsOfServiceURL = "https://iriswallet.com/testnet/terms_of_service.html"
    const val mainnetTermsOfServiceURL = "https://iriswallet.com/mainnet/terms_of_service.html"

    const val httpConnectTimeout = 3L
    const val httpReadWriteTimeout = 60L

    const val receiveDataClipLabel = "rgb_receive_data"
    const val assetIdClipLabel = "rgb_asset_id"

    const val transferDateFmt = "yyyy-MM-dd"
    const val transferTimeFmt = "HH:mm:ss"
    const val transferFullDateFmt = "$transferDateFmt $transferTimeFmt"

    const val waitDoubleBackTime = 2000L

    const val veryLongTimeout = 120000L
    const val longTimeout = 40000L
    const val shortTimeout = 20000L

    const val BUNDLE_APP_ASSETS = "app_assets"
    const val BUNDLE_ASSET_ID = "asset_id"
    const val BUNDLE_TRANSFER_ID = "transfer_id"

    const val DOWNLOAD_MEDIA_NOTIFICATION_CHANNEL = "IrisWallet.downloadMedia"
    const val DOWNLOAD_MEDIA_NOTIFICATION_ID = 134

    const val DOWNLOAD_LOGS_NOTIFICATION_CHANNEL = "IrisWallet.downloadLogs"
    const val DOWNLOAD_LOGS_NOTIFICATION_ID = 135

    const val BACKUP_LOGS_NOTIFICATION_CHANNEL = "IrisWallet.doBackup"
    const val BACKUP_LOGS_NOTIFICATION_ID = 136
}
