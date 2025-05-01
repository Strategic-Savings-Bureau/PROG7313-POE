package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySavingsEntryBinding
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.models.SavingsEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class SavingsEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavingsEntryBinding
    private lateinit var viewModel: SavingsEntryViewModel
    private lateinit var db: AppDatabase
    private val datePicker by lazy {
        MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_savings_entry)
        binding.lifecycleOwner = this

        // Initialize ViewModel with no types (unused here)
        viewModel = SavingsEntryViewModel()
        binding.viewmodel = viewModel

        // Initialize database
        db = AppDatabase.getInstance(this)

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
            if (viewModel.validateAll()) {
                saveToDb()
            } else {
                Toast.makeText(this, "Please complete all required fields.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnCancelSavings.setOnClickListener { finish() }
        binding.btnRewards.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToDb() {
        Log.d("SavingsEntryActivity", "Starting save to DB...")

        val saving = Saving(
            date = Date(selectedDateMillis ?: System.currentTimeMillis()),
            title = viewModel.titleOrName.value.orEmpty(),
            amount = viewModel.amount.value?.toDoubleOrNull() ?: 0.0,
            description = viewModel.description.value.orEmpty(),
            savingGoalId = selectedSavingGoalId ?: 0
        )

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Log.d("SavingsEntryActivity", "Inserting saving into DB...")
                db.savingDao.upsertSaving(saving)
            }

            Log.d("SavingsEntryActivity", "Saving successfully recorded.")
            Toast.makeText(this@SavingsEntryActivity, "Saving recorded!", Toast.LENGTH_SHORT).show()

            // Send Intent to MainActivity
            val intent = Intent(this@SavingsEntryActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
