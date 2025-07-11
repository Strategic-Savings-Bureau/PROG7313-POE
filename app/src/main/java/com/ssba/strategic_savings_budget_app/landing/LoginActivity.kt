package com.ssba.strategic_savings_budget_app.landing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.SyncCheckActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityLoginBinding
import com.ssba.strategic_savings_budget_app.helpers.BiometricUtils
import com.ssba.strategic_savings_budget_app.interfaces.BiometricAuthListener
import com.ssba.strategic_savings_budget_app.landing.RegisterActivity.AppConstants
import kotlinx.coroutines.launch

/*
 * Code Attribution
 * Purpose: Implementing Firebase Authentication for user login and registration in an Android app
 * Author: Firebase Team
 * Source: Firebase Documentation - Firebase Authentication for Android
 * URL: https://firebase.google.com/docs/auth/android/start
*/


class LoginActivity : AppCompatActivity(), BiometricAuthListener {

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
    private lateinit var tvLoginBiometric: TextView // Added for Biometric Login
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
        tvLoginBiometric = binding.tvLoginBiometric // Initialize Biometric Login TextView

        // check if shared preferences exist
        val sharedPrefCheck = this.getSharedPreferences(RegisterActivity.AppConstants.PREFERENCE_FILE_KEY,MODE_PRIVATE) ?: return
        val emailSharedPref = sharedPrefCheck.getString(getString(R.string.saved_email),"")
        val passwordSharedPref = sharedPrefCheck.getString(getString(R.string.saved_password),"")

        // if shared preferences do not exist, hide the biometric login button
        if (emailSharedPref.isNullOrEmpty() || passwordSharedPref.isNullOrEmpty())
        {
            tvLoginBiometric.visibility = View.GONE
        }
        else
        {
            tvLoginBiometric.visibility = View.VISIBLE
        }

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

                // Attempt To Login
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

                    if (task.isSuccessful)
                    {
                        val sharedPrefKey =
                            getString(R.string.saved_email) // Make sure saved_email_key exists in strings.xml
                        val sharedPref =
                            getSharedPreferences(AppConstants.PREFERENCE_FILE_KEY, MODE_PRIVATE)
                                ?: return@addOnCompleteListener

                        with(sharedPref.edit()) {
                            putString(sharedPrefKey, email) // Use the defined key
                            apply()
                        }

                        val sharedPrefKeyPass = getString(R.string.saved_password)
                        val sharedPrefPassword =
                            getSharedPreferences(AppConstants.PREFERENCE_FILE_KEY, MODE_PRIVATE)
                                ?: return@addOnCompleteListener
                        with(sharedPrefPassword.edit()) {
                            putString(sharedPrefKeyPass, password) // Use the defined key

                            apply()
                        }
                        // Display success message
                        Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()

                        // Navigate to SyncCheckActivity
                        val intent = Intent(this@LoginActivity, SyncCheckActivity::class.java)
                        startActivity(intent)
                        finish()

                    }
                    else
                    {
                        // Show error message if login fails
                        val errorMessage = "Invalid Email or Password"
                        if (task.exception != null)
                        {
                            Log.e("LoginActivity", "Login failed", task.exception)
                            // errorMessage += " (${task.exception?.localizedMessage})"
                        }

//                        if (user == null && task.isSuccessful) { // Firebase login ok, but user not in Room
//                            errorMessage = "Login data mismatch. Please contact support." // Or handle as needed
//                            Log.w("LoginActivity", "User authenticated with Firebase but not found in local Room DB: $email")
//                        }

                        Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Direct User to Register Activity On Click Listener
        btnRegister.setOnClickListener {
            // Navigate to UserEmailPasswordActivity
            startActivity(Intent(this, RegisterActivity::class.java))
            // finish() // Consider if you want to finish or allow back navigation
        }

        // Login with Biometrics Click Listener
        tvLoginBiometric.setOnClickListener {
            Log.d("LoginActivity", "Biometric login TextView clicked.")

            if (BiometricUtils.isBiometricReady(this)) {
                BiometricUtils.showBiometricPrompt(
                    activity = this,
                    listener = this,
                    cryptoObject = null,
                )
            }
            else
            {
                Snackbar.make(binding.root, "Biometric feature not available", Snackbar.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("Authenticated",true)
                startActivity(intent)
                finish()
            }

        }
    }
    override fun onBiometricAuthenticateError(error: Int, errMsg: String)
    {
        when (error)
        {
            BiometricPrompt.ERROR_USER_CANCELED -> {

                Snackbar.make(binding.root, "Biometric authentication cancelled", Snackbar.LENGTH_SHORT).show()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()

            }

            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {

                Snackbar.make(binding.root, "Biometric authentication cancelled", Snackbar.LENGTH_SHORT).show()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    override fun onBiometricAuthenticateSuccess(result: BiometricPrompt.AuthenticationResult) {
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val sharedPref = getSharedPreferences(RegisterActivity.AppConstants.PREFERENCE_FILE_KEY,MODE_PRIVATE) ?: return
        val email = sharedPref.getString(getString(R.string.saved_email),"")
        val password = sharedPref.getString(getString(R.string.saved_password),"")
        etEmail.text.clear()
        etPassword.text.clear()
        etEmail.setText(email)
        etPassword.setText(password)
        btnLogin.performClick()
    }

}