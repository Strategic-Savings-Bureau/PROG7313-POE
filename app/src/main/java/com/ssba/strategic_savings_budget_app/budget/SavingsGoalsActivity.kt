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
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.SavingsActivity
import com.ssba.strategic_savings_budget_app.SavingsGoalActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsGoalsBinding
import com.ssba.strategic_savings_budget_app.entities.SavingGoal
import com.ssba.strategic_savings_budget_app.models.SavingsGoalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class SavingGoalEntryActivity : AppCompatActivity() {

    private val viewModel: SavingsGoalViewModel by viewModels()
    private lateinit var binding: ActivitySavingsGoalsBinding
    private lateinit var db: AppDatabase
    private lateinit var savingGoalDao: com.ssba.strategic_savings_budget_app.daos.SavingGoalDao
    private val auth = FirebaseAuth.getInstance()

    private var selectedDateMillis: Long? = null
    private val datePicker by lazy {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now()) // Allow only today or future
            .build()

        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SavingGoalEntryActivity", "onCreate called")
        enableEdgeToEdge()

        binding = ActivitySavingsGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)
        savingGoalDao = db.savingsGoalDao

        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        setupDatePicker()
        setupValidationObservers()
        setupActions()
    }

    private fun setupDatePicker() {
        binding.etSavingsGoalDate.setOnClickListener {
            Log.d("SavingGoalEntryActivity", "Date picker clicked")
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            selectedDateMillis = selectedDate
            val dateStr = datePicker.headerText
            binding.etSavingsGoalDate.setText(dateStr)
            viewModel.date.value = dateStr
            Log.d("SavingGoalEntryActivity", "Date selected: $dateStr")
        }
    }

    private fun setupValidationObservers() {
        viewModel.titleError.observe(this) { binding.etSavingsGoalName.error = it }
        viewModel.amountError.observe(this) { binding.etSavingsAmount.error = it }
        viewModel.dateError.observe(this) { binding.etSavingsGoalDate.error = it }
        viewModel.descriptionError.observe(this) { binding.etSavingsDescription.error = it }
    }

    private fun setupActions() {
        binding.btnSaveGoal.setOnClickListener {
            Log.d("SavingGoalEntryActivity", "Save button clicked")
            Log.d("SavingGoalEntry", "Errors → title=${viewModel.titleError.value} ▸ amount=${viewModel.amountError.value} ▸ date=${viewModel.dateError.value} ▸ desc=${viewModel.descriptionError.value}")
            if (viewModel.validateAll()) {
                val title = viewModel.titleOrName.value.orEmpty()
                val amount = viewModel.amount.value?.toDouble() ?: 0.0
                val description = viewModel.description.value.orEmpty()
                val goalDate = Date(selectedDateMillis ?: System.currentTimeMillis())

                val savingGoal = SavingGoal(
                    userId = auth.currentUser?.uid.toString(),
                    title = title,
                    targetAmount = amount,
                    description = description,
                    endDate = goalDate
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    Log.d("SavingGoalEntryActivity", "Saving goal to DB: $savingGoal")
                    savingGoalDao.upsertSavingGoal(savingGoal)
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@SavingGoalEntryActivity,
                            "Saving Goal Saved",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("SavingGoalEntryActivity", "Navigating to SavingsActivity")
                        startActivity(Intent(this@SavingGoalEntryActivity, SavingsActivity::class.java))
                        finish()
                    }
                }
            } else {
                Log.d("SavingGoalEntryActivity", "Validation failed")
            }
        }

        binding.btnCancelGoal.setOnClickListener {
            Log.d("SavingGoalEntryActivity", "Cancel button clicked")
            finish()
        }
    }
}
