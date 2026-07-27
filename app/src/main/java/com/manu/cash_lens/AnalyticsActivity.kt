package com.manu.cash_lens

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.manu.cash_lens.database.CashLensDatabase
import com.manu.cash_lens.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var repository: TransactionRepository

    private lateinit var txtTotalReceived: TextView
    private lateinit var txtTotalSent: TextView
    private lateinit var txtTotalPayBill: TextView
    private lateinit var txtTransactionCount: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_analytics)

        val database = CashLensDatabase.getDatabase(this)
        repository = TransactionRepository(database.transactionDao())

        txtTotalReceived = findViewById(R.id.txtTotalReceived)
        txtTotalSent = findViewById(R.id.txtTotalSent)
        txtTotalPayBill = findViewById(R.id.txtTotalPayBill)
        txtTransactionCount = findViewById(R.id.txtTransactionCount)


        val btnToday = findViewById<Button>(R.id.btnToday)
        val btnWeek = findViewById<Button>(R.id.btnWeek)
        val btnMonth = findViewById<Button>(R.id.btnMonth)


        btnToday.setOnClickListener {
            loadAnalytics(getTodayStart())
        }


        btnWeek.setOnClickListener {
            loadAnalytics(getWeekStart())
        }


        btnMonth.setOnClickListener {
            loadAnalytics(getMonthStart())
        }


        // Default view
        loadAnalytics(getMonthStart())
    }


    private fun loadAnalytics(startTime: Long) {

        lifecycleScope.launch {

            val received = repository.getReceivedSince(startTime)
            val sent = repository.getSentSince(startTime)
            val payBill = repository.getPayBillSince(startTime)

            txtTotalReceived.text =
                "Money In\nKSh %.2f".format(received)

            txtTotalSent.text =
                "Money Out\nKSh %.2f".format(sent)

            txtTotalPayBill.text =
                "PayBill\nKSh %.2f".format(payBill)

            val count = repository.getTransactionCount()

            txtTransactionCount.text =
                "Transactions\n$count"
        }
    }


    private fun getTodayStart(): Long {

        return Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

        }.timeInMillis
    }


    private fun getWeekStart(): Long {

        return Calendar.getInstance().apply {

            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

        }.timeInMillis
    }


    private fun getMonthStart(): Long {

        return Calendar.getInstance().apply {

            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

        }.timeInMillis
    }
}