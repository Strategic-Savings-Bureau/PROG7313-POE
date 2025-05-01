package com.ssba.strategic_savings_budget_app.budget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
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
    private val datePicker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Select a deadline")
        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the binding and set the content view
        binding = ActivitySavingsGoalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database and DAO
        db = AppDatabase.getInstance(this)
        savingGoalDao = db.savingsGoalDao

        // Link viewmodel and binding
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        setupDatePicker()
        setupValidationObservers()
        setupActions()
    }

    private fun setupDatePicker() {
        // Show date picker when the date field is clicked
        binding.etSavingsGoalDate.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            selectedDateMillis = selectedDate
            val dateStr = datePicker.headerText
            binding.etSavingsGoalDate.setText(dateStr)
            viewModel.date.value = dateStr
        }
    }

    private fun setupValidationObservers() {
        // Observe validation errors from the ViewModel and set them in the UI
        viewModel.titleError.observe(this) { binding.etSavingsGoalName.error = it }
        viewModel.amountError.observe(this) { binding.etSavingsAmount.error = it }
        viewModel.dateError.observe(this) { binding.etSavingsGoalDate.error = it }
        viewModel.descriptionError.observe(this) { binding.etSavingsDescription.error = it }
    }

    private fun setupActions() {
        // Handle Save button click
        binding.btnSaveGoal.setOnClickListener {
            if (viewModel.validateAll()) {
                val title = viewModel.titleOrName.value.orEmpty()
                val amount = viewModel.amount.value?.toDouble() ?: 0.0
                val description = viewModel.description.value.orEmpty()
                val goalDate = Date(selectedDateMillis ?: System.currentTimeMillis())

                // Create the SavingGoal object
                val savingGoal = SavingGoal(
                    userId = auth.currentUser?.uid.toString(),
                    title = title,
                    targetAmount = amount,
                    description = description,
                    endDate = goalDate
                )

                // Save the saving goal in the database
                lifecycleScope.launch(Dispatchers.IO) {
                    savingGoalDao.upsertSavingGoal(savingGoal)
                }

                // Show success message and close the activity
                Toast.makeText(this, "Saving Goal Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Handle Cancel button click
        binding.btnCancelGoal.setOnClickListener { finish() }
        binding.btnRewards.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
