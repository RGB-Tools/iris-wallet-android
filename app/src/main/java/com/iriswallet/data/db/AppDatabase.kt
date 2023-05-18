package com.iriswallet.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    version = 3,
    entities = [HiddenAsset::class, RgbPendingAsset::class],
    autoMigrations =
        [
            AutoMigration(from = 1, to = 2),
            AutoMigration(
                from = 2,
                to = 3,
                spec = AppDatabase.DropAutomaticTransactionsMigration::class
            )
        ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hiddenAssetDao(): HiddenAssetDao

    abstract fun rgbPendingAssetDao(): RgbPendingAssetDao

    @DeleteTable(tableName = "AutomaticTransaction")
    class DropAutomaticTransactionsMigration : AutoMigrationSpec
}
