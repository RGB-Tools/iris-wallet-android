package com.iriswallet.data.db

import androidx.room.*

@Dao
interface AutomaticTransactionDao {
    @Insert fun insertAutomaticTransactions(vararg automaticTransactions: AutomaticTransaction)

    @Transaction
    @Query("SELECT * FROM AutomaticTransaction")
    fun getAutomaticTransactions(): List<AutomaticTransaction>
}

@Dao
interface HiddenAssetDao {
    @Insert fun insertHiddenAsset(hiddenAsset: HiddenAsset)

    @Transaction @Query("SELECT * FROM HiddenAsset") fun getHiddenAssets(): List<HiddenAsset>

    @Query("DELETE FROM HiddenAsset WHERE id = :hiddenAssetID")
    fun deleteHiddenAsset(hiddenAssetID: String)
}

@Dao
interface RgbPendingAssetDao {
    @Insert fun insertRgbPendingAsset(rgbPendingAsset: RgbPendingAsset)

    @Transaction
    @Query("SELECT * FROM RgbPendingAsset")
    fun getRgbPendingAssets(): List<RgbPendingAsset>

    @Query("DELETE FROM RgbPendingAsset WHERE assetID = :rgbPendingAssetID")
    fun deleteRgbPendingAsset(rgbPendingAssetID: String)
}
