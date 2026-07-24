package com.manu.cash_lens.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY smsTimestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE receipt = :receipt)")
    suspend fun receiptExists(receipt: String): Boolean

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT balance FROM transactions ORDER BY smsTimestamp DESC LIMIT 1")
    suspend fun getCurrentBalance(): Double?
    @Query("""
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE type = 'Sent'
""")
    suspend fun getTotalSent(): Double


    @Query("""
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE type = 'Received'
""")
    suspend fun getTotalReceived(): Double


    @Query("""
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE type = 'PayBill'
""")
    suspend fun getTotalPayBill(): Double
}