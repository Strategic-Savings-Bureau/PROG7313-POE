package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.TransactionHistoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityTransactionsBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.graph_data.TransactionGraphData
import com.ssba.strategic_savings_budget_app.graph_data.TransactionType
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.StreakManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 	* Code Attribution
 	* Purpose:
 	*   - Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	*   - Creating and displaying an AlertDialog in an Android app
 	*   - Setting up Bottom Navigation View with OnItemSelectedListener for navigation between activities
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Implementing the Material DatePicker for selecting dates in the app
 	*   - Data Visualisation with Line Chart
 	* Author: Android Developers / Firebase Team / MPAndroidChart
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
 	*   - Bottom Navigation View: https://developer.android.com/reference/com/google/android/material/bottomnavigation/BottomNavigationView
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Material DatePicker: https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker
 	*   - MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
*/

class TransactionsActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityTransactionsBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View components
    private lateinit var btnRewards: ImageButton
    private lateinit var btnIncomeTransactions: ImageButton
    private lateinit var btnExpenseTransactions: ImageButton
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var lcTransactions: LineChart
    private lateinit var cgDays: ChipGroup
    // endregion

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miTransactions

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialise view components
        btnRewards = binding.btnRewards
        btnIncomeTransactions = binding.btnIncomeTransactions
        btnExpenseTransactions = binding.btnExpenseTransactions
        tvTotalIncome = binding.tvTotalIncome
        tvTotalExpenses = binding.tvTotalExpense
        rvTransactions = binding.rvTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        lcTransactions = binding.lineChart
        cgDays = binding.chipGroup
        // endregion

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        lifecycleScope.launch {

            // Get the current user's ID
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@TransactionsActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Get the total income and expenses for the current user
            val totalIncome = getTotalIncome(db, userId)
            val totalExpenses = getTotalExpenses(db, userId)

            // Set the text of the total income and expenses
            tvTotalIncome.text = currencyFormat.format(totalIncome)
            tvTotalExpenses.text = currencyFormat.format(totalExpenses)

            // set up the recycler view and Line Graph
            val transactions = getAllTransactions(db, userId)

            if (transactions.isEmpty())
            {
                rvTransactions.visibility = View.GONE
                tvNoTransactions.visibility = View.VISIBLE
                cgDays.visibility = View.GONE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                // Set up the recycler view
                val adapter = TransactionHistoryAdapter(transactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@TransactionsActivity)

                rvTransactions.adapter = adapter

                // Set up the Line Graph
                val graphData = parseTransactionGraphData(transactions)
                setupLineChart(lcTransactions, graphData)
            }
        }


        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    @SuppressLint("SetTextI18n")
    @Suppress("LABEL_NAME_CLASH")
    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            StreakManager(this).showStreakDialog()
        }

        btnIncomeTransactions.setOnClickListener {

            // Navigate to Income Transactions Activity
            startActivity(Intent(this, IncomeHistoryActivity::class.java))
        }

        btnExpenseTransactions.setOnClickListener {

            // Navigate to Expense Transactions Activity
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }

        btnDateFilter.setOnClickListener {

            // show date picker dialog
            val dialogView = layoutInflater.inflate(R.layout.dialog_date_range_filter, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            // access view components in dialog
            val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
            val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
            val btnApplyDateFilter = dialogView.findViewById<Button>(R.id.btnApplyDateFilter)
            val btnClearFilter = dialogView.findViewById<Button>(R.id.btnClearFilter)

            etStartDate.setOnClickListener {
                showDatePicker(true, etStartDate, etEndDate)
            }

            etEndDate.setOnClickListener {
                showDatePicker(false, etStartDate, etEndDate)
            }

            btnApplyDateFilter.setOnClickListener {

                // get the selected dates
                val startDate = etStartDate.text.toString()
                val endDate = etEndDate.text.toString()

                // check if the dates are empty
                if (startDate.isEmpty() || endDate.isEmpty())
                {
                    Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // convert the dates to Date objects
                val startDateObj =
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(startDate)
                val endDateObj =
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(endDate)

                // check if the dates are valid
                if (startDateObj != null && endDateObj != null)
                {
                    lifecycleScope.launch {

                        // Get the current user's ID
                        val userId = auth.currentUser?.uid

                        if (userId == null)
                        {
                            startActivity(Intent(this@TransactionsActivity, LoginActivity::class.java))
                            finish()
                            return@launch
                        }

                        // set up the recycler view and Line Graph
                        val transactions = getAllTransactions(db, userId)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            cgDays.visibility = View.GONE

                            dialog.dismiss()
                            Toast.makeText(this@TransactionsActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // filter the transactions by date
                        val filteredTransactions = filterTransactionsByDateRange(transactions, startDateObj, endDateObj)

                        if (filteredTransactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            cgDays.visibility = View.GONE
                            Toast.makeText(this@TransactionsActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {

                            val totalIncomeAndExpenses = calculateIncomeAndExpenseValues(filteredTransactions)

                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                            tvTotalIncome.text = currencyFormat.format(totalIncomeAndExpenses[0])
                            tvTotalExpenses.text = currencyFormat.format(totalIncomeAndExpenses[1])

                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE
                            cgDays.visibility = View.GONE

                            // Set up the recycler view
                            val adapter = TransactionHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@TransactionsActivity)

                            rvTransactions.adapter = adapter

                            // Set up the Line Graph
                            val graphData = parseTransactionGraphData(filteredTransactions)
                            setupLineChart(lcTransactions, graphData)

                            dialog.dismiss()
                            Toast.makeText(this@TransactionsActivity, "Transactions Filtered Successfully", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                    }
                }
                else
                {
                    Toast.makeText(this, "Invalid Date Range", Toast.LENGTH_SHORT).show()
                }

            }

            btnClearFilter.setOnClickListener {

                lifecycleScope.launch {

                    // Get the current user's ID
                    val userId = auth.currentUser?.uid

                    if (userId == null)
                    {
                        startActivity(Intent(this@TransactionsActivity, LoginActivity::class.java))
                        finish()
                        return@launch
                    }

                    // set up the recycler view
                    val transactions = getAllTransactions(db, userId)

                    if (transactions.isEmpty())
                    {
                        rvTransactions.visibility = View.GONE
                        tvNoTransactions.visibility = View.VISIBLE
                        cgDays.visibility = View.GONE

                        dialog.dismiss()
                        Toast.makeText(this@TransactionsActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    else
                    {

                        val totalIncomeAndExpenses = calculateIncomeAndExpenseValues(transactions)

                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                        tvTotalIncome.text = currencyFormat.format(totalIncomeAndExpenses[0])
                        tvTotalExpenses.text = currencyFormat.format(totalIncomeAndExpenses[1])

                        rvTransactions.visibility = View.VISIBLE
                        tvNoTransactions.visibility = View.GONE
                        cgDays.visibility = View.VISIBLE

                        // Set up the recycler view and Line Graph
                        val adapter = TransactionHistoryAdapter(transactions)

                        rvTransactions.layoutManager = LinearLayoutManager(this@TransactionsActivity)

                        rvTransactions.adapter = adapter

                        // Set up the Line Graph
                        val graphData = parseTransactionGraphData(transactions)
                        setupLineChart(lcTransactions, graphData)

                        dialog.dismiss()
                        Toast.makeText(this@TransactionsActivity, "Filter Cleared", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                }
            }

        }

        // Set up Bottom Navigation View onClickListener
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                // Navigate to Main (Home) Activity
                R.id.miHome -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Analysis Activity
                R.id.miAnalysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Transactions Activity
                R.id.miTransactions -> true
                // Navigate to Savings Activity
                R.id.miSavings -> {
                    startActivity(Intent(this, SavingsActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Profile Activity
                R.id.miSettings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }

        // Set a listener for when the selection state of chips changes
        cgDays.setOnCheckedStateChangeListener { _, checkedIds ->

            // Get the first selected chip ID, or return if none is selected
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            // Get the currently authenticated user's ID
            val userId = auth.currentUser?.uid

            // If the user is not authenticated, redirect them to the login screen
            if (userId == null) {
                startActivity(Intent(this@TransactionsActivity, LoginActivity::class.java))
                finish()
                return@setOnCheckedStateChangeListener
            }

            lifecycleScope.launch {
                // Retrieve all transactions from the database for the current user
                val transactions = getAllTransactions(db, userId)

                // Convert raw transactions into a format suitable for the graph
                val graphData = parseTransactionGraphData(transactions)

                // Filter and update the graph based on the selected chip
                when (checkedId) {
                    R.id.chip7days -> filterGraphTransactionsByDays(7, graphData, lcTransactions)
                    R.id.chip14days -> filterGraphTransactionsByDays(14, graphData, lcTransactions)
                    R.id.chip30days -> filterGraphTransactionsByDays(30, graphData, lcTransactions)
                    else -> setupLineChart(lcTransactions, graphData) // Show all data if no chip matched
                }
            }
        }

    }

    // region Transaction Helper Methods

    // Method to get all the expenses for the current user
    private suspend fun getAllExpensesForUser(db: AppDatabase, userId: String): List<Expense>
    {
        val allExpenses = mutableListOf<Expense>()

        // Step 1: Get the user's expense categories
        val userWithCategories = db.userDao.getUserWithExpenseCategories(userId)

        if (userWithCategories.isNotEmpty())
        {
            val expenseCategories = userWithCategories[0].expenseCategories

            // Step 2: For each category, get the expenses
            for (category in expenseCategories)
            {
                val expensesWithCategory = db.expenseCategoryDao.getExpensesByCategoryName(category.name)

                if (expensesWithCategory.isNotEmpty())
                {
                    allExpenses.addAll(expensesWithCategory[0].expenses)
                }
            }
        }

        // Step 3: Return the combined list
        return allExpenses
    }

    private suspend fun getAllSavingTransactionsForUser(db: AppDatabase, userId: String): List<Saving> {

        val allSavings = mutableListOf<Saving>()

        // Step 1: Get the user's saving goals
        val userWithSavingGoals = db.userDao.getUserWithSavingGoals(userId)

        if (userWithSavingGoals.isNotEmpty()) {
            val savingGoals = userWithSavingGoals[0].savingGoals

            // Step 2: For each category, get the expenses
            for (goal in savingGoals) {
                val savingGoalsWithSavings =
                    db.savingsGoalDao.getSavingsBySavingGoalTitle(goal.title)

                if (savingGoalsWithSavings.isNotEmpty()) {
                    allSavings.addAll(savingGoalsWithSavings[0].savings)
                }
            }
        }

        // Step 3: Return the combined list
        return allSavings
    }

    // Method to get all transactions for the current user
    private suspend fun getAllTransactions(db: AppDatabase, userId: String): List<Any>
    {
        // get all the incomes for the current user
        val incomes = db.userDao.getUserWithIncomes(userId).firstOrNull()?.incomes ?: emptyList()

        // get all the expenses for the current user
        val expenses = getAllExpensesForUser(db, userId)

        // get all the savings for the current user
        val savings = getAllSavingTransactionsForUser(db, userId)

        // combine the three lists
        val combinedTransactions = mutableListOf<Any>()
        combinedTransactions.addAll(incomes)
        combinedTransactions.addAll(expenses)
        combinedTransactions.addAll(savings)

        // Sort by date descending
        val sortedList = combinedTransactions.sortedWith(
            compareByDescending<Any> { item ->
                when (item) {
                    is Income -> item.date
                    is Expense -> item.date
                    is Saving -> item.date
                    else -> Date(0)
                }
            }.thenByDescending { item ->
                when (item) {
                    is Income -> item.date.time
                    is Expense -> item.date.time
                    is Saving -> item.date.time
                    else -> 0L
                }
            }
        )


        // return the sorted list
        return sortedList
    }

    @SuppressLint("DefaultLocale")
    private suspend fun getTotalIncome(db: AppDatabase, userId: String): Double
    {
        val userWithIncomes = db.userDao.getUserWithIncomes(userId)

        var totalIncome = 0.00

        if (userWithIncomes.isEmpty())
        {
            return totalIncome
        }

        for (income in userWithIncomes[0].incomes)
        {
            totalIncome += income.amount
        }


        return totalIncome
    }


    @SuppressLint("DefaultLocale")
    private suspend fun getTotalExpenses(db: AppDatabase, userId: String): Double
    {
        var totalExpenses = 0.00

        val expenses = getAllExpensesForUser(db, userId)

        if (expenses.isEmpty())
        {
            return totalExpenses
        }

        for (expense in expenses)
        {
            totalExpenses += expense.amount
        }

        return totalExpenses
    }


    // Method to filter transactions by date range
    private fun filterTransactionsByDateRange(list: List<Any>, startDate: Date, endDate: Date): List<Any>
    {
        return list
            .filter { item ->
                val date = when (item) {
                    is Income -> item.date
                    is Expense -> item.date
                    is Saving -> item.date
                    else -> null
                }
                date != null && date in startDate..endDate
            }
            .sortedWith(
                compareByDescending<Any> { item ->
                    when (item) {
                        is Income -> item.date
                        is Expense -> item.date
                        is Saving -> item.date
                        else -> Date(0)
                    }
                }.thenByDescending { item ->
                    when (item) {
                        is Income -> item.date.time
                        is Expense -> item.date.time
                        is Saving -> item.date.time
                        else -> 0L
                    }
                }
            )
    }

    // calculate income and expense values from a list of transactions
    @SuppressLint("DefaultLocale")
    private fun calculateIncomeAndExpenseValues(list: List<Any>): List<Double>
    {
        var totalIncome = 0.00
        var totalExpenses = 0.00

        if (list.isEmpty())
        {
            return listOf(totalIncome, totalExpenses)
        }

        for (item in list)
        {
            if (item is Income)
            {
                totalIncome += item.amount

            }
            else if (item is Expense)
            {
                totalExpenses += item.amount

            }
        }

        return listOf(totalIncome, totalExpenses)
    }


    // Converts a list of transactions (Income, Expense, Saving) into a list of TransactionGraphData
    private fun parseTransactionGraphData(transactions: List<Any>): List<TransactionGraphData>
    {
        // Initialize a mutable list to store the parsed graph data
        val graphData = mutableListOf<TransactionGraphData>()

        // If the transaction list is empty, return the empty list early
        if (transactions.isEmpty()) {
            return graphData
        }

        // Iterate through each transaction in the list
        for (transaction in transactions)
        {
            // Extract the date based on the transaction type
            val date = when (transaction) {
                is Income -> transaction.date
                is Expense -> transaction.date
                is Saving -> transaction.date
                else -> Date(0)
            }

            // Extract the amount based on the transaction type
            val amount = when (transaction) {
                is Income -> transaction.amount
                is Expense -> transaction.amount
                is Saving -> transaction.amount
                else -> 0.00
            }

            // Determine the transaction type for categorization in the graph
            val type = when (transaction) {
                is Income -> TransactionType.INCOME
                is Expense -> TransactionType.EXPENSE
                is Saving -> TransactionType.SAVINGS
                else -> TransactionType.INCOME
            }

            // Add the parsed data to the list as a TransactionGraphData object
            graphData.add(TransactionGraphData(amount.toFloat(), date, type))
        }

        // Return the list sorted by date in ascending order (oldest to newest)
        return graphData.sortedBy { it.date }
    }

    // endregion

    // region Date picker Launcher
    private fun showDatePicker(isStart: Boolean, etStartDate: EditText, etEndDate: EditText)
    {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // Only allow today or earlier
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Select Start Date" else "Select End Date")
            .setCalendarConstraints(constraints)
            .build()

        // Use supportFragmentManager instead of childFragmentManager
        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = dateFormat.format(Date(selection))
            if (isStart)
            {
                etStartDate.setText(selectedDate)
            }
            else
            {
                etEndDate.setText(selectedDate)
            }
        }
    }
    // endregion

    // region Set Up Line Graph

    // Filters transactions to include only those within the last 'days' and updates the chart
    private fun filterGraphTransactionsByDays(days: Int, allTransactions: List<TransactionGraphData>, lineChart: LineChart)
    {
        // Get the current date and time
        val now = Date()

        // Create a Calendar instance and subtract the given number of days from the current date
        val calendar = Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_YEAR, -days)
        }

        // Get the calculated start date
        val startDate = calendar.time

        // Filter transactions that fall within the start date and now
        val filteredTransactions = allTransactions.filter {
            it.date in startDate..now
        }

        // Update the chart with the filtered transactions
        setupLineChart(lineChart, filteredTransactions)
    }


    // Configures and displays a line chart based on the provided transaction data
    private fun setupLineChart(lineChart: LineChart, transactions: List<TransactionGraphData>) {

        // Detect whether the app is in dark mode for styling purposes
        val isDarkMode = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        // Set colors based on theme
        val chartTextColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val backgroundColor = if (isDarkMode) Color.TRANSPARENT else Color.WHITE

        // Separate lists for income, expense, and savings data points
        val incomeEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        val savingsEntries = ArrayList<Entry>()

        // Formatter for x-axis dates
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "ZA"))

        // Populate entries for each transaction type
        transactions.forEach {
            val xValue = it.date.time.toFloat() // Use epoch time as x-axis value

            // Skip transactions with zero or negative amounts for a cleaner graph
            if (it.amount <= 0f) return@forEach

            // Add entry to the correct dataset
            when (it.type) {
                TransactionType.INCOME -> incomeEntries.add(Entry(xValue, it.amount))
                TransactionType.EXPENSE -> expenseEntries.add(Entry(xValue, it.amount))
                TransactionType.SAVINGS -> savingsEntries.add(Entry(xValue, it.amount))
            }
        }

        // Sort entries chronologically to ensure proper chart rendering
        incomeEntries.sortBy { it.x }
        expenseEntries.sortBy { it.x }
        savingsEntries.sortBy { it.x }

        // Define line colors using theme resources
        val colourGreen = ContextCompat.getColor(this@TransactionsActivity, R.color.income_green)
        val colourRed = ContextCompat.getColor(this@TransactionsActivity, R.color.expense_red)
        val colourBlue = ContextCompat.getColor(this@TransactionsActivity, R.color.savings_blue)

        // Configure dataset for income
        val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
            color = colourGreen
            setCircleColor(colourGreen)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
        }

        // Configure dataset for expenses
        val expenseDataSet = LineDataSet(expenseEntries, "Expenses").apply {
            color = colourRed
            setCircleColor(colourRed)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Configure dataset for savings
        val savingsDataSet = LineDataSet(savingsEntries, "Savings").apply {
            color = colourBlue
            setCircleColor(colourBlue)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Combine datasets into one LineData object, include only non-empty datasets
        val lineData = LineData().apply {
            if (incomeEntries.isNotEmpty()) addDataSet(incomeDataSet)
            if (expenseEntries.isNotEmpty()) addDataSet(expenseDataSet)
            if (savingsEntries.isNotEmpty()) addDataSet(savingsDataSet)
        }

        // Set the data on the chart
        lineChart.data = lineData

        // Configure x-axis (date-based)
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return dateFormat.format(Date(value.toLong()))
            }
        }
        xAxis.labelRotationAngle = -45f // Tilt labels for better readability
        xAxis.textColor = chartTextColor
        xAxis.gridColor = chartTextColor
        xAxis.axisLineColor = chartTextColor
        xAxis.granularity = 24 * 60 * 60 * 1000f // One-day granularity

        // Add padding before and after the data range for better visuals
        if (transactions.isNotEmpty()) {
            val sortedDates = transactions.map { it.date.time }.sorted()
            val twoDaysMillis = 2 * 24 * 60 * 60 * 1000L
            xAxis.axisMinimum = (sortedDates.first() - twoDaysMillis).toFloat()
            xAxis.axisMaximum = (sortedDates.last() + twoDaysMillis).toFloat()
        }

        // Configure left Y-axis
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.textColor = chartTextColor
        yAxisLeft.gridColor = chartTextColor
        yAxisLeft.axisLineColor = chartTextColor

        // Disable right Y-axis
        lineChart.axisRight.isEnabled = false

        // Configure legend
        lineChart.legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)
            textColor = chartTextColor
        }

        // General chart appearance settings
        lineChart.setBackgroundColor(backgroundColor)
        lineChart.setNoDataText("No data to show")
        lineChart.setNoDataTextColor(chartTextColor)
        lineChart.extraBottomOffset = 30f // Space for label rotation
        lineChart.description.isEnabled = false // Hide default description
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.isHighlightPerTapEnabled = false
        lineChart.animateX(1500, Easing.EaseInOutQuad) // Animate x-axis load

        // Redraw the chart
        lineChart.invalidate()
    }

    // endregion
}