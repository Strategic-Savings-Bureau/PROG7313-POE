package com.ssba.strategic_savings_budget_app.models

import android.content.Context
import android.util.Log
import java.util.Calendar
import androidx.core.content.edit

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
