package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.entities.relations.ExpenseCategoryWithExpenses

/*
 	* Code Attribution
 	* Purpose: Creating DAO interfaces to define SQL queries for Room Database operations
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/accessing-data
*/

@Dao
interface ExpenseCategoryDao {

    //region Insert, Update, and Delete Operations

    /**
     * Inserts or updates an expense category.
     *
     * @param expenseCategory The expense category to insert or update.
     */
    @Upsert
    suspend fun upsertExpenseCategory(expenseCategory: ExpenseCategory)

    /**
     * Deletes the specified expense category.
     *
     * @param expenseCategory The expense category to delete.
     */
    @Delete
    suspend fun deleteExpenseCategory(expenseCategory: ExpenseCategory)

    //endregion

    //region Queries by Identifier or Name

    /**
     * Retrieves a specific expense category by its unique ID.
     *
     * @param categoryId The ID of the category.
     * @return The matching expense category or null.
     */
    @Query("SELECT * FROM expense_category WHERE categoryId = :categoryId")
    suspend fun getExpenseCategoryById(categoryId: Int): ExpenseCategory?

    /**
     * Retrieves a specific expense category by its name.
     *
     * @param name The name of the category.
     * @return The matching expense category or null.
     */
    @Query("SELECT * FROM expense_category WHERE name = :name")
    suspend fun getExpenseCategoryByName(name: String): ExpenseCategory?

    //endregion

    //region Queries by User

    /**
     * Retrieves all expense categories associated with a given user.
     *
     * @param userId The ID of the user.
     * @return A list of the user's expense categories.
     */
    @Query("SELECT * FROM expense_category WHERE userId = :userId")
    suspend fun getExpenseCategoriesByUserId(userId: String): List<ExpenseCategory>

    //endregion

    //region General Queries

    /**
     * Retrieves all expense categories in the database.
     *
     * @return A list of all available expense categories.
     */
    @Query("SELECT * FROM expense_category")
    suspend fun getAllExpenseCategories(): List<ExpenseCategory>

    /**
     * Calculates the total of all maximum monthly budget limits across categories.
     *
     * @return The sum of all `maximumMonthlyTotal` values.
     */
    @Query("SELECT SUM(maximumMonthlyTotal) FROM expense_category")
    suspend fun getTotalMaximumMonthlyBudgetAcrossCategories(): Double

    //endregion

    //region One-to-Many Relationship Queries

    /**
     * Retrieves a list of expenses linked to a category using its name.
     *
     * @param categoryName The name of the category.
     * @return A list of `ExpenseCategoryWithExpenses` entities.
     */
    @Transaction
    @Query("SELECT * FROM expense_category WHERE name = :categoryName")
    suspend fun getExpensesByCategoryName(categoryName: String): List<ExpenseCategoryWithExpenses>

    /**
     * Retrieves a list of expenses linked to a category using its ID.
     *
     * @param categoryId The ID of the category.
     * @return A list of `ExpenseCategoryWithExpenses` entities.
     */
    @Transaction
    @Query("SELECT * FROM expense_category WHERE categoryId = :categoryId")
    suspend fun getExpensesByCategoryId(categoryId: Int): List<ExpenseCategoryWithExpenses>

    //endregion

    // region Sync Queries

    @Query("SELECT * FROM expense_category WHERE userId = :userId AND isSynced = false")
    suspend fun getUnSyncedExpenseCategories(userId: String): List<ExpenseCategory>

    // endregion
}
