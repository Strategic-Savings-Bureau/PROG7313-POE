package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.budget.BudgetSettingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySettingsBinding
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.StreakManager
import com.ssba.strategic_savings_budget_app.settings.NotificationsSettingsActivity
import com.ssba.strategic_savings_budget_app.settings.ProfileActivity
import kotlinx.coroutines.launch

/*
 	* Code Attribution
 	* Purpose:
 	*   - Setting up Bottom Navigation View with OnItemSelectedListener for navigation between activities
 	*   - Accessing the authenticated user and checking if the user is logged in with Firebase Authentication
 	*   - Loading and displaying images using Glide library
 	* Author: Android Developers / Firebase Team / BumpTech
 	* Date Accessed: 2 May 2025
 	* Sources:
 	*   - Bottom Navigation View: https://developer.android.com/reference/com/google/android/material/bottomnavigation/BottomNavigationView
 	*   - Firebase Authentication - Check if User is Logged In: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Glide: https://github.com/bumptech/glide
*/

class SettingsActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivitySettingsBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Database
    private lateinit var db: AppDatabase

    // Themes
    private val sharedPreferences: SharedPreferences by lazy { getSharedPreferences("MODE", MODE_PRIVATE) }
    private val editor: SharedPreferences.Editor by lazy { sharedPreferences.edit() }
    // endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialise
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Database Instance
        db = AppDatabase.getInstance(this)

        // Load User Profile
        lifecycleScope.launch {
            // Get user from database
            val user = db.userDao().getUserById(auth.currentUser?.uid ?: return@launch)

            // Assign values to views
            binding.tvFullName.text = user?.fullName
            binding.tvUsername.text = user?.username
            // Null check if a URL is null or empty
            val picUrl = user?.profilePictureUrl
                .takeUnless { it.isNullOrBlank() }
            Glide.with(this@SettingsActivity)
                .load(picUrl)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(binding.ivProfilePic)
        }

        // Highlight the Menu Item
        binding.bottomNav.selectedItemId = R.id.miSettings

        // Set up On Click Listeners
        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {
        binding.btnRewards.setOnClickListener {
            StreakManager(this).showStreakDialog()
        }

        // Navigate to update user profile
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.btnCurrencyConverter.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsSettingsActivity::class.java))
        }

        // Navigate to Budget Settings
        binding.btnBudgeting.setOnClickListener {
            startActivity(Intent(this, BudgetSettingsActivity::class.java))
        }

        // Set switch state based on shared preferences
        binding.themeSwitch.isChecked = sharedPreferences.getBoolean("night", false)

        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("night", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked)
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Button to Log Out the Current User
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Set up Bottom Navigation View onClickListener
        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                // Navigate to Main (Home) Activity
                R.id.miHome -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Analysis Activity
                R.id.miAnalysis -> {
                    startActivity(Intent(this, AnalysisActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Transactions Activity
                R.id.miTransactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Savings Activity
                R.id.miSavings -> {
                    startActivity(Intent(this, SavingsActivity::class.java))
                    finish()
                    true
                }
                // Navigate to Profile Activity
                R.id.miSettings -> true

                else -> false
            }
        }
    }
}