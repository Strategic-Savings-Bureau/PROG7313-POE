package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.ExpenseCategoryAdapter
import com.ssba.strategic_savings_budget_app.budget.CreateCategoryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityAnalysisBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.StreakManager
import kotlinx.coroutines.launch

/*
 	* Code Attribution
 	* Purpose:
 	*   - Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	*   - Creating and displaying an AlertDialog in an Android app
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	* Author: Android Developers / Firebase Team
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
*/

class AnalysisActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityAnalysisBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View components
    private lateinit var btnRewards: ImageButton
    private lateinit var btnAddCategory: ImageButton
    private lateinit var tvNoCategories: TextView
    private lateinit var rvCategories: RecyclerView
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miAnalysis

        // Initialise Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialise Database
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        btnAddCategory = binding.btnAddCategory
        tvNoCategories = binding.tvNoExpenseCategories
        rvCategories = binding.rvExpenseCategories
        // endregion

        lifecycleScope.launch {

            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@AnalysisActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // region Set up the RecyclerView

            // get all Expense Categories for the current user
            val expenseCategories = getExpenseCategories(db, userId)

            if (expenseCategories.isEmpty())
            {
                tvNoCategories.visibility = View.VISIBLE
                rvCategories.visibility = View.GONE
            }
            else
            {
                tvNoCategories.visibility = View.GONE
                rvCategories.visibility = View.VISIBLE

                // set up the adapter
                val adapter = ExpenseCategoryAdapter(expenseCategories)

                // set up the layout manager
                rvCategories.layoutManager = LinearLayoutManager(this@AnalysisActivity)

                // set rv adapter
                rvCategories.adapter = adapter
            }

            // endregion

        }

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            val streakManager = StreakManager(this)
            streakManager.updateStreak()
            val currentStreak = streakManager.getCurrentStreak()

            Toast.makeText(this, "Current streak: $currentStreak days.", Toast.LENGTH_LONG).show()
        }

        btnAddCategory.setOnClickListener {

            // Navigate to Add Saving Goal Activity
            startActivity(Intent(this, CreateCategoryActivity::class.java))
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
                R.id.miAnalysis -> true
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

    // region Expense Category Helper Methods

    // method to get all expense categories for the current user
    private suspend fun getExpenseCategories(db: AppDatabase, userId: String): List<ExpenseCategory>
    {
        val expenseCategories = mutableListOf<ExpenseCategory>()

        val userWithCategories = db.userDao.getUserWithExpenseCategories(userId)

        if (userWithCategories.isNotEmpty())
        {
            expenseCategories.addAll(userWithCategories[0].expenseCategories)

            // order the expense categories by their monthly budget
            expenseCategories.sortByDescending { it.maximumMonthlyTotal }

            return expenseCategories
        }

        return expenseCategories
    }

    // endregion
}