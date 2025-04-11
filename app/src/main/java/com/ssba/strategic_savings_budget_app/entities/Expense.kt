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
 */
@Entity(
    tableName = "expense",
    indices = [Index(value = ["title"]), Index(value = ["date"]), Index(value = ["categoryId"])]
)
data class Expense(

    @PrimaryKey(autoGenerate = true)
    val expenseId: Int? = 0, // Unique ID for the expense (auto-incremented)

    val title: String, // Title or name of the expense (indexed for faster search)

    val date: Date, // Date the expense was incurred (indexed for filtering by date)

    val amount: Double, // Monetary amount of the expense

    val description: String, // Description of the expense (e.g., "Lunch at a restaurant")

    val receiptPictureUrl: String, // URL to the receipt image stored in Supabase Storage (optional)

    val categoryId: Int // Foreign key linking this expense to a specific expense category (indexed for filtering by category)
)
