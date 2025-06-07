package com.ssba.strategic_savings_budget_app.models

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ssba.strategic_savings_budget_app.R
import java.util.Calendar

/*
 * Code Attribution
 * Purpose:
 *   - Track and manage a user’s activity streak based on daily app usage
 *   - Show a custom Material AlertDialog with dynamic messages and milestone tracking
 *   - Store and retrieve persistent data using SharedPreferences
 * Author: Android Developers / Material Components / Kotlin Team
 * Sources:
 *   - SharedPreferences: https://developer.android.com/reference/android/content/SharedPreferences
 *   - MaterialAlertDialogBuilder: https://m3.material.io/components/dialogs/android
 *   - LayoutInflater: https://developer.android.com/reference/android/view/LayoutInflater
 *   - Kotlin KDoc: https://kotlinlang.org/docs/kdoc.html
 */

/**
 * Manages the user's streak data using SharedPreferences.
 * A streak is defined as consecutive days of app usage/logging.
 *
 * @property context The application or activity context used for accessing resources and preferences.
 */
class StreakManager(val context: Context) {

    // SharedPreferences for storing streak-related data persistently
    private val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)

    /**
     * Updates the current streak based on the last login timestamp.
     * Increments the streak for consecutive days, resets if days are skipped,
     * and does nothing if the user already logged in today.
     */
    fun updateStreak() {
        val now = System.currentTimeMillis()
        val lastMillis = prefs.getLong("last_logged_date", 0L)
        val lastDay = truncateToDay(lastMillis)
        val today = truncateToDay(now)
        val current = prefs.getInt("current_streak", 0)

        prefs.edit(commit = true) {
            when {
                lastMillis == 0L -> {
                    // First login ever: start new streak
                    putInt("current_streak", 1)
                    Log.d("StreakManager", "First-ever log: start at 1")
                }

                today == lastDay -> {
                    // Already logged in today: do nothing
                    Log.d("StreakManager", "Same day: do nothing (streak stays as is)")
                }

                today == lastDay + 1 -> {
                    // Consecutive login: increment streak
                    putInt("current_streak", current + 1)
                    Log.d("StreakManager", "Consecutive day: bump streak")
                }

                else -> {
                    // Break in streak: reset to 1
                    putInt("current_streak", 1)
                    Log.d("StreakManager", "Gap of more than one day: reset to 1")
                }
            }

            // Always update the last login timestamp
            putLong("last_logged_date", now)
            Log.d("StreakManager", "Updated last-logged-date to “now”")
        }
    }

    /**
     * Displays a custom Material Dialog showing the current streak and motivational messages.
     *
     * @param context The context used to inflate the dialog and access resources.
     */
    @SuppressLint("SetTextI18n")
    fun showStreakDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_rewards, null)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setBackground(Color.TRANSPARENT.toDrawable()) // Keep transparent background for your custom card shape
            .create()

        // Ensures dialog transparency
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val streakManager = StreakManager(context)
        val streakCount = streakManager.getCurrentStreak()

        // Assign TextViews and update with dynamic data
        dialogView.findViewById<TextView>(R.id.tvStreak).text = streakCount.toString()

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)
        val tvNextMilestone = dialogView.findViewById<TextView>(R.id.tvNextMilestone)

        tvTitle.text = context.getString(R.string.current_streak_title)

        // Show motivation based on streak length
        tvMessage.text = when {
            streakCount == 0 -> "Start building your streak today!"
            streakCount < 3 -> "Keep going! Every day counts 💪"
            streakCount < 7 -> "You're building momentum! 🔥"
            streakCount < 14 -> "Amazing consistency! ✨"
            else -> "Legendary discipline! 🏆"
        }

        // Suggest next milestone
        val nextMilestone = when {
            streakCount < 3 -> 3
            streakCount < 7 -> 7
            streakCount < 14 -> 14
            streakCount < 30 -> 30
            else -> 60
        }

        // Display milestone days using a formatted string resource
        tvNextMilestone.text = context.getString(R.string.next_milestone_days, nextMilestone)

        // Show the streak dialog
        dialog.show()
    }

    /**
     * Retrieves the current streak value from SharedPreferences.
     *
     * @return The current streak as an integer.
     */
    private fun getCurrentStreak(): Int {
        return prefs.getInt("current_streak", 0)
    }

    /**
     * Truncates a given time in milliseconds to a day-based unit (zeroes time portion).
     * Used for daily comparison logic.
     *
     * @param millis The time in milliseconds to truncate.
     * @return The number of days since epoch.
     */
    private fun truncateToDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (24 * 60 * 60 * 1000)  // count of days since epoch
    }
}