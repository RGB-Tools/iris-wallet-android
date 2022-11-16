package com.iriswallet.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    version = 2,
    entities = [AutomaticTransaction::class, HiddenAsset::class, RgbPendingAsset::class],
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun automaticTransactionDao(): AutomaticTransactionDao

    abstract fun hiddenAssetDao(): HiddenAssetDao

    abstract fun rgbPendingAssetDao(): RgbPendingAssetDao
}
