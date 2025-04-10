package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Represents a saving entry that contributes to a specific saving goal.
 *
 * This entity records individual saving transactions made by the user,
 * including the amount saved, the date of the transaction, and a description.
 * Each saving entry is associated with a specific saving goal via `savingGoalId`.
 *
 * @property savingId Auto-generated unique identifier for the saving entry.
 * @property title A short name or label for the saving entry (e.g., "January Deposit").
 * @property date The date when the saving entry was recorded.
 * @property amount The amount of money saved in this entry.
 * @property description Optional notes or explanation for the saving entry.
 * @property savingGoalId The ID of the associated saving goal (foreign key).
 */
@Entity(tableName = "saving")
data class Saving(

    @PrimaryKey(autoGenerate = true)
    val savingId: Int? = 0, // Unique ID for the saving entry (auto-incremented)

    val title: String, // Label or short title for the saving entry

    val date: Date, // Date of the saving transaction

    val amount: Double, // Amount saved in this transaction

    val description: String, // Additional details about the saving entry

    val savingGoalId: Int // Foreign key referencing the associated saving goal
)
