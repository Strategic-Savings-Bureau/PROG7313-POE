package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth

import com.google.firebase.ktx.Firebase
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityBudgetSettingsBinding
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.models.BudgetSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        // edge-to-edge inset support
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        setupValidationObservers()
        setupButtons()
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

        binding.btnRewards.setOnClickListener {
            Log.d("BudgetSettingsActivity", "Rewards button clicked")
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
