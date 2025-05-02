package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.adapters.BudgetCategoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityAdvancedBudgetSettingsBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.BudgetSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdvancedBudgetSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdvancedBudgetSettingsBinding
    private lateinit var adapter: BudgetCategoryAdapter
    private lateinit var db: AppDatabase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Keep a snapshot of the original list so we can detect changes
    private var originalCategories = emptyList<ExpenseCategory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvancedBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Ensure the user is logged in
        val userId = auth.currentUser?.uid.orEmpty().also {
            if (it.isBlank()) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        db = AppDatabase.getInstance(this)

        // 2) Read and display the incoming max / current expense values
        val maxExpenses  = intent.getStringExtra("maxExpenses").orEmpty().takeIf { it.isNotBlank() } ?: "0"
        val currExpenses = intent.getStringExtra("currCategoryExpenses").orEmpty().takeIf { it.isNotBlank() } ?: "0"
        binding.tvMaximumExpense.text = "Maximum Expense: R $maxExpenses"
        binding.tvCurrentExpense.text = "Current Expense: R $currExpenses"

        // 3) Load categories from Room and wire up the RecyclerView
        lifecycleScope.launch {
            originalCategories = withContext(Dispatchers.IO) {
                db.expenseCategoryDao.getExpenseCategoriesByUserId(userId)
            }
            adapter = BudgetCategoryAdapter(originalCategories) { /* optional click handler */ }
            binding.rvBudgetCategories.apply {
                layoutManager = LinearLayoutManager(this@AdvancedBudgetSettingsActivity)
                adapter = this@AdvancedBudgetSettingsActivity.adapter
            }
        }

        // 4) Save Changes button persists edits immediately
        binding.btnSaveChanges.setOnClickListener {
            saveAllEdits()
        }

        // 5) Done button checks for unsaved edits, prompts if needed, then returns
        binding.btnDone.setOnClickListener {
            handleDone()
        }
    }

    /** Reads back the edited limits, upserts them into the DB, and refreshes the “Current Expense” label */
    private fun saveAllEdits() {
        val edited = adapter.getEditedCategories(binding.rvBudgetCategories)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    edited.forEach { db.expenseCategoryDao.upsertExpenseCategory(it) }
                }
                // Update the current-total display
                val total = edited.sumOf { it.maximumMonthlyTotal }
                binding.tvCurrentExpense.text = "Current Expense: R %.2f".format(total)
                Toast.makeText(this@AdvancedBudgetSettingsActivity, "Changes saved.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@AdvancedBudgetSettingsActivity, "Failed to save changes.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** If there are unsaved edits, prompts the user to Save / Discard / Cancel; otherwise returns immediately */
    private fun handleDone() {
        val edited = adapter.getEditedCategories(binding.rvBudgetCategories)
        val changed = edited.zip(originalCategories).any { (e, o) ->
            e.maximumMonthlyTotal != o.maximumMonthlyTotal
        }

        if (changed) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved edits. Save before leaving?")
                .setPositiveButton("Save")    { _, _ ->
                    saveAllEdits()
                    returnToBudgetSettings()
                }
                .setNegativeButton("Discard") { _, _ -> returnToBudgetSettings() }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            returnToBudgetSettings()
        }
    }

    /** Packages up the current displayed max & current values and returns to BudgetSettingsActivity */
    private fun returnToBudgetSettings() {
        val updatedMax  = binding.tvMaximumExpense.text.toString().substringAfter(": ").trim()
        val updatedCurr = binding.tvCurrentExpense.text.toString().substringAfter(": ").trim()
        Intent(this, BudgetSettingsActivity::class.java).apply {
            putExtra("updatedMaxExpense", updatedMax)
            putExtra("updatedTotalExpense", updatedCurr)
            startActivity(this)
        }
        finish()
    }
}


