package com.iriswallet.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iriswallet.data.retrofit.RgbAsset

@Entity
data class HiddenAsset(
    @PrimaryKey val id: String,
)

@Entity
data class RgbCertifiedAsset(
    @PrimaryKey val assetID: String,
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
    val timestamp: Long,
    @ColumnInfo(defaultValue = "0") val certified: Boolean,
) {
    constructor(
        rgbAsset: RgbAsset,
        certified: Boolean,
    ) : this(
        rgbAsset.assetID,
        rgbAsset.schema,
        rgbAsset.amount,
        rgbAsset.name,
        rgbAsset.precision,
        rgbAsset.ticker,
        rgbAsset.description,
        System.currentTimeMillis(),
        certified,
    )
}
