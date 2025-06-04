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
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.TransactionsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityExpenseEntryBinding
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.helpers.SupabaseUtils
import com.ssba.strategic_savings_budget_app.models.ExpenseEntryViewModel
import com.ssba.strategic_savings_budget_app.models.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID

class ExpenseEntryActivity : AppCompatActivity() {

    private val viewModel: ExpenseEntryViewModel by viewModels()
    private lateinit var binding: ActivityExpenseEntryBinding

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var receiptUri: Uri? = null
   private lateinit var auth: FirebaseAuth
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

        // Initialize Supabase Client
        SupabaseUtils.init(this)

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

            val adapter = ArrayAdapter(
                this@ExpenseEntryActivity,
                android.R.layout.simple_spinner_item,
                categories
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
                        Log.d("ExpenseEntryActivity", "Category position selected: $position")
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                }

            Log.d("ExpenseEntryActivity", "Categories loaded: ${categories.size}")
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
        binding.btnSave.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Save button clicked")

            if (!viewModel.validateAll()) {
                Toast.makeText(this, "Please fill in all required fields correctly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Launch a coroutine so we can call your suspend upload function
            lifecycleScope.launch {
                // 1) upload to Supabase (if there's a receiptUri) and await the URL
                val publicUrl = receiptUri?.let { uri ->
                    uploadImageToSupabase(uri, "receipt_${UUID.randomUUID()}.jpg")
                } ?: ""

                Log.d("ExpenseEntryActivity", "Received Supabase URL: $publicUrl")

                // get the correct category ID
                val selectedCategory = binding.spinnerCategory.selectedItem as ExpenseCategory

                val selectedCategoryId = selectedCategory.categoryId!!

                // 2) build the Expense object
                val expense = Expense(
                    title               = viewModel.titleOrName.value!!.trim(),
                    date                = Date(selectedDateMillis ?: System.currentTimeMillis()),
                    amount              = viewModel.amount.value!!.toDouble(),
                    description         = viewModel.description.value!!.trim(),
                    receiptPictureUrl   = publicUrl,
                    categoryId          = selectedCategoryId
                )

                // 3) write into Room on IO dispatcher
                withContext(Dispatchers.IO) {
                    expenseDao.upsertExpense(expense)
                }

                // 4) feedback + navigate away
                Toast.makeText(this@ExpenseEntryActivity, "Expense Saved", Toast.LENGTH_SHORT).show()

                // 5) Update the streak
                val streakManager = StreakManager(this@ExpenseEntryActivity)
                streakManager.updateStreak()

                // navigate to Transactions
                startActivity(Intent(this@ExpenseEntryActivity, TransactionsActivity::class.java))
                finish()
            }
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Cancel button clicked")
            finish()
        }

        Log.d("ExpenseEntryActivity", "setupActions completed")
    }
    private suspend fun uploadImageToSupabase(uri: Uri, fileName: String): String {

        // Read the image bytes
        val fileBytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return ""

        return try {

            return SupabaseUtils.uploadReceiptImageToStorage(fileName, fileBytes)
        }
        catch (e: Exception)
        {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ExpenseEntryActivity,
                    "Upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e("ExpenseEntry", "upload failed", e)
            ""
        }
    }
}
