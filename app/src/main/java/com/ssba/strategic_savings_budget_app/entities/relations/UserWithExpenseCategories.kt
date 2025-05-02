package com.ssba.strategic_savings_budget_app.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.entities.User

/**
 * Represents a one-to-many relationship between a user and their expense categories.
 *
 * This class is used to fetch a user along with all the expense categories associated with them.
 * It uses Room's `@Relation` annotation to define the relationship where each user can have
 * multiple expense categories. The `userId` in the `User` table links to the `userId` in the
 * `ExpenseCategory` table, establishing the connection between the two entities.
 *
 * @property user The user entity, representing an individual user in the system.
 * @property expenseCategories The list of expense categories associated with the user.
 */

/*
 	* Code Attribution
 	* Purpose: Defining one-to-many and many-to-many relationships in Room Database
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/relationships
*/


data class UserWithExpenseCategories(

    @Embedded
    val user: User, // The user entity, representing the user who can have multiple expense categories.

    @Relation(
        parentColumn = "userId", // The foreign key in the ExpenseCategory table linking to the User table.
        entityColumn = "userId" // The foreign key in the User table linking to the ExpenseCategory table.
    )
    val expenseCategories: List<ExpenseCategory> // The list of expense categories associated with the user.
)
