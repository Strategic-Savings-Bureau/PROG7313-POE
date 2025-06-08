package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a financial saving goal set by a user.
 *
 * This entity tracks the user's savings objectives, including the target amount,
 * end date, and a description of the goal. Each saving goal is linked to a user
 * through the `userId` foreign key.
 *
 * Indices:
 * - `title`: Index is created to speed up searches and queries based on the saving goal's title.
 * - `endDate`: Index is created to speed up searches and queries based on the goal's deadline.
 * - `userId`: Index is created to optimize queries that filter goals by the associated user's ID.
 *
 * @property savingGoalId Auto-generated unique identifier for the saving goal.
 * @property title A short, descriptive title for the saving goal (e.g., "Emergency Fund").
 * @property targetAmount The amount of money the user aims to save by the end date.
 * @property endDate The deadline by which the user intends to reach the savings target.
 * @property description A detailed explanation or motivation behind the saving goal.
 * @property userId The unique identifier (from Firebase Auth) of the user who owns this goal.
 * @property isSynced Indicates if the goal has been synced with the backend.
 * @property lastUpdatedTimestamp Timestamp of the last local modification to support conflict resolution.
 */

/*
    * Code Attribution
    * Purpose: Setting up a Room Database and enabling Firestore compatibility in a Kotlin Android app.
    * Authors: Android Developers & Firebase Documentation Team
    * Date Accessed: 10 April 2025
    * Sources:
    * - Room: https://developer.android.com/training/data-storage/room
    * - Firestore Kotlin Model Classes: https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects
*/

@Entity(
    tableName = "saving_goal",
    indices = [Index(value = ["title"]), Index(value = ["endDate"]), Index(value = ["userId"])]
)
data class SavingGoal(

    @PrimaryKey(autoGenerate = true)
    val savingGoalId: Int? = null, // Unique ID for the saving goal (auto-incremented)

    val title: String, // Name of the saving goal

    val targetAmount: Double, // Target amount to be saved

    val endDate: Date, // Deadline for achieving the goal

    val description: String, // Optional details or notes about the goal

    val userId: String, // Firebase UID acting as a foreign key linking to the user

    var isSynced: Boolean = false, // Sync flag for cloud sync status

    var lastUpdatedTimestamp: Long = System.currentTimeMillis() // Timestamp of last local update
) {
    /**
     * No-argument constructor required by Firebase Firestore for deserialization.
     * Firestore uses reflection to instantiate objects and requires a no-arg constructor.
     * This does not interfere with Room, which uses the primary constructor.
     */
    constructor() : this(
        savingGoalId = null,
        title = "",
        targetAmount = 0.0,
        endDate = Date(0),
        description = "",
        userId = "",
        isSynced = false,
        lastUpdatedTimestamp = 0L
    )
}