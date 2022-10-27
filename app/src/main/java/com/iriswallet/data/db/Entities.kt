package com.iriswallet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iriswallet.data.retrofit.RgbAsset

@Entity
data class AutomaticTransaction(
    @PrimaryKey val txid: String,
)

@Entity
data class RgbPendingAsset(
    @PrimaryKey val assetID: String,
    val schema: String,
    val amount: Long,
    val name: String,
    val precision: Int,
    val ticker: String?,
    val description: String?,
    val parentID: String?,
    val timestamp: Long,
) {
    constructor(
        rgbAsset: RgbAsset
    ) : this(
        rgbAsset.assetID,
        rgbAsset.schema,
        rgbAsset.amount,
        rgbAsset.name,
        rgbAsset.precision,
        rgbAsset.ticker,
        rgbAsset.description,
        rgbAsset.parentID,
        System.currentTimeMillis()
    )
}
