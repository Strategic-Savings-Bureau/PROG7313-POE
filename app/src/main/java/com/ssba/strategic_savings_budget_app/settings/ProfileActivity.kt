package com.ssba.strategic_savings_budget_app.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.SettingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityProfileBinding
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.models.UserViewModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/*
 	* Code Attribution
 	* Purpose:
 	*   - Setting up Supabase client in an Android app
 	*   - Uploading an image to a Supabase bucket
 	*   - Implementing Swipe to Refresh functionality in an Android app
 	*   - Loading and displaying images using Glide library
 	*   - Accessing the authenticated user and updating the user's password using Firebase Authentication
 	* Author: Supabase Community / Android Developers / BumpTech / Firebase Team
 	* Sources:
 	*   - Supabase Android Client: https://supabase.com/docs/guides/with-react-native/android
 	*   - Uploading Files to Bucket: https://supabase.com/docs/guides/storage/upload-files
 	*   - Swipe to Refresh: https://developer.android.com/reference/android/widget/SwipeRefreshLayout
 	*   - Glide: https://github.com/bumptech/glide
 	*   - Firebase Authentication - Update Password: https://firebase.google.com/docs/auth/android/manage-users#update_a_users_password
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

    // Supabase Storage
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://bxpptnwmvrqqvdwpzucp.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ4cHB0bndtdnJxcXZkd3B6dWNwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQxNDU0OTUsImV4cCI6MjA1OTcyMTQ5NX0.rl2dikHc7MA6ECiBUvgD5LnHctCujKq3AU9p-nh-1CI"
    ) {
        install(Postgrest)
        install(Storage)
    }

    private var profilePictureUri: Uri? = null

    // Image Picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.ivProfilePic.setImageURI(it)
                profilePictureUri = it

                lifecycleScope.launch {
                    // Fetch UserID from Firebase
                    val userID = auth.currentUser!!.uid
                    // Fetch RoomDB user
                    val user = db.userDao.getUserById(userID) ?: return@launch

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
        // Back Button to Return to Menu On Click Listener
        binding.btnBackButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        // Add Profile Picture On Click Listener
        binding.btnAddProfilePicture.setOnClickListener {
            // Launch the image picker
            pickImageLauncher.launch("image/*")
        }

        // Button to Update User Profile On Click Listener
        binding.btnSaveChanges.setOnClickListener {
            // Get the user inputs
            val username = binding.etUsername.text.toString().trim()
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            // Validate the user inputs
            viewModel.validateUsername()
            viewModel.validateFullName()
            viewModel.validateEmail()
            if (viewModel.usernameError.value != null ||
                viewModel.fullNameError.value != null ||
                viewModel.emailError.value != null
            ) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Get the user credentials
                val current = auth.currentUser ?: return@launch
                val uid = current.uid

                // Update Firebase Authentication
                if (email.isNotEmpty() && email != current.email) {
                    try {
                        @Suppress("DEPRECATION")
                        current.updateEmail(email).await()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Failed to update email: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        Log.e("ProfileActivity", "Failed to update email", e)
                        return@launch
                    }
                }

                // Update Room DB
                val existing = withContext(Dispatchers.IO) {
                    db.userDao.getUserById(uid)
                } ?: run {
                    // fallback: if not in DB yet, initialize with empty picture URL
                    User(uid, username, fullName, email, "")
                }

                // Create updated user
                val updatedUser = User(
                    userId = uid,
                    username = username,
                    fullName = fullName,
                    email = email,
                    profilePictureUrl = existing.profilePictureUrl
                )

                // Upsert updated user
                withContext(Dispatchers.IO) {
                    db.userDao.upsertUser(updatedUser)
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
            val newPassword = binding.etPassword.text.toString()

            // Validate the user inputs
            viewModel.validatePassword()
            viewModel.validateConfirmPassword()
            if (viewModel.passwordError.value != null ||
                viewModel.confirmPasswordError.value != null
            ) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Get current user
                val user = auth.currentUser ?: return@launch

                try {
                    // Update password
                    user.updatePassword(newPassword).await()

                    // Display message and clear inputs
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Password updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        // clear the fields
                        binding.etPassword.text?.clear()
                        binding.etConfirmPassword.text?.clear()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Failed to update password: ${e.message}",
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
        // Initialize the storage bucket name
        val bucketId = "user-profile-pictures"

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
            // Initialize the storage bucket
            val bucket = supabase.storage.from(bucketId)

            // Upload the image to the specified file path within the bucket
            bucket.upload(fileName, fileBytes)
            {
                upsert = true
                contentType = ContentType.Image.JPEG // Set the content type to JPEG
            }

            // Retrieve and return the public URL of the uploaded image
            return bucket.publicUrl(fileName)
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
        val user = db.userDao.getUserById(auth.currentUser?.uid ?: return)
        withContext(Dispatchers.Main) {
            binding.etUsername.setText(user?.username)
            binding.etFullName.setText(user?.fullName)
            binding.etEmail.setText(user?.email)
            val picUrl = user?.profilePictureUrl.takeUnless { it.isNullOrBlank() }
            Glide.with(this@ProfileActivity)
                .load(picUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(binding.ivProfilePic)
        }
    }
}
