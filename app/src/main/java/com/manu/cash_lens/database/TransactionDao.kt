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
    AND smsTimestamp >= :startTime
""")
    suspend fun getSentSince(startTime: Long): Double


    @Query("""
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE type = 'Received'
    AND smsTimestamp >= :startTime
""")
    suspend fun getReceivedSince(startTime: Long): Double


    @Query("""
    SELECT COALESCE(SUM(amount), 0)
    FROM transactions
    WHERE type = 'PayBill'
    AND smsTimestamp >= :startTime
""")
    suspend fun getPayBillSince(startTime: Long): Double
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



    @Query("""
    SELECT COALESCE(SUM(amount),0)
    FROM transactions
    WHERE type IN ('Sent','PayBill','Withdraw')
""")
    suspend fun getTotalSpent(): Double


    @Query("""
    SELECT COUNT(*)
    FROM transactions
""")
    suspend fun getTransactionCount(): Int

    @Query("""
    SELECT COALESCE(SUM(amount),0)
    FROM transactions
    WHERE type IN ('Sent','PayBill','Withdraw','Fuliza Repayment')
    AND smsTimestamp >= :startTime
""")
    suspend fun getSpentSince(startTime: Long): Double

    @Query("""
SELECT COALESCE(SUM(amount),0)
FROM transactions
WHERE type='Received'
AND smsTimestamp BETWEEN :startTime AND :endTime
""")
    suspend fun getReceivedBetween(
        startTime: Long,
        endTime: Long
    ): Double

    @Query("""
SELECT COALESCE(SUM(amount),0)
FROM transactions
WHERE type IN ('Sent','PayBill','Withdraw')
AND smsTimestamp BETWEEN :startTime AND :endTime
""")
    suspend fun getSpentBetween(
        startTime: Long,
        endTime: Long
    ): Double

    @Query("""
SELECT COUNT(*)
FROM transactions
WHERE smsTimestamp BETWEEN :startTime AND :endTime
""")
    suspend fun getTransactionCountBetween(
        startTime: Long,
        endTime: Long
    ): Int
}