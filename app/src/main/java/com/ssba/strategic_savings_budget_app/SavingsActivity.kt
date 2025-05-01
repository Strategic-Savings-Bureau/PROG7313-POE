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
import com.ssba.strategic_savings_budget_app.adapters.SavingsGoalAdapter
import com.ssba.strategic_savings_budget_app.budget.SavingGoalEntryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsBinding
import com.ssba.strategic_savings_budget_app.entities.SavingGoal
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch

class SavingsActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivitySavingsBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View components
    private lateinit var btnRewards: ImageButton
    private lateinit var btnAddGoal: ImageButton
    private lateinit var tvNoSavingsGoals: TextView
    private lateinit var rvSavingsGoals: RecyclerView
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivitySavingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miSavings

        // Initialise Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialise Database
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        btnAddGoal = binding.btnAddGoal
        tvNoSavingsGoals = binding.tvNoSavingsGoals
        rvSavingsGoals = binding.rvSavingGoals
        // endregion

        lifecycleScope.launch {

            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@SavingsActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // region Set up the RecyclerView

            // get all saving goals for the current user
            val savingGoals = getSavingGoals(db, userId)

            if (savingGoals.isEmpty())
            {
                tvNoSavingsGoals.visibility = View.VISIBLE
                rvSavingsGoals.visibility = View.GONE
            }
            else
            {
                tvNoSavingsGoals.visibility = View.GONE
                rvSavingsGoals.visibility = View.VISIBLE

                // set up the adapter
                val adapter = SavingsGoalAdapter(savingGoals)

                rvSavingsGoals.layoutManager = LinearLayoutManager(this@SavingsActivity)

                rvSavingsGoals.adapter = adapter
            }

            // endregion
        }

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnAddGoal.setOnClickListener {

            // Navigate to Add Saving Goal Activity
            startActivity(Intent(this, SavingGoalEntryActivity::class.java))
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
                R.id.miTransactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Savings Activity
                R.id.miSavings -> true
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

    // region Saving Goal Helper Methods

    // method to get all saving goals for the current user
    private suspend fun getSavingGoals(db: AppDatabase, userId: String): List<SavingGoal>
    {
        val savingGoals = mutableListOf<SavingGoal>()

        val userWithGoals = db.userDao.getUserWithSavingGoals(userId)

        if (userWithGoals.isNotEmpty())
        {
            savingGoals.addAll(userWithGoals[0].savingGoals)

            // order the goals by date, closest end date first
            savingGoals.sortBy { it.endDate }

            return savingGoals
        }

        return savingGoals
    }

    // endregion
}