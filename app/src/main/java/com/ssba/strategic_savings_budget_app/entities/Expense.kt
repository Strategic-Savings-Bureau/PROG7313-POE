package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents an individual expense made by the user.
 *
 * This entity defines the structure of an expense, including its title, date, amount, description,
 * receipt picture URL, and the associated expense category. Each expense is linked to a category
 * through the `categoryId` foreign key, and it also holds information on how much was spent and
 * when the expense occurred. The `receiptPictureUrl` is a URL that links to the stored receipt image.
 *
 * Indices:
 * - `title`: Index is created on the `title` field to optimize queries that search or filter expenses based on their title.
 * - `date`: Index is created on the `date` field to optimize queries filtering expenses by date or date range.
 * - `categoryId`: Index is created on the `categoryId` field to optimize queries filtering expenses by their associated category.
 *
 * @property expenseId Unique identifier for the expense (auto-incremented).
 * @property title The title or name of the expense (e.g., "Lunch", "Electricity Bill").
 * @property date The date when the expense was incurred.
 * @property amount The monetary amount spent for this particular expense.
 * @property description A detailed description of the expense (e.g., "Lunch at a restaurant").
 * @property receiptPictureUrl URL to the receipt image stored in Supabase Storage (optional).
 * @property categoryId The ID of the category to which this expense belongs (foreign key referencing the expense_category table).
 * @property userId The ID of the user who owns this expense.
 * @property isSynced Flag indicating whether the data has been synced to Firestore.
 * @property lastUpdatedTimestamp Timestamp used for conflict resolution during syncing.
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
    tableName = "expense",
    indices = [Index(value = ["title"]), Index(value = ["date"]), Index(value = ["categoryId"])]
)
data class Expense(

    @PrimaryKey(autoGenerate = true)
    val expenseId: Int? = null, // Unique ID for the expense (auto-incremented)

    val title: String, // Title or name of the expense (indexed for faster search)

    val date: Date, // Date the expense was incurred (indexed for filtering by date)

    val amount: Double, // Monetary amount of the expense

    val description: String, // Description of the expense (e.g., "Lunch at a restaurant")

    val receiptPictureUrl: String, // URL to the receipt image stored in Supabase Storage (optional)

    val categoryId: Int, // Foreign key linking this expense to a specific expense category (indexed for filtering by category)

    val userId: String, // Foreign key linking this expense to a specific user

    // Sync fields
    var isSynced: Boolean = false, // Whether this record has been synced to Firestore
    var lastUpdatedTimestamp: Long = System.currentTimeMillis() // Timestamp of last modification
) {
    /**
     * No-argument constructor required by Firebase Firestore for automatic deserialization.
     * Firestore requires this to instantiate the object using reflection.
     * Room continues to use the primary constructor, so this does not interfere with Room.
     */
    constructor() : this(
        expenseId = null,
        title = "",
        date = Date(0),
        amount = 0.0,
        description = "",
        receiptPictureUrl = "",
        categoryId = 0,
        userId = "",
        isSynced = false,
        lastUpdatedTimestamp = 0L
    )
}