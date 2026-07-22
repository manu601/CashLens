package com.manu.cash_lens

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manu.cash_lens.adapters.TransactionAdapter
import com.manu.cash_lens.permission.PermissionHelper
import com.manu.cash_lens.sms.SmsReader

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissionHelper = PermissionHelper(this)

        if (!permissionHelper.hasSmsPermission()) {
            permissionHelper.requestSmsPermission()
        }

        val smsReader = SmsReader(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerTransactions)
        recycler.layoutManager = LinearLayoutManager(this)

        val importButton = findViewById<Button>(R.id.btnImportSms)

        importButton.setOnClickListener {

            val transactions = smsReader.getMpesaMessages()

            Log.d(
                "CashLens",
                "Found ${transactions.size} M-PESA transactions"
            )

            recycler.adapter = TransactionAdapter(transactions)
        }
    }
}