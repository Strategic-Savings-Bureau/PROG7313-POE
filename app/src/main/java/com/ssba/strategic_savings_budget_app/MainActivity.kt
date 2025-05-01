package com.ssba.strategic_savings_budget_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch
import java.util.Date

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
    }

    private fun setupOnClickListeners() {

        btnAddIncome.setOnClickListener {

            // Start Add Income Intent Here
            startActivity(Intent(this, IncomeEntryActivity::class.java))
        }

        btnAddExpense.setOnClickListener {

            // Start Add Expense Intent Here
            startActivity(Intent(this, ExpenseEntryActivity::class.java))
        }

        btnAddSavings.setOnClickListener {

            // Start Add Savings Intent Here
            startActivity(Intent(this, SavingsEntryActivity::class.java))
        }

        btnRewards.setOnClickListener {

            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()

            // Start Rewards Intent Here
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

    // endregion
}