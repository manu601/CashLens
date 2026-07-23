package com.manu.cash_lens.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(

    @PrimaryKey
    val receipt: String,

    val provider: String,

    val amount: Double,

    val recipient: String,

    val date: String,

    val time: String,

    val type: String,

    val balance: Double,

    val fee: Double,

    val smsTimestamp: Long
)