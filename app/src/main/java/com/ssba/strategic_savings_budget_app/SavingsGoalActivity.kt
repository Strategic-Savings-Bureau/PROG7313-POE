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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
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

            val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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

    @Suppress("LABEL_NAME_CLASH")
    private fun setupOnClickListeners()
    {
        btnRewards.setOnClickListener {
            Toast.makeText(this, "Rewards Coming Soon", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnAddSaving.setOnClickListener {

            // navigate to the add saving activity
            val intent = Intent(this, SavingsEntryActivity::class.java)
            startActivity(intent)
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


                        // set up the recycler view
                        val transactions = getAllSavingsForGoal(savingsGoalTitle, db)

                        if (transactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE

                            dialog.dismiss()
                            Toast.makeText(this@SavingsGoalActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // filter the transactions by date
                        val filteredTransactions = filterSavingsByDateRange(transactions, startDateObj, endDateObj)

                        if (filteredTransactions.isEmpty())
                        {
                            rvTransactions.visibility = View.GONE
                            tvNoTransactions.visibility = View.VISIBLE
                            Toast.makeText(this@SavingsGoalActivity, "No Transactions Found", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            return@launch
                        }
                        else
                        {
                            rvTransactions.visibility = View.VISIBLE
                            tvNoTransactions.visibility = View.GONE

                            // Set up the recycler view
                            val adapter = SavingGoalTransactionHistoryAdapter(filteredTransactions)

                            rvTransactions.layoutManager = LinearLayoutManager(this@SavingsGoalActivity)

                            rvTransactions.adapter = adapter

                            dialog.dismiss()
                            Toast.makeText(this@SavingsGoalActivity, "Transactions Filtered Successfully", Toast.LENGTH_SHORT).show()
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


                    // set up the recycler view
                    val transactions = getAllSavingsForGoal(savingsGoalTitle, db)

                    if (transactions.isEmpty()) {
                        rvTransactions.visibility = View.GONE
                        tvNoTransactions.visibility = View.VISIBLE

                        dialog.dismiss()
                        Toast.makeText(
                            this@SavingsGoalActivity,
                            "No Transactions Found",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    else
                    {
                        // region Set up RecyclerView

                        // get all the savings for the goal
                        val savingsTransactions = getAllSavingsForGoal(savingsGoalTitle, db)

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

                        dialog.dismiss()
                        Toast.makeText(
                            this@SavingsGoalActivity,
                            "Filter Cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                }
            }
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

    // Method to filter Savings transactions by date range
    private fun filterSavingsByDateRange(list: List<Saving>, startDate: Date, endDate: Date): List<Saving> {
        return list
            .filter { it.date in startDate..endDate }
            .sortedByDescending { it.date }
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