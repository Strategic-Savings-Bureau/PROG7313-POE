package com.ssba.strategic_savings_budget_app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.RecentTransactionAdapter
import com.ssba.strategic_savings_budget_app.budget.ExpenseEntryActivity
import com.ssba.strategic_savings_budget_app.budget.IncomeEntryActivity
import com.ssba.strategic_savings_budget_app.budget.SavingsEntryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityMainBinding
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.StreakManager
import kotlinx.coroutines.launch
import java.util.Calendar

/*
 	* Code Attribution
 	* Purpose:
 	*   - Creating and displaying an AlertDialog in an Android app
 	*   - Requesting POST_NOTIFICATIONS permission on Android 13 (API level 33) and above
 	*   - Setting up Bottom Navigation View with OnItemSelectedListener for navigation between activities
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Data Visualisation with BarChart
 	* Author: Android Developers / Firebase Team / MPAndroidChart
 	* Sources:
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
 	*   - POST_NOTIFICATIONS Permission: https://developer.android.com/develop/ui/views/notifications/permission
 	*   - Bottom Navigation View: https://developer.android.com/reference/com/google/android/material/bottomnavigation/BottomNavigationView
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
*/

class MainActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityMainBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Database
    private lateinit var db: AppDatabase

    // View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var tvUsername: TextView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnAddIncome: ImageButton
    private lateinit var btnAddExpense: ImageButton
    private lateinit var btnAddSavings: ImageButton
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var bcMonthlyOverview: BarChart
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialisation of View Components
        btnRewards = binding.btnRewards
        tvUsername = binding.tvUsername
        btnAddIncome = binding.btnAddIncome
        btnAddExpense = binding.btnAddExpense
        btnAddSavings = binding.btnAddSavings
        rvRecentTransactions = binding.rvRecentTransactions
        tvNoTransactions = binding.tvNoTransactions
        bcMonthlyOverview = binding.barChart
        // endregion

        // Set Up Recycler View
        lifecycleScope.launch {

            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // get the recent transactions
            val recentTransactions = getRecentTransactions(db, userId)

            if (recentTransactions.isEmpty())
            {
                rvRecentTransactions.visibility = View.GONE
                tvNoTransactions.visibility = View.VISIBLE
            }
            else
            {
                rvRecentTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                // set up the recycler view
                val adapter = RecentTransactionAdapter(recentTransactions)
                rvRecentTransactions.layoutManager = LinearLayoutManager(this@MainActivity)

                rvRecentTransactions.adapter = adapter
            }
        }

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miHome

        lifecycleScope.launch {

            // Get the Current Users Id
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            val user = getCurrentUser(db, userId)

            if (user == null)
            {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            tvUsername.text = user.username
        }

        // Method to Set Up onClickListeners
        setupOnClickListeners()

        // Check if a budget is set up
        lifecycleScope.launch {
            val userID = auth.currentUser?.uid
            if (db.budgetDao.getBudgetByUserId(userID!!) == null) {
                showBudgetSetupDialog(userID)
            }
        }

        lifecycleScope.launch {
            // Get the Current Users Id
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            setUpMonthlyComparisonBarChart(db, userId, bcMonthlyOverview)
        }
    }

    private fun setupOnClickListeners() {

        btnAddIncome.setOnClickListener {

            // Start Add Income Intent Here
            startActivity(Intent(this, IncomeEntryActivity::class.java))
        }

        btnAddExpense.setOnClickListener {

            lifecycleScope.launch {
                val userId = auth.currentUser?.uid

                if (userId == null)
                {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }

                val hasCategories = hasExpenseCategories(db, userId)

                if (!hasCategories)
                {
                    // display toast
                    Toast.makeText(this@MainActivity, "Please add expense categories first", Toast.LENGTH_SHORT).show()

                    // redirect to analysis activity
                    startActivity(Intent(this@MainActivity, AnalysisActivity::class.java))
                }
                else
                {
                    // Start Add Expense Intent Here
                    startActivity(Intent(this@MainActivity, ExpenseEntryActivity::class.java))
                }
            }
        }

        btnAddSavings.setOnClickListener {

            lifecycleScope.launch {
                val userId = auth.currentUser?.uid

                if (userId == null)
                {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }

                val hasSavingsGoals = hasSavingsGoals(db, userId)

                if (!hasSavingsGoals)
                {
                    // display toast
                    Toast.makeText(this@MainActivity, "Please add a savings goal first", Toast.LENGTH_SHORT).show()

                    // redirect to savings activity
                    startActivity(Intent(this@MainActivity, SavingsActivity::class.java))
                }
                else
                {
                    // Start Add Savings Intent Here
                    startActivity(Intent(this@MainActivity, SavingsEntryActivity::class.java))
                }
            }
        }

        btnRewards.setOnClickListener {
            StreakManager(this).showStreakDialog()
        }

        // Set up Bottom Navigation View onClickListener
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                // Navigate to Main (Home) Activity
                R.id.miHome -> true
                // Navigate to Analysis Activity
                R.id.miAnalysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Transactions Activity
                R.id.miTransactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    finish()
                    true
                }
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
    }

    private suspend fun getCurrentUser(db: AppDatabase, userId: String): User? {
        return db.userDao.getUserById(userId)
    }

    private fun showBudgetSetupDialog(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_setup_budget, null)
        val inputIncome = dialogView.findViewById<TextInputEditText>(R.id.input_min_income)
        val inputExpenses = dialogView.findViewById<TextInputEditText>(R.id.input_max_expenses)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Create Budget", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val incomeStr = inputIncome.text.toString().trim()
                val expensesStr = inputExpenses.text.toString().trim()

                if (incomeStr.isEmpty() || expensesStr.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                try {
                    val income = incomeStr.replace(',', '.').toDouble()
                    val expenses = expensesStr.replace(',', '.').toDouble()

                    if (income <= 0 || expenses <= 0) {
                        Toast.makeText(this, "Values must be positive", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    lifecycleScope.launch {
                        val newBudget = Budget(
                            minimumMonthlyIncome = income,
                            maximumMonthlyExpenses = expenses,
                            userId = userId
                        )
                        db.budgetDao.upsertBudget(newBudget)
                        Toast.makeText(this@MainActivity, "Budget created successfully", Toast.LENGTH_SHORT).show()
                        Log.i("MainActivity", "Budget created successfully")
                        dialog.dismiss()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // region Transaction Helper Methods
    // Method to get the recent transactions for the current user
    private suspend fun getRecentTransactions(db: AppDatabase, userId: String): List<Any>
    {
        // get all the incomes for the current user
        val incomes = db.userDao.getUserWithIncomes(userId).firstOrNull()?.incomes ?: emptyList()

        // get all the expenses for the current user
         val expenses = getAllExpensesForUser(db, userId)

        // combine the two lists
        val combinedTransactions = mutableListOf<Any>()
        combinedTransactions.addAll(incomes)
        combinedTransactions.addAll(expenses)

        // Sort by date and time descending
        val sortedList = combinedTransactions.sortedByDescending { item ->
            when (item) {
                is Income -> item.date.time
                is Expense -> item.date.time
                else -> 0L // fallback
            }
        }

        // Pick top 10
        val top10 = sortedList.take(10)

        return top10
    }

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

    // method to check if user has any expense categories return bool
    private suspend fun hasExpenseCategories(db: AppDatabase, userId: String): Boolean
    {
        val userWithCategories = db.userDao.getUserWithExpenseCategories(userId)

        if (userWithCategories.isEmpty())
        {
            return false
        }

        if (userWithCategories[0].expenseCategories.isEmpty())
        {
            return false
        }

        return true
    }

    // method to check if user has any savings goals return bool
    private suspend fun hasSavingsGoals(db: AppDatabase, userId: String): Boolean
    {
        val userWithGoals = db.userDao.getUserWithSavingGoals(userId)

        if (userWithGoals.isEmpty())
        {
            return false
        }

        if (userWithGoals[0].savingGoals.isEmpty())
        {
            return false
        }

        return true
    }

    // method to get the users budget
    private suspend fun getMinimumIncome(db: AppDatabase, userId: String): Double
    {
        val budget = db.budgetDao.getBudgetByUserId(userId) ?: return 0.00

        return budget.minimumMonthlyIncome
    }

    // method to get total Income for current month
    @SuppressLint("DefaultLocale")
    private suspend fun getTotalIncomeForCurrentMonth(db: AppDatabase, userId: String): Double {
        val userWithIncomes = db.userDao.getUserWithIncomes(userId)
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

    // method to get the users budget
    private suspend fun getMaximumExpenseLimit(db: AppDatabase, userId: String): Double
    {
        val budget = db.budgetDao.getBudgetByUserId(userId) ?: return 0.00

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

    private suspend fun getTotalSavingsForCurrentMonth(db: AppDatabase, userId: String): Double
    {
        val savingsGoalsWithUser = db.userDao.getUserWithSavingGoals(userId)

        if (savingsGoalsWithUser.isEmpty()) {
            return 0.0
        }

        var totalSavings = 0.0

        val savingsTransactions = mutableListOf<Saving>()

        // get all the savingGoalsWithSavings for the current user
        for (goal in savingsGoalsWithUser[0].savingGoals)
        {
            // get the savings for the current goal
            val savingsGoalWithSavings = db.savingsGoalDao.getSavingsBySavingGoalTitle(goal.title)

            if (savingsGoalWithSavings.isNotEmpty())
            {
                // add the savings to the list
                savingsTransactions.addAll(savingsGoalWithSavings[0].savings)
            }
        }

        if (savingsTransactions.isEmpty()) {
            return totalSavings
        }

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        for (saving in savingsTransactions)
        {
            val savingCalendar = Calendar.getInstance()
            savingCalendar.time = saving.date

            val savingMonth = savingCalendar.get(Calendar.MONTH)
            val savingYear = savingCalendar.get(Calendar.YEAR)

            if (savingMonth == currentMonth && savingYear == currentYear) {
                totalSavings += saving.amount
            }
        }

        return totalSavings
    }


    // endregion

    // region Bar Chart SetUp
    private suspend fun setUpMonthlyComparisonBarChart(db: AppDatabase, userId: String, chart: BarChart)
    {
        // Retrieve income and goal income for the current month
        val income = getTotalIncomeForCurrentMonth(db, userId)
        val incomeGoal = getMinimumIncome(db, userId)

        // Retrieve expenses and expense limit for the current month
        val expenses = getTotalExpensesForCurrentMonth(db, userId)
        val expenseLimit = getMaximumExpenseLimit(db, userId)

        // Retrieve savings data for the current month
        val savings = getTotalSavingsForCurrentMonth(db, userId)

        // Initialize and configure the chart with retrieved values
        initialiseMonthlyComparisonBarChart(
            chart = chart,
            income = income.toFloat(),
            incomeGoal = incomeGoal.toFloat(),
            expenses = expenses.toFloat(),
            expenseLimit = expenseLimit.toFloat(),
            savings = savings.toFloat()
        )

    }

    // Function to configure and display the grouped bar chart
    private fun initialiseMonthlyComparisonBarChart(
        chart: BarChart,
        income: Float,
        incomeGoal: Float,
        expenses: Float,
        expenseLimit: Float,
        savings: Float
    )
    {
        // Detect current UI mode (light or dark)
        val isDarkMode = when (chart.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        // Set chart text and background colors based on UI mode
        val chartTextColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val backgroundColor = if (isDarkMode) Color.TRANSPARENT else Color.WHITE

        // Define layout properties for grouped bars
        val groupCount = 3
        val barSpace = 0.05f
        val barWidth = 0.35f
        val groupSpace = 0.2f

        // Create entries for actual values (current performance)
        val actualEntries = listOf(
            BarEntry(0f, income),
            BarEntry(1f, expenses),
            BarEntry(2f, savings)
        )

        // Create entries for goal/target values
        val goalEntries = listOf(
            BarEntry(0f, incomeGoal),
            BarEntry(1f, expenseLimit)
        )

        // Set up the dataset for actual values with conditional coloring
        val actualDataSet = BarDataSet(actualEntries, "Actual").apply {
            valueTextSize = 14f
            valueTextColor = chartTextColor
            colors = listOf(
                if (income > incomeGoal) ContextCompat.getColor(chart.context, R.color.income_green) else Color.BLUE,
                if (expenses > expenseLimit) Color.RED else ContextCompat.getColor(chart.context, R.color.expense_gold),
                ContextCompat.getColor(chart.context, R.color.savings_blue)
            )
        }

        // Set up the dataset for goal values using light gray
        val goalDataSet = BarDataSet(goalEntries, "Target").apply {
            color = Color.LTGRAY
            valueTextSize = 14f
            valueTextColor = chartTextColor
        }

        // Combine both datasets into BarData
        val barData = BarData(goalDataSet, actualDataSet)
        barData.barWidth = barWidth
        chart.data = barData

        // Apply chart-wide visual settings
        chart.apply {
            setBackgroundColor(backgroundColor)
            setNoDataTextColor(chartTextColor)
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = chartTextColor
            }
            animateY(1000)
            setFitBars(true)
            groupBars(0f, groupSpace, barSpace) // Group the bars properly for comparison

            // Configure X-axis labels and formatting
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setCenterAxisLabels(true)
                axisMinimum = 0f
                axisMaximum = barData.getGroupWidth(groupSpace, barSpace) * groupCount
                textColor = chartTextColor
                gridColor = textColor
                axisLineColor = textColor
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when (value.toInt()) {
                            0 -> "Income"
                            1 -> "Expenses"
                            2 -> "Savings"
                            else -> ""
                        }
                    }
                }
            }

            // Configure Y-axis range and style
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = maxOf(income, incomeGoal, expenses, expenseLimit, savings) + 2000f
                granularity = 2000f
                textColor = chartTextColor
                gridColor = textColor
                axisLineColor = textColor
            }

            // Disable right Y-axis for clarity
            axisRight.isEnabled = false
            invalidate() // Refresh the chart with new data
        }
    }

    // endregion
}