package com.iriswallet.data.db

import androidx.room.*

@Dao
interface HiddenAssetDao {
    @Insert fun insertHiddenAsset(hiddenAsset: HiddenAsset)

    @Transaction @Query("SELECT * FROM HiddenAsset") fun getHiddenAssets(): List<HiddenAsset>

    @Query("DELETE FROM HiddenAsset WHERE id = :hiddenAssetID")
    fun deleteHiddenAsset(hiddenAssetID: String)
}

@Dao
interface RgbCertifiedAssetDao {
    @Insert fun insertRgbCertifiedAsset(rgbCertifiedAsset: RgbCertifiedAsset)

    @Query("SELECT * FROM RgbCertifiedAsset WHERE assetID = :rgbCertifiedAssetID")
    fun getRgbCertifiedAsset(rgbCertifiedAssetID: String): RgbCertifiedAsset?

    @Query("DELETE FROM RgbCertifiedAsset WHERE assetID = :rgbCertifiedAssetID")
    fun deleteRgbCertifiedAsset(rgbCertifiedAssetID: String)
}

@Dao
interface RgbPendingAssetDao {
    @Insert fun insertRgbPendingAsset(rgbPendingAsset: RgbPendingAsset)

    @Transaction
    @Query("SELECT * FROM RgbPendingAsset")
    fun getRgbPendingAssets(): List<RgbPendingAsset>

    @Query("DELETE FROM RgbPendingAsset") fun deleteAll()

    @Query("DELETE FROM RgbPendingAsset WHERE assetID = :rgbPendingAssetID")
    fun deleteRgbPendingAsset(rgbPendingAssetID: String)
}
