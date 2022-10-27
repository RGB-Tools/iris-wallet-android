package com.iriswallet.data.retrofit

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface RgbFaucetApiService {
    @GET("receive/config/{xpub}")
    suspend fun getConfig(
        @Header("X-Api-Key") apiKey: String,
        @Path("xpub") xpub: String
    ): Response<FaucetConfig>

    @GET("receive/asset/{xpub}/{blindedutxo}")
    suspend fun receiveAsset(
        @Header("X-Api-Key") apiKey: String,
        @Path("xpub") xpub: String,
        @Path("blindedutxo") blindedutxo: String,
        @Query("asset_group") assetGroup: String,
    ): Response<ReceiveAssetResponse>
}

@Keep
data class RgbAssetGroup(
    @SerializedName("requests_left") val requestsLeft: Int,
    @SerializedName("label") val name: String,
)

@Keep
data class FaucetConfig(
    val name: String,
    val groups: HashMap<String, RgbAssetGroup>,
)

@Keep data class ReceiveAssetResponse(val asset: RgbAsset?, val error: String?)

@Keep
data class RgbAsset(
    @SerializedName("asset_id") val assetID: String,
    val schema: String,
    val amount: Long,
    val name: String,
    val precision: Int,
    val ticker: String?,
    val description: String?,
    @SerializedName("parent_id") val parentID: String?,
)
