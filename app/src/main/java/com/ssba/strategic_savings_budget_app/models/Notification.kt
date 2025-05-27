package com.ssba.strategic_savings_budget_app.models

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ssba.strategic_savings_budget_app.R

/*
 * Code Attribution:
 * Schedule Local Notifications Android Studio Kotlin Tutorial
 * Code With Cal
 * 11 November 2021
 * https://www.youtube.com/watch?v=_Z2S63O-1HE
 */

class Notification : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        showNotification(context)
    }

    private fun showNotification(context: Context) {
        // Create channel (required for Android 8+)
        val channel = NotificationChannel(
            "reminder_channel",
            "Expense Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Expense logging reminders" }

        val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        // Build notification
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.mipmap.ic_app_ss_round)
            .setContentTitle("Expense Reminder")
            .setContentText("Don't forget to log your expenses!")
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}