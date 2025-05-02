package com.ssba.strategic_savings_budget_app.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.User

/**
 * Represents a one-to-many relationship between a user and their incomes.
 *
 * This class is used to fetch a user along with all the incomes associated with them.
 * It uses Room's `@Relation` annotation to create the relationship where each user can have
 * multiple incomes. The `userId` in the `User` table links to the `userId` in the `Income`
 * table, representing the association between the two entities.
 *
 * @property user The user entity, which can have multiple income records.
 * @property incomes The list of income records associated with the user.
 */

/*
 	* Code Attribution
 	* Purpose: Defining one-to-many and many-to-many relationships in Room Database
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/relationships
*/


data class UserWithIncomes(

    @Embedded
    val user: User, // The user entity, representing the individual user in the system.

    @Relation(
        parentColumn = "userId", // Column in the User table (foreign key).
        entityColumn = "userId" // Column in the Income table (foreign key).
    )
    val incomes: List<Income> // The list of incomes associated with the user.
)
