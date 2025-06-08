package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ssba.strategic_savings_budget_app.AnalysisActivity
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityCreateCategoryBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.helpers.SupabaseUtils
import com.ssba.strategic_savings_budget_app.models.CreateCategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/*
 	* Code Attribution
 	* Purpose:
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	* Author: Firebase Team
 	* Sources:
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
*/


class CreateCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCategoryBinding

    private lateinit var db: AppDatabase
    private val auth = Firebase.auth
    private val viewModel: CreateCategoryViewModel by viewModels()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                receiptUri = it

                binding.ivPhoto.setImageURI(it)
                binding.ivPhoto.visibility = android.view.View.VISIBLE
            }
        }
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                // Show thumbnail immediately
                binding.ivPhoto.setImageBitmap(it)
                binding.ivPhoto.visibility = android.view.View.VISIBLE

                val baos = ByteArrayOutputStream().apply {
                    it.compress(Bitmap.CompressFormat.JPEG, 85, this)
                }
                receiptBytes = baos.toByteArray()
            }
        }

    private var receiptUri: Uri? = null
    private var receiptBytes: ByteArray? = null

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

        // Initialize Supabase Client
        SupabaseUtils.init(this)

        setupButtons()
    }

    private fun setupButtons() {
        // Attach button
        binding.btnAttach.setOnClickListener {
            Log.d("ExpenseEntryActivity", "Attach button clicked")
            AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                    if (which == 0) {
                        // LAUNCH CAMERA (thumbnail-only)
                        cameraLauncher.launch(null)
                    } else {
                        // LAUNCH GALLERY (exactly as before)
                        pickImageLauncher.launch("image/*")
                    }
                }
                .show()
        }

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
        // Save to database
        lifecycleScope.launch {
            // Get the image URL
            val publicUrl = receiptUri?.let { uri ->
                uploadImageToSupabase(uri, null, "category_${UUID.randomUUID()}.jpg")
            } ?: receiptBytes?.let { byteArray ->
                uploadImageToSupabase(null, byteArray, "category_${UUID.randomUUID()}.jpg")
            } ?: ""

            // Create the new category instance
            val newCategory = ExpenseCategory(
                name = viewModel.categoryName.value.orEmpty(),
                description = viewModel.categoryDescription.value.orEmpty(),
                icon = publicUrl,
                maximumMonthlyTotal = viewModel.maximumMonthlyTotal.value?.toDoubleOrNull() ?: 0.0,
                userId = auth.currentUser?.uid.toString()
            )

            Log.d("CreateCategoryActivity", "Saving category: $newCategory")

            withContext(Dispatchers.IO) {
                db.expenseCategoryDao.upsertExpenseCategory(newCategory)
            }
            Toast.makeText(this@CreateCategoryActivity, "Category created!", Toast.LENGTH_SHORT)
                .show()
            Log.d("CreateCategoryActivity", "Category saved successfully")

            // Intent to navigate to AnalysisActivity
            val intent = Intent(this@CreateCategoryActivity, AnalysisActivity::class.java)
            startActivity(intent)
            finish() // Finish activity after saving and navigating
        }
    }

    private suspend fun uploadImageToSupabase(
        uri: Uri?,
        byteArray: ByteArray?,
        fileName: String
    ): String {
        val fileBytes = withContext(Dispatchers.IO) {
            when {
                uri != null -> {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                byteArray != null -> byteArray
                else -> null
            }
        } ?: return ""

        return try {
            SupabaseUtils.uploadCategoryToStorage(fileName, fileBytes)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CreateCategoryActivity,
                    "Upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e("ExpenseEntry", "upload failed", e)
            ""
        }
    }
}
