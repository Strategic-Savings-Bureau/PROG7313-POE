package com.ssba.strategic_savings_budget_app.landing

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
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityRegisterBinding
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.helpers.SupabaseUtils
import com.ssba.strategic_savings_budget_app.models.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/*
 	* Code Attribution
 	* Purpose:
 	*   - Implementing Firebase Authentication for user login and registration
 	* Author: Developers / Firebase Team
 	* Sources:
 	*   - Firebase Authentication for Android: https://firebase.google.com/docs/auth/android/start
*/


class RegisterActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityRegisterBinding

    // Data Binding
    private val viewModel: UserViewModel by viewModels()

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Room Database
    private lateinit var db: AppDatabase

    // Profile Picture Uri/ByteArray
    private var profilePictureUri: Uri? = null
    private var profilePictureBytes: ByteArray? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                // Show thumbnail immediately
                binding.ivProfilePic.setImageBitmap(it)

                val baos = ByteArrayOutputStream().apply {
                    it.compress(Bitmap.CompressFormat.JPEG, 85, this)
                }
                profilePictureBytes = baos.toByteArray()
            }
        }

    // Image Picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.ivProfilePic.setImageURI(it)
                profilePictureUri = it
            }
        }
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Data Binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Room Database
        db = AppDatabase.getInstance(this)

        setupTextWatchers()
        setupValidationObservers()
        setupButtonClickListeners()

        // Initialize Supabase Client
        SupabaseUtils.init(this)
    }

    // Text Watcher for Live Input Validation
    private fun setupTextWatchers() {
        binding.etUsername.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.username.value = text
            viewModel.validateUsername()
        }
        binding.etFullName.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.fullName.value = text
            viewModel.validateFullName()
        }
        binding.etEmail.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.email.value = text
            viewModel.validateEmail()
        }
        binding.etPassword.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.password.value = text
            viewModel.validatePassword()
        }
        binding.etConfirmPassword.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.confirmPassword.value = text
            viewModel.validateConfirmPassword()
        }
    }

    // Validation Observer for All Input Errors
    private fun setupValidationObservers() {
        viewModel.usernameError.observe(this) { error ->
            binding.etUsername.error = error
        }
        viewModel.fullNameError.observe(this) { error ->
            binding.etFullName.error = error
        }
        viewModel.emailError.observe(this) { error ->
            binding.etEmail.error = error
        }
        viewModel.passwordError.observe(this) { error ->
            binding.etPassword.error = error
        }
        viewModel.confirmPasswordError.observe(this) { error ->
            binding.etConfirmPassword.error = error
        }
    }

    // Implementation for Button On Click Listeners
    private fun setupButtonClickListeners() {
        // Add Profile Picture On Click Listener
        binding.btnAddProfilePicture.setOnClickListener {
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

        // Create Account On Click Listener
        binding.btnCreate.setOnClickListener {
            // Redundant Final Check
            if (viewModel.validateAll()) {
                // Proceed with registration
                registerUser()
            }
        }

        // Return to Login On Click Listener
        binding.btnLogin.setOnClickListener {
            // Navigate to Login Activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Method for Registration Logic
    private fun registerUser() {
        auth.createUserWithEmailAndPassword(
            binding.etEmail.text.toString().trim(),
            binding.etPassword.text.toString().trim()
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get User ID
                val userId = auth.currentUser!!.uid

                lifecycleScope.launch {
                    // Upload the image if present; otherwise, default to an empty string.
                    val profilePictureUrl = profilePictureUri?.let { uri ->
                        uploadImageToSupabase(uri, userId)
                    } ?: profilePictureBytes?.let { byteArray ->
                        SupabaseUtils.uploadProfileImageToStorage(userId, byteArray)
                    } ?: ""

                    // Create the user object with the result of the upload (or default URL).
                    val user = User(
                        userId = userId,
                        username = binding.etUsername.text.toString(),
                        fullName = binding.etFullName.text.toString(),
                        email = binding.etEmail.text.toString(),
                        profilePictureUrl = profilePictureUrl
                    )

                    // Insert user into RoomDB.
                    db.userDao.upsertUser(user)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration Successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finish()
                    }
                }

            } else {
                // Show error message if registration fails
                Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
                Log.e("RegisterActivity", "Registration failed", task.exception)
            }
        }
    }

    // Method for Image Upload to Supabase
    private suspend fun uploadImageToSupabase(uri: Uri, fileName: String): String {

        // Read the bytes of the image file
        val fileBytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        }

        // Return an empty string if the file bytes are null
        if (fileBytes == null) {
            return ""
        }

        return try {

            // Retrieve and return the public URL of the uploaded image
            return SupabaseUtils.uploadProfileImageToStorage(fileName, fileBytes)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Failed to Upload Profile Picture",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e("RegisterActivity", "Image upload failed", e)
            ""
        }
    }
}