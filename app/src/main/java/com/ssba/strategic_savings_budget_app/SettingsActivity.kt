package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivitySettingsBinding
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.settings.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.databinding.ActivityProfileBinding
import com.ssba.strategic_savings_budget_app.landing.LoginActivity

class SettingsActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivitySettingsBinding

    // endregion

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // region View Components
    // View Binding
    private lateinit var btnLogout: Button
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialise
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // region Initialise View Components
        btnLogout = binding.btnLogout
        // endregion

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miSettings

        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {
        binding.btnRewards.setOnClickListener {

        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.btnSharedBudget.setOnClickListener {

        }

        binding.btnCurrencyConverter.setOnClickListener {

        }

        binding.btnNotifications.setOnClickListener {

        }

        binding.btnBudgeting.setOnClickListener {

        }

        binding.btnYourData.setOnClickListener {

        }

        binding.btnLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()

        // Button to Log Out the Current User
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
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
                R.id.miSavings -> {
                    startActivity(Intent(this, SavingsActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Profile Activity
                R.id.miSettings -> true

                else -> false
            }
        }
    }
}