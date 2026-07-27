package com.manu.cash_lens.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.manu.cash_lens.R
import com.manu.cash_lens.models.Transaction
import com.manu.cash_lens.models.TransactionListItem

class TransactionAdapter(
    private var transactions: List<TransactionListItem>,
    private val onHeaderClick: (TransactionListItem.Header) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val header: TextView =
            itemView.findViewById(R.id.txtMonthHeader)

        val arrow: TextView =
            itemView.findViewById(R.id.txtArrow)
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipient: TextView = itemView.findViewById(R.id.txtRecipient)
        val amount: TextView = itemView.findViewById(R.id.txtAmount)
        val date: TextView = itemView.findViewById(R.id.txtDate)
        val type: TextView = itemView.findViewById(R.id.txtType)
    }

    override fun getItemViewType(position: Int): Int {

        return when (transactions[position]) {
            is TransactionListItem.Header -> TYPE_HEADER
            is TransactionListItem.Item -> TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == TYPE_HEADER) {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_month_header, parent, false)

            HeaderViewHolder(view)

        } else {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)

            TransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = transactions[position]) {

            is TransactionListItem.Header -> {

                val viewHolder = holder as HeaderViewHolder

                viewHolder.header.text = item.title
                viewHolder.arrow.text =
                    if (item.expanded) "▼" else "▶"

                viewHolder.itemView.setOnClickListener {
                    onHeaderClick(item)
                }
            }

            is TransactionListItem.Item -> {

                val transaction: Transaction = item.transaction

                val viewHolder = holder as TransactionViewHolder

                val formattedAmount = String.format(
                    "KSh %,d",
                    transaction.amount.toInt()
                )

                viewHolder.recipient.text = when (transaction.type) {
                    "Fuliza Borrow" -> "Fuliza Borrow"
                    "Fuliza Repayment" -> "Fuliza Repayment"
                    else -> transaction.recipient
                }

                viewHolder.date.text =
                    "${transaction.date} • ${transaction.time}"

                viewHolder.type.text = transaction.type

                when (transaction.type) {

                    "Received" -> {

                        viewHolder.amount.setTextColor(
                            Color.parseColor("#2E7D32")
                        )

                        viewHolder.amount.text = "+ $formattedAmount"
                    }

                    "Sent" -> {

                        viewHolder.amount.setTextColor(
                            Color.parseColor("#D32F2F")
                        )

                        viewHolder.amount.text = "- $formattedAmount"
                    }

                    "PayBill",
                    "Fuliza Repayment" -> {

                        viewHolder.amount.setTextColor(
                            Color.parseColor("#F57C00")
                        )

                        viewHolder.amount.text = "- $formattedAmount"
                    }

                    else -> {

                        viewHolder.amount.text = formattedAmount
                    }
                }

                when (transaction.type) {

                    "Received" ->
                        viewHolder.type.setTextColor(
                            Color.parseColor("#2E7D32")
                        )

                    "Sent" ->
                        viewHolder.type.setTextColor(
                            Color.parseColor("#D32F2F")
                        )

                    "PayBill" ->
                        viewHolder.type.setTextColor(
                            Color.parseColor("#1976D2")
                        )

                    "Fuliza Repayment" ->
                        viewHolder.type.setTextColor(
                            Color.parseColor("#F57C00")
                        )

                    else ->
                        viewHolder.type.setTextColor(Color.GRAY)
                }
            }
        }
    }

    override fun getItemCount(): Int = transactions.size
    fun updateData(newItems: List<TransactionListItem>) {
        transactions = newItems
        notifyDataSetChanged()
    }
}