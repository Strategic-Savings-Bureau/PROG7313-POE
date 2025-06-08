package com.ssba.strategic_savings_budget_app.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivityNotificationsSettingsBinding
import com.ssba.strategic_savings_budget_app.models.Notification
import android.provider.Settings
import android.os.SystemClock
import androidx.activity.enableEdgeToEdge

class NotificationsSettingsActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityNotificationsSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotificationsSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simple spinner setup
        val items = arrayOf("Daily", "Weekly")
        binding.spnChoice.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            items
        )

        // Back button
        binding.btnBackButton.setOnClickListener { finish() }

        // Add this permission launcher
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                scheduleReminder()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Modify your save button click listener
        binding.btnSave.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Request permission first
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Directly schedule for older Android versions
                scheduleReminder()
            }
        }
    }

    private fun scheduleReminder() {
        // Compute your interval: daily or weekly
        val interval = when (binding.spnChoice.selectedItem.toString()) {
            "Daily" -> AlarmManager.INTERVAL_DAY
            else -> AlarmManager.INTERVAL_DAY * 7L
        }

        // Build your PendingIntent as before
        val intent = Intent(this, Notification::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Get AlarmManager and schedule an inexact repeating alarm
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // Schedule the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && !alarmManager.canScheduleExactAlarms()) {
            // prompt user to allow exact alarms
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval,
                pendingIntent
            )
        }

        Toast.makeText(this, "Reminder Set!", Toast.LENGTH_SHORT).show()
    }
}
