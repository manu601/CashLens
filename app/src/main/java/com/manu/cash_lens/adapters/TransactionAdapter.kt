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

        holder.recipient.text = transaction.recipient
        holder.amount.text = "KSh %.2f".format(transaction.amount)
        holder.date.text = "${transaction.date} • ${transaction.time}"
        holder.type.text = transaction.type
    }

    override fun getItemCount(): Int = transactions.size
}