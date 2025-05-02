package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityBudgetSettingsBinding
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.BudgetSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetSettingsBinding
    private val viewModel: BudgetSettingsViewModel by viewModels()

    private lateinit var db: AppDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = AppDatabase.getInstance(this)
        auth = Firebase.auth
        binding = ActivityBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        setupValidationObservers()
        setupButtons()
        loadCurrentBudget()
    }

    private fun loadCurrentBudget() {
        // Get the current user's ID
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // Log the error and redirect to login
            Log.e("BudgetSettingsActivity", "User ID is null")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val userWithBudget = db.userDao.getUserWithBudget(userId)

            if (userWithBudget.isNotEmpty()) {
                // Get the budget details
                val budget = userWithBudget[0].budget

                withContext(Dispatchers.Main) {
                    // Set current values in the ViewModel
                    viewModel.minimumMonthlyIncome.value = budget.minimumMonthlyIncome.toString()
                    viewModel.maximumMonthlyExpenses.value = budget.maximumMonthlyExpenses.toString()
                }
            } else {
                Log.e("BudgetSettingsActivity", "User with ID $userId not found")
            }
        }
    }

    private fun setupValidationObservers() {
        viewModel.minIncomeError.observe(this) {
            binding.etMinIncome.error = it
        }
        viewModel.maxExpensesError.observe(this) {
            binding.etMaxExpenses.error = it
        }
    }

    private fun setupButtons() {
        binding.btnAdvancedSettings.setOnClickListener {
            Log.d("BudgetSettingsActivity", "Advanced settings button clicked")

            val maxExpenses = viewModel.maximumMonthlyExpenses.value ?: "0"
            val currIncome = viewModel.minimumMonthlyIncome.value ?: "0"
            val currCategoryExpenses = viewModel.currExpenseTotal.value ?: "0" // Adjust if needed

            // Passing the data to the AdvancedBudgetSettingsActivity
            val intent = Intent(this@BudgetSettingsActivity, AdvancedBudgetSettingsActivity::class.java).apply {
                putExtra("maxExpenses", maxExpenses)
                putExtra("currIncome", currIncome)
                putExtra("currCategoryExpenses", currCategoryExpenses)
            }
            startActivity(intent)
            finish()
        }

        binding.btnSaveBudget.setOnClickListener {
            Log.d("BudgetSettingsActivity", "Save button clicked")

            if (viewModel.validateAll() && auth.currentUser != null) {
                Log.d("BudgetSettingsActivity", "Validation passed, saving budget")

                val minIncome = viewModel.minimumMonthlyIncome.value!!.toDouble()
                val maxExpenses = viewModel.maximumMonthlyExpenses.value!!.toDouble()
                val userId = auth.currentUser?.uid.toString()

                // Create or update budget entry
                var budget = Budget(
                    minimumMonthlyIncome = minIncome,
                    maximumMonthlyExpenses = maxExpenses,
                    userId = userId
                )

                lifecycleScope.launch {
                    // Check if budget already exists for the user
                    val existingBudget = db.budgetDao.getBudgetByUserId(userId)
                    if (existingBudget != null) {
                        // If a budget already exists, update it
                        budget = Budget(
                            budgetId =    existingBudget.budgetId,
                            minimumMonthlyIncome = minIncome,
                            maximumMonthlyExpenses = maxExpenses,
                            userId = userId
                        )

                    }

                    db.budgetDao.upsertBudget(budget)
                    Log.d("BudgetSettingsActivity", "Budget saved to database")

                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@BudgetSettingsActivity,
                            "Budget saved",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to the MainActivity after saving
                        val intent = Intent(this@BudgetSettingsActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Finish this activity after navigating
                        Log.d("BudgetSettingsActivity", "Navigating to HomeActivity")
                    }
                }
            } else {
                Log.d("BudgetSettingsActivity", "Validation failed or user not authenticated")
                Toast.makeText(
                    this,
                    "Please complete all required fields or log in.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnCancelBudget.setOnClickListener {
            Log.d("BudgetSettingsActivity", "Cancel button clicked, finishing activity")
            finish()
        }
    }
}
