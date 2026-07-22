package com.manu.cash_lens.models

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val recipient: String,
    val date: String,
    val type: String
)