package com.ssba.strategic_savings_budget_app.landing

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivityRegisterBinding
import com.ssba.strategic_savings_budget_app.landing.models.RegistrationViewModel

class RegisterActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityRegisterBinding

    // Data Binding
    private val viewModel: RegistrationViewModel by viewModels()

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Image Picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                binding.ivProfilePic.setImageURI(it)
                // Upload to server or save locally (eg. using Bitmap or ByteArray)
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
        binding.etBio.addTextChangedListener { editable ->
            val text = editable.toString()
            viewModel.bio.value = text
            viewModel.validateBio()
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
        viewModel.bioError.observe(this) { error ->
            binding.etBio.error = error
        }
    }

    // Implementation for Button On Click Listeners
    private fun setupButtonClickListeners() {
        // Add Profile Picture On Click Listener
        binding.btnAddProfilePicture.setOnClickListener {
            pickImageLauncher.launch("image/*")
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
        // !!! Subject to change depending on FireStore/Supabase or use of a 'Data' class !!!
        auth.createUserWithEmailAndPassword(
            binding.etEmail.text.toString(),
            binding.etPassword.text.toString()
        ).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Add user to server or save locally
                // FireStore Implementation eg. 'firestore.collection("users").document(newUser.userId).set(newUser)'

                // Navigate to Login Activity
                startActivity(Intent(this, MainActivity::class.java))

                // Display success message
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Show error message if registration fails
                Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}