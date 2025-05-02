package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.budget.ExpenseEntryActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseCategoryAnalysisBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ExpenseCategoryAnalysisActivity : AppCompatActivity()
{

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityExpenseCategoryAnalysisBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    private lateinit var categoryName: String

    // region View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnAddExpense: ImageButton
    private lateinit var tvCategoryName: TextView
    private lateinit var tvMaxMonthlyLimit: TextView
    private lateinit var tvDescription: TextView
    private lateinit var pbLimit: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    // endregion

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize View Binding
        binding = ActivityExpenseCategoryAnalysisBinding.inflate(layoutInflater)
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
        btnAddExpense = binding.btnAddExpense
        tvCategoryName = binding.tvCategoryName
        tvMaxMonthlyLimit = binding.tvMaxMonthlyLimit
        tvDescription = binding.tvDescription
        pbLimit = binding.progressLimit
        tvProgressPercentage = binding.tvProgressPercentage
        // endregion

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        // get the title of the savings goal from the intent
        categoryName = intent.getStringExtra("EXPENSE_CATEGORY_NAME") ?: ""

        if (categoryName.isEmpty())
        {
            finish()
            return
        }

        lifecycleScope.launch {


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

        btnAddExpense.setOnClickListener {

            // navigate to the add saving activity
            val intent = Intent(this, ExpenseEntryActivity::class.java)
            startActivity(intent)
        }

        btnDateFilter.setOnClickListener {

            Toast.makeText(this, "Date Filter Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }
}