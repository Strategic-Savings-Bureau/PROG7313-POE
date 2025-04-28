package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityMainBinding
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    // region Declarations
    // View Binding
    private lateinit var binding: ActivityMainBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Database
    private lateinit var db: AppDatabase

    // View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var tvUsername: TextView
    private lateinit var btnAddIncome: ImageButton
    private lateinit var btnAddExpense: ImageButton
    private lateinit var btnAddSavings: ImageButton
    private lateinit var rvRecentTransactions: RecyclerView
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialisation of View Components
        btnRewards = binding.btnRewards
        tvUsername = binding.tvUsername
        btnAddIncome = binding.btnAddIncome
        btnAddExpense = binding.btnAddExpense
        btnAddSavings = binding.btnAddSavings
        rvRecentTransactions = binding.rvRecentTransactions
        // endregion

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miHome

        lifecycleScope.launch {

            // Get the Current Users Id
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            val user = getCurrentUser(db, userId)

            if (user == null)
            {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            tvUsername.text = user.username
        }


        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {

        btnAddIncome.setOnClickListener {

            Toast.makeText(this, "Add Income Coming Soon", Toast.LENGTH_SHORT).show()

            // Start Add Income Intent Here
        }

        btnAddExpense.setOnClickListener {

            Toast.makeText(this, "Add Expense Coming Soon", Toast.LENGTH_SHORT).show()

            // Start Add Expense Intent Here
        }

        btnAddSavings.setOnClickListener {

            Toast.makeText(this, "Add Savings Coming Soon", Toast.LENGTH_SHORT).show()

            // Start Add Savings Intent Here
        }

        btnRewards.setOnClickListener {

            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()

            // Start Rewards Intent Here
        }

        // Set up Bottom Navigation View onClickListener
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                // Navigate to Main (Home) Activity
                R.id.miHome -> true
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
                R.id.miProfile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    suspend fun getCurrentUser(db: AppDatabase, userId: String): User? {
        return db.userDao.getUserById(userId)
    }

}