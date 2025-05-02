package com.ssba.strategic_savings_budget_app.budget
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.ssba.strategic_savings_budget_app.TransactionsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseEntryBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.models.ExpenseEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/*
 	* Code Attribution
 	* Purpose:
 	*   - Setting up Supabase client in an Android app
 	*   - Uploading an image to a Supabase bucket
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Implementing the Material DatePicker for selecting dates in the app
 	* Author: Supabase Community / Developers / Firebase Team / Android Developers
 	* Sources:
 	*   - Supabase Android Client: https://supabase.com/docs/guides/with-react-native/android
 	*   - Uploading Files to Bucket: https://supabase.com/docs/guides/storage/upload-files
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Material DatePicker: https://developer.android.com/reference/com/google/android/material/datepicker/MaterialDatePicker
*/

class ExpenseEntryActivity : AppCompatActivity() {

    private val viewModel: ExpenseEntryViewModel by viewModels()
    private lateinit var binding: ActivityExpenseEntryBinding

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var receiptUri: Uri? = null

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: com.ssba.strategic_savings_budget_app.daos.ExpenseDao
    private lateinit var categoryDao: com.ssba.strategic_savings_budget_app.daos.ExpenseCategoryDao

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

        Log.d("ExpenseEntryActivity", "onCreate started")

        // Initialize database and DAOs
        db = AppDatabase.getInstance(this)
        expenseDao = db.expenseDao
        categoryDao = db.expenseCategoryDao

        // Inflate view binding
        binding = ActivityExpenseEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.lifecycleOwner = this
        binding.viewmodel = viewModel

        Log.d("ExpenseEntryActivity", "Binding initialized")

        setupImagePicker()
        setupValidationObservers()
        setupDatePicker()
        loadCategories()
        setupActions()

        Log.d("ExpenseEntryActivity", "onCreate completed")
    }

    private fun setupImagePicker() {
        Log.d("ExpenseEntryActivity", "setupImagePicker started")

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                receiptUri = it
                Toast.makeText(this, "Receipt selected", Toast.LENGTH_SHORT).show()
                Log.d("ExpenseEntryActivity", "Receipt selected: $uri")
            }
        }

        Log.d("ExpenseEntryActivity", "setupImagePicker completed")
    }

    private fun setupValidationObservers() {
        Log.d("ExpenseEntryActivity", "setupValidationObservers started")

        viewModel.titleError.observe(this) { binding.etTitle.error = it }
        viewModel.dateError.observe(this) { binding.etDate.error = it }
        viewModel.amountError.observe(this) { binding.etAmount.error = it }
        viewModel.categoryError.observe(this) {
            it?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                Log.d("ExpenseEntryActivity", "Category error: $msg")
            }
        }
        viewModel.descriptionError.observe(this) { binding.etDescription.error = it }

        Log.d("ExpenseEntryActivity", "setupValidationObservers completed")
    }

    private fun setupDatePicker() {
        Log.d("ExpenseEntryActivity", "setupDatePicker started")

        binding.etDate.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Date picker clicked")
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        datePicker.addOnPositiveButtonClickListener { sel ->
            selectedDateMillis = sel
            val dateStr = datePicker.headerText
            binding.etDate.setText(dateStr)
            viewModel.date.value = dateStr
            Log.d("ExpenseEntryActivity", "Selected date: $dateStr")
        }

        Log.d("ExpenseEntryActivity", "setupDatePicker completed")
    }

    private fun loadCategories() {
        Log.d("ExpenseEntryActivity", "loadCategories started")

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
                        Log.d("ExpenseEntryActivity", "Category selected: $position")
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                }

            Log.d("ExpenseEntryActivity", "Categories loaded: ${names.size}")
        }

        Log.d("ExpenseEntryActivity", "loadCategories completed")
    }

    private fun setupActions() {
        Log.d("ExpenseEntryActivity", "setupActions started")

        // Attach button
        binding.btnAttach.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Attach button clicked")
            pickImageLauncher.launch("image/*")
        }

        // Save button
        // Save button
        binding.btnSave.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Save button clicked")
            if (viewModel.validateAll()) {
                val title = viewModel.titleOrName.value.orEmpty()
                val amount = viewModel.amount.value?.toDoubleOrNull() ?: 0.0
                val description = viewModel.description.value.orEmpty()
                val categoryId = (binding.spinnerCategory.selectedItemPosition + 1)
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

                // Save expense in background
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        expenseDao.upsertExpense(expense)
                    }
                    // Once saved, show Toast and navigate to TransactionsActivity
                    Toast.makeText(this@ExpenseEntryActivity, "Expense Saved", Toast.LENGTH_SHORT).show()
                    Log.d("ExpenseEntryActivity", "Expense saved: $expense")

                    // Intent to navigate to TransactionsActivity
                    val intent = Intent(this@ExpenseEntryActivity, TransactionsActivity::class.java)
                    startActivity(intent)

                    // Optionally, you can finish this activity if you don't want the user to go back
                    finish()
                }
            } else {
                Toast.makeText(this, "Please fill in all required fields correctly.", Toast.LENGTH_SHORT).show()
                Log.d("ExpenseEntryActivity", "Validation failed")
            }
        }


        // Cancel button
        binding.btnCancel.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Cancel button clicked")
            finish()
        }

        Log.d("ExpenseEntryActivity", "setupActions completed")
    }
}
