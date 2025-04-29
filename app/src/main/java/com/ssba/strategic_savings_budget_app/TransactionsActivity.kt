package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityTransactionsBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch
import java.util.Date

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
    // endregion

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
        }


        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnIncomeTransactions.setOnClickListener {
            Toast.makeText(this, "Income Transactions Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnExpenseTransactions.setOnClickListener {
            Toast.makeText(this, "Expense Transactions Coming Soon", Toast.LENGTH_SHORT).show()
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
        val userWithIncomes = db.userDao.getUserWithIncomes(userId)
        val incomes = userWithIncomes[0].incomes

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

    // endregion
}