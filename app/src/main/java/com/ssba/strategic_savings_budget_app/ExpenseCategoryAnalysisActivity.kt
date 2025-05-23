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
import com.ssba.strategic_savings_budget_app.adapters.ExpenseCategoryTransactionHistoryAdapter
import com.ssba.strategic_savings_budget_app.budget.ExpenseEntryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseCategoryAnalysisBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
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
 	* Author: Android Developers / Firebase Team
 	* Date Accessed: 30 April 2025
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Material DatePicker: https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker
*/

class ExpenseCategoryAnalysisActivity : AppCompatActivity()
{

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityExpenseCategoryAnalysisBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    private lateinit var categoryName: String

    // region View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnAddExpense: ImageButton
    private lateinit var tvCategoryTotal: TextView
    private lateinit var tvMaxMonthlyLimit: TextView
    private lateinit var tvDescription: TextView
    private lateinit var pbLimit: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvTitle: TextView
    // endregion

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize View Binding
        binding = ActivityExpenseCategoryAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        rvTransactions = binding.rvExpenseTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        btnBack = binding.btnBack
        btnAddExpense = binding.btnAddExpense
        tvCategoryTotal = binding.tvCategoryTotal
        tvMaxMonthlyLimit = binding.tvMaxMonthlyLimit
        tvDescription = binding.tvDescription
        pbLimit = binding.progressLimit
        tvProgressPercentage = binding.tvProgressPercentage
        tvTitle = binding.tvTitle
        // endregion

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        // get the title of the savings goal from the intent
        categoryName = intent.getStringExtra("EXPENSE_CATEGORY_NAME") ?: ""

        if (categoryName.isEmpty())
        {
            finish()
            return
        }

