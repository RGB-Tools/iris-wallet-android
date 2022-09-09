package org.iriswallet.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AutomaticTransactionDao {
    @Insert fun insertAutomaticTransactions(vararg automaticTransactions: AutomaticTransaction)

    @Transaction
    @Query("SELECT * FROM AutomaticTransaction")
    fun getAutomaticTransactions(): List<AutomaticTransaction>
}
