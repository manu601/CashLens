package com.manu.cash_lens

import kotlinx.coroutines.launch
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manu.cash_lens.adapters.TransactionAdapter
import com.manu.cash_lens.permission.PermissionHelper
import com.manu.cash_lens.sms.SmsReader
import androidx.lifecycle.lifecycleScope
import com.manu.cash_lens.database.CashLensDatabase
import com.manu.cash_lens.mapper.toEntity
import com.manu.cash_lens.repository.TransactionRepository
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissionHelper = PermissionHelper(this)

        if (!permissionHelper.hasSmsPermission()) {
            permissionHelper.requestSmsPermission()
        }

        val smsReader = SmsReader(this)
        val database = CashLensDatabase.getDatabase(this)
        val repository = TransactionRepository(database.transactionDao())

        val recycler = findViewById<RecyclerView>(R.id.recyclerTransactions)
        recycler.layoutManager = LinearLayoutManager(this)

        val importButton = findViewById<Button>(R.id.btnImportSms)
        val resetButton = findViewById<Button>(R.id.btnResetDatabase)
        val importStatus = findViewById<LinearLayout>(R.id.importStatus)
        val importText = findViewById<TextView>(R.id.importText)
        val progressBar = findViewById<ProgressBar>(R.id.importProgress)

        resetButton.setOnClickListener {

            lifecycleScope.launch {

                repository.deleteAll()

                recycler.adapter = TransactionAdapter(emptyList())

                findViewById<TextView>(R.id.txtBalance).text = "KSh 0.00"

                Log.d("CashLens", "Database cleared.")

            }
        }

        importButton.setOnClickListener {

            lifecycleScope.launch {
                importStatus.visibility = View.VISIBLE
                importText.text = "Reading M-PESA messages..."
                importButton.isEnabled = false

                val transactions = smsReader.getMpesaMessages()

                Log.d(
                    "CashLens",
                    "Found ${transactions.size} M-PESA transactions"
                )

                // Save only new transactions
                transactions.forEach { transaction ->

                    if (!repository.receiptExists(transaction.receipt)) {

                        repository.insert(
                            transaction.toEntity()
                        )

                    } else {

                        Log.d(
                            "CashLens",
                            "Skipping duplicate: ${transaction.receipt}"
                        )

                    }
                }
                importText.text = "Import complete"


                // Read back from Room
                val savedTransactions = repository.getAllTransactions()
                Log.d(
                    "TOP_TRANSACTION",
                    "Balance=${savedTransactions.firstOrNull()?.balance} " +
                            "Receipt=${savedTransactions.firstOrNull()?.receipt}"
                )


                // Convert Entity -> UI model
                val displayList = savedTransactions.map {

                    com.manu.cash_lens.models.Transaction(
                        receipt = it.receipt,
                        provider = it.provider,
                        amount = it.amount,
                        recipient = it.recipient,
                        date = it.date,
                        time = it.time,
                        type = it.type,
                        balance = it.balance,
                        fee = it.fee,
                        smsTimestamp = it.smsTimestamp

                    )
                }
                val balanceText = findViewById<TextView>(R.id.txtBalance)

                val currentBalance = displayList.firstOrNull()?.balance ?: 0.0

                Log.d(
                    "DASHBOARD_BALANCE",
                    "Using first transaction balance = $currentBalance"
                )

                balanceText.text = "Ksh %.2f".format(currentBalance)
                val fuliza = smsReader.getLatestFulizaStatus()

                val outstandingText = findViewById<TextView>(R.id.txtFulizaOutstanding)
                val limitText = findViewById<TextView>(R.id.txtFulizaLimit)

                outstandingText.text = "KSh %.2f".format(fuliza.outstanding)
                limitText.text = "KSh %.2f".format(fuliza.availableLimit)

                recycler.adapter = TransactionAdapter(displayList)
                importStatus.visibility = View.GONE
                importButton.isEnabled = true
            }
        }
    }
}