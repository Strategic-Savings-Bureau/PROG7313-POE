package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.Budget

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
}
