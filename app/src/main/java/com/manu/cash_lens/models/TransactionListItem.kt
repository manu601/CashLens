package com.manu.cash_lens.models

sealed class TransactionListItem {

    data class Header(
        val title: String
    ) : TransactionListItem()

    data class Item(
        val transaction: Transaction
    ) : TransactionListItem()
}