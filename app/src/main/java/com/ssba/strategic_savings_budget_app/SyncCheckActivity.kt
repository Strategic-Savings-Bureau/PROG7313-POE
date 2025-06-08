package com.ssba.strategic_savings_budget_app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.concurrent.futures.await
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Import for Material Design dialogs
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySyncCheckBinding
import com.ssba.strategic_savings_budget_app.helpers.FirestoreToRoomSyncWorker
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/*
 * Code Attribution
 *
 * Purpose:
 * - Authenticate user session status with Firebase Authentication.
 * - Query local Room database for existing user data before syncing.
 * - Prompt user to initiate data sync from Firestore if local data is missing.
 * - Employ Android WorkManager for reliable one-time background sync tasks.
 * - Use Kotlin Coroutines for asynchronous database and sync operations.
 * - Present sync progress and status updates in a custom AlertDialog.
 * - Implement network connectivity checks to ensure internet availability before sync.
 * - Handle sync failure scenarios with retry/cancel dialogs and database cleanup.
 *
 * Authors/Technologies Used:
 * - Firebase Authentication: Google Firebase Team
 * - Android Jetpack WorkManager & Room Persistence Library: Android Developers
 * - Kotlin Coroutines: Kotlin Team
 * - UI Components & AlertDialog: AndroidX / Android Developers
 *
 * Date Accessed: 6 June 2025
 *
 * References:
 * - Firebase Authentication (Android): https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 * - Android Room Persistence Library: https://developer.android.com/training/data-storage/room
 * - WorkManager (One-Time Work Request): https://developer.android.com/topic/libraries/architecture/workmanager
 * - Kotlin Coroutines integration with WorkManager: https://developer.android.com/kotlin/coroutines
 * - AlertDialog customization: https://developer.android.com/reference/androidx/appcompat/app/AlertDialog
 * - Connectivity checks with NetworkCapabilities: https://developer.android.com/training/basics/network-ops/connecting
 */

class SyncCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncCheckBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: AppDatabase

    // Reference to the currently enqueued sync WorkManager job ID
    private var currentSyncWorkId: UUID? = null

    // SharedPreferences for app-wide preferences, including last sync time
    private val appSharedPreferences: SharedPreferences by lazy { getSharedPreferences("APP_PREFS", MODE_PRIVATE) }
    private val editor: SharedPreferences.Editor by lazy { appSharedPreferences.edit() }

    // Constant for SharedPreferences key (must match the one in SettingsActivity)
    private val keyLastSyncTime = "last_sync_time"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // Enable full edge-to-edge display for modern UI

        binding = ActivitySyncCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply system window insets as padding for proper layout insets handling
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = AppDatabase.getInstance(this)

        lifecycleScope.launch {
            // Check if user is currently authenticated
            val userId = auth.currentUser?.uid
            if (userId == null) {
                redirectToLogin()
                return@launch
            }

            // Query local Room DB on IO thread to find existing user data
            val localUser = withContext(Dispatchers.IO) {
                db.userDao().getUserById(userId)
            }

            if (localUser != null) {
                // Local user data found - proceed to main app screen
                goToMain()
            } else {
                // No local data - prompt sync from Firestore
                promptSync(userId)
            }
        }
    }

    /**
     * Prompts the user to sync data from Firestore if local data is absent.
     * If no internet is available, informs user and signs out.
     */
    private fun promptSync(userId: String) {
        if (!isInternetAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show()
            signOutAndRedirect()
            return
        }

        // Use MaterialAlertDialogBuilder for modern dialog styling
        MaterialAlertDialogBuilder(this)
            .setTitle("Sync Required")
            .setMessage("No local data found. Do you want to sync from the cloud?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                showSyncingDialogAndStartFirestoreSync(userId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Sync canceled. Cannot proceed.", Toast.LENGTH_LONG).show()
                signOutAndRedirect()
            }
            .show()
    }

    /**
     * Shows a syncing progress dialog and enqueues a Firestore to Room sync task using WorkManager.
     * Observes work status and updates UI accordingly.
     * Handles success, failure, and timeout scenarios.
     */
    private fun showSyncingDialogAndStartFirestoreSync(userId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sync_status, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val statusText = dialogView.findViewById<TextView>(R.id.statusText)

        // Use MaterialAlertDialogBuilder for modern dialog styling
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
        progressBar.isVisible = true
        statusText.text = getString(R.string.tv_status_syncing)

        // Cancel any ongoing sync task before starting a new one
        currentSyncWorkId?.let { WorkManager.getInstance(this).cancelWorkById(it) }

        // Build and enqueue new one-time WorkManager sync request
        val syncRequest = OneTimeWorkRequestBuilder<FirestoreToRoomSyncWorker>().build()
        currentSyncWorkId = syncRequest.id
        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(syncRequest)

        // Observe work status live and update UI accordingly
        workManager.getWorkInfoByIdLiveData(syncRequest.id).observe(this) { workInfo ->

            if (workInfo != null && workInfo.state.isFinished) {
                progressBar.isVisible = false

                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    statusText.text = getString(R.string.text_sync_complete)
                    // Save the current sync time to SharedPreferences
                    saveLastSyncTime(getCurrentTimeStamp())
                } else {
                    statusText.text = getString(R.string.text_sync_failed)
                }

                statusText.postDelayed({
                    alertDialog.dismiss()

                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        goToMain()
                    } else {
                        // On failure, clear local DB and prompt retry or cancel
                        clearDatabaseTables()
                        showRetryCancelDialog(userId)
                    }
                }, 2000)
            }
        }

        // Timeout logic to cancel sync after 90 seconds to avoid indefinite waits
        lifecycleScope.launch {
            launch {
                delay(90000)
                val workInfo = workManager.getWorkInfoById(syncRequest.id).await()
                if (workInfo != null && !workInfo.state.isFinished) {
                    workManager.cancelWorkById(syncRequest.id)
                    alertDialog.dismiss()
                    clearDatabaseTables()
                    showRetryCancelDialog(userId, timedOut = true)
                }
            }
        }
    }

    /**
     * Shows a dialog to retry or cancel after a failed or timed-out sync attempt.
     */
    private fun showRetryCancelDialog(userId: String, timedOut: Boolean = false) {
        val message = if (timedOut) {
            "Sync timed out. Do you want to retry?"
        } else {
            "Failed to sync from the cloud. Do you want to try again?"
        }

        // Use MaterialAlertDialogBuilder for modern dialog styling
        MaterialAlertDialogBuilder(this)
            .setTitle("Sync Failed")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ ->
                showSyncingDialogAndStartFirestoreSync(userId)
            }
            .setNegativeButton("Cancel") { _, _ ->
                signOutAndRedirect()
            }
            .show()
    }

    /**
     * Clears all data from all tables in the Room database on a background thread.
     * Ensures database is clean before a retry sync attempt.
     */
    private fun clearDatabaseTables() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.clearAllTables()
            } catch (ex: Exception) {
                // Consider logging the error to a crash reporting system
                ex.printStackTrace()
            }
        }
    }

    /**
     * Signs the user out of Firebase Authentication and redirects to the Login screen.
     */
    private fun signOutAndRedirect() {
        auth.signOut()
        redirectToLogin()
    }

    /**
     * Starts LoginActivity and finishes this activity.
     */
    private fun redirectToLogin() {
        startActivity(Intent(this@SyncCheckActivity, LoginActivity::class.java))
        finish()
    }

    /**
     * Starts MainActivity and finishes this activity.
     */
    private fun goToMain() {
        startActivity(Intent(this@SyncCheckActivity, MainActivity::class.java))
        finish()
    }

    /**
     * Checks for active internet connectivity using NetworkCapabilities.
     * Returns true if internet is available.
     */
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
     * Gets the current timestamp formatted as "dd MMM, HH:mm".
     * This method is duplicated from SettingsActivity for convenience in SyncCheckActivity.
     *
     * @return A string representation of the current date and time.
     */
    private fun getCurrentTimeStamp(): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}