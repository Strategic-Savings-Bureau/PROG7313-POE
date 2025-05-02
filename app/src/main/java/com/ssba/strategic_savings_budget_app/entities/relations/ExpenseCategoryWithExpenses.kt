package com.ssba.strategic_savings_budget_app.entities.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory

/**
 * Represents a one-to-many relationship between an expense category and its associated expenses.
 *
 * This class is used to fetch an expense category along with the list of expenses that belong to it.
 * It uses Room's `@Relation` annotation to define the relationship where each expense category
 * can have multiple associated expenses. The `categoryId` in the `ExpenseCategory` table
 * links to the `categoryId` in the `Expense` table, establishing the connection between the two entities.
 *
 * @property expenseCategory The expense category entity that categorizes the expenses.
 * @property expenses A list of expenses associated with the expense category.
 */

/*
 	* Code Attribution
 	* Purpose: Defining one-to-many and many-to-many relationships in Room Database
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/relationships
*/

data class ExpenseCategoryWithExpenses(

    @Embedded
    val expenseCategory: ExpenseCategory, // The expense category containing information about the category name, description, etc.

    @Relation(
        parentColumn = "categoryId", // The foreign key in the Expense table that references the ExpenseCategory table.
        entityColumn = "categoryId"  // The primary key in the ExpenseCategory table that is referenced in the Expense table.
    )
    val expenses: List<Expense> // The list of expenses related to the specific expense category.
)
