package com.manu.cash_lens.models

data class Transaction(
    val id: Long = 0,
    val receipt: String,
    val amount: Double,
    val recipient: String,
    val date: String,
    val time: String,
    val type: String,
    val balance: Double
)