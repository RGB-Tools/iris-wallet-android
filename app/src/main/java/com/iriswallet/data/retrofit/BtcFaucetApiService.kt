package com.iriswallet.data.retrofit

import androidx.annotation.Keep
import retrofit2.Response
import retrofit2.http.*

interface BtcFaucetApiService {
    @GET("receive/{address}")
    suspend fun receiveBitcoins(
        @Header("X-Api-Key") apiKey: String,
        @Path("address") address: String,
    ): Response<ReceiveBitcoinsResponse>
}

@Keep data class ReceiveBitcoinsResponse(val txid: String?, val error: String?)
