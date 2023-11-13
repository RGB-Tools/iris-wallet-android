package com.iriswallet.data.retrofit

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.iriswallet.R
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppException
import retrofit2.Response
import retrofit2.http.*

interface RgbFaucetApiService {
    @GET("receive/config/{walletID}")
    suspend fun getConfig(
        @Header("X-Api-Key") apiKey: String,
        @Path("walletID") walletID: String
    ): Response<FaucetConfig>

    @Headers("Content-Type: application/json")
    @POST("receive/asset")
    suspend fun receiveAsset(
        @Header("X-Api-Key") apiKey: String,
        @Body receiveAssetBody: ReceiveAssetBody
    ): Response<ReceiveAssetResponse>
}

@Keep
data class RgbAssetGroup(
    @SerializedName("requests_left") val requestsLeft: Int,
    @SerializedName("label") val name: String,
    val distribution: Distribution?,
)

@Keep
data class FaucetConfig(
    val name: String,
    val groups: HashMap<String, RgbAssetGroup>,
)

@Keep
data class ReceiveAssetBody(
    @SerializedName("wallet_id") val walletID: String,
    val invoice: String,
    @SerializedName("asset_group") val assetGroup: String?,
)

@Keep
data class ReceiveAssetResponse(
    val asset: RgbAsset?,
    val distribution: Distribution?,
    val error: String?
)

enum class DistributionMode {
    STANDARD,
    RANDOM,
}

@Keep
data class Distribution(
    val mode: Int,
    @SerializedName("random_params") val randomParams: RandomParams?,
) {
    fun modeEnum(): DistributionMode {
        return when (mode) {
            1 -> DistributionMode.STANDARD
            2 -> DistributionMode.RANDOM
            else ->
                throw AppException(
                    AppContainer.appContext.getString(R.string.faucet_unexpected_res)
                )
        }
    }
}

@Keep
data class RandomParams(
    @SerializedName("request_window_open") val requestWindowOpen: String,
    @SerializedName("request_window_close") val requestWindowClose: String,
)

@Keep
data class RgbAsset(
    @SerializedName("asset_id") val assetID: String,
    val schema: String,
    val amount: Long,
    val name: String,
    val precision: Int,
    val ticker: String?,
    val description: String?,
)
