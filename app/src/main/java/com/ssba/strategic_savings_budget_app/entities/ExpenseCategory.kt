package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a category for expenses.
 *
 * This entity defines various expense categories that the user can assign to their individual expenses.
 * Each category contains details such as a name, description, and an icon. Additionally, the category has
 * a maximum monthly total, which can be used to help track and manage the user's expenses within that category.
 * Each expense category is linked to a specific user through the `userId` field.
 *
 * Indices:
 * - `name`: Index is created on the `name` field to optimize queries that search or filter categories based on their name.
 * - `userId`: Index is created on the `userId` field to optimize queries that filter or retrieve categories for a specific user.
 *
 * @property categoryId Unique identifier for the expense category (auto-incremented).
 * @property name Name of the expense category (e.g., "Groceries", "Utilities").
 * @property description A detailed description of the expense category (e.g., "All grocery-related expenses").
 * @property icon Icon representing the expense category (e.g., image file name or URL).
 * @property maximumMonthlyTotal The maximum allowable total for expenses in this category each month.
 * @property userId The ID of the associated user (foreign key referencing the user table).
 * @property isSynced Flag indicating if the entry has been synced to the cloud.
 * @property lastUpdatedTimestamp Timestamp of the last update, used for sync conflict resolution.
 */

/*
    * Code Attribution
    * Purpose: Setting up a Room Database and Firestore-compatible data model in a Kotlin Android app.
    * Authors: Android Developers, Firebase Documentation Team
    * Date Accessed: 10 April 2025
    * Sources:
    * - Room: https://developer.android.com/training/data-storage/room
    * - Firestore Kotlin Data Classes: https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects
 */

@Entity(
    tableName = "expense_category",
    indices = [Index(value = ["name"]), Index(value = ["userId"])]
)
data class ExpenseCategory(

    @PrimaryKey(autoGenerate = true)
    val categoryId: Int? = null, // Unique ID for the expense category (auto-incremented)

    val name: String, // Name of the expense category (indexed for fast queries)

    val description: String, // Description of the expense category

    val icon: String, // Icon representing the category (e.g., image file name or URL)

    val maximumMonthlyTotal: Double, // Maximum allowed total for expenses in this category per month

    val userId: String, // Foreign key linking the category to a specific user

    // Sync fields
    var isSynced: Boolean = false, // Whether this entry is synced to Firestore
    var lastUpdatedTimestamp: Long = System.currentTimeMillis() // Last updated timestamp
) {
    /**
     * No-argument constructor required by Firebase Firestore for automatic deserialization.
     * Firestore uses reflection and requires a public empty constructor.
     * This constructor does not interfere with Room, which uses the main constructor.
     */
    constructor() : this(
        categoryId = null,
        name = "",
        description = "",
        icon = "",
        maximumMonthlyTotal = 0.0,
        userId = "",
        isSynced = false,
        lastUpdatedTimestamp = 0L
    )

    override fun toString(): String {
        return name
    }
}