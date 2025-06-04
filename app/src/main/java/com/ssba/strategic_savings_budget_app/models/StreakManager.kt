package com.ssba.strategic_savings_budget_app.models

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.widget.TextView
import java.util.Calendar
import androidx.core.content.edit
import com.ssba.strategic_savings_budget_app.R

class StreakManager(val context: Context) {

    private val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)

    fun updateStreak() {
        val now = System.currentTimeMillis()
        val lastMillis = prefs.getLong("last_logged_date", 0L)
        val lastDay = truncateToDay(lastMillis)
        val today = truncateToDay(now)
        val current = prefs.getInt("current_streak", 0)

        prefs.edit(commit = true) {
            when {
                lastMillis == 0L -> {
                    // first-ever log: start at 1
                    putInt("current_streak", 1)
                    Log.d("StreakManager", "First-ever log: start at 1")
                }

                today == lastDay -> {
                    // same day: do nothing (streak stays as is)
                    Log.d("StreakManager", "Same day: do nothing (streak stays as is)")
                }

                today == lastDay + 1 -> {
                    // consecutive day: bump streak
                    putInt("current_streak", current + 1)
                    Log.d("StreakManager", "Consecutive day: bump streak")
                }

                else -> {
                    // gap of more than one day: reset to 1
                    putInt("current_streak", 1)
                    Log.d("StreakManager", "Gap of more than one day: reset to 1")
                }
            }

            // always update last-logged-date to “now”
            putLong("last_logged_date", now)
            Log.d("StreakManager", "Updated last-logged-date to “now”")
        }
    }

    fun showStreakDialog() {
        val dialog = Dialog(context).apply {
            setContentView(R.layout.dialog_rewards)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val streakManager = StreakManager(context)
        val streakCount = streakManager.getCurrentStreak()

        // Update UI components
        dialog.findViewById<TextView>(R.id.tvStreak).text = streakCount.toString()

        val tvMessage = dialog.findViewById<TextView>(R.id.tvMessage)
        val tvNextMilestone = dialog.findViewById<TextView>(R.id.tvNextMilestone)

        // Dynamic messages based on streak
        tvMessage.text = when {
            streakCount == 0 -> "Start building your streak today!"
            streakCount < 3 -> "Keep going! Every day counts 💪"
            streakCount < 7 -> "You're building momentum! 🔥"
            streakCount < 14 -> "Amazing consistency! ✨"
            else -> "Legendary discipline! 🏆"
        }

        // Milestone tracker
        val nextMilestone = when {
            streakCount < 3 -> 3
            streakCount < 7 -> 7
            streakCount < 14 -> 14
            streakCount < 30 -> 30
            else -> 60
        }
        tvNextMilestone.text = "$nextMilestone days"

        // Display the dialog
        dialog.show()
    }

    fun getCurrentStreak(): Int {
        return prefs.getInt("current_streak", 0)
    }

    // helper to zero‐out the time portion
    private fun truncateToDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (24 * 60 * 60 * 1000)  // count of days since epoch
    }
}
