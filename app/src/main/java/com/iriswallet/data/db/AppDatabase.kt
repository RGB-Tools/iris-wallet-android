package com.iriswallet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    version = 1,
    entities = [HiddenAsset::class, RgbPendingAsset::class, RgbCertifiedAsset::class],
    autoMigrations = [],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hiddenAssetDao(): HiddenAssetDao

    abstract fun rgbCertifiedAssetDao(): RgbCertifiedAssetDao

    abstract fun rgbPendingAssetDao(): RgbPendingAssetDao
}
