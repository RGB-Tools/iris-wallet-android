package com.iriswallet.data

import com.google.gson.Gson
import com.iriswallet.data.retrofit.FaucetConfig
import com.iriswallet.data.retrofit.ReceiveAssetResponse
import com.iriswallet.data.retrofit.RetrofitModule
import com.iriswallet.data.retrofit.RgbAsset
import com.iriswallet.utils.AppException

object RgbFaucetRepository {
    suspend fun getConfig(url: String, walletIdentifier: String): FaucetConfig? {
        return RetrofitModule.getRgbFaucetApiService(url)
            .getConfig(Keys.rgbFaucetApiKey(), walletIdentifier)
            .body()
    }

    suspend fun receiveRgbAsset(
        url: String,
        walletIdentifier: String,
        blindedUTXO: String,
        group: String
    ): RgbAsset {
        val receiveRgbAssetResponse =
            RetrofitModule.getRgbFaucetApiService(url)
                .receiveAsset(Keys.rgbFaucetApiKey(), walletIdentifier, blindedUTXO, group)
        val successResponse = receiveRgbAssetResponse.body()
        if (successResponse?.asset == null) {
            val errorResponse =
                Gson()
                    .fromJson(
                        receiveRgbAssetResponse.errorBody()!!.charStream(),
                        ReceiveAssetResponse::class.java
                    )
            throw AppException(errorResponse.error)
        }
        return successResponse.asset
    }
}
