package org.iriswallet.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AutomaticTransaction(
    @PrimaryKey val txid: String,
)
