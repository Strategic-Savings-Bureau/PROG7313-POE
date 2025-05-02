package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.ssba.strategic_savings_budget_app.SavingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsEntryBinding
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.models.SavingsEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/*
 	* Code Attribution
 	* Purpose:
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Implementing the Material DatePicker for selecting dates in the app
 	* Author: Firebase Team / Android Developers
 	* Sources:
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Material DatePicker: https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker
*/

class SavingsEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavingsEntryBinding
    private val viewModel: SavingsEntryViewModel by viewModels()
    private lateinit var db: AppDatabase
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
    private val auth = Firebase.auth
    private var selectedDateMillis: Long? = null
    private var savingGoalIds: List<Int> = emptyList()
    private var selectedSavingGoalId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // DataBinding setup
        binding = ActivitySavingsEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(this)


        binding.lifecycleOwner = this
        binding.viewmodel = viewModel
        loadSavingGoals()
        setupDatePicker()
        setupButtons()
    }

    private fun setupDatePicker() {
        binding.etSavingsDate.setOnClickListener {
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDateMillis = selection
            val dateStr = datePicker.headerText
            binding.etSavingsDate.setText(dateStr)
            viewModel.date.value = dateStr
        }
    }

    private fun loadSavingGoals() {
        lifecycleScope.launch {
            val goals = withContext(Dispatchers.IO) {
                db.savingsGoalDao.getSavingGoalsByUserId(auth.currentUser?.uid.toString())
            }

            val titles = goals.map { it.title }
            savingGoalIds = goals.mapNotNull { it.savingGoalId }
            viewModel.savingsGoals.value = titles
            val adapter = ArrayAdapter(
                this@SavingsEntryActivity,
                android.R.layout.simple_spinner_item,
                titles
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerSavingsGoal.adapter = adapter

            binding.spinnerSavingsGoal.setSelection(viewModel.typePosition.value ?: 0)
            binding.spinnerSavingsGoal.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {

                        viewModel.typePosition.value = position
                        Log.w("SavingsEntry","Current value ${viewModel.typePosition.value}")
                        selectedSavingGoalId = savingGoalIds.getOrNull(position)

                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        viewModel.typePosition.value = -1 // Or some default behavior
                    }
                }
        }
    }

    private fun setupButtons() {
        binding.btnSaveSavings.setOnClickListener {
            Log.w("check","Current value ${viewModel.typePosition.value}")
            Log.w("check","Current goal id $selectedSavingGoalId")
            if (viewModel.validateAll()) {
                saveToDb()
            } else {
                Toast.makeText(this, "Please complete all required fields.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnCancelSavings.setOnClickListener { finish() }
    }

    private fun saveToDb() {
        Log.d("SavingsEntryActivity", "Starting save to DB...")

        val saving = Saving(
            date = Date(selectedDateMillis ?: System.currentTimeMillis()),
            title = viewModel.titleOrName.value.orEmpty(),
            amount = viewModel.amount.value?.toDoubleOrNull() ?: 0.0,
            description = viewModel.description.value.orEmpty(),
            savingGoalId = selectedSavingGoalId!!
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Log.d("SavingsEntryActivity", "Inserting saving into DB...")
                db.savingDao.upsertSaving(saving)
            }

            Log.d("SavingsEntryActivity", "Saving successfully recorded.")
            Toast.makeText(this@SavingsEntryActivity, "Saving recorded!", Toast.LENGTH_SHORT).show()

            // Send Intent to SavingsActivity
            val intent = Intent(this@SavingsEntryActivity, SavingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
