package com.ssba.strategic_savings_budget_app.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
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
import com.squareup.picasso.Picasso

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

    // region Image Picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.ivProfilePic.setImageURI(it)
                profilePictureUri = it

                lifecycleScope.launch {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Not logged in",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        return@launch
                    }

                    // Upload to Supabase
                    val newUrl = try {
                        uploadImageToSupabase(uri, firebaseUser.uid)
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

                    // Fetch existing user
                    val existing = withContext(Dispatchers.IO) {
                        db.userDao.getUserById(firebaseUser.uid)
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "User record not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    // Only update if it really changed
                    if (newUrl != existing.profilePictureUrl) {
                        val updated = existing.copy(profilePictureUrl = newUrl)
                        withContext(Dispatchers.IO) {
                            db.userDao.upsertUser(updated)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Profile picture updated",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Same picture, no update needed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
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

        // Populate User Profile
        lifecycleScope.launch {
            val user = db.userDao.getUserById(auth.currentUser?.uid ?: return@launch)

            binding.etUsername.setText(user?.username)
            binding.etFullName.setText(user?.fullName)
            binding.etEmail.setText(user?.email)
            Picasso.get()
                .load(user?.profilePictureUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(binding.ivProfilePic)
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
    @Suppress("DEPRECATION")
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
                        current.updateEmail(email).await()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Failed to update email: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
                    binding.etUsername.text?.clear()
                    binding.etFullName.text?.clear()
                    binding.etEmail.text?.clear()

                    // Reload the activity to reflect changes
                    recreate()
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
                    }
                }
            }
        }
    }

    // Method for Image Upload to Supabase
    private suspend fun uploadImageToSupabase(uri: Uri, fileName: String): String {
        val bucketId = "user-profile-pictures"

        val fileBytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        }

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
            ""
        }
    }
}
