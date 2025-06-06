package com.ssba.strategic_savings_budget_app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.budget.BudgetSettingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySettingsBinding
import com.ssba.strategic_savings_budget_app.helpers.RoomToFirestoreSyncWorker
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.StreakManager
import com.ssba.strategic_savings_budget_app.settings.NotificationsSettingsActivity
import com.ssba.strategic_savings_budget_app.settings.ProfileActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Code Attribution
 *
 * Purpose:
 * This activity manages the Settings screen in the Strategic Savings Budget App, providing:
 * - User profile loading using Firebase and Room DB
 * - Image loading via Glide
 * - Theme switching using SharedPreferences and AppCompatDelegate
 * - Internet connectivity checks using NetworkCapabilities
 * - Background sync to Firestore using WorkManager and LiveData
 * - Custom alert dialog for sync status using AlertDialog with custom layout
 * - Navigation using BottomNavigationView
 * - User logout and navigation to sub-settings screens
 *
 * Authors/Technologies Used:
 * - Firebase Authentication & Firestore: Google Firebase Team
 * - Android Jetpack (WorkManager, LifecycleScope, AppCompatDelegate): Android Developers
 * - Glide (Image Loading): BumpTech
 * - Room (Local Database): Android Architecture Components
 *
 * Date Accessed: 2 May 2025
 *
 * References:
 * - Firebase Auth: https://firebase.google.com/docs/auth/android/manage-users
 * - Room Persistence: https://developer.android.com/training/data-storage/room
 * - WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
 * - Glide Library: https://github.com/bumptech/glide
 * - NetworkCapabilities (Internet Check): https://developer.android.com/reference/android/net/NetworkCapabilities
 * - AlertDialog (Custom Views): https://developer.android.com/guide/topics/ui/dialogs
 * - BottomNavigationView: https://developer.google.com/android/reference/com/google/android/material/bottomnavigation/BottomNavigationView
 */

class SettingsActivity : AppCompatActivity() {

    // region Declarations
    // View Binding
    private lateinit var binding: ActivitySettingsBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Database
    private lateinit var db: AppDatabase

    // Themes and Sync Time SharedPreferences
    private val appSharedPreferences: SharedPreferences by lazy { getSharedPreferences("APP_PREFS", MODE_PRIVATE) }
    private val editor: SharedPreferences.Editor by lazy { appSharedPreferences.edit() }

    // Constant for SharedPreferences key
    private val keyLastSyncTime = "last_sync_time"
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

        // Load the last sync time when the activity is created
        loadLastSyncTime()

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
        binding.themeSwitch.isChecked = appSharedPreferences.getBoolean("night", false)

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

        binding.btnSync.setOnClickListener {

            // Step 1: Check for internet connectivity before syncing
            if (!isInternetAvailable(this))
            {
                showAlertDialog("Sync Failed", "No internet connection. Please try again later.")
                return@setOnClickListener
            }

            // Step 2: Inflate custom dialog layout for sync status
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sync_status, null)
            val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
            val statusText = dialogView.findViewById<TextView>(R.id.statusText)

            // Step 3: Create and configure the AlertDialog
            // MaterialAlertDialogBuilder for modern dialog styling
            val alertDialog = MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false) // Disable back button dismissal
                .create()

            alertDialog.setCanceledOnTouchOutside(false) // Disable outside touch dismissal

            alertDialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            alertDialog.show()

            // Step 4: Show syncing status with progress spinner
            statusText.text = getString(R.string.tv_status_syncing)
            progressBar.isVisible = true

            // Step 5: Build and enqueue the sync work request
            val syncWorkRequest = OneTimeWorkRequestBuilder<RoomToFirestoreSyncWorker>().build()
            WorkManager.getInstance(this).enqueue(syncWorkRequest)

            // Step 6: Observe the sync job status using LiveData
            WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(syncWorkRequest.id)
                .observe(this as LifecycleOwner) { workInfo ->

                    // Only act once the work is finished
                    if (workInfo != null && workInfo.state.isFinished)
                    {
                        progressBar.isVisible = false // Hide spinner

                        if (workInfo.state == WorkInfo.State.SUCCEEDED)
                        {
                            // Sync succeeded – update the UI with the current timestamp
                            statusText.text = getString(R.string.text_sync_complete)
                            val currentTime = getCurrentTimeStamp()
                            binding.tvLastSync.text =
                                getString(R.string.tv_last_synced_update, currentTime)
                            // Save the current sync time
                            saveLastSyncTime(currentTime)
                            // Make sure the TextView is visible if a sync just occurred
                            binding.tvLastSync.isVisible = true
                        }
                        else
                        {
                            // Sync failed – inform the user
                            statusText.text = getString(R.string.text_sync_failed)
                        }

                        // Step 7: Dismiss dialog after short delay (for user to read status)
                        statusText.postDelayed({
                            alertDialog.dismiss()
                        }, 2000)
                    }
                }
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

    /**
     * Saves the last successful sync time to SharedPreferences.
     *
     * @param timestamp The formatted timestamp string to save.
     */
    private fun saveLastSyncTime(timestamp: String) {
        editor.putString(keyLastSyncTime, timestamp).apply()
    }

    /**
     * Loads the last successful sync time from SharedPreferences and displays it.
     * If no value is found, the tvLastSync TextView will be hidden.
     */
    private fun loadLastSyncTime() {
        val lastSyncTime = appSharedPreferences.getString(keyLastSyncTime, null)
        if (lastSyncTime != null) {
            binding.tvLastSync.text = getString(R.string.tv_last_synced_update, lastSyncTime)
            binding.tvLastSync.isVisible = true
        } else {
            binding.tvLastSync.isVisible = false
        }
    }

    // region Sync Helper Functions

    /**
     * Checks if the device currently has an active internet connection.
     *
     * @param context The context used to access the system connectivity service.
     * @return True if internet is available, false otherwise.
     */
    private fun isInternetAvailable(context: Context): Boolean
    {
        // Get the connectivity manager from the system services
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Get the currently active network; return false if none
        val network = connectivityManager.activeNetwork ?: return false

        // Get the capabilities of the active network; return false if unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Check if the network has internet capability
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Displays a simple alert dialog with a title, message, and an OK button.
     *
     * @param title Title of the dialog.
     * @param message Message content of the dialog.
     * @param context Context in which to show the dialog (default is `this`).
     */
    private fun showAlertDialog(title: String, message: String, context: Context = this)
    {
        // MaterialAlertDialogBuilder for modern dialog styling
        MaterialAlertDialogBuilder(context)
            .setTitle(title)         // Set the dialog title
            .setMessage(message)     // Set the message to display
            .setPositiveButton("OK", null) // Set an OK button to dismiss the dialog
            .show()                  // Show the dialog
    }

    /**
     * Gets the current timestamp formatted as "dd MMM yyyy, HH:mm".
     *
     * @return A string representation of the current date and time.
     */
    private fun getCurrentTimeStamp(): String
    {
        // Define the date format
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        // Return the current date/time formatted
        return sdf.format(Date())
    }

    // endregion
}