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
import com.ssba.strategic_savings_budget_app.adapters.IncomeHistoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityIncomeHistoryBinding
import com.ssba.strategic_savings_budget_app.entities.Income
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


class IncomeHistoryActivity : AppCompatActivity()
{
    // region Declarations
    // View Binding
    private lateinit var binding: ActivityIncomeHistoryBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var tvTotalIncome: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvMinIncomeGoal: TextView
    private lateinit var pbIncomeGoal: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    private lateinit var lcIncome: LineChart
    private lateinit var cgDays: ChipGroup
    // endregion


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityIncomeHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        tvTotalIncome = binding.tvTotalIncome
        rvTransactions = binding.rvIncomeTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        btnBack = binding.btnBack
        tvMinIncomeGoal = binding.tvMinIncomeGoal
        pbIncomeGoal = binding.progressIncomeGoal
        tvProgressPercentage = binding.tvProgressPercentage
        lcIncome = binding.lineChartIncome
        cgDays = binding.chipGroup
        // endregion

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        lifecycleScope.launch {

            // Get the current user's ID
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Get the total income for the current user
            val totalIncome = getTotalIncome(db, userId)

            // Set the text of the total income
            tvTotalIncome.text = currencyFormat.format(totalIncome)

            // Get the minimum monthly income goal for the current user
            val minimumIncome = getMinimumIncome(db, userId)

            // Set the text of the minimum monthly income goal
            tvMinIncomeGoal.text = "Minimum Monthly Income: ${currencyFormat.format(minimumIncome)}"

            // Get the total income for the current month
            val totalIncomeForCurrentMonth = getTotalIncomeForCurrentMonth(db, userId)

            // Calculate the progress percentage
            val progressPercentage = (totalIncomeForCurrentMonth / minimumIncome) * 100

            // Set the progress of the progress bar
            pbIncomeGoal.progress = progressPercentage.toInt()

            // Set the text of the progress percentage
            tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly goal"

            // region Set up RecyclerView

            // get all the incomes for the current user
            val incomeTransactions = getAllIncomeTransactions(db, userId)

            if (incomeTransactions.isEmpty())
            {
                tvNoTransactions.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
                cgDays.visibility = View.GONE
                lcIncome.visibility = View.GONE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE
                cgDays.visibility = View.VISIBLE
                lcIncome.visibility = View.VISIBLE

                val adapter = IncomeHistoryAdapter(incomeTransactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@IncomeHistoryActivity)

                rvTransactions.adapter = adapter

                // Set up the Line Graph
                val graphData = parseTransactionGraphData(incomeTransactions)
                setupLineChart(lcIncome, graphData)
            }

            // endregion

        }


        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    @SuppressLint("SetTextI18n")
    @Suppress("LABEL_NAME_CLASH")
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
                            startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                            finish()
                            return@launch
                        }

                        // set up the recycler view
                        val transactions = getAllIncomeTransactions(db, userId)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            cgDays.visibility = View.GONE
                            lcIncome.visibility = View.GONE

                            dialog.dismiss()
                            Toast.makeText(this@IncomeHistoryActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // filter the transactions by date
                        val filteredTransactions = filterIncomeByDateRange(transactions, startDateObj, endDateObj)

                        if (filteredTransactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            cgDays.visibility = View.GONE
                            lcIncome.visibility = View.GONE

                            Toast.makeText(this@IncomeHistoryActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {
                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                            binding.cardIncomeGoal.visibility = View.GONE

                            val totalIncome = calculateTotalIncome(filteredTransactions)

                            tvTotalIncome.text = currencyFormat.format(totalIncome)

                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE
                            cgDays.visibility = View.GONE
                            lcIncome.visibility = View.VISIBLE

                            // Set up the recycler view
                            val adapter = IncomeHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@IncomeHistoryActivity)

                            rvTransactions.adapter = adapter

                            // Set up the Line Graph
                            val graphData = parseTransactionGraphData(filteredTransactions)
                            setupLineChart(lcIncome, graphData)

                            dialog.dismiss()
                            Toast.makeText(this@IncomeHistoryActivity, "Transactions Filtered Successfully", Toast.LENGTH_SHORT).show()
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
                        startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                        finish()
                        return@launch
                    }

                    // set up the recycler view
                    val transactions = getAllIncomeTransactions(db, userId)

                    if (transactions.isEmpty()) {
                        rvTransactions.visibility = View.GONE
                        tvNoTransactions.visibility = View.VISIBLE
                        cgDays.visibility = View.GONE

                        dialog.dismiss()
                        Toast.makeText(
                            this@IncomeHistoryActivity,
                            "No Transactions Found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    else
                    {

                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                        // Get the total income for the current user
                        val totalIncome = getTotalIncome(db, userId)

                        // Set the text of the total income
                        tvTotalIncome.text = currencyFormat.format(totalIncome)

                        // make income goal card visible
                        binding.cardIncomeGoal.visibility = View.VISIBLE

                        // Get the minimum monthly income goal for the current user
                        val minimumIncome = getMinimumIncome(db, userId)

                        // Set the text of the minimum monthly income goal
                        tvMinIncomeGoal.text = "Minimum Monthly Income: ${currencyFormat.format(minimumIncome)}"

                        // Get the total income for the current month
                        val totalIncomeForCurrentMonth = getTotalIncomeForCurrentMonth(db, userId)

                        // Calculate the progress percentage
                        val progressPercentage = (totalIncomeForCurrentMonth / minimumIncome) * 100

                        // Set the progress of the progress bar
                        pbIncomeGoal.progress = progressPercentage.toInt()

                        // Set the text of the progress percentage
                        tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly goal"

                        // region Set up RecyclerView

                        // get all the incomes for the current user
                        val incomeTransactions = getAllIncomeTransactions(db, userId)

                        if (incomeTransactions.isEmpty())
                        {
                            tvNoTransactions.visibility = View.VISIBLE
                            rvTransactions.visibility = View.GONE
                            cgDays.visibility = View.GONE
                            lcIncome.visibility = View.GONE
                        }
                        else
                        {
                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE
                            cgDays.visibility = View.VISIBLE
                            lcIncome.visibility = View.VISIBLE

                            val adapter = IncomeHistoryAdapter(incomeTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@IncomeHistoryActivity)

                            rvTransactions.adapter = adapter

                            // Set up the Line Graph
                            val graphData = parseTransactionGraphData(incomeTransactions)
                            setupLineChart(lcIncome, graphData)
                        }

                        dialog.dismiss()
                        Toast.makeText(
                            this@IncomeHistoryActivity,
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
                    startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                    finish()
                    return@setOnCheckedStateChangeListener
                }

                lifecycleScope.launch {

                    // Get all transactions for the current user
                    val transactions = getAllIncomeTransactions(db, userId)
                    val graphData = parseTransactionGraphData(transactions)

                    // Show all data (no filter)
                    setupLineChart(lcIncome, graphData)
                }

                return@setOnCheckedStateChangeListener
            }

            // Get the first selected chip ID, or return if none is selected
            val checkedId = checkedIds.first()

            // Get the currently authenticated user's ID
            val userId = auth.currentUser?.uid

            // If the user is not authenticated, redirect them to the login screen
            if (userId == null) {
                startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                finish()
                return@setOnCheckedStateChangeListener
            }

            lifecycleScope.launch {
                // Retrieve all transactions from the database for the current user
                val transactions = getAllIncomeTransactions(db, userId)

                // Convert raw transactions into a format suitable for the graph
                val graphData = parseTransactionGraphData(transactions)

                // Filter and update the graph based on the selected chip
                when (checkedId) {
                    R.id.chip7DaysIncome -> filterGraphTransactionsByDays(7, graphData, lcIncome)
                    R.id.chip14DaysIncome -> filterGraphTransactionsByDays(14, graphData, lcIncome)
                    R.id.chip30DaysIncome -> filterGraphTransactionsByDays(30, graphData, lcIncome)
                    else -> setupLineChart(lcIncome, graphData) // Show all data if no chip matched
                }
            }
        }
    }

    // region Transaction Helper Methods

    // Calculate total income from a list of Income transactions
    @SuppressLint("DefaultLocale")
    private fun calculateTotalIncome(list: List<Income>): Double
    {
        val totalIncome = list.sumOf { it.amount }
        return totalIncome
    }


    // Method to filter only Income transactions by date range
    private fun filterIncomeByDateRange(list: List<Income>, startDate: Date, endDate: Date): List<Income> {
        return list
            .filter { it.date in startDate..endDate }
            .sortedByDescending { it.date.time }
    }


    private suspend fun getAllIncomeTransactions(db: AppDatabase, userId: String): List<Income>
    {
         // get all the incomes for the current user
            val userWithIncomes = db.userDao().getUserWithIncomes(userId)
            val incomeTransactions = userWithIncomes[0].incomes

        // sort the incomes by date and time in descending order
        return incomeTransactions.sortedByDescending { it.date.time }
    }

    @SuppressLint("DefaultLocale")
    private suspend fun getTotalIncome(db: AppDatabase, userId: String): Double
    {
        val userWithIncomes = db.userDao().getUserWithIncomes(userId)

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

    // method to get the users budget
    private suspend fun getMinimumIncome(db: AppDatabase, userId: String): Double
    {
        val budget = db.budgetDao().getBudgetByUserId(userId) ?: return 0.00

        return budget.minimumMonthlyIncome
    }

    // method to get total Income for current month
    @SuppressLint("DefaultLocale")
    private suspend fun getTotalIncomeForCurrentMonth(db: AppDatabase, userId: String): Double {
        val userWithIncomes = db.userDao().getUserWithIncomes(userId)
        var totalIncome = 0.00

        if (userWithIncomes.isEmpty()) {
            return totalIncome
        }

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        for (income in userWithIncomes[0].incomes)
        {
            val incomeCalendar = Calendar.getInstance()
            incomeCalendar.time = income.date

            val incomeMonth = incomeCalendar.get(Calendar.MONTH)
            val incomeYear = incomeCalendar.get(Calendar.YEAR)

            if (incomeMonth == currentMonth && incomeYear == currentYear) {
                totalIncome += income.amount
            }
        }

        return totalIncome
    }

    // Converts a list of transactions (Income) into a list of TransactionGraphData
    private fun parseTransactionGraphData(transactions: List<Income>): List<TransactionGraphData>
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

        // list for income
        val incomeEntries = ArrayList<Entry>()

        // Formatter for x-axis dates
        val dateFormat = SimpleDateFormat("dd MMM", Locale("en", "ZA"))

        // Populate entries for transaction
        transactions.forEach {
            val xValue = it.date.time.toFloat() // Use epoch time as x-axis value

            // Skip transactions with zero or negative amounts for a cleaner graph
            if (it.amount <= 0f) return@forEach

            // Add entry to the correct dataset
            incomeEntries.add(Entry(xValue, it.amount))
        }

        // Sort entries chronologically to ensure proper chart rendering
        incomeEntries.sortBy { it.x }

        // Define line colors using theme resources
        val colourGreen = ContextCompat.getColor(this@IncomeHistoryActivity, R.color.income_green)

        // Configure dataset for income
        val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
            color = colourGreen
            setCircleColor(colourGreen)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curves
        }

        // Combine dataset into one LineData object, include only non-empty datasets
        val lineData = LineData().apply {
            if (incomeEntries.isNotEmpty()) addDataSet(incomeDataSet)
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