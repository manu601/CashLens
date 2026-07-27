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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: TransactionAdapter

    private var originalList =
        mutableListOf<com.manu.cash_lens.models.TransactionListItem>()
    private var currentSort = "Newest"
    private var currentSearch = ""

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
                val displayList = savedTransactions
                    .sortedByDescending { it.smsTimestamp }
                    .map {

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


                val todaySpent = repository.getSentSince(today)
                val todayReceived = repository.getReceivedSince(today)

                val monthSpent = repository.getSentSince(thisMonth)
                val monthReceived = repository.getReceivedSince(thisMonth)


                Log.d(
                    "SUMMARY",
                    """
    Today Spent: $todaySpent
    Today Received: $todayReceived
    Month Sent: $monthSpent
    Month Received: $monthReceived
    """.trimIndent()

                )
                findViewById<TextView>(R.id.txtReceivedMonth).text =
                    "KSh %.2f".format(monthReceived)

                findViewById<TextView>(R.id.txtSentMonth).text =
                    "KSh %.2f".format(monthSpent)

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
                listItems.forEachIndexed { index, item ->

                    if (item is TransactionListItem.Header) {
                        item.expanded = index == 0
                    }

                }
                adapter = TransactionAdapter(listItems) { header ->

                    header.expanded = !header.expanded

                    updateTransactionList()

                }
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

                        currentSearch = s.toString().trim().lowercase()


                        updateTransactionList()
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                importStatus.visibility = View.GONE
                importButton.isEnabled = true
            }
        }
        val analyticsButton = findViewById<Button>(R.id.btnAnalytics)

        analyticsButton.setOnClickListener {

            val intent = Intent(this, AnalyticsActivity::class.java)
            startActivity(intent)

        }
    }
    private fun updateTransactionList() {

        if (!::adapter.isInitialized) return

        val filteredList = mutableListOf<TransactionListItem>()

        var currentHeader: TransactionListItem.Header? = null
        var headerAdded = false

        originalList.forEach { item ->

            when (item) {

                is TransactionListItem.Header -> {

                    currentHeader = item
                    headerAdded = false
                }

                is TransactionListItem.Item -> {

                    val t = item.transaction

                    val matches =
                        currentSearch.isBlank() ||
                                t.recipient.lowercase().contains(currentSearch) ||
                                t.type.lowercase().contains(currentSearch) ||
                                t.receipt.lowercase().contains(currentSearch) ||
                                t.amount.toString().contains(currentSearch)

                    if (matches) {

                        if (!headerAdded && currentHeader != null) {
                            filteredList.add(currentHeader!!)
                            headerAdded = true
                        }

                        if (currentHeader?.expanded == true) {
                            filteredList.add(item)
                        }
                    }
                }
            }
        }


        val finalList = mutableListOf<TransactionListItem>()

        var monthTransactions =
            mutableListOf<TransactionListItem.Item>()

        var activeHeader: TransactionListItem.Header? = null


        fun addMonth() {

            if (activeHeader != null) {

                // Always show month header
                finalList.add(activeHeader!!)


                // Show transactions only if expanded
                if (activeHeader!!.expanded) {

                    finalList.addAll(
                        when (currentSort) {

                            "Highest Amount" ->
                                monthTransactions.sortedByDescending {
                                    it.transaction.amount
                                }

                            "Lowest Amount" ->
                                monthTransactions.sortedBy {
                                    it.transaction.amount
                                }

                            "Recipient A-Z" ->
                                monthTransactions.sortedBy {
                                    it.transaction.recipient
                                }

                            "Recipient Z-A" ->
                                monthTransactions.sortedByDescending {
                                    it.transaction.recipient
                                }

                            "Oldest" ->
                                monthTransactions.sortedBy {
                                    it.transaction.smsTimestamp
                                }

                            else ->
                                monthTransactions
                        }
                    )
                }
            }

            monthTransactions.clear()
        }


        filteredList.forEach { item ->

            when (item) {

                is TransactionListItem.Header -> {

                    addMonth()

                    activeHeader = item
                }


                is TransactionListItem.Item -> {

                    monthTransactions.add(item)
                }
            }
        }


        // Add last month
        addMonth()


        adapter.updateData(finalList)
    }
    private fun buildTransactionList(
        displayList: List<Transaction>
    ): MutableList<TransactionListItem> {

        val listItems = mutableListOf<TransactionListItem>()

        var currentMonth = ""
        var firstHeader = true

        displayList.forEach { transaction ->

            val normalizedDate = try {

                val inputFormats = listOf(
                    "d/M/yy",
                    "dd/MM/yyyy"
                )

                var parsedDate: Date? = null

                for (format in inputFormats) {

                    try {
                        parsedDate = SimpleDateFormat(
                            format,
                            Locale.getDefault()
                        ).parse(transaction.date)

                        if (parsedDate != null) break

                    } catch (_: Exception) {

                    }
                }

                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(parsedDate!!)

            } catch (e: Exception) {

                transaction.date
            }

            val parts = normalizedDate.split("/")

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
                    TransactionListItem.Header(
                        title = title,
                        expanded = firstHeader
                    )
                )

                firstHeader = false
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