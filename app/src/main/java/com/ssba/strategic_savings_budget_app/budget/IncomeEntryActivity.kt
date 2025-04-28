package com.ssba.strategic_savings_budget_app.budget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

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
     val auth = Firebase.auth
    private lateinit var db: AppDatabase
    private lateinit var incomeDao: com.ssba.strategic_savings_budget_app.daos.IncomeDao

    private var selectedDateMillis: Long? = null
    private val datePicker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Select a date")
        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init DB and DAO
        db = AppDatabase.getInstance(this)
        incomeDao = db.incomeDao

        // Inflate\ binding
        binding = ActivityIncomeEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

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
        viewModel.titleError.observe(this) { binding.etTitle.error = it }
        viewModel.dateError.observe(this) { binding.etDate.error = it }
        viewModel.descriptionError.observe(this) { binding.etDescription.error = it }
    }

    private fun setupActions() {
        binding.btnSave.setOnClickListener {
            if (viewModel.validateAll()) {
                val title = viewModel.titleOrName.value.orEmpty()
                val description = viewModel.description.value.orEmpty()
                val date = Date(selectedDateMillis ?: System.currentTimeMillis())

                val income = Income(
                 userId =  auth.currentUser?.email.toString() ,
                    title = title,
                    date = date,
                    amount = 0.0, // default amount
                    description = description
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    incomeDao.upsertIncome(income)
                }
                Toast.makeText(this, "Income Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        binding.btnCancel.setOnClickListener { finish() }
    }
}
