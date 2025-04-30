package com.ssba.strategic_savings_budget_app.budget

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

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
            if (viewModel.validateAll() && auth.currentUser!=null) {
                val minIncome = viewModel.minimumMonthlyIncome.value!!.toDouble()
                val maxExpenses = viewModel.maximumMonthlyExpenses.value!!.toDouble()

                val budget = Budget(
                    minimumMonthlyIncome = minIncome,
                    maximumMonthlyExpenses = maxExpenses,
                    userId = auth.currentUser?.uid.toString()
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    db.budgetDao.upsertBudget(budget)
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@BudgetSettingsActivity, "Budget saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        binding.btnCancelBudget.setOnClickListener {
            finish()
        }
        binding.btnRewards.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}