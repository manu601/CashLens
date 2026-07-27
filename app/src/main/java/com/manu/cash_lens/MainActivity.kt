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
import java.util.Calendar
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import android.widget.Spinner
import android.widget.ArrayAdapter
import com.manu.cash_lens.models.Transaction
import com.manu.cash_lens.models.TransactionListItem
import android.widget.AdapterView

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: TransactionAdapter

    private var originalList =
        mutableListOf<com.manu.cash_lens.models.TransactionListItem>()
    private var currentSort = "Newest"

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
        val progressBar = findViewById<ProgressBar>(R.id.importProgress)
        val spinnerSort = findViewById<Spinner>(R.id.spinnerSort)

        val recycler = findViewById<RecyclerView>(R.id.recyclerTransactions)
        recycler.layoutManager = LinearLayoutManager(this)
        val sortOptions = listOf(
            "Newest",
            "Oldest",
            "Highest Amount",
            "Lowest Amount",
            "Recipient A-Z",
            "Recipient Z-A"
        )

        val sortAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            sortOptions
        )

        spinnerSort.adapter = sortAdapter
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                currentSort = sortOptions[position]

                updateTransactionList()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }
        val searchBox = findViewById<android.widget.EditText>(R.id.etSearch)

        val importButton = findViewById<Button>(R.id.btnImportSms)
        val resetButton = findViewById<Button>(R.id.btnResetDatabase)
        val importStatus = findViewById<LinearLayout>(R.id.importStatus)
        val importText = findViewById<TextView>(R.id.importText)


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
                val calendar = Calendar.getInstance()

// Start of today
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis


// Start of this month
                val thisMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis


                val todaySent = repository.getSentSince(today)
                val todayReceived = repository.getReceivedSince(today)

                val monthSent = repository.getSentSince(thisMonth)
                val monthReceived = repository.getReceivedSince(thisMonth)


                Log.d(
                    "SUMMARY",
                    """
    Today Sent: $todaySent
    Today Received: $todayReceived
    Month Sent: $monthSent
    Month Received: $monthReceived
    """.trimIndent()

                )
                findViewById<TextView>(R.id.txtReceivedMonth).text =
                    "KSh %.2f".format(monthReceived)

                findViewById<TextView>(R.id.txtSentMonth).text =
                    "KSh %.2f".format(monthSent)

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

                val listItems = buildTransactionList(displayList)
                adapter = TransactionAdapter(listItems)
                recycler.adapter = adapter

                originalList.clear()
                originalList.addAll(listItems)
                searchBox.addTextChangedListener(object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        val query = s.toString().trim().lowercase()

                        if (query.isEmpty()) {
                            adapter.updateData(originalList)
                            return
                        }

                        val filteredList = mutableListOf<com.manu.cash_lens.models.TransactionListItem>()

                        var currentHeader:
                                com.manu.cash_lens.models.TransactionListItem.Header? = null

                        var headerAdded = false

                        originalList.forEach { item ->

                            when (item) {

                                is com.manu.cash_lens.models.TransactionListItem.Header -> {

                                    currentHeader = item
                                    headerAdded = false
                                }

                                is com.manu.cash_lens.models.TransactionListItem.Item -> {

                                    val t = item.transaction

                                    val matches =
                                        t.recipient.lowercase().contains(query) ||
                                                t.type.lowercase().contains(query) ||
                                                t.receipt.lowercase().contains(query) ||
                                                t.amount.toString().contains(query)

                                    if (matches) {

                                        if (!headerAdded && currentHeader != null) {
                                            filteredList.add(currentHeader!!)
                                            headerAdded = true
                                        }

                                        filteredList.add(item)
                                    }
                                }
                            }
                        }

                        adapter.updateData(filteredList)
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                importStatus.visibility = View.GONE
                importButton.isEnabled = true
            }
        }
    }
    private fun updateTransactionList() {
        if (!::adapter.isInitialized) return

        val sorted = when (currentSort) {

            "Newest" ->
                originalList

            "Oldest" ->
                originalList.reversed()

            "Highest Amount" -> {

                val headers = mutableListOf<TransactionListItem>()

                val transactions = originalList
                    .filterIsInstance<TransactionListItem.Item>()
                    .sortedByDescending { it.transaction.amount }

                headers.addAll(transactions)

                headers
            }

            "Lowest Amount" -> {

                val headers = mutableListOf<TransactionListItem>()

                val transactions = originalList
                    .filterIsInstance<TransactionListItem.Item>()
                    .sortedBy { it.transaction.amount }

                headers.addAll(transactions)

                headers
            }

            "Recipient A-Z" -> {

                val headers = mutableListOf<TransactionListItem>()

                val transactions = originalList
                    .filterIsInstance<TransactionListItem.Item>()
                    .sortedBy { it.transaction.recipient }

                headers.addAll(transactions)

                headers
            }

            "Recipient Z-A" -> {

                val headers = mutableListOf<TransactionListItem>()

                val transactions = originalList
                    .filterIsInstance<TransactionListItem.Item>()
                    .sortedByDescending { it.transaction.recipient }

                headers.addAll(transactions)

                headers
            }

            else -> originalList
        }

        adapter.updateData(sorted)
    }
    private fun buildTransactionList(
        displayList: List<Transaction>
    ): MutableList<TransactionListItem> {

        val listItems = mutableListOf<TransactionListItem>()

        var currentMonth = ""

        displayList.forEach { transaction ->

            val parts = transaction.date.split("/")

            val monthName = when (parts[1].toInt()) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                else -> "December"
            }

            val title = "$monthName ${parts[2]}"

            if (title != currentMonth) {

                currentMonth = title

                listItems.add(
                    TransactionListItem.Header(title)
                )
            }

            listItems.add(
                TransactionListItem.Item(transaction)
            )
        }

        return listItems
    }
    private suspend fun importTransactions() {

    }
}