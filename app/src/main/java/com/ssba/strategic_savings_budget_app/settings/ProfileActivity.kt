package com.ssba.strategic_savings_budget_app.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.SettingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityProfileBinding
import com.ssba.strategic_savings_budget_app.models.UserViewModel

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

    // Profile Picture Uri
    private var profilePictureUri: Uri? = null

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
        binding = ActivityProfileBinding.inflate(layoutInflater)
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
        binding.ivBackButton.setOnClickListener {
            // Update Primary and Secondary Currency
            // Set Colour Theme App Wide?
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        // Add Profile Picture On Click Listener
        binding.btnAddProfilePicture.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Button to Update User Profile On Click Listener
        binding.btnSaveChanges.setOnClickListener {
            viewModel.validateUsername()
            viewModel.validateFullName()
            viewModel.validateEmail()


        }

        // Button to Update User Password On Click Listener
        binding.btnUpdatePassword.setOnClickListener {
            viewModel.validatePassword()
            viewModel.validateConfirmPassword()


        }
    }

    // Method to Update User Profile in Firebase Authentication and Room Database

    // Method to Update User Password in Firebase Authentication

}
