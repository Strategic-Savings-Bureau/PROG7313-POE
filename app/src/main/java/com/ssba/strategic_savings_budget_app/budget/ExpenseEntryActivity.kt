package com.ssba.strategic_savings_budget_app.budget

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseEntryBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.models.ExpenseEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class ExpenseEntryActivity : AppCompatActivity() {

    private val viewModel: ExpenseEntryViewModel by viewModels()
    private lateinit var binding: ActivityExpenseEntryBinding

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var receiptUri: Uri? = null

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: com.ssba.strategic_savings_budget_app.daos.ExpenseDao
    private lateinit var categoryDao: com.ssba.strategic_savings_budget_app.daos.ExpenseCategoryDao

    private var selectedDateMillis: Long? = null
    private val datePicker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Select a date")
        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database and DAOs
        db = AppDatabase.getInstance(this)
        expenseDao = db.expenseDao
        categoryDao = db.expenseCategoryDao

        // Inflate view binding
        binding = ActivityExpenseEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        setupImagePicker()
        setupValidationObservers()
        setupDatePicker()
        loadCategories()
        setupActions()
    }

    private fun setupImagePicker() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                receiptUri = it
                Toast.makeText(this, "Receipt selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupValidationObservers() {
        viewModel.titleError.observe(this) { binding.etTitle.error = it }
        viewModel.dateError.observe(this) { binding.etDate.error = it }
        viewModel.amountError.observe(this) { binding.etAmount.error = it }
        viewModel.categoryError.observe(this) {
            it?.let { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
        }
        viewModel.descriptionError.observe(this) { binding.etDescription.error = it }
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

    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = withContext(Dispatchers.IO) {
                categoryDao.getAllExpenseCategories()
            }
            val names = categories.map(ExpenseCategory::name)
            val adapter = ArrayAdapter(
                this@ExpenseEntryActivity,
                android.R.layout.simple_spinner_item,
                names
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            binding.spinnerCategory.adapter = adapter

            // Restore prior selection
            binding.spinnerCategory.setSelection(viewModel.categoryPosition.value ?: 0)
            binding.spinnerCategory.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>,
                        view: android.view.View?,
                        position: Int,
                        id: Long,
                    ) {
                        viewModel.categoryPosition.value = position
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                }
        }
    }

    private fun setupActions() {
        // Attach button
        binding.btnAttach.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Save button
        binding.btnSave.setOnClickListener {
            if (viewModel.validateAll()) {
                val title = viewModel.titleOrName.value.orEmpty()
                val amount = viewModel.amount.value?.toDoubleOrNull() ?: 0.0
                val description = viewModel.description.value.orEmpty()
                val categoryId = binding.spinnerCategory.selectedItemPosition
                val receiptUrl = receiptUri?.toString().orEmpty()
                val date = selectedDateMillis?.let { Date(it) } ?: Date()

                val expense = Expense(
                    title = title,
                    date = date,
                    amount = amount,
                    description = description,
                    receiptPictureUrl = receiptUrl,
                    categoryId = categoryId
                )

                // Save expense in backg
                // round
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        expenseDao.upsertExpense(expense)
                    }
                    // Once saved, show Toast and close Activity on Main thread
                    Toast.makeText(this@ExpenseEntryActivity, "Expense Saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Please fill in all required fields correctly.", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            finish()
        }
        binding.btnRewards.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
}
