package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.ExpenseHistoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseHistoryBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
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
 	* Author: Android Developers
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
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
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                val adapter = ExpenseHistoryAdapter(expenseTransactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)

                rvTransactions.adapter = adapter
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
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
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

                            // Set up the recycler view
                            val adapter = ExpenseHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)

                            rvTransactions.adapter = adapter

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
                        }
                        else
                        {
                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE

                            val adapter = ExpenseHistoryAdapter(expenseTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseHistoryActivity)

                            rvTransactions.adapter = adapter
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
}