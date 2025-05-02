package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityCreateCategoryBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.models.CreateCategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCategoryBinding

    private lateinit var db: AppDatabase
    private val auth = Firebase.auth
    private val viewModel: CreateCategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // DataBinding setup
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_category)
        binding.lifecycleOwner = this

        // Initialize ViewModel
        binding.viewmodel = viewModel

        // Initialize database
        db = AppDatabase.getInstance(this)

        setupButtons()
    }

    private fun setupButtons() {
        // Save button
        binding.btnSaveCategory.setOnClickListener {
            Log.d("CreateCategoryActivity", "Save button clicked")
            if (viewModel.validateAll()) {
                Log.d("CreateCategoryActivity", "Validation passed, saving category...")
                saveCategoryToDb()
            } else {
                Toast.makeText(this, "Please complete all required fields.", Toast.LENGTH_SHORT)
                    .show()
                Log.d("CreateCategoryActivity", "Validation failed, fields missing")
            }
        }

        // Cancel button
        binding.btnCancelCategory.setOnClickListener {
            Log.d("CreateCategoryActivity", "Cancel button clicked, finishing activity")
            finish()
        }
    }

    private fun saveCategoryToDb() {
        // Create the new category instance
        val newCategory = ExpenseCategory(
            categoryId = 0, // Auto-generated in DB
            name = viewModel.categoryName.value.orEmpty(),
            description = viewModel.categoryDescription.value.orEmpty(),
            icon = viewModel.categoryIcon.value.orEmpty(),
            maximumMonthlyTotal = viewModel.maximumMonthlyTotal.value?.toDoubleOrNull() ?: 0.0,
            userId = auth.currentUser?.uid.toString()
        )

        Log.d("CreateCategoryActivity", "Saving category: $newCategory")

        // Save to database
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.expenseCategoryDao.upsertExpenseCategory(newCategory)
            }
            Toast.makeText(this@CreateCategoryActivity, "Category created!", Toast.LENGTH_SHORT)
                .show()
            Log.d("CreateCategoryActivity", "Category saved successfully")

            // Intent to navigate to HomeActivity
            val intent = Intent(this@CreateCategoryActivity, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish activity after saving and navigating
        }
    }
}
