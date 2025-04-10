package com.ssba.strategic_savings_budget_app.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ssba.strategic_savings_budget_app.entities.SavingGoal
import com.ssba.strategic_savings_budget_app.entities.User

/**
 * Represents a one-to-many relationship between a user and their saving goals.
 *
 * This class is used to fetch a user along with all the saving goals associated with them.
 * It uses Room's `@Relation` annotation to create the relationship where each user can have
 * multiple saving goals. The `userId` in the `User` table links to the `userId` in the
 * `SavingGoal` table, representing the association between the two entities.
 *
 * @property user The user entity, which can have multiple saving goals.
 * @property savingGoals The list of saving goals associated with the user.
 */
data class UserWithSavingGoals(

    @Embedded
    val user: User, // The user entity, representing the individual user in the system.

    @Relation(
        parentColumn = "userId", // Column in the User table (foreign key).
        entityColumn = "userId" // Column in the SavingGoal table (foreign key).
    )
    val savingGoals: List<SavingGoal> // The list of saving goals associated with the user.
)