        lifecycleScope.launch {

            // get the expense category from the database
            val expenseCategory = db.expenseCategoryDao.getExpenseCategoryByName(categoryName)

            if (expenseCategory == null)
            {
                finish()
                return@launch
            }

            // set the expense category name
            tvTitle.text = "${expenseCategory.name} Analysis"

            // get the total of all expenses for the current category
            val totalExpenses = getTotalExpensesForCategory(categoryName, db)

            // set the total of all expenses for the current category
            tvCategoryTotal.text = "Total Expenses: ${currencyFormat.format(totalExpenses)}"

            // set the max monthly limit
            tvMaxMonthlyLimit.text = "Monthly Limit: ${currencyFormat.format(expenseCategory.maximumMonthlyTotal)}"

            // set the description
            tvDescription.text = expenseCategory.description

            // get the total of all expenses for the current category in the current month
            val totalMonthlyExpenses = getTotalMonthlyExpensesForCategory(categoryName, db)

            val monthlyLimit = expenseCategory.maximumMonthlyTotal

            // calculate the progress percentage
            val progressPercentage = (totalMonthlyExpenses / monthlyLimit) * 100

            // set the progress bar progress
            pbLimit.progress = progressPercentage.toInt()

            // set the progress percentage text
            tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly limit"

            // if the total monthly expenses is greater than the monthly limit, set the text color to red
            if (totalMonthlyExpenses >= monthlyLimit)
            {
                tvProgressPercentage.setTextColor(getColor(R.color.expense_red))
                tvProgressPercentage.text = "${progressPercentage.toInt()}% past monthly limit"
            }

            // region Set up RecyclerView

            // get all the expenses for the current goal
            val expenseTransactions = getAllExpensesForCategory(expenseCategory.name, db)

            if (expenseTransactions.isEmpty())
            {
                tvNoTransactions.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                // set up adapter
                val adapter = ExpenseCategoryTransactionHistoryAdapter(expenseTransactions)

                // set up layout manager
                rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseCategoryAnalysisActivity)

                // set adapter in rv
                rvTransactions.adapter = adapter
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
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnAddExpense.setOnClickListener {

            // navigate to the add saving activity
            val intent = Intent(this, ExpenseEntryActivity::class.java)
            startActivity(intent)
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

                        // set up the recycler view
                        val transactions = getAllExpensesForCategory(categoryName, db)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE

                            dialog.dismiss()
                            Toast.makeText(this@ExpenseCategoryAnalysisActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // filter the transactions by date
                        val filteredTransactions = filterExpensesByDateRange(transactions, startDateObj, endDateObj)

                        if (filteredTransactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            Toast.makeText(this@ExpenseCategoryAnalysisActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {
                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE

                            tvMaxMonthlyLimit.visibility = View.GONE
                            pbLimit.visibility = View.GONE
                            tvProgressPercentage.visibility = View.GONE

                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                            // re-calculate transaction total based on filtered transactions
                            val totalExpenses = calculateTotalExpensesForCategory(filteredTransactions)

                            // set the total of all expenses for the current category
                            tvCategoryTotal.text = "Total Expenses: ${currencyFormat.format(totalExpenses)}"

                            // Set up the recycler view
                            val adapter = ExpenseCategoryTransactionHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseCategoryAnalysisActivity)

                            rvTransactions.adapter = adapter

                            dialog.dismiss()
                            Toast.makeText(this@ExpenseCategoryAnalysisActivity, "Transactions Filtered Successfully", Toast.LENGTH_SHORT).show()
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

                    // get the expense category from the database
                    val expenseCategory = db.expenseCategoryDao.getExpenseCategoryByName(categoryName)

                    if (expenseCategory == null)
                    {
                        finish()
                        return@launch
                    }

                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                    // get the total of all expenses for the current category
                    val totalExpenses = getTotalExpensesForCategory(categoryName, db)

                    // set the total of all expenses for the current category
                    tvCategoryTotal.text = "Total Expenses: ${currencyFormat.format(totalExpenses)}"

                    // set the max monthly limit
                    tvMaxMonthlyLimit.text = "Monthly Limit: ${currencyFormat.format(expenseCategory.maximumMonthlyTotal)}"

                    // get the total of all expenses for the current category in the current month
                    val totalMonthlyExpenses = getTotalMonthlyExpensesForCategory(categoryName, db)

                    val monthlyLimit = expenseCategory.maximumMonthlyTotal

                    // calculate the progress percentage
                    val progressPercentage = (totalMonthlyExpenses / monthlyLimit) * 100

                    // set the progress bar progress
                    pbLimit.progress = progressPercentage.toInt()

                    // set the progress percentage text
                    tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly limit"

                    // if the total monthly expenses is greater than the monthly limit, set the text color to red
                    if (totalMonthlyExpenses >= monthlyLimit)
                    {
                        tvProgressPercentage.setTextColor(getColor(R.color.expense_red))
                        tvProgressPercentage.text = "${progressPercentage.toInt()}% past monthly limit"
                    }

                    tvMaxMonthlyLimit.visibility = View.VISIBLE
                    pbLimit.visibility = View.VISIBLE
                    tvProgressPercentage.visibility = View.VISIBLE

                    // set up the recycler view
                    val transactions = getAllExpensesForCategory(categoryName, db)

                    if (transactions.isEmpty()) {
                        rvTransactions.visibility = View.GONE
                        tvNoTransactions.visibility = View.VISIBLE

                        dialog.dismiss()
                        Toast.makeText(
                            this@ExpenseCategoryAnalysisActivity,
                            "No Transactions Found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    else
                    {
                        // region Set up RecyclerView

                        // get all the expenses for the category
                        val expenseTransactions = getAllExpensesForCategory(expenseCategory.name, db)

                        if (expenseTransactions.isEmpty())
                        {
                            tvNoTransactions.visibility = View.VISIBLE
                            rvTransactions.visibility = View.GONE
                        }
                        else
                        {
                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE

                            val adapter = ExpenseCategoryTransactionHistoryAdapter(expenseTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@ExpenseCategoryAnalysisActivity)

                            rvTransactions.adapter = adapter
                        }

                        dialog.dismiss()
                        Toast.makeText(
                            this@ExpenseCategoryAnalysisActivity,
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

    // Method to filter expense transactions by date range
    private fun filterExpensesByDateRange(list: List<Expense>, startDate: Date, endDate: Date): List<Expense> {
        return list
            .filter { it.date in startDate..endDate }
            .sortedByDescending { it.date.time }
    }

    // get the total of all expenses for the current category
    private suspend fun getTotalExpensesForCategory(categoryName: String, db: AppDatabase): Double
    {
        val categoryWithExpenses = db.expenseCategoryDao.getExpensesByCategoryName(categoryName)

        if (categoryWithExpenses.isEmpty()) {
            return 0.0
        }

        val expensesList = categoryWithExpenses[0].expenses

        if (expensesList.isEmpty()) {
            return 0.0
        }

        var expenses = 0.0

        for (expense in expensesList) {
            expenses += expense.amount
        }

        return expenses
    }

    // get the total of all expenses for the current category (filtered)
    private fun calculateTotalExpensesForCategory(expenses: List<Expense>): Double {
        return expenses.sumOf { it.amount }
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

    // get all expenses for the current category
    private suspend fun getAllExpensesForCategory(categoryName: String, db: AppDatabase): List<Expense>
    {
        // get the expense category from the database
        val categoryWithExpenses = db.expenseCategoryDao.getExpensesByCategoryName(categoryName)

        if (categoryWithExpenses.isEmpty()) {
            return emptyList()
        }

        // Order the expenses by most recent (including time) and return
        return categoryWithExpenses[0].expenses.sortedByDescending { it.date.time }
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