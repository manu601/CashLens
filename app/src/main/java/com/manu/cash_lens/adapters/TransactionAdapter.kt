package com.manu.cash_lens.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.manu.cash_lens.R
import com.manu.cash_lens.models.Transaction

class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipient: TextView = itemView.findViewById(R.id.txtRecipient)
        val amount: TextView = itemView.findViewById(R.id.txtAmount)
        val date: TextView = itemView.findViewById(R.id.txtDate)
        val type: TextView = itemView.findViewById(R.id.txtType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)

        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val formattedAmount = String.format(
            "KSh %,d",
            transaction.amount.toInt()
        )


        holder.recipient.text = transaction.recipient
        holder.date.text = "${transaction.date} • ${transaction.time}"
        holder.type.text = transaction.type
        holder.amount.text = formattedAmount
        holder.recipient.text = when(transaction.type) {
            "FulizaBorrow" -> "Fuliza Borrow"
            "FulizaRepayment" -> "Fuliza Repayment"
            else -> transaction.recipient
        }
        when (transaction.type) {

            "Received" -> {
                holder.amount.setTextColor(
                    android.graphics.Color.parseColor("#2E7D32")
                )
                holder.amount.text = "+ $formattedAmount"
            }

            "Sent" -> {
                holder.amount.setTextColor(
                    android.graphics.Color.parseColor("#D32F2F")
                )
                holder.amount.text = "- $formattedAmount"
            }

            "PayBill" -> {
                holder.amount.setTextColor(
                    android.graphics.Color.parseColor("#F57C00")
                )
                holder.amount.text = "- $formattedAmount"
            }

        }
        when (transaction.type) {

            "Received" -> holder.type.setTextColor(
                android.graphics.Color.parseColor("#2E7D32")
            )

            "Sent" -> holder.type.setTextColor(
                android.graphics.Color.parseColor("#D32F2F")
            )

            "PayBill" -> holder.type.setTextColor(
                android.graphics.Color.parseColor("#1976D2")
            )

            else -> holder.type.setTextColor(
                android.graphics.Color.GRAY
            )

        }
    }

    override fun getItemCount(): Int = transactions.size
}