package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.SavingGoalTransactionHistoryAdapter
import com.ssba.strategic_savings_budget_app.budget.SavingsEntryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsGoalBinding
import com.ssba.strategic_savings_budget_app.entities.Saving
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var btnAddSaving: ImageButton
    private lateinit var tvSavingsGoalTitle: TextView
    private lateinit var tvTargetAmount: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var pbGoal: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    // endregion

    @SuppressLint("SetTextI18n")
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
        rvTransactions = binding.rvSavingTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        btnBack = binding.btnBack
        tvSavingsGoalTitle = binding.tvSavingsGoalTitle
        tvTargetAmount = binding.tvTargetAmount
        tvEndDate = binding.tvEndDate
        tvDescription = binding.tvDescription
        pbGoal = binding.progressGoal
        tvProgressPercentage = binding.tvProgressPercentage
        btnAddSaving = binding.btnAddSaving
        // endregion

        // get the title of the savings goal from the intent
        savingsGoalTitle = intent.getStringExtra("SAVINGS_GOAL_TITLE") ?: ""

        if (savingsGoalTitle.isEmpty())
        {
            finish()
            return
        }

        lifecycleScope.launch {

            // get the savings goal from the database
            val savingsGoal = db.savingsGoalDao.getSavingGoalByTitle(savingsGoalTitle)

            if (savingsGoal == null)
            {
                finish()
                return@launch
            }

            tvSavingsGoalTitle.text = savingsGoal.title
            tvTargetAmount.text = "R ${savingsGoal.targetAmount}"

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            tvEndDate.text = "End Date: ${dateFormatter.format(savingsGoal.endDate)}"

            // if end date is in the past change color to red
            if (savingsGoal.endDate.before(Date())) {
                tvEndDate.setTextColor(ContextCompat.getColor(this@SavingsGoalActivity, R.color.expense_red))
            }

            tvDescription.text = savingsGoal.description

            // get the total savings for the current goal
            val totalSavings = getTotalSavingsForGoal(savingsGoal.title, db)

            val target = savingsGoal.targetAmount

            // calculate the progress percentage
            val progressPercentage = (totalSavings / target) * 100

            pbGoal.progress = progressPercentage.toInt()
            tvProgressPercentage.text = "${progressPercentage.toInt()}% saved"

            // region Set up RecyclerView

            // get all the incomes for the current user
            val savingsTransactions = getAllSavingsForGoal(savingsGoal.title, db)

            if (savingsTransactions.isEmpty())
            {
                tvNoTransactions.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                val adapter = SavingGoalTransactionHistoryAdapter(savingsTransactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@SavingsGoalActivity)

                rvTransactions.adapter = adapter
            }

            // endregion

        }

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

        btnAddSaving.setOnClickListener {

            // navigate to the add saving activity
            val intent = Intent(this, SavingsEntryActivity::class.java)
            startActivity(intent)
        }
    }

    // region Helper Methods

    // get the total of all savings for the current goal
    private suspend fun getTotalSavingsForGoal(goalTitle: String, db: AppDatabase): Double
    {
        val savingsGoalWithSavings = db.savingsGoalDao.getSavingsBySavingGoalTitle(goalTitle)

        if (savingsGoalWithSavings.isEmpty()) {
            return 0.0
        }

        val savingsList = savingsGoalWithSavings[0].savings

        if (savingsList.isEmpty()) {
            return 0.0
        }

        var savings = 0.0

        for (saving in savingsList) {
            savings += saving.amount
        }

        return savings
    }

    // get all savings for the current goal
    private suspend fun getAllSavingsForGoal(goalTitle: String, db: AppDatabase): List<Saving>
    {
        // get the savings goal from the database
        val savingsGoalWithSavings = db.savingsGoalDao.getSavingsBySavingGoalTitle(goalTitle)

        if (savingsGoalWithSavings.isEmpty()) {
            return emptyList()
        }

        // order the savings by most recent and return
        return savingsGoalWithSavings[0].savings.sortedByDescending { it.date }
    }

    // endregion
}