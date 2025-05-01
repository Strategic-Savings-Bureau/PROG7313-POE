package com.ssba.strategic_savings_budget_app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsGoalBinding

class SavingsGoalActivity : AppCompatActivity()
{
    // region Declarations
    // View Binding
    private lateinit var binding: ActivitySavingsGoalBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    private lateinit var savingsGoalTitle: String

    // region View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvSavingsGoalTitle: TextView
    // endregion

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivitySavingsGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        rvTransactions = binding.rvExpenseTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        btnBack = binding.btnBack
        tvSavingsGoalTitle = binding.tvSavingsGoalTitle
        // endregion

        // get the title of the savings goal from the intent
        savingsGoalTitle = intent.getStringExtra("SAVINGS_GOAL_TITLE") ?: ""

        if (savingsGoalTitle.isEmpty())
        {
            finish()
            return
        }

        tvSavingsGoalTitle.text = savingsGoalTitle

        // Method to Set Up onClickListeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners()
    {
        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnDateFilter.setOnClickListener {
            Toast.makeText(this, "Date Filter Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }
}