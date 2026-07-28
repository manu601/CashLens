package com.manu.cash_lens

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.manu.cash_lens.database.CashLensDatabase
import com.manu.cash_lens.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var repository: TransactionRepository

    private lateinit var txtSelectedMonth: TextView

    private lateinit var txtMoneyIn: TextView
    private lateinit var txtMoneyOut: TextView
    private lateinit var txtNetCashFlow: TextView

    private lateinit var txtLargestExpense: TextView
    private lateinit var txtMostPaidRecipient: TextView
    private lateinit var txtCategory: TextView
    private lateinit var txtTransactionCount: TextView

    private lateinit var btnPreviousMonth: ImageButton
    private lateinit var btnNextMonth: ImageButton
    private val selectedMonth = java.util.Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        val database = CashLensDatabase.getDatabase(this)
        repository = TransactionRepository(database.transactionDao())

        txtSelectedMonth = findViewById(R.id.txtSelectedMonth)

        txtMoneyIn = findViewById(R.id.txtMoneyIn)
        txtMoneyOut = findViewById(R.id.txtMoneyOut)
        txtNetCashFlow = findViewById(R.id.txtNetCashFlow)

        txtLargestExpense = findViewById(R.id.txtLargestExpense)
        txtMostPaidRecipient = findViewById(R.id.txtMostPaidRecipient)
        txtCategory = findViewById(R.id.txtCategory)
        txtTransactionCount = findViewById(R.id.txtTransactionCount)

        btnPreviousMonth = findViewById(R.id.btnPreviousMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        btnPreviousMonth.setOnClickListener {

            selectedMonth.add(Calendar.MONTH, -1)

            updateMonthTitle()

            loadAnalytics()

        }

        btnNextMonth.setOnClickListener {

            selectedMonth.add(Calendar.MONTH, 1)

            updateMonthTitle()

            loadAnalytics()

        }

        updateMonthTitle()
        loadAnalytics()
    }
    private fun updateMonthTitle() {

        val formatter = SimpleDateFormat(
            "MMMM yyyy",
            Locale.getDefault()
        )

        txtSelectedMonth.text = formatter.format(selectedMonth.time)
    }
    private fun loadAnalytics() {

        lifecycleScope.launch {

            val start = selectedMonth.clone() as Calendar

            start.set(Calendar.DAY_OF_MONTH, 1)
            start.set(Calendar.HOUR_OF_DAY, 0)
            start.set(Calendar.MINUTE, 0)
            start.set(Calendar.SECOND, 0)
            start.set(Calendar.MILLISECOND, 0)

            val end = selectedMonth.clone() as Calendar

            end.set(
                Calendar.DAY_OF_MONTH,
                end.getActualMaximum(Calendar.DAY_OF_MONTH)
            )
            end.set(Calendar.HOUR_OF_DAY, 23)
            end.set(Calendar.MINUTE, 59)
            end.set(Calendar.SECOND, 59)
            end.set(Calendar.MILLISECOND, 999)

            val startTime = start.timeInMillis
            val endTime = end.timeInMillis

            val received =
                repository.getReceivedBetween(startTime, endTime)

            val spent =
                repository.getSpentBetween(startTime, endTime)

            val transactionCount =
                repository.getTransactionCountBetween(startTime, endTime)

            val netCashFlow = received - spent

            txtMoneyIn.text =
                "KSh %.2f".format(received)

            txtMoneyOut.text =
                "KSh %.2f".format(spent)

            txtNetCashFlow.text =
                "KSh %.2f".format(netCashFlow)

            txtTransactionCount.text =
                transactionCount.toString()
        }
    }
}