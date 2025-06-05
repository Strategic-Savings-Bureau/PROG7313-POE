package com.ssba.strategic_savings_budget_app.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ssba.strategic_savings_budget_app.entities.User
import com.ssba.strategic_savings_budget_app.entities.relations.*

/*
 	* Code Attribution
 	* Purpose: Creating DAO interfaces to define SQL queries for Room Database operations
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/accessing-data
*/


@Dao
interface UserDao {

    //region Insert, Update, Delete Operations

    /**
     * Inserts a new user or updates an existing user based on the primary key.
     *
     * @param user The user entity to insert or update.
     */
    @Upsert
    suspend fun upsertUser(user: User)

    /**
     * Deletes a user from the database.
     *
     * @param user The user entity to delete.
     */
    @Delete
    suspend fun deleteUser(user: User)

    //endregion

    //region Basic User Queries

    /**
     * Retrieves a user by their unique ID.
     *
     * @param userId The ID of the user.
     * @return The user if found, or null otherwise.
     */
    @Query("SELECT * FROM user WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    /**
     * Retrieves a user by their username.
     *
     * @param username The username of the user.
     * @return The user if found, or null otherwise.
     */
    @Query("SELECT * FROM user WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email of the user.
     * @return The user if found, or null otherwise.
     */
    @Query("SELECT * FROM user WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    /**
     * Retrieves a user by their full name.
     *
     * @param fullName The full name of the user.
     * @return The user if found, or null otherwise.
     */
    @Query("SELECT * FROM user WHERE fullName = :fullName")
    suspend fun getUserByFullName(fullName: String): User?

    //endregion

    //region User Relationship Queries

    /**
     * Retrieves the budget associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A list containing the user's budget.
     */
    @Transaction
    @Query("SELECT * FROM user WHERE userId = :userId")
    suspend fun getUserWithBudget(userId: String): List<UserAndBudget>

    /**
     * Retrieves the incomes associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A list containing the user's incomes.
     */
    @Transaction
    @Query("SELECT * FROM user WHERE userId = :userId")
    suspend fun getUserWithIncomes(userId: String): List<UserWithIncomes>

    /**
     * Retrieves the expense categories associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of the user's expense categories.
     */
    @Transaction
    @Query("SELECT * FROM user WHERE userId = :userId")
    suspend fun getUserWithExpenseCategories(userId: String): List<UserWithExpenseCategories>

    /**
     * Retrieves the saving goals associated with a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of the user's saving goals.
     */
    @Transaction
    @Query("SELECT * FROM user WHERE userId = :userId")
    suspend fun getUserWithSavingGoals(userId: String): List<UserWithSavingGoals>

    //endregion

    // region Sync Queries

    /**
     * Retrieves a list of all un-synced users.
     *
     * @return A list of all users.
     */
    @Query("SELECT * FROM user WHERE isSynced = false")
    suspend fun getAllUnSyncedUsers(): List<User>

    // endregion
}
