package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityIncomeEntryBinding
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.models.IncomeEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class IncomeEntryActivity : AppCompatActivity() {

    private val viewModel: IncomeEntryViewModel by viewModels()
    private lateinit var binding: ActivityIncomeEntryBinding
    private val auth = Firebase.auth
    private lateinit var db: AppDatabase
    private lateinit var incomeDao: com.ssba.strategic_savings_budget_app.daos.IncomeDao

    private var selectedDateMillis: Long? = null
    private val datePicker by lazy {

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now()) // Allow only today or earlier
            .build()

        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // init DB & DAO
        db = AppDatabase.getInstance(this)
        incomeDao = db.incomeDao

        binding = ActivityIncomeEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        setupDatePicker()
        setupValidationObservers()
        setupActions()
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        datePicker.addOnPositiveButtonClickListener { sel ->
            selectedDateMillis = sel
            val dateStr = datePicker.headerText
            binding.etDate.setText(dateStr)
            viewModel.date.value = dateStr
        }
    }

    private fun setupValidationObservers() {
        viewModel.titleError.observe(this)       { binding.etTitle.error       = it }
        viewModel.dateError.observe(this)        { binding.etDate.error        = it }
        viewModel.amountError.observe(this)      { binding.etAmount.error      = it }
        viewModel.descriptionError.observe(this) { binding.etDescription.error = it }
    }

    private fun setupActions() {
        binding.btnSave.setOnClickListener {
            Log.d("IncomeEntryActivity", "Save button clicked")

            if (viewModel.validateAll()) {
                Log.d("IncomeEntryActivity", "Validation passed, saving income")

                val title       = viewModel.titleOrName.value!!.trim()
                val description = viewModel.description.value!!.trim()
                val date        = Date(selectedDateMillis ?: System.currentTimeMillis())
                val amountVal   = viewModel.amount.value!!.toDouble() // safe: validated >0

                val income = Income(
                    userId      = auth.currentUser?.uid.toString(),
                    title       = title,
                    date        = date,
                    amount      = amountVal,
                    description = description
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    incomeDao.upsertIncome(income)
                    Log.d("IncomeEntryActivity", "Income saved to database")

                    launch(Dispatchers.Main) {
                        Toast.makeText(this@IncomeEntryActivity, "Income Saved", Toast.LENGTH_SHORT).show()

                        // Intent to navigate to HomeActivity
                        val intent = Intent(this@IncomeEntryActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Finish this activity after navigating to the Home screen
                        Log.d("IncomeEntryActivity", "Navigating to HomeActivity")
                    }
                }
            } else {
                Log.d("IncomeEntryActivity", "Validation failed")
                Toast.makeText(this, "Please complete all required fields.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            Log.d("IncomeEntryActivity", "Cancel button clicked, finishing activity")
            finish()
        }

        binding.btnRewards.setOnClickListener {
            Log.d("IncomeEntryActivity", "Rewards button clicked")
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
