package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityProfileBinding
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miProfile

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {
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
                R.id.miSavings -> {
                    startActivity(Intent(this, SavingsActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Profile Activity
                R.id.miProfile -> true

                else -> false
            }
        }
    }
}