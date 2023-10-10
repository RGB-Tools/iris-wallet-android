package com.iriswallet.utils

import org.rgbtools.AssetSchema

// Constants shared across the whole app
object AppConstants {
    const val APP_DB_NAME = "iris_wallet_db"
    const val RGB_DIR_NAME = ".rgb"
    const val RGB_DOWNLOAD_LOGS_FILE_NAME = "iris-logs-%s-%s.txt"
    const val RGB_DOWNLOAD_MEDIA_FILE_NAME = "media_%s"
    const val SHARED_PREFERENCES_NAME = "shared_prefs"

    const val BACKUP_NAME = "%s.rgb_backup"
    const val BACKUP_SERVER_CLIENT_ID_DEBUG =
        "1083669778297-4do77o15vgt7rpcabs571npevi9aoe3h.apps.googleusercontent.com"
    const val BACKUP_SERVER_CLIENT_ID_RELEASE =
        "767215285080-tj6oj2djrcpuutqdjon985b9qpbugkqn.apps.googleusercontent.com"
    const val BACKUP_RESTORE_TIMEOUT = 120000L

    const val MAX_ASSETS = 50
    const val SATS_FOR_RGB = 9000UL
    const val RGB_BLIND_DURATION = 86400U
    const val RGB_DEFAULT_PRECISION: UByte = 0U
    const val U_LONG_MAX_AMOUNT: ULong = 18446744073709551615UL
    const val ISSUE_MAX_AMOUNT = U_LONG_MAX_AMOUNT
    const val MAX_MEDIA_BYTES = 5 * 1024 * 1024
    const val DEFAULT_FEE_RATE = 2
    const val MIN_FEE_RATE = 1
    const val MAX_FEE_RATE = 1000
    const val FEE_RATE_INTEGER_PLACES = 4
    val supportedSchemas = listOf(AssetSchema.CFA, AssetSchema.NIA)

    const val COLORED_WALLET = "colored"
    const val VANILLA_WALLET = "vanilla"

    const val DERIVATION_CHANGE_VANILLA = 1

    const val BITCOIN_ASSET_ID = "BTC"
    const val BITCOIN_ASSET_NAME = "bitcoin"

    const val RGB_RPC_URI = "rpc://"
    const val RGB_TLS_RPC_URI = "rpcs://"
    private const val PROXY_BASE_URL = "proxy.iriswallet.com/0.2/json-rpc"
    const val PROXY_TRANSPORT_ENDPOINT = RGB_TLS_RPC_URI + PROXY_BASE_URL

    const val SIGNET_ELECTRUM_URL = "ssl://electrum.iriswallet.com:50033"
    const val TESTNET_ELECTRUM_URL = "ssl://electrum.iriswallet.com:50013"
    const val MAINNET_ELECTRUM_URL = "ssl://electrum.iriswallet.com:50003"

    const val SIGNET_EXPLORER_URL = "https://mempool.space/signet/tx/"
    const val TESTNET_EXPLORER_URL = "https://mempool.space/testnet/tx/"
    const val MAINNET_EXPLORER_URL = "https://mempool.space/tx/"

    val signetHelpFaucets = listOf("https://signetfaucet.com/", "https://alt.signetfaucet.com/")
    val testnetHelpFaucets =
        listOf(
            "https://testnet-faucet.mempool.co/",
            "https://bitcoinfaucet.uo1.net/",
            "https://coinfaucet.eu/en/btc-testnet/",
            "https://testnet-faucet.com/btc-testnet/",
        )

    const val BTC_TESTNET_FAUCET_URL = "https://btc-faucet.iriswallet.com"

    const val ASSET_CERTIFICATION_SERVER_URL = "https://iriswallet.com"

    const val PRIVACY_POLICY_URL = "https://iriswallet.com/privacy_policy.html"

    const val TESTNET_TERMS_OF_SERVICE_URL = "https://iriswallet.com/testnet/terms_of_service.html"
    const val MAINNET_TERMS_OF_SERVICE_URL = "https://iriswallet.com/mainnet/terms_of_service.html"

    const val HTTP_CONNECT_TIMEOUT = 3L
    const val HTTP_READ_WRITE_TIMEOUT = 60L

    const val RECEIVE_DATA_CLIP_LABEL = "rgb_receive_data"
    const val ASSET_ID_CLIP_LABEL = "rgb_asset_id"

    const val TRANSFER_DATE_FMT = "yyyy-MM-dd"
    const val TRANSFER_TIME_FMT = "HH:mm:ss"
    const val TRANSFER_FULL_DATE_FMT = "$TRANSFER_DATE_FMT $TRANSFER_TIME_FMT"

    const val WAIT_DOUBLE_BACK_TIME = 2000L

    const val VERY_LONG_TIMEOUT = 120000L
    const val LONG_TIMEOUT = 40000L
    const val SHORT_TIMEOUT = 20000L

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
