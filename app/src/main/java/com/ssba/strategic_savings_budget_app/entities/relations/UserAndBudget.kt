package com.ssba.strategic_savings_budget_app.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.entities.User

/**
 * Represents a one-to-one relationship between a user and their budget.
 *
 * This class is used to fetch a user along with the budget associated with them.
 * It uses Room's `@Relation` annotation to define the relationship where each user
 * has exactly one budget. The `userId` in the `User` table links to the `userId`
 * in the `Budget` table, establishing the connection between the two entities.
 *
 * @property user The user entity representing an individual user in the system.
 * @property budget The budget entity associated with the user.
 */

/*
 	* Code Attribution
 	* Purpose: Defining one-to-many and many-to-many relationships in Room Database
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/relationships
*/

data class UserAndBudget(

    @Embedded
    val user: User, // The user entity representing the user whose budget is being fetched.

    @Relation(
        parentColumn = "userId", // The foreign key in the Budget table that references the User table.
        entityColumn = "userId"  // The primary key in the User table that references the Budget table.
    )
    val budget: Budget // The budget associated with the user.
)
