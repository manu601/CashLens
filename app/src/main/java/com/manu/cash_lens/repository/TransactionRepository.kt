package com.manu.cash_lens.repository

import com.manu.cash_lens.database.TransactionDao
import com.manu.cash_lens.database.TransactionEntity

class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    suspend fun insert(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun getAllTransactions(): List<TransactionEntity> {
        return transactionDao.getAllTransactions()
    }

    suspend fun deleteAll() {
        transactionDao.deleteAll()
    }
    suspend fun receiptExists(receipt: String): Boolean {
        return transactionDao.receiptExists(receipt)
    }
    suspend fun getCurrentBalance(): Double? {
        return transactionDao.getCurrentBalance()
    }
    suspend fun getSentSince(startTime: Long): Double {
        return transactionDao.getSentSince(startTime)
    }

    suspend fun getReceivedSince(startTime: Long): Double {
        return transactionDao.getReceivedSince(startTime)
    }

    suspend fun getPayBillSince(startTime: Long): Double {
        return transactionDao.getPayBillSince(startTime)
    }
    suspend fun getTotalSent(): Double {
        return transactionDao.getTotalSent()
    }

    suspend fun getTotalReceived(): Double {
        return transactionDao.getTotalReceived()
    }

    suspend fun getTotalPayBill(): Double {
        return transactionDao.getTotalPayBill()
    }
}