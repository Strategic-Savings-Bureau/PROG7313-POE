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
    private lateinit var auth : FirebaseAuth

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
        // get the current user's id
        val userId = auth.currentUser?.uid

        if (userId == null)
        {
            // log the error
            Log.e("BudgetSettingsActivity", "User ID is null")

            // redirect to login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {

            val userWithBudget = db.userDao.getUserWithBudget(userId.toString())

            if (userWithBudget.isNotEmpty()) {
                // get the budget
                val budget = userWithBudget[0].budget

                withContext(Dispatchers.Main) {
                    viewModel.minimumMonthlyIncome.value = budget.minimumMonthlyIncome.toString()
                    viewModel.maximumMonthlyExpenses.value = budget.maximumMonthlyExpenses.toString()
                }
            }
            else {
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

        binding.btnSaveBudget.setOnClickListener {
            Log.d("BudgetSettingsActivity", "Save button clicked")

            if (viewModel.validateAll() && auth.currentUser != null) {
                Log.d("BudgetSettingsActivity", "Validation passed, saving budget")

                val minIncome = viewModel.minimumMonthlyIncome.value!!.toDouble()
                val maxExpenses = viewModel.maximumMonthlyExpenses.value!!.toDouble()

                val budget = Budget(
                    minimumMonthlyIncome = minIncome,
                    maximumMonthlyExpenses = maxExpenses,
                    userId = auth.currentUser?.uid.toString()
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    db.budgetDao.upsertBudget(budget)
                    Log.d("BudgetSettingsActivity", "Budget saved to database")

                    launch(Dispatchers.Main) {
                        Toast.makeText(this@BudgetSettingsActivity, "Budget saved", Toast.LENGTH_SHORT).show()

                        // Intent to navigate to HomeActivity
                        val intent = Intent(this@BudgetSettingsActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Finish this activity after navigating to the Home screen
                        Log.d("BudgetSettingsActivity", "Navigating to HomeActivity")
                    }
                }
            } else {
                Log.d("BudgetSettingsActivity", "Validation failed or user not authenticated")
                Toast.makeText(this, "Please complete all required fields or log in.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancelBudget.setOnClickListener {
            Log.d("BudgetSettingsActivity", "Cancel button clicked, finishing activity")
            finish()
        }
    }
}
