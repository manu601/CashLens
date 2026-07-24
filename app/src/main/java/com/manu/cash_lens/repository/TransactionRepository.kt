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
}