package com.ssba.strategic_savings_budget_app.landing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivityLoginBinding

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivityLoginBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth
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
        // endregion

        // Delay the redirection by 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is already logged in
            val currentUser = auth.currentUser

            if (currentUser != null) {
                // User is already logged in, navigate to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()

            } else {
                // User is not logged in, navigate to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 1000)

    }
}