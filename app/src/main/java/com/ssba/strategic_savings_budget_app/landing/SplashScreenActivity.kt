package com.ssba.strategic_savings_budget_app.landing

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth

import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivitySplashScreenBinding

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    // region Declarations
    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var auth: FirebaseAuth
    private var keepSplashOnScreen = true
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialisation
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep splash screen on-screen until auth state is checked
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        // Check the user preferences for the theme
        val prefs = getSharedPreferences("MODE", MODE_PRIVATE)
        val isNight = prefs.getBoolean("night", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isNight)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        // Wait for both persistence load AND minimal delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 300)
    }

    // Method to check authentication state
    private fun checkAuthState() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val destination =
            if(currentUser!=null){
                MainActivity::class.java
            }else {
                LoginActivity::class.java
            }

        keepSplashOnScreen = false
        startActivity(Intent(this, destination))
        finish()
    }
}