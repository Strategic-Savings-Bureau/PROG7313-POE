package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.Budget

/*
 	* Code Attribution
 	* Purpose: Creating DAO interfaces to define SQL queries for Room Database operations
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/accessing-data
*/

@Dao
interface BudgetDao {

    //region Insert and Delete Operations

    /**
     * Inserts a new budget or updates it if it already exists.
     *
     * @param budget The [Budget] entity to be inserted or updated.
     */
    @Upsert
    suspend fun upsertBudget(budget: Budget)

    /**
     * Deletes the specified budget from the database.
     *
     * @param budget The [Budget] entity to delete.
     */
    @Delete
    suspend fun deleteBudget(budget: Budget)

    //endregion

    //region Query Operations

    /**
     * Retrieves the budget associated with a specific user.
     * Assumes one-to-one relationship (one budget per user).
     *
     * @param userId The ID of the user.
     * @return The [Budget] associated with the user or null if not found.
     */
    @Transaction
    @Query("SELECT * FROM budget WHERE userId = :userId")
    suspend fun getBudgetByUserId(userId: String): Budget?

    /**
     * Retrieves a budget by its unique ID.
     *
     * @param budgetId The unique ID of the budget.
     * @return The [Budget] with the given ID or null if not found.
     */
    @Query("SELECT * FROM budget WHERE budgetId = :budgetId")
    suspend fun getBudgetById(budgetId: Int): Budget?

    //endregion

    // region Sync Operations

    /**
     * Retrieves all un-synced budgets from the database.
     *
     * @return A list of all [Budget] entities in the database.
     */
    @Query("SELECT * FROM budget WHERE userId = :userId AND isSynced = false")
    suspend fun getUnSyncedBudgets(userId: String): List<Budget>

    //endregion
}
