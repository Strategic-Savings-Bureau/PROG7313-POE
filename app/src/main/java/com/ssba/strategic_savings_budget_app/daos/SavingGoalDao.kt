package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.SavingGoal
import com.ssba.strategic_savings_budget_app.entities.relations.SavingGoalWithSavings
import java.util.Date

@Dao
interface SavingGoalDao {

    //region Insert, Update, Delete Operations

    /**
     * Inserts a new saving goal or updates it if it already exists (based on primary key).
     *
     * @param savingGoal The saving goal to insert or update.
     */
    @Upsert
    suspend fun upsertSavingGoal(savingGoal: SavingGoal)

    /**
     * Deletes a specific saving goal from the database.
     *
     * @param savingGoal The saving goal to be deleted.
     */
    @Delete
    suspend fun deleteSavingGoal(savingGoal: SavingGoal)

    //endregion

    //region Basic Saving Goal Queries

    /**
     * Retrieves a saving goal by its unique ID.
     *
     * @param savingGoalId The ID of the saving goal.
     * @return The saving goal if found, otherwise null.
     */
    @Query("SELECT * FROM saving_goal WHERE savingGoalId = :savingGoalId")
    suspend fun getSavingGoalById(savingGoalId: Int): SavingGoal?

    /**
     * Retrieves a saving goal by its title.
     *
     * @param title The title of the saving goal.
     * @return The saving goal if found, otherwise null.
     */
    @Query("SELECT * FROM saving_goal WHERE title = :title")
    suspend fun getSavingGoalByTitle(title: String): SavingGoal?

    /**
     * Retrieves all saving goals for a given user.
     *
     * @param userId The ID of the user.
     * @return A list of saving goals belonging to the user.
     */
    @Query("SELECT * FROM saving_goal WHERE userId = :userId")
    suspend fun getSavingGoalsByUserId(userId: String): List<SavingGoal>

    /**
     * Retrieves all saving goals that have not yet expired.
     *
     * @param currentDate The current date used to check expiry.
     * @return A list of saving goals with future end dates.
     */
    @Query("SELECT * FROM saving_goal WHERE endDate > :currentDate")
    suspend fun getActiveSavingGoals(currentDate: Date): List<SavingGoal>

    //endregion

    //region Relational Queries

    /**
     * Retrieves all savings entries associated with a specific saving goal by its ID.
     *
     * @param savingGoalId The ID of the saving goal.
     * @return A list containing the saving goal and its related savings.
     */
    @Transaction
    @Query("SELECT * FROM saving_goal WHERE savingGoalId = :savingGoalId")
    suspend fun getSavingsBySavingGoalId(savingGoalId: Int): List<SavingGoalWithSavings>

    /**
     * Retrieves all savings entries associated with a specific saving goal by its title.
     *
     * @param title The title of the saving goal.
     * @return A list containing the saving goal and its related savings.
     */
    @Transaction
    @Query("SELECT * FROM saving_goal WHERE title = :title")
    suspend fun getSavingsBySavingGoalTitle(title: String): List<SavingGoalWithSavings>

    //endregion
}
