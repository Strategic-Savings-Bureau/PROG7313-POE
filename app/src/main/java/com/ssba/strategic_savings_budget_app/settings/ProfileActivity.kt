package com.ssba.strategic_savings_budget_app.settings

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.SettingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityProfileBinding
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.helpers.SupabaseUtils
import com.ssba.strategic_savings_budget_app.landing.RegisterActivity.AppConstants
import com.ssba.strategic_savings_budget_app.models.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/*
    * Code Attribution
    * Purpose:
    * - Implementing Swipe to Refresh functionality in an Android app
    * - Loading and displaying images using Glide library
    * - Accessing the authenticated user and updating the user's password using Firebase Authentication
    * Author: Android Developers / BumpTech / Firebase Team
    * Sources:
    * - Swipe to Refresh: https://developer.android.com/reference/android/widget/SwipeRefreshLayout
    * - Glide: https://github.com/bumptech/glide
    * - Firebase Authentication - Update Password: https://firebase.google.com/docs/auth/android/manage-users#update_a_users_password
*/

class ProfileActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityProfileBinding

    // Data Binding
    private val viewModel: UserViewModel by viewModels()

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Room Database
    private lateinit var db: AppDatabase

    private var profilePictureUri: Uri? = null

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.ivProfilePic.setImageURI(it)
                profilePictureUri = it

                lifecycleScope.launch {
                    // Fetch UserID from Firebase
                    val userID = auth.currentUser!!.uid
                    // Fetch RoomDB user
                    val user = db.userDao().getUserById(userID) ?: return@launch

                    // Upload to Supabase
                    val newUrl = try {
                        uploadImageToSupabase(uri, userID)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Upload failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    // Update the user profile picture in RoomDB
                    val updated = user.copy(profilePictureUrl = newUrl)
                    withContext(Dispatchers.IO) {
                        db.userDao().upsertUser(updated)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Profile picture updated (Please restart the app)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                // 2a) Show thumbnail immediately
                binding.ivProfilePic.setImageBitmap(it)

                // 2b) Convert Bitmap → JPEG bytes and upload
                lifecycleScope.launch {
                    val userID = auth.currentUser!!.uid
                    val baos = ByteArrayOutputStream().apply {
                        it.compress(Bitmap.CompressFormat.JPEG, 85, this)
                    }
                    val jpegBytes = baos.toByteArray()

                    val newUrl = try {
                        SupabaseUtils.uploadProfileImageToStorage(userID, jpegBytes)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Upload failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    val user = db.userDao.getUserById(userID) ?: return@launch
                    val updated = user.copy(profilePictureUrl = newUrl)
                    withContext(Dispatchers.IO) {
                        db.userDao.upsertUser(updated)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Profile picture updated (Please restart the app)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Data Binding
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Room Database
        db = AppDatabase.getInstance(this)

        // Set up SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                loadUserData()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Initial data load
        lifecycleScope.launch {
            loadUserData()
        }

        // Initialize Supabase Client
        SupabaseUtils.init(this)

        setupTextWatchers()
        setupValidationObservers()
        setupButtonClickListeners()
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
        // ADDED: Text Watcher for Current Password
        binding.etCurrentPassword.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.currentPassword.value = text
            viewModel.validateCurrentPassword()
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
        // ADDED: Observer for Current Password Error
        viewModel.currentPasswordError.observe(this) { error ->
            binding.etCurrentPassword.error = error
        }
        viewModel.passwordError.observe(this) { error ->
            binding.etPassword.error = error
        }
        viewModel.confirmPasswordError.observe(this) { error ->
            binding.etConfirmPassword.error = error
        }
    }

    // Implementation for Button On Click Listeners
    @SuppressLint("UseKtx")
    private fun setupButtonClickListeners() {
        // Back Button to Return to Menu On Click Listener
        binding.btnBackButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        // Add Profile Picture On Click Listener
        binding.btnAddProfilePicture.setOnClickListener {
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
        }

        // Button to Update User Profile On Click Listener
        binding.btnSaveChanges.setOnClickListener {
            // Get the user inputs
            val username = binding.etUsername.text.toString().trim()
            val fullName = binding.etFullName.text.toString().trim()

            // Validate the user inputs
            viewModel.validateUsername()
            viewModel.validateFullName()
            if (viewModel.usernameError.value != null ||
                viewModel.fullNameError.value != null
            ) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Get the user credentials
                val current = auth.currentUser ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "No user logged in.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                val uid = current.uid


                // Update Room DB
                val existing = withContext(Dispatchers.IO) {
                    db.userDao().getUserById(uid)
                } ?: run {
                    // fallback: if not in DB yet, initialize with empty picture URL
                    User(uid, username, fullName, current.email ?: "", "")
                }

                // Create updated user
                val updatedUser = User(
                    userId = uid,
                    username = username,
                    fullName = fullName,
                    email = existing.email,
                    profilePictureUrl = existing.profilePictureUrl
                )

                // Upsert updated user
                withContext(Dispatchers.IO) {
                    db.userDao().upsertUser(updatedUser)
                }

                // Display message and clear inputs
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Profile saved", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Button to Update User Password On Click Listener
        binding.btnUpdatePassword.setOnClickListener {
            // Get the user inputs
            val currentPassword = binding.etCurrentPassword.text.toString() // Get current password
            val newPassword = binding.etPassword.text.toString()

            // Validate all necessary user inputs
            viewModel.validateCurrentPassword() // Validate current password
            viewModel.validatePassword()
            viewModel.validateConfirmPassword()

            // Check for validation errors
            if (viewModel.currentPasswordError.value != null || // Check current password error
                viewModel.passwordError.value != null ||
                viewModel.confirmPasswordError.value != null
            ) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Get current user
                val user = auth.currentUser ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "No user logged in.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val userEmail = user.email

                if (userEmail == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "User email not found for reauthentication.", Toast.LENGTH_LONG).show()
                    }
                    Log.e("ProfileActivity", "User email is null, cannot reauthenticate.")
                    return@launch
                }

                try {
                    // Step 1: Reauthenticate the user
                    val credential = EmailAuthProvider.getCredential(userEmail, currentPassword)
                    user.reauthenticate(credential).await()

                    // If reauthentication succeeds, proceed to update the password
                    user.updatePassword(newPassword).await()

                    // Display message and clear inputs
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Password updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        val sharedPrefKeyPass = getString(R.string.saved_password)
                        val sharedPrefPassword =getSharedPreferences(AppConstants.PREFERENCE_FILE_KEY,MODE_PRIVATE)?:return@withContext
                        with(sharedPrefPassword.edit()) {
                            putString(sharedPrefKeyPass, newPassword) // Use the defined key

                            apply()
                        }

                        // clear the fields
                        binding.etCurrentPassword.text?.clear() // Clear current password field
                        binding.etPassword.text?.clear()
                        binding.etConfirmPassword.text?.clear()

                        startActivity(Intent(this@ProfileActivity, SettingsActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = when (e) {
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid current password. Please try again."
                            is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException -> "This operation requires recent authentication. Please log out and log back in, then try again."
                            else -> "Failed to update password: ${e.message}"
                        }
                        Toast.makeText(
                            this@ProfileActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        Log.e("ProfileActivity", "Failed to update password", e)
                    }
                }
            }
        }
    }

    // Method for Image Upload to Supabase
    private suspend fun uploadImageToSupabase(uri: Uri, fileName: String): String {
//        // Initialize the storage bucket name
//        val bucketId = "user-profile-pictures"

        // Read the image data from the URI
        val fileBytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        }

        // Check if the file bytes are null
        if (fileBytes == null) {
            return ""
        }

        return try {
            // Retrieve and return the public URL of the uploaded image
            return SupabaseUtils.uploadProfileImageToStorage(fileName, fileBytes)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Failed to Upload Profile Picture",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e("ProfileActivity", "Failed to Upload Profile Picture", e)
            ""
        }
    }

    // Extract data loading into a suspend function
    private suspend fun loadUserData() {
        val user = db.userDao().getUserById(auth.currentUser?.uid ?: return)
        withContext(Dispatchers.Main) {
            binding.etUsername.setText(user?.username)
            binding.etFullName.setText(user?.fullName)
            val picUrl = user?.profilePictureUrl.takeUnless { it.isNullOrBlank() }
            Glide.with(this@ProfileActivity)
                .load(picUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(binding.ivProfilePic)
        }
    }
}