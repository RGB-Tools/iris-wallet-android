package com.iriswallet.data

import com.google.gson.Gson
import com.iriswallet.R
import com.iriswallet.data.retrofit.Distribution
import com.iriswallet.data.retrofit.FaucetConfig
import com.iriswallet.data.retrofit.ReceiveAssetBody
import com.iriswallet.data.retrofit.ReceiveAssetResponse
import com.iriswallet.data.retrofit.RetrofitModule
import com.iriswallet.data.retrofit.RgbAsset
import com.iriswallet.utils.AppContainer
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
        invoice: String,
        group: String
    ): Pair<RgbAsset, Distribution> {
        val receiveAssetBody = ReceiveAssetBody(walletIdentifier, invoice, group)
        val receiveRgbAssetResponse =
            RetrofitModule.getRgbFaucetApiService(url)
                .receiveAsset(Keys.rgbFaucetApiKey(), receiveAssetBody)
        val successResponse = receiveRgbAssetResponse.body()
        if (successResponse?.asset == null || successResponse.distribution == null) {
            var errDetailsMsg = AppContainer.appContext.getString(R.string.faucet_unexpected_res)
            val errBody = receiveRgbAssetResponse.errorBody()
            if (errBody != null) {
                val errorResponse =
                    Gson().fromJson(errBody.charStream(), ReceiveAssetResponse::class.java)
                if (!errorResponse.error.isNullOrBlank()) errDetailsMsg = errorResponse.error
            }
            throw AppException(errDetailsMsg)
        }
        return Pair(successResponse.asset, successResponse.distribution)
    }
}
