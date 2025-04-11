package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.Saving
import java.util.Date

@Dao
interface SavingDao {

    //region Insert, Update, Delete Operations

    /**
     * Inserts a new saving record or updates it if it already exists.
     *
     * @param saving The saving entity to insert or update.
     */
    @Upsert
    suspend fun upsertSaving(saving: Saving)

    /**
     * Deletes a saving record from the database.
     *
     * @param saving The saving entity to be deleted.
     */
    @Delete
    suspend fun deleteSaving(saving: Saving)

    //endregion

    //region Queries by Attributes

    /**
     * Retrieves a saving by its unique ID.
     *
     * @param savingId The ID of the saving.
     * @return The saving record, or null if not found.
     */
    @Query("SELECT * FROM saving WHERE savingId = :savingId")
    suspend fun getSavingById(savingId: Int): Saving?

    /**
     * Retrieves a saving by its title.
     *
     * @param title The title of the saving.
     * @return The saving record, or null if not found.
     */
    @Query("SELECT * FROM saving WHERE title = :title")
    suspend fun getSavingByTitle(title: String): Saving?

    /**
     * Retrieves a saving by its date.
     *
     * @param date The date the saving was made.
     * @return The saving record, or null if not found.
     */
    @Query("SELECT * FROM saving WHERE date = :date")
    suspend fun getSavingByDate(date: Date): Saving?

    //endregion

    //region Queries by Saving Goal

    /**
     * Retrieves all savings associated with a specific saving goal.
     *
     * @param savingGoalId The ID of the saving goal.
     * @return A list of savings linked to the saving goal.
     */
    @Query("SELECT * FROM saving WHERE savingGoalId = :savingGoalId")
    suspend fun getSavingsByGoalId(savingGoalId: Int): List<Saving>

    /**
     * Calculates the total amount saved under a specific saving goal.
     *
     * @param savingGoalId The ID of the saving goal.
     * @return The sum of all savings amounts linked to the goal.
     */
    @Query("SELECT SUM(amount) FROM saving WHERE savingGoalId = :savingGoalId")
    suspend fun getTotalSavedForGoal(savingGoalId: Int): Double

    //endregion

    //region Queries by Date Range

    /**
     * Calculates the total amount saved within a given date range.
     *
     * @param startDate The start date of the range.
     * @param endDate The end date of the range.
     * @return The sum of savings between the specified dates.
     */
    @Query("SELECT SUM(amount) FROM saving WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalSavedInDateRange(startDate: Date, endDate: Date): Double

    //endregion
}
