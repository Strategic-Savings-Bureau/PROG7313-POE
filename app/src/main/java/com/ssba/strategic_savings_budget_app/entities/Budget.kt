package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents the user's budget, including their monthly income and expenses.
 *
 * This entity defines the user's financial budget, focusing on the minimum monthly income required
 * and the maximum allowable monthly expenses. The `userId` links the budget to a specific user.
 * This helps track the financial goals for each user and their spending limits.
 *
 * Indices:
 * - `userId`: Index is created on the `userId` field to optimize queries related to retrieving a budget for a specific user.
 *
 * @property budgetId Unique identifier for the budget (auto-incremented).
 * @property minimumMonthlyIncome The minimum income required for the user to meet their budgetary needs each month.
 * @property maximumMonthlyExpenses The maximum allowable expenses for the user in a given month.
 * @property userId The ID of the user who owns this budget (foreign key referencing the user table).
 * @property isSynced Flag to track if this budget has been synced with Firestore.
 * @property lastUpdatedTimestamp Timestamp of the last update to support conflict resolution in syncing.
 */

/*
    * Code Attribution
    * Purpose: Setting up a Room Database and Firestore-compatible data model in an Android app.
    * Authors: Android Developers, Firebase Documentation Team
    * Date Accessed: 10 April 2025
    * Sources:
    * - Room: https://developer.android.com/training/data-storage/room
    * - Firestore Kotlin Data Classes: https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects
 */

@Entity(
    tableName = "budget",
    indices = [Index(value = ["userId"])]
)
data class Budget(

    @PrimaryKey(autoGenerate = true)
    val budgetId: Int? = null, // Unique identifier for the budget (auto-incremented)

    val minimumMonthlyIncome: Double, // Minimum required income for the user each month

    val maximumMonthlyExpenses: Double, // Maximum allowable expenses for the user each month

    val userId: String, // Foreign key that links the budget to a specific user (references user table)

    // Sync fields
    var isSynced: Boolean = false, // Indicates whether the record has been synced to Firestore
    var lastUpdatedTimestamp: Long = System.currentTimeMillis() // Timestamp of the last update
) {
    /**
     * No-argument constructor required by Firebase Firestore for automatic deserialization.
     * This is only used by Firestore and does not affect Room, which continues to use the primary constructor.
     */
    constructor() : this(
        budgetId = null,
        minimumMonthlyIncome = 0.0,
        maximumMonthlyExpenses = 0.0,
        userId = "",
        isSynced = false,
        lastUpdatedTimestamp = 0L
    )
}