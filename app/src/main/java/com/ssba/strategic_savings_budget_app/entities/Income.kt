package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents an income entry for a user.
 *
 * This entity records individual income transactions made by the user, including the
 * amount of income, the date of receipt, and a description of the source. Each income
 * entry is associated with a specific user via the `userId` field.
 *
 * Indices:
 * - `title`: Index is created on the `title` field to speed up searches or queries filtering income entries based on their title.
 * - `date`: Index is created on the `date` field to optimize queries that filter or sort income entries by the date received.
 * - `userId`: Index is created on the `userId` field to optimize queries that filter income entries based on the associated user.
 *
 * @property incomeId Auto-generated unique identifier for the income entry.
 * @property title A short name or label for the income entry (e.g., "Salary", "Freelance Work").
 * @property date The date when the income was received.
 * @property amount The amount of income received.
 * @property description Optional notes or explanation for the income entry (e.g., "Freelance project payment").
 * @property userId The ID of the associated user (foreign key referencing the user table).
 */
@Entity(
    tableName = "income",
    indices = [Index(value = ["title"]), Index(value = ["date"]), Index(value = ["userId"])]
)
data class Income(

    @PrimaryKey(autoGenerate = true)
    val incomeId: Int? = 0, // Unique ID for the income entry (auto-incremented)

    val title: String, // Label or short title for the income entry

    val date: Date, // Date when the income was received

    val amount: Double, // Amount of income received

    val description: String, // Additional details about the income entry

    val userId: String // Foreign key referencing the associated user
)
