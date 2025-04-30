package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.IncomeHistoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityIncomeHistoryBinding
import com.ssba.strategic_savings_budget_app.databinding.ActivityTransactionsBinding
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class IncomeHistoryActivity : AppCompatActivity()
{
    // region Declarations
    // View Binding
    private lateinit var binding: ActivityIncomeHistoryBinding
    // endregion

    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth

    // region View Components
    private lateinit var btnRewards: ImageButton
    private lateinit var tvTotalIncome: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var btnDateFilter: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvMinIncomeGoal: TextView
    private lateinit var pbIncomeGoal: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    // endregion


    override fun onCreate(savedInstanceState: Bundle?)
    {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityIncomeHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // region Initialise View Components
        btnRewards = binding.btnRewards
        tvTotalIncome = binding.tvTotalIncome
        rvTransactions = binding.rvIncomeTransactions
        tvNoTransactions = binding.tvNoTransactions
        btnDateFilter = binding.btnDateFilter
        btnBack = binding.btnBack
        tvMinIncomeGoal = binding.tvMinIncomeGoal
        pbIncomeGoal = binding.progressIncomeGoal
        tvProgressPercentage = binding.tvProgressPercentage
        // endregion

        lifecycleScope.launch {

            // Get the current user's ID
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Get the total income and expenses for the current user
            val totalIncome = getTotalIncome(db, userId)

            // Set the text of the total income
            tvTotalIncome.text = "R $totalIncome"

            // Get the minimum monthly income goal for the current user
            val minimumIncome = getMinimumIncome(db, userId)

            // Set the text of the minimum monthly income goal
            tvMinIncomeGoal.text = "Minimum Monthly Income: R $minimumIncome"

            // Get the total income for the current month
            val totalIncomeForCurrentMonth = getTotalIncomeForCurrentMonth(db, userId)

            // Calculate the progress percentage
            val progressPercentage = (totalIncomeForCurrentMonth / minimumIncome) * 100

            // Set the progress of the progress bar
            pbIncomeGoal.progress = progressPercentage.toInt()

            // Set the text of the progress percentage
            tvProgressPercentage.text = "${progressPercentage.toInt()}% towards goal"

            // region Set up RecyclerView

            // get all the incomes for the current user
            val incomeTransactions = getIncomeAllTransactions(db, userId)

            if (incomeTransactions.isEmpty())
            {
                tvNoTransactions.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            }
            else
            {
                rvTransactions.visibility = View.VISIBLE
                tvNoTransactions.visibility = View.GONE

                val adapter = IncomeHistoryAdapter(incomeTransactions)

                rvTransactions.layoutManager = LinearLayoutManager(this@IncomeHistoryActivity)

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
    }

    // region Transaction Helper Methods

    private suspend fun getIncomeAllTransactions(db: AppDatabase, userId: String): List<Income>
    {
         // get all the incomes for the current user
            val userWithIncomes = db.userDao.getUserWithIncomes(userId)
            val incomeTransactions = userWithIncomes[0].incomes

        // sort the incomes by date in descending order
        return incomeTransactions.sortedByDescending { it.date }
    }

    @SuppressLint("DefaultLocale")
    private suspend fun getTotalIncome(db: AppDatabase, userId: String): Double
    {
        val userWithIncomes = db.userDao.getUserWithIncomes(userId)

        var totalIncome = 0.00

        if (userWithIncomes.isEmpty())
        {
            return totalIncome
        }

        for (income in userWithIncomes[0].incomes)
        {
            totalIncome += income.amount
        }

        // round to 2 decimal places
        totalIncome = String.format("%.2f", totalIncome).toDouble()

        return totalIncome
    }

    // method to get the users budget
    private suspend fun getMinimumIncome(db: AppDatabase, userId: String): Double
    {
        val budget = db.budgetDao.getBudgetByUserId(userId) ?: return 0.00

        return budget.minimumMonthlyIncome
    }

    // method to get total Income for current month
    @SuppressLint("DefaultLocale")
    private suspend fun getTotalIncomeForCurrentMonth(db: AppDatabase, userId: String): Double {
        val userWithIncomes = db.userDao.getUserWithIncomes(userId)
        var totalIncome = 0.00

        if (userWithIncomes.isEmpty()) {
            return totalIncome
        }

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        for (income in userWithIncomes[0].incomes)
        {
            val incomeCalendar = Calendar.getInstance()
            incomeCalendar.time = income.date

            val incomeMonth = incomeCalendar.get(Calendar.MONTH)
            val incomeYear = incomeCalendar.get(Calendar.YEAR)

            if (incomeMonth == currentMonth && incomeYear == currentYear) {
                totalIncome += income.amount
            }
        }

        // Round to 2 decimal places
        totalIncome = String.format("%.2f", totalIncome).toDouble()

        return totalIncome
    }


    // endregion

    // region Date picker Launcher
    private fun showDatePicker(isStart: Boolean, etStartDate: EditText, etEndDate: EditText)
    {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // Only allow today or earlier
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Select Start Date" else "Select End Date")
            .setCalendarConstraints(constraints)
            .build()

        // Use supportFragmentManager instead of childFragmentManager
        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = dateFormat.format(Date(selection))
            if (isStart)
            {
                etStartDate.setText(selectedDate)
            }
            else
            {
                etEndDate.setText(selectedDate)
            }
        }
    }
    // endregion
}