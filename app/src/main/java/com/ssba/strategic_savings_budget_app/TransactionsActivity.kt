package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
import com.ssba.strategic_savings_budget_app.adapters.TransactionHistoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityTransactionsBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        // endregion

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
            tvTotalIncome.text = "R $totalIncome"
            tvTotalExpenses.text = "R $totalExpenses"

            // set up the recycler view
            val transactions = getAllTransactions(db, userId)

            if (transactions.isEmpty())
            {
                rvTransactions.visibility = View.GONE
                tvNoTransactions.visibility = View.VISIBLE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                // Set up the recycler view
                val adapter = TransactionHistoryAdapter(transactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@TransactionsActivity)

                rvTransactions.adapter = adapter
            }
        }


        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    @SuppressLint("SetTextI18n")
    @Suppress("LABEL_NAME_CLASH")
    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
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
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate)
                val endDateObj =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(endDate)

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

                        // set up the recycler view
                        val transactions = getAllTransactions(db, userId)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE

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
                            Toast.makeText(this@TransactionsActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {

                            val totalIncomeAndExpenses = calculateIncomeAndExpenseValues(filteredTransactions)

                            tvTotalIncome.text = "R ${totalIncomeAndExpenses[0]}"
                            tvTotalExpenses.text = "R ${totalIncomeAndExpenses[1]}"

                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE

                            // Set up the recycler view
                            val adapter = TransactionHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@TransactionsActivity)

                            rvTransactions.adapter = adapter

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

                        dialog.dismiss()
                        Toast.makeText(this@TransactionsActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    else
                    {

                        val totalIncomeAndExpenses = calculateIncomeAndExpenseValues(transactions)

                        tvTotalIncome.text = "R ${totalIncomeAndExpenses[0]}"
                        tvTotalExpenses.text = "R ${totalIncomeAndExpenses[1]}"

                        rvTransactions.visibility = View.VISIBLE
                        tvNoTransactions.visibility = View.GONE

                        // Set up the recycler view
                        val adapter = TransactionHistoryAdapter(transactions)

                        rvTransactions.layoutManager = LinearLayoutManager(this@TransactionsActivity)

                        rvTransactions.adapter = adapter

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
        val sortedList = combinedTransactions.sortedByDescending { item ->
            when (item) {
                is Income -> item.date
                is Expense -> item.date
                is Saving -> item.date
                else -> Date(0) // fallback
            }
        }

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

        // round to 2 decimal places
        totalIncome = String.format("%.2f", totalIncome).toDouble()

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

        // round to 2 decimal places
        totalExpenses = String.format("%.2f", totalExpenses).toDouble()

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
            .sortedByDescending { item ->
                when (item) {
                    is Income -> item.date
                    is Expense -> item.date
                    is Saving -> item.date
                    else -> Date(0)
                }
            }
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

        // round to 2 decimal places
        totalIncome = String.format("%.2f", totalIncome).toDouble()
        totalExpenses = String.format("%.2f", totalExpenses).toDouble()

        return listOf(totalIncome, totalExpenses)
    }

    // endregion

    // region Date picker Launcher
    private fun showDatePicker(isStart: Boolean, etStartDate: EditText, etEndDate: EditText)
    {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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