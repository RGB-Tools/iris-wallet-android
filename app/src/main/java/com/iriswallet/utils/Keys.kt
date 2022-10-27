object Keys {

    init {
        System.loadLibrary("native-lib")
    }

    external fun btcFaucetApiKey(): String

    external fun rgbFaucetApiKey(): String
}
