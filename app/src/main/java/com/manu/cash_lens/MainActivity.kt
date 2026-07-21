package com.manu.cash_lens

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manu.cash_lens.adapters.TransactionAdapter
import com.manu.cash_lens.models.Transaction

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.recyclerTransactions)

        recycler.layoutManager = LinearLayoutManager(this)

        val sampleTransactions = listOf(
            Transaction(2500.0, "John Doe", "21 Jul 2026", "Received"),
            Transaction(800.0, "KPLC", "20 Jul 2026", "PayBill"),
            Transaction(350.0, "Safaricom", "19 Jul 2026", "Airtime"),
            Transaction(1200.0, "Jane", "18 Jul 2026", "Sent")
        )

        recycler.adapter = TransactionAdapter(sampleTransactions)
    }
}