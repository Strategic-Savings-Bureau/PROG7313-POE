package com.ssba.strategic_savings_budget_app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.ExpenseCategoryAdapter
import com.ssba.strategic_savings_budget_app.budget.CreateCategoryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityAnalysisBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.graph_data.CategoryGraphData
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.StreakManager
import kotlinx.coroutines.launch
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar

/*
 	* Code Attribution
 	* Purpose:
 	*   - Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	*   - Creating and displaying an AlertDialog in an Android app
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Data Visualisation with BarChart
 	* Author: Android Developers / Firebase Team / MPAndroidChart
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
*/

class AnalysisActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityAnalysisBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View components
    private lateinit var btnRewards: ImageButton
    private lateinit var btnAddCategory: ImageButton
    private lateinit var tvNoCategories: TextView
    private lateinit var rvCategories: RecyclerView
    private lateinit var bcCategories: BarChart
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miAnalysis

        // Initialise Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialise Database
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        btnAddCategory = binding.btnAddCategory
        tvNoCategories = binding.tvNoExpenseCategories
        rvCategories = binding.rvExpenseCategories
        bcCategories = binding.barChartCategorySpending

        // endregion

        lifecycleScope.launch {

            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@AnalysisActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // region Set up the RecyclerView

            // get all Expense Categories for the current user
            val expenseCategories = getExpenseCategories(db, userId)

            if (expenseCategories.isEmpty())
            {
                tvNoCategories.visibility = View.VISIBLE
                rvCategories.visibility = View.GONE
            }
            else
            {
                tvNoCategories.visibility = View.GONE
                rvCategories.visibility = View.VISIBLE

                // set up the adapter
                val adapter = ExpenseCategoryAdapter(expenseCategories)

                // set up the layout manager
                rvCategories.layoutManager = LinearLayoutManager(this@AnalysisActivity)

                // set rv adapter
                rvCategories.adapter = adapter
            }

            // endregion

            setupBarChart(bcCategories, this@AnalysisActivity, db, userId)

        }

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            StreakManager(this).showStreakDialog()
        }

        btnAddCategory.setOnClickListener {

            // Navigate to Add Saving Goal Activity
            startActivity(Intent(this, CreateCategoryActivity::class.java))
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
                R.id.miAnalysis -> true
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

    // region Expense Category Helper Methods

    // method to get all expense categories for the current user
    private suspend fun getExpenseCategories(db: AppDatabase, userId: String): List<ExpenseCategory>
    {
        val expenseCategories = mutableListOf<ExpenseCategory>()

        val userWithCategories = db.userDao.getUserWithExpenseCategories(userId)

        if (userWithCategories.isNotEmpty())
        {
            expenseCategories.addAll(userWithCategories[0].expenseCategories)

            // order the expense categories by their monthly budget
            expenseCategories.sortByDescending { it.maximumMonthlyTotal }

            return expenseCategories
        }

        return expenseCategories
    }

    // get the total of all expenses for the current category in the current month
    private suspend fun getTotalMonthlyExpensesForCategory(categoryTitle: String, db: AppDatabase): Double
    {
        val categoryWithExpenses = db.expenseCategoryDao.getExpensesByCategoryName(categoryTitle)

        if (categoryWithExpenses.isEmpty()) {
            return 0.0
        }

        val expensesList = categoryWithExpenses[0].expenses

        if (expensesList.isEmpty()) {
            return 0.0
        }

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var monthlyExpenses = 0.0

        for (expense in expensesList)
        {
            val expenseCalendar = Calendar.getInstance()
            expenseCalendar.time = expense.date

            val expenseMonth = expenseCalendar.get(Calendar.MONTH)
            val expenseYear = expenseCalendar.get(Calendar.YEAR)

            if (expenseMonth == currentMonth && expenseYear == currentYear) {
                monthlyExpenses += expense.amount
            }
        }

        return monthlyExpenses
    }

    // endregion

    // region Bar Chart Setup

    private suspend fun setupBarChart(chart: BarChart, context: Context, db: AppDatabase, userId: String)
    {
        val expenseCategories = getExpenseCategories(db, userId)

        val categories = mutableListOf<CategoryGraphData>()

        if (expenseCategories.isNotEmpty())
        {
            // For each expense category, calculate total spent and add to list with monthly limit
            for (category in expenseCategories)
            {
                val totalSpent = getTotalMonthlyExpensesForCategory(category.name, db)
                categories.add(CategoryGraphData(category.name, totalSpent.toFloat(), category.maximumMonthlyTotal.toFloat()))
            }

            // Initialize and display the bar chart with prepared data
            initialiseCategoryBarChart(chart, categories, context)
        }
    }

    private fun initialiseCategoryBarChart(
        chart: BarChart,
        categories: List<CategoryGraphData>,
        context: Context
    )
    {
        // Detect if the device is in dark mode for appropriate color settings
        val isDarkMode = when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        // Define colors based on the current UI mode
        val chartTextColor = if (isDarkMode) Color.WHITE else Color.BLACK
        val backgroundColor = if (isDarkMode) Color.TRANSPARENT else Color.WHITE

        // Prepare BarEntries for actual spending and monthly limit values
        val actualEntries = mutableListOf<BarEntry>()
        val goalEntries = mutableListOf<BarEntry>()
        val categoryLabels = mutableListOf<String>()

        // Populate entries and labels for each category
        categories.forEachIndexed { index, category ->
            actualEntries.add(BarEntry(index.toFloat(), category.totalSpent))
            goalEntries.add(BarEntry(index.toFloat(), category.monthlyLimit))
            categoryLabels.add(category.name)
        }

        // Configure BarDataSet for actual spending with conditional colors
        val actualDataSet = BarDataSet(actualEntries, "Spent").apply {
            valueTextSize = 12f
            valueTextColor = chartTextColor
            colors = categories.map {
                if (it.totalSpent > it.monthlyLimit)
                    Color.RED
                else
                    ContextCompat.getColor(context, R.color.expense_gold)
            }
        }

        // Configure BarDataSet for monthly limit bars with light gray color
        val goalDataSet = BarDataSet(goalEntries, "Limit").apply {
            color = Color.LTGRAY
            valueTextSize = 12f
            valueTextColor = chartTextColor
        }

        // Create BarData object containing both datasets and set bar width
        val barData = BarData(goalDataSet, actualDataSet).apply {
            barWidth = 0.35f
        }

        chart.apply {
            data = barData
            setBackgroundColor(backgroundColor)
            setNoDataText("No data to show")
            setNoDataTextColor(chartTextColor)
            description.isEnabled = false
            legend.apply {
                isEnabled = false
            }
            animateY(1000) // Animate chart vertically for 1 second

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM // Position X axis labels at botto
                granularity = 1f // Set minimum interval for labels
                setCenterAxisLabels(true) // Center the labels between groups
                axisMinimum = -0.5f // Start slightly before first bar for padding
                axisMaximum = categories.size.toFloat() // End after last group
                textColor = chartTextColor
                valueFormatter = IndexAxisValueFormatter(categoryLabels) // Show category names on X axis
            }

            axisLeft.textColor = chartTextColor  // Y axis label color
            axisRight.isEnabled = false // Disable right Y axis

            // Additional X axis setup for grouping bars properly
            xAxis.setCenterAxisLabels(true)
            xAxis.granularity = 1f
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 0f + barData.getGroupWidth(0.2f, 0.05f) * categories.size

            axisLeft.axisMinimum = 0f // Remove bottom gap on Y axis (start at zero)


            groupBars(0f, 0.2f, 0.05f) // Group bars together with spacing: (fromX, groupSpace, barSpace)
            invalidate() // Refresh chart to display changes
        }
    }


    // endregion
}