package com.ssba.strategic_savings_budget_app

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivitySyncCheckBinding
import com.ssba.strategic_savings_budget_app.helpers.FirestoreToRoomSyncWorker
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 	* Code Attribution
 	* Purpose:
 	*   - Verifying if a user is authenticated using Firebase Authentication
 	*   - Checking local Room database for existing user data
 	*   - Prompting the user to sync data from Firestore if local data is absent
 	*   - Using Android WorkManager to perform one-time sync jobs
 	*   - Displaying sync status in a custom AlertDialog
 	*   - Managing internet connectivity checks and redirection to Login or Main screen
 	* Author: Android Developers / Firebase Team / Android Jetpack / Google
 	* Date Accessed: 6 June 2025
 	* Sources:
 	*   - Firebase Authentication: https://firebase.google.com/docs/auth/android/manage-users#check_if_a_user_is_signed_in
 	*   - Room Database: https://developer.android.com/training/data-storage/room
 	*   - WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
 	*   - AlertDialog: https://developer.android.com/reference/androidx/appcompat/app/AlertDialog
 	*   - Internet Connectivity Check: https://developer.android.com/training/basics/network-ops/connecting
*/
class SyncCheckActivity : AppCompatActivity()
{

    // View binding object for accessing layout views
    private lateinit var binding: ActivitySyncCheckBinding

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // Reference to the Room database
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge content rendering

        binding = ActivitySyncCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Adjusts the layout to handle system bars (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Authentication and Room database
        auth = FirebaseAuth.getInstance()
        db = AppDatabase.getInstance(this)

        // Check user authentication and data sync in a coroutine scope
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid

            // If no user is logged in, redirect to LoginActivity
            if (userId == null)
            {
                startActivity(Intent(this@SyncCheckActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            // Check if user data exists locally in Room DB
            val localUser = withContext(Dispatchers.IO)
            {
                db.userDao().getUserById(userId)
            }

            // If data exists, navigate to main screen; otherwise prompt to sync
            if (localUser != null)
            {
                goToMain()
            }
            else
            {
                promptSync(userId)
            }
        }
    }

    /**
     * Displays a dialog prompting the user to sync data from Firestore if no local data is found.
     * Handles internet availability and redirect on cancellation.
     */
    private fun promptSync(userId: String)
    {
        if (!isInternetAvailable(this))
        {
            Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show()
            auth.signOut()
            startActivity(Intent(this@SyncCheckActivity, LoginActivity::class.java))
            finish()
            return
        }

        AlertDialog.Builder(this)
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
                auth.signOut()
                startActivity(Intent(this@SyncCheckActivity, LoginActivity::class.java))
                finish()
            }
            .show()
    }

    /**
     * Shows a custom syncing dialog and initiates a one-time sync job using WorkManager.
     * Handles both success and failure states, including retry logic.
     */
    private fun showSyncingDialogAndStartFirestoreSync(userId: String)
    {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sync_status, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val statusText = dialogView.findViewById<TextView>(R.id.statusText)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
        progressBar.isVisible = true
        statusText.text = getString(R.string.tv_status_syncing)

        // Create a one-time sync request to fetch Firestore data to Room
        val syncRequest = OneTimeWorkRequestBuilder<FirestoreToRoomSyncWorker>().build()
        val workManager = WorkManager.getInstance(this)

        workManager.enqueue(syncRequest)

        // Observe the status of the sync job
        workManager.getWorkInfoByIdLiveData(syncRequest.id)
            .observe(this) { workInfo ->

                if (workInfo != null && workInfo.state.isFinished)
                {
                    progressBar.isVisible = false

                    if (workInfo.state == WorkInfo.State.SUCCEEDED)
                    {
                        statusText.text = getString(R.string.text_sync_complete)
                    }
                    else
                    {
                        statusText.text = getString(R.string.text_sync_failed)
                    }

                    // After showing result, delay then dismiss dialog
                    statusText.postDelayed({
                        alertDialog.dismiss()

                        if (workInfo.state == WorkInfo.State.SUCCEEDED)
                        {
                            goToMain()
                        }
                        else
                        {
                            // Prompt retry or cancel on sync failure
                            AlertDialog.Builder(this)
                                .setTitle("Sync Failed")
                                .setMessage("Failed to sync from the cloud. Do you want to try again?")
                                .setCancelable(false)
                                .setPositiveButton("Retry") { _, _ ->
                                    showSyncingDialogAndStartFirestoreSync(userId)
                                }
                                .setNegativeButton("Cancel") { _, _ ->
                                    auth.signOut()
                                    startActivity(Intent(this@SyncCheckActivity, LoginActivity::class.java))
                                    finish()
                                }
                                .show()
                        }
                    }, 2000)
                }
            }
    }

    /**
     * Navigates the user to the MainActivity and finishes the current one.
     */
    private fun goToMain()
    {
        startActivity(Intent(this@SyncCheckActivity, MainActivity::class.java))
        finish()
    }

    /**
     * Checks if the device currently has an active internet connection.
     *
     * @param context The context used to access the system connectivity service.
     * @return True if internet is available, false otherwise.
     */
    private fun isInternetAvailable(context: Context): Boolean
    {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}