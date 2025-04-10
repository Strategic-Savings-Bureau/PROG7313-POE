package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a financial saving goal set by a user.
 *
 * This entity tracks the user's savings objectives, including the target amount,
 * end date, and a description of the goal. Each saving goal is linked to a user
 * through the `userId` foreign key.
 *
 * @property savingGoalId Auto-generated unique identifier for the saving goal.
 * @property title A short, descriptive title for the saving goal (e.g., "Emergency Fund").
 * @property targetAmount The amount of money the user aims to save by the end date.
 * @property endDate The deadline by which the user intends to reach the savings target.
 * @property description A detailed explanation or motivation behind the saving goal.
 * @property userId The unique identifier (from Firebase Auth) of the user who owns this goal.
 */
@Entity(tableName = "saving_goal")
data class SavingGoal(

    @PrimaryKey(autoGenerate = true)
    val savingGoalId: Int? = 0, // Unique ID for the saving goal (auto-incremented)

    val title: String, // Name of the saving goal

    val targetAmount: Double, // Target amount to be saved

    val endDate: Date, // Deadline for achieving the goal

    val description: String, // Optional details or notes about the goal

    val userId: String // Firebase UID acting as a foreign key linking to the user
)
