package com.manu.cash_lens.models

data class SmsMessage(
    val sender: String,
    val body: String,
    val date: Long
)