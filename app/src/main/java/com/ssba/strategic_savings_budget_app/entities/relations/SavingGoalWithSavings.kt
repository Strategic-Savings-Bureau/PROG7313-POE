package com.ssba.strategic_savings_budget_app.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ssba.strategic_savings_budget_app.entities.Saving
import com.ssba.strategic_savings_budget_app.entities.SavingGoal

/**
 * Represents a one-to-many relationship between a saving goal and its associated savings.
 *
 * This class is used to fetch a saving goal along with the list of savings associated with it.
 * It uses Room's `@Relation` annotation to define the relationship where each saving goal
 * can have multiple associated savings. The `savingGoalId` in the `SavingGoal` table
 * links to the `savingGoalId` in the `Saving` table, establishing the connection between the two entities.
 *
 * @property savingGoal The saving goal entity representing the target the user is aiming to achieve.
 * @property savings A list of savings that are associated with the saving goal.
 */
data class SavingGoalWithSavings(

    @Embedded
    val savingGoal: SavingGoal, // The saving goal entity containing information about the target.

    @Relation(
        parentColumn = "savingGoalId", // The foreign key in the Saving table that references the SavingGoal table.
        entityColumn = "savingGoalId"  // The primary key in the SavingGoal table that is referenced in the Saving table.
    )
    val savings: List<Saving> // The list of savings related to the specific saving goal.
)
