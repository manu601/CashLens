package com.manu.cash_lens.mapper

import com.manu.cash_lens.database.TransactionEntity
import com.manu.cash_lens.models.Transaction

fun Transaction.toEntity(): TransactionEntity {

    return TransactionEntity(
        receipt = receipt,
        provider = provider,
        amount = amount,
        recipient = recipient,
        date = date,
        time = time,
        type = type,
        balance = balance,
        fee = fee,
        smsTimestamp = smsTimestamp
    )
}