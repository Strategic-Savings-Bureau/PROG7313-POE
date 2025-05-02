package com.ssba.strategic_savings_budget_app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 	* Code Attribution
 	* Purpose:
 	*   - Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	*   - Creating and displaying an AlertDialog in an Android app
 	* Author: Android Developers
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - AlertDialog: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
*/


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


    @SuppressLint("SetTextI18n")
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

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        lifecycleScope.launch {

            // Get the current user's ID
            val userId = auth.currentUser?.uid

            if (userId == null)
            {
                startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Get the total income for the current user
            val totalIncome = getTotalIncome(db, userId)

            // Set the text of the total income
            tvTotalIncome.text = currencyFormat.format(totalIncome)

            // Get the minimum monthly income goal for the current user
            val minimumIncome = getMinimumIncome(db, userId)

            // Set the text of the minimum monthly income goal
            tvMinIncomeGoal.text = "Minimum Monthly Income: ${currencyFormat.format(totalIncome)}"

            // Get the total income for the current month
            val totalIncomeForCurrentMonth = getTotalIncomeForCurrentMonth(db, userId)

            // Calculate the progress percentage
            val progressPercentage = (totalIncomeForCurrentMonth / minimumIncome) * 100

            // Set the progress of the progress bar
            pbIncomeGoal.progress = progressPercentage.toInt()

            // Set the text of the progress percentage
            tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly goal"

            // region Set up RecyclerView

            // get all the incomes for the current user
            val incomeTransactions = getAllIncomeTransactions(db, userId)

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

    @SuppressLint("SetTextI18n")
    @Suppress("LABEL_NAME_CLASH")
    private fun setupOnClickListeners()
    {
        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnDateFilter.setOnClickListener {

            // show date picker dialog
            val dialogView = layoutInflater.inflate(R.layout.dialog_date_range_filter, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.show()

            // access view components in dialog
            val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
            val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
            val btnApplyDateFilter = dialogView.findViewById<Button>(R.id.btnApplyDateFilter)
            val btnClearFilter = dialogView.findViewById<Button>(R.id.btnClearFilter)

            etStartDate.setOnClickListener {
                showDatePicker(true, etStartDate, etEndDate)
            }

            etEndDate.setOnClickListener {
                showDatePicker(false, etStartDate, etEndDate)
            }

            btnApplyDateFilter.setOnClickListener {

                // get the selected dates
                val startDate = etStartDate.text.toString()
                val endDate = etEndDate.text.toString()

                // check if the dates are empty
                if (startDate.isEmpty() || endDate.isEmpty())
                {
                    Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // convert the dates to Date objects
                val startDateObj =
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(startDate)
                val endDateObj =
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).parse(endDate)

                // check if the dates are valid
                if (startDateObj != null && endDateObj != null)
                {
                    lifecycleScope.launch {

                        // Get the current user's ID
                        val userId = auth.currentUser?.uid

                        if (userId == null)
                        {
                            startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                            finish()
                            return@launch
                        }

                        // set up the recycler view
                        val transactions = getAllIncomeTransactions(db, userId)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE

                            dialog.dismiss()
                            Toast.makeText(this@IncomeHistoryActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // filter the transactions by date
                        val filteredTransactions = filterIncomeByDateRange(transactions, startDateObj, endDateObj)

                        if (filteredTransactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            Toast.makeText(this@IncomeHistoryActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {
                            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                            binding.cardIncomeGoal.visibility = View.GONE

                            val totalIncome = calculateTotalIncome(filteredTransactions)

                            tvTotalIncome.text = currencyFormat.format(totalIncome)

                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE

                            // Set up the recycler view
                            val adapter = IncomeHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@IncomeHistoryActivity)

                            rvTransactions.adapter = adapter

                            dialog.dismiss()
                            Toast.makeText(this@IncomeHistoryActivity, "Transactions Filtered Successfully", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                    }
                }
                else
                {
                    Toast.makeText(this, "Invalid Date Range", Toast.LENGTH_SHORT).show()
                }

            }

            btnClearFilter.setOnClickListener {

                lifecycleScope.launch {

                    // Get the current user's ID
                    val userId = auth.currentUser?.uid

                    if (userId == null) {
                        startActivity(Intent(this@IncomeHistoryActivity, LoginActivity::class.java))
                        finish()
                        return@launch
                    }

                    // set up the recycler view
                    val transactions = getAllIncomeTransactions(db, userId)

                    if (transactions.isEmpty()) {
                        rvTransactions.visibility = View.GONE
                        tvNoTransactions.visibility = View.VISIBLE

                        dialog.dismiss()
                        Toast.makeText(
                            this@IncomeHistoryActivity,
                            "No Transactions Found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    else
                    {

                        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

                        // Get the total income for the current user
                        val totalIncome = getTotalIncome(db, userId)

                        // Set the text of the total income
                        tvTotalIncome.text = currencyFormat.format(totalIncome)

                        // make income goal card visible
                        binding.cardIncomeGoal.visibility = View.VISIBLE

                        // Get the minimum monthly income goal for the current user
                        val minimumIncome = getMinimumIncome(db, userId)

                        // Set the text of the minimum monthly income goal
                        tvMinIncomeGoal.text = "Minimum Monthly Income: ${currencyFormat.format(minimumIncome)}"

                        // Get the total income for the current month
                        val totalIncomeForCurrentMonth = getTotalIncomeForCurrentMonth(db, userId)

                        // Calculate the progress percentage
                        val progressPercentage = (totalIncomeForCurrentMonth / minimumIncome) * 100

                        // Set the progress of the progress bar
                        pbIncomeGoal.progress = progressPercentage.toInt()

                        // Set the text of the progress percentage
                        tvProgressPercentage.text = "${progressPercentage.toInt()}% towards monthly goal"

                        // region Set up RecyclerView

                        // get all the incomes for the current user
                        val incomeTransactions = getAllIncomeTransactions(db, userId)

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

                        dialog.dismiss()
                        Toast.makeText(
                            this@IncomeHistoryActivity,
                            "Filter Cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                }
            }
        }
    }

    // region Transaction Helper Methods

    // Calculate total income from a list of Income transactions
    @SuppressLint("DefaultLocale")
    private fun calculateTotalIncome(list: List<Income>): Double
    {
        val totalIncome = list.sumOf { it.amount }
        return totalIncome
    }


    // Method to filter only Income transactions by date range
    private fun filterIncomeByDateRange(list: List<Income>, startDate: Date, endDate: Date): List<Income> {
        return list
            .filter { it.date in startDate..endDate }
            .sortedByDescending { it.date.time }
    }


    private suspend fun getAllIncomeTransactions(db: AppDatabase, userId: String): List<Income>
    {
         // get all the incomes for the current user
            val userWithIncomes = db.userDao.getUserWithIncomes(userId)
            val incomeTransactions = userWithIncomes[0].incomes

        // sort the incomes by date and time in descending order
        return incomeTransactions.sortedByDescending { it.date.time }
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

        return totalIncome
    }


    // endregion

    // region Date picker Launcher
    private fun showDatePicker(isStart: Boolean, etStartDate: EditText, etEndDate: EditText)
    {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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