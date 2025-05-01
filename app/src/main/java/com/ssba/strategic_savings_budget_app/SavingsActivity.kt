package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsBinding

class SavingsActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivitySavingsBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View components
    private lateinit var btnRewards: ImageButton
    private lateinit var btnDateRangeFilter: ImageButton
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
        btnDateRangeFilter = binding.btnDateFilter
        tvNoSavingsGoals = binding.tvNoSavingsGoals
        rvSavingsGoals = binding.rvSavingGoals
        // endregion

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {

        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnDateRangeFilter.setOnClickListener {
            Toast.makeText(this, "Date Range Filter Coming Soon", Toast.LENGTH_SHORT).show()
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
}