package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.Income
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
interface IncomeDao {

    //region Insert, Update, Delete Operations

    /**
     * Inserts a new income record or updates it if it already exists.
     *
     * @param income The income entity to insert or update.
     */
    @Upsert
    suspend fun upsertIncome(income: Income)

    /**
     * Deletes an income record from the database.
     *
     * @param income The income entity to delete.
     */
    @Delete
    suspend fun deleteIncome(income: Income)

    //endregion

    //region Queries by Attributes

    /**
     * Retrieves an income entry by its unique ID.
     *
     * @param incomeId The ID of the income.
     * @return The income record, or null if not found.
     */
    @Query("SELECT * FROM income WHERE incomeId = :incomeId")
    suspend fun getIncomeById(incomeId: Int): Income?

    /**
     * Retrieves an income entry by its title.
     *
     * @param title The title of the income.
     * @return The income record, or null if not found.
     */
    @Query("SELECT * FROM income WHERE title = :title")
    suspend fun getIncomeByTitle(title: String): Income?

    /**
     * Retrieves an income entry by its associated date.
     *
     * @param date The date the income was recorded.
     * @return The income record, or null if not found.
     */
    @Query("SELECT * FROM income WHERE date = :date")
    suspend fun getIncomeByDate(date: Date): Income?

    //endregion

    //region Queries by User

    /**
     * Retrieves all income entries associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of income records for the user.
     */
    @Query("SELECT * FROM income WHERE userId = :userId")
    suspend fun getIncomesByUserId(userId: String): List<Income>

    //endregion

    //region Queries by Date Range

    /**
     * Retrieves all income entries within a given date range.
     *
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return A list of income records between the specified dates.
     */
    @Query("SELECT * FROM income WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getIncomesWithinDateRange(startDate: Date, endDate: Date): List<Income>

    /**
     * Calculates the total income amount recorded within a specific date range.
     *
     * @param startDate The start of the date range.
     * @param endDate The end of the date range.
     * @return The sum of all income amounts in the given range.
     */
    @Query("SELECT SUM(amount) FROM income WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncomeWithinDateRange(startDate: Date, endDate: Date): Double

    //endregion
}
