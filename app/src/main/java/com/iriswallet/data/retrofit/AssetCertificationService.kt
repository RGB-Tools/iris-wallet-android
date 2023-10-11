package com.iriswallet.data.retrofit

import retrofit2.Response
import retrofit2.http.*

interface AssetCertificationService {
    @GET("asset-check/{asset_id}")
    suspend fun isAssetCertified(@Path("asset_id") assetID: String): Response<Unit>
}
