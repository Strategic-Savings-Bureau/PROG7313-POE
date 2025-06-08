package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.entities.Expense
import java.util.Date

/*
 	* Code Attribution
 	* Purpose: Creating DAO interfaces to define SQL queries for Room Database operations
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/accessing-data
*/

@Dao
interface ExpenseDao {

    //region Insert, Update, and Delete Operations

    /**
     * Inserts a new expense or updates it if it already exists.
     *
     * @param expense The expense entity to insert or update.
     */
    @Upsert
    suspend fun upsertExpense(expense: Expense)

    /**
     * Deletes an existing expense from the database.
     *
     * @param expense The expense entity to delete.
     */
    @Delete
    suspend fun deleteExpense(expense: Expense)

    //endregion

    //region Queries by Identifiers or Attributes

    /**
     * Retrieves an expense by its unique ID.
     *
     * @param expenseId The ID of the expense to retrieve.
     * @return The expense object, or null if not found.
     */
    @Query("SELECT * FROM expense WHERE expenseId = :expenseId")
    suspend fun getExpenseById(expenseId: Int): Expense?

    /**
     * Retrieves an expense by its title.
     *
     * @param title The title of the expense.
     * @return The expense object, or null if not found.
     */
    @Query("SELECT * FROM expense WHERE title = :title")
    suspend fun getExpenseByTitle(title: String): Expense?

    /**
     * Retrieves an expense by a specific date.
     *
     * @param date The date of the expense.
     * @return The expense object, or null if not found.
     */
    @Query("SELECT * FROM expense WHERE date = :date")
    suspend fun getExpenseByDate(date: Date): Expense?

    //endregion

    //region Queries by Date Range

    /**
     * Retrieves all expenses recorded between the specified start and end dates.
     *
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return A list of expenses that fall within the specified date range.
     */
    @Query("SELECT * FROM expense WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getExpensesWithinDateRange(startDate: Date, endDate: Date): List<Expense>

    /**
     * Calculates the total amount of expenses recorded between the specified start and end dates.
     *
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return The sum of expense amounts in the specified range.
     */
    @Query("SELECT SUM(amount) FROM expense WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseAmountWithinDateRange(startDate: Date, endDate: Date): Double

    //endregion

    //region Queries by Category

    /**
     * Retrieves all expenses that belong to a specific category.
     *
     * @param categoryId The ID of the expense category.
     * @return A list of expenses under the given category.
     */
    @Query("SELECT * FROM expense WHERE categoryId = :categoryId")
    suspend fun getExpensesByCategoryId(categoryId: Int): List<Expense>

    /**
     * Calculates the total expense amount for a specific category.
     *
     * @param categoryId The ID of the expense category.
     * @return The sum of expenses in that category.
     */
    @Query("SELECT SUM(amount) FROM expense WHERE categoryId = :categoryId")
    suspend fun getTotalExpenseAmountByCategoryId(categoryId: Int): Double

    //endregion

    // region Sync Queries

    /**
     * Retrieves all un-synced expenses from the database.
     *
     * @return A list of all expenses in the database.
     */
    @Query("SELECT * FROM expense WHERE userId = :userId AND isSynced = false")
    suspend fun getUnSyncedExpenses(userId: String): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    // endregion
}
