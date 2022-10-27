package com.iriswallet.data

import com.iriswallet.data.retrofit.RetrofitModule.btcFaucetApiService

object BtcFaucetRepository {

    suspend fun receiveBitcoins(address: String): String {
        val receiveBitcoinsResponse =
            btcFaucetApiService.receiveBitcoins(Keys.btcFaucetApiKey(), address).body()
        if (receiveBitcoinsResponse == null || receiveBitcoinsResponse.txid.isNullOrEmpty())
            throw RuntimeException("Error requesting bitcoins: ${receiveBitcoinsResponse?.error}")
        return receiveBitcoinsResponse.txid
    }
}
