package com.ssba.strategic_savings_budget_app.budget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
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
    private var selectedDateMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // DataBinding setup
        binding = DataBindingUtil.setContentView(this, R.layout.activity_savings_entry)
        binding.lifecycleOwner = this

        // Initialize ViewModel with no types (unused here)
        viewModel = SavingsEntryViewModel(emptyList())
        binding.viewmodel = viewModel

        // Initialize database
        db = AppDatabase.getInstance(this)

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

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

    private fun setupButtons() {
        binding.btnSaveSavings.setOnClickListener {
            if (viewModel.validateAll()) {
                saveToDb()
            } else {
                Toast.makeText(this, "Please complete all required fields.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnCancelSavings.setOnClickListener { finish() }
    }

    private fun saveToDb() {
      // remember to fetch the current clicked category to use the reference for the ID
        val saving = Saving(
            savingId = 0,
            title = viewModel.titleOrName.value.orEmpty(),
            date = Date(selectedDateMillis ?: System.currentTimeMillis()),
            amount = viewModel.amount.value?.toDoubleOrNull() ?: 0.0,
            description = viewModel.description.value.orEmpty(),
            savingGoalId = TODO()
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.savingDao.upsertSaving(saving)
            }
            Toast.makeText(this@SavingsEntryActivity, "Saving recorded!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
