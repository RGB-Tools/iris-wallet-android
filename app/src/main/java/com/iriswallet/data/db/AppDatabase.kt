package com.iriswallet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    version = 1,
    entities = [AutomaticTransaction::class, RgbPendingAsset::class],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun automaticTransactionDao(): AutomaticTransactionDao

    abstract fun rgbPendingAssetDao(): RgbPendingAssetDao
}
