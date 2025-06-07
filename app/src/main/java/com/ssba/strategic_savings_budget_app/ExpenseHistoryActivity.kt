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
import android.widget.ProgressBar
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
import com.ssba.strategic_savings_budget_app.adapters.ExpenseHistoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseHistoryBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
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
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Implementing the Material DatePicker for selecting dates in the app
 	*   - Data Visualisation with Line Chart
 	* Author: Android Developers / Firebase Team / MPAndroidChart
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Material DatePicker: https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker
 	*   - MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
*/

class ExpenseHistoryActivity : AppCompatActivity()
{
    // region Declarations
    // View Binding
    private lateinit var binding: ActivityExpenseHistoryBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var tvTotalExpense: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvMaxExpenseLimit: TextView
    private lateinit var pbExpenseLimit: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    private lateinit var lcExpense: LineChart
    private lateinit var cgDays: ChipGroup
    // endregion

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityExpenseHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        tvTotalExpense = binding.tvTotalExpense
        rvTransactions = binding.rvExpenseTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        btnBack = binding.btnBack
        tvMaxExpenseLimit = binding.tvMaxExpenseLimit
        pbExpenseLimit = binding.progressExpenseLimit
        tvProgressPercentage = binding.tvProgressPercentage
        lcExpense = binding.lineChartExpense
        cgDays = binding.chipGroupExpense
        // endregion

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        lifecycleScope.launch {

            // Get the current user's ID
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@ExpenseHistoryActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Get the total expense for the current user
            val totalExpense = getTotalExpenses(db, userId)

            // Set the text of the total expense
            tvTotalExpense.text = currencyFormat.format(totalExpense)

            // Get the maximum monthly expense limit for the current user
            val maximumExpenseLimit = getMaximumExpenseLimit(db, userId)

            // Set the text of the maximum monthly expense limit
            tvMaxExpenseLimit.text = "Maximum Monthly Expense Limit: ${currencyFormat.format(maximumExpenseLimit)}"

            // Get the total expense for the current month
            val totalExpensesForCurrentMonth = getTotalExpensesForCurrentMonth(db, userId)

            // Calculate the progress percentage
            val progressPercentage = (totalExpensesForCurrentMonth / maximumExpenseLimit) * 100

            // Set the progress of the progress bar
            pbExpenseLimit.progress = progressPercentage.toInt()

            // Set the text of the progress percentage
            tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly limit"

            // region Set up RecyclerView

            // get all the incomes for the current user
            val expenseTransactions = getAllExpensesForUser(db, userId)

            if (expenseTransactions.isEmpty())
            {
                tvNoTransactions.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
                lcExpense.visibility = View.GONE
                cgDays.visibility = View.GONE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE
                lcExpense.visibility = View.VISIBLE
                cgDays.visibility = View.VISIBLE

                val adapter = ExpenseHistoryAdapter(expenseTransactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)

                rvTransactions.adapter = adapter

                // Set up the Line Graph
                val graphData = parseTransactionGraphData(expenseTransactions)
                setupLineChart(lcExpense, graphData)
            }

            // endregion
        }

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    @Suppress("LABEL_NAME_CLASH")
    @SuppressLint("SetTextI18n")
    private fun setupOnClickListeners()
    {
        btnRewards.setOnClickListener {
            StreakManager(this).showStreakDialog(this)
        }

        btnBack.setOnClickListener {
            finish()
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
                            startActivity(Intent(this@ExpenseHistoryActivity, LoginActivity::class.java))
                            finish()
                            return@launch
                        }

                        // set up the recycler view
                        val transactions = getAllExpensesForUser(db, userId)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            cgDays.visibility = View.GONE
                            lcExpense.visibility = View.GONE

                            dialog.dismiss()
                            Toast.makeText(this@ExpenseHistoryActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // filter the transactions by date
                        val filteredTransactions = filterExpensesByDateRange(transactions, startDateObj, endDateObj)

                        if (filteredTransactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            cgDays.visibility = View.GONE
                            lcExpense.visibility = View.GONE

                            Toast.makeText(this@ExpenseHistoryActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {
                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                            binding.cardExpenseLimit.visibility = View.GONE

                            val totalExpense = calculateTotalExpense(filteredTransactions)

                            tvTotalExpense.text = currencyFormat.format(totalExpense)

                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE
                            cgDays.visibility = View.VISIBLE
                            lcExpense.visibility = View.VISIBLE

                            // Set up the recycler view
                            val adapter = ExpenseHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)

                            rvTransactions.adapter = adapter

                            // Set up the Line Graph
                            val graphData = parseTransactionGraphData(filteredTransactions)
                            setupLineChart(lcExpense, graphData)

                            dialog.dismiss()
                            Toast.makeText(this@ExpenseHistoryActivity, "Transactions Filtered Successfully", Toast.LENGTH_SHORT).show()
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

                    if (userId == null) {
                        startActivity(Intent(this@ExpenseHistoryActivity, LoginActivity::class.java))
                        finish()
                        return@launch
                    }

                    // set up the recycler view
                    val transactions = getAllExpensesForUser(db, userId)

                    if (transactions.isEmpty()) {
                        rvTransactions.visibility = View.GONE
                        tvNoTransactions.visibility = View.VISIBLE
                        cgDays.visibility = View.GONE
                        lcExpense.visibility = View.GONE

                        dialog.dismiss()
                        Toast.makeText(
                            this@ExpenseHistoryActivity,
                            "No Transactions Found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    else
                    {

                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                        // Get the total expenses for the current user
                        val totalExpense = getTotalExpenses(db, userId)

                        // Set the text of the total income
                        tvTotalExpense.text = currencyFormat.format(totalExpense)

                        // make expense limit card visible
                        binding.cardExpenseLimit.visibility = View.VISIBLE

                        // Get the maximum monthly expense limit for the current user
                        val maximumExpenseLimit = getMaximumExpenseLimit(db, userId)

                        // Set the text of the maximum monthly expense limit
                        tvMaxExpenseLimit.text = "Maximum Monthly Expense Limit: ${currencyFormat.format(maximumExpenseLimit)}"

                        // Get the total expense for the current month
                        val totalExpensesForCurrentMonth = getTotalExpensesForCurrentMonth(db, userId)

                        // Calculate the progress percentage
                        val progressPercentage = (totalExpensesForCurrentMonth / maximumExpenseLimit) * 100

                        // Set the progress of the progress bar
                        pbExpenseLimit.progress = progressPercentage.toInt()

                        // Set the text of the progress percentage
                        tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly limit"

                        // region Set up RecyclerView

                        // get all the expenses for the current user
                        val expenseTransactions = getAllExpensesForUser(db, userId)

                        if (expenseTransactions.isEmpty())
                        {
                            tvNoTransactions.visibility = View.VISIBLE
                            rvTransactions.visibility = View.GONE
                            cgDays.visibility = View.GONE
                            lcExpense.visibility = View.GONE
                        }
                        else
                        {
                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE
                            cgDays.visibility = View.VISIBLE
                            lcExpense.visibility = View.VISIBLE

                            val adapter = ExpenseHistoryAdapter(expenseTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)

                            rvTransactions.adapter = adapter

                            // Set up the Line Graph
                            val graphData = parseTransactionGraphData(expenseTransactions)
                            setupLineChart(lcExpense, graphData)
                        }

                        dialog.dismiss()
                        Toast.makeText(
                            this@ExpenseHistoryActivity,
                            "Filter Cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                }
            }
        }

        // Set a listener for when the selection state of chips changes
        cgDays.setOnCheckedStateChangeListener { _, checkedIds ->

            // If no chips are checked, show all data
            if (checkedIds.isEmpty())
            {
                // Get user ID
                val userId = auth.currentUser?.uid

                // If the user is not authenticated, redirect them to the login screen
                if (userId == null) {
                    startActivity(Intent(this@ExpenseHistoryActivity, LoginActivity::class.java))
                    finish()
                    return@setOnCheckedStateChangeListener
                }

                lifecycleScope.launch {

                    // Get all transactions for the current user
                    val transactions = getAllExpensesForUser(db, userId)
                    val graphData = parseTransactionGraphData(transactions)

                    // Show all data (no filter)
                    setupLineChart(lcExpense, graphData)
                }

                return@setOnCheckedStateChangeListener
            }

            // Get the first selected chip ID, or return if none is selected
            val checkedId = checkedIds.first()

            // Get the currently authenticated user's ID
            val userId = auth.currentUser?.uid

            // If the user is not authenticated, redirect them to the login screen
            if (userId == null) {
                startActivity(Intent(this@ExpenseHistoryActivity, LoginActivity::class.java))
                finish()
                return@setOnCheckedStateChangeListener
            }

            lifecycleScope.launch {
                // Retrieve all transactions from the database for the current user
                val transactions = getAllExpensesForUser(db, userId)

                // Convert raw transactions into a format suitable for the graph
                val graphData = parseTransactionGraphData(transactions)

                // Filter and update the graph based on the selected chip
                when (checkedId) {
                    R.id.chip7DaysExpense -> filterGraphTransactionsByDays(7, graphData, lcExpense)
                    R.id.chip7DaysExpense -> filterGraphTransactionsByDays(14, graphData, lcExpense)
                    R.id.chip30DaysExpense -> filterGraphTransactionsByDays(30, graphData, lcExpense)
                    else -> setupLineChart(lcExpense, graphData) // Show all data if no chip matched
                }
            }
        }
    }

    // region Transaction Helper Methods

    // Calculate total expense from a list of Expense transactions
    @SuppressLint("DefaultLocale")
    private fun calculateTotalExpense(list: List<Expense>): Double
    {
        val totalExpense = list.sumOf { it.amount }
        return totalExpense
    }


    // Method to filter only Expense transactions by date range
    private fun filterExpensesByDateRange(list: List<Expense>, startDate: Date, endDate: Date): List<Expense> {
        return list
            .filter { it.date in startDate..endDate }
            .sortedByDescending { it.date.time }
    }

    // Method to get all the expenses for the current user
    private suspend fun getAllExpensesForUser(db: AppDatabase, userId: String): List<Expense>
    {
        val allExpenses = mutableListOf<Expense>()

        // Step 1: Get the user's expense categories
        val userWithCategories = db.userDao().getUserWithExpenseCategories(userId)

        if (userWithCategories.isNotEmpty())
        {
            val expenseCategories = userWithCategories[0].expenseCategories

            // Step 2: For each category, get the expenses
            for (category in expenseCategories)
            {
                val expensesWithCategory = db.expenseCategoryDao().getExpensesByCategoryName(category.name)

                if (expensesWithCategory.isNotEmpty())
                {
                    allExpenses.addAll(expensesWithCategory[0].expenses)
                }
            }
        }

        // Step 3: Return the list
        return allExpenses.sortedByDescending { it.date.time }
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

    // method to get the users budget
    private suspend fun getMaximumExpenseLimit(db: AppDatabase, userId: String): Double
    {
        val budget = db.budgetDao().getBudgetByUserId(userId) ?: return 0.00

        return budget.maximumMonthlyExpenses
    }

    // method to get total Expenses for current month
    @SuppressLint("DefaultLocale")
    private suspend fun getTotalExpensesForCurrentMonth(db: AppDatabase, userId: String): Double
    {
        val expenses = getAllExpensesForUser(db, userId)

        var totalExpenses = 0.00

        if (expenses.isEmpty()) {
            return totalExpenses
        }

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        for (expense in expenses)
        {
            val expenseCalendar = Calendar.getInstance()
            expenseCalendar.time = expense.date

            val expenseMonth = expenseCalendar.get(Calendar.MONTH)
            val expenseYear = expenseCalendar.get(Calendar.YEAR)

            if (expenseMonth == currentMonth && expenseYear == currentYear) {
                totalExpenses += expense.amount
            }
        }

        return totalExpenses
    }

    // Converts a list of transactions (Expense) into a list of TransactionGraphData
    private fun parseTransactionGraphData(transactions: List<Expense>): List<TransactionGraphData>
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
            val date = transaction.date

            // Extract the amount based on the transaction type
            val amount = transaction.amount

            // Determine the transaction type for categorization in the graph
            val type = TransactionType.INCOME

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

        // list to hold entries for the line chart for income
        val expenseEntries = ArrayList<Entry>()

        // Formatter for x-axis dates
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "ZA"))

        // Populate entries for transaction
        transactions.forEach {
            val xValue = it.date.time.toFloat() // Use epoch time as x-axis value

            // Skip transactions with zero or negative amounts for a cleaner graph
            if (it.amount <= 0f) return@forEach

            // Add entry to the correct dataset
            expenseEntries.add(Entry(xValue, it.amount))
        }

        // Sort entries chronologically to ensure proper chart rendering
        expenseEntries.sortBy { it.x }

        // Define line colors using theme resources
        val colourRed = ContextCompat.getColor(this@ExpenseHistoryActivity, R.color.expense_red)

        // Configure dataset for expenses
        val expenseDataSet = LineDataSet(expenseEntries, "Expenses").apply {
            color = colourRed
            setCircleColor(colourRed)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Combine dataset into one LineData object, include only non-empty datasets
        val lineData = LineData().apply {
            if (expenseEntries.isNotEmpty()) addDataSet(expenseDataSet)
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