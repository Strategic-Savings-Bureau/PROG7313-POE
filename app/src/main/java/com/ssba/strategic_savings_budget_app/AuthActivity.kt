package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.ssba.strategic_savings_budget_app.databinding.ActivityAuthBinding
import com.ssba.strategic_savings_budget_app.helpers.BiometricUtils
import com.ssba.strategic_savings_budget_app.interfaces.BiometricAuthListener

class AuthActivity : AppCompatActivity(), BiometricAuthListener {
    private lateinit var binding: ActivityAuthBinding

    private lateinit var biometricPromptBtn: Button

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        biometricPromptBtn = binding.btnBiometrics

        biometricPromptBtn.setOnClickListener {

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
            }
        }
    }

    override fun onBiometricAuthenticateError(error: Int, errMsg: String)
    {
        when (error)
        {
            BiometricPrompt.ERROR_USER_CANCELED -> {

                Snackbar.make(binding.root, "Biometric authentication cancelled", Snackbar.LENGTH_SHORT).show()

                startActivity(Intent(this,AuthActivity::class.java))
                finish()

            }

            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {

                Snackbar.make(binding.root, "Biometric authentication cancelled", Snackbar.LENGTH_SHORT).show()

                startActivity(Intent(this,AuthActivity::class.java))
                finish()
            }
        }
    }

    override fun onBiometricAuthenticateSuccess(result: BiometricPrompt.AuthenticationResult)
    {
        Snackbar.make(binding.root, "Biometric authentication successful", Snackbar.LENGTH_LONG).show()

        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}