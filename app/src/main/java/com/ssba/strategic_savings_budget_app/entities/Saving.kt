package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a saving entry that contributes to a specific saving goal.
 *
 * This entity records individual saving transactions made by the user, including the
 * amount saved, the date of the transaction, and a description. Each saving entry is
 * associated with a specific saving goal via the `savingGoalId`.
 *
 * Indices:
 * - `title`: Index is created on the `title` field to speed up searches based on the name/label of the saving entry.
 * - `date`: Index is created on the `date` field to improve queries that filter or sort saving entries by the date.
 * - `savingGoalId`: Index is created on the `savingGoalId` field to optimize queries that filter saving entries by their associated goal.
 *
 * @property savingId Auto-generated unique identifier for the saving entry.
 * @property title A short name or label for the saving entry (e.g., "January Deposit").
 * @property date The date when the saving entry was recorded.
 * @property amount The amount of money saved in this entry.
 * @property description Optional notes or explanation for the saving entry.
 * @property savingGoalId The ID of the associated saving goal (foreign key).
 * @property userId The Firebase Auth UID of the user who created this entry.
 * @property isSynced Boolean indicating if this entry is synced with the backend.
 * @property lastUpdatedTimestamp Timestamp for the last update (for sync conflict resolution).
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
    tableName = "saving",
    indices = [Index(value = ["title"]), Index(value = ["date"]), Index(value = ["savingGoalId"])]
)
data class Saving(

    @PrimaryKey(autoGenerate = true)
    val savingId: Int? = null, // Unique ID for the saving entry (auto-incremented)

    val title: String, // Label or short title for the saving entry

    val date: Date, // Date of the saving transaction

    val amount: Double, // Amount saved in this transaction

    val description: String, // Additional details about the saving entry

    val savingGoalId: Int, // Foreign key referencing the associated saving goal

    val userId: String, // Firebase Auth UID of the user

    // Sync fields
    var isSynced: Boolean = false, // Sync flag
    var lastUpdatedTimestamp: Long = System.currentTimeMillis() // Last update timestamp
) {
    /**
     * No-argument constructor required by Firebase Firestore for deserialization.
     * Firestore uses reflection to instantiate objects and requires an empty constructor.
     * This constructor should not be used directly and will not interfere with Room,
     * which uses the primary constructor for entity instantiation.
     */
    constructor() : this(
        savingId = null,
        title = "",
        date = Date(0),
        amount = 0.0,
        description = "",
        savingGoalId = 0,
        userId = "",
        isSynced = false,
        lastUpdatedTimestamp = 0L
    )
}