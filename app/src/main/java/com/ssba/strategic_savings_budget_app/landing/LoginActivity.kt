package com.ssba.strategic_savings_budget_app.landing

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/*
 	* Code Attribution
 	* Purpose: Implementing Firebase Authentication for user login and registration in an Android app
 	* Author: Firebase Team
 	* Source: Firebase Documentation - Firebase Authentication for Android
 	* URL: https://firebase.google.com/docs/auth/android/start
*/


class LoginActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityLoginBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Room Database
    private lateinit var db: AppDatabase

    // View Components
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    // endregion

    // onCreate Method
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Room Database
        db = AppDatabase.getInstance(this)

        // View Components
        etEmail = binding.etEmail
        etPassword = binding.etPassword
        btnLogin = binding.btnLogin
        btnRegister = binding.btnRegister

        // Log into Account On Click Listener
        btnLogin.setOnClickListener {
            // Get Email and Password from EditText
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Email Validation
            when {
                email.isEmpty() -> {
                    etEmail.error = "Email is Required"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Invalid Email"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
            }
            // Password validation
            if (password.isEmpty()) {
                etPassword.error = "Password is Required"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Check if User Exists in Room Database
                val user = db.userDao.getUserByEmail(email)

                // Attempt To Login
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful && user != null) {
                        // Display success message
                        Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()

                        // Navigate to MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Show error message if login fails
                        Toast.makeText(this@LoginActivity, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "Login failed", task.exception)
                    }
                }
            }
        }

        // Direct User to Register Activity On Click Listener
        btnRegister.setOnClickListener {
            // Navigate to UserEmailPasswordActivity
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}