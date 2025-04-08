package com.ssba.strategic_savings_budget_app.landing

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityLoginBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // View Components
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    // endregion

    // onCreate Method
    override fun onCreate(savedInstanceState: Bundle?) {

        // region Initialisation
        // Default
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // View Components
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        btnLogin = binding.btnLogin
        btnRegister = binding.btnRegister
        // endregion

        // btnLogin On Click Listener
        btnLogin.setOnClickListener {
            // Get Email and Password from EditText
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Email validation
            if (email.isEmpty()) {
                etEmail.error = "Email is Required"
                etEmail.requestFocus()
                return@setOnClickListener
            }
            // Password validation
            if (password.isEmpty()) {
                etPassword.error = "Password is Required"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // Attempt To Login
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Display success message
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Show error message if login fails
                    Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // On click listener for the register button
        btnRegister.setOnClickListener {
            // Navigate to UserEmailPasswordActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}