package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user in the Strategic Savings Budget App.
 *
 * This entity stores user-specific details and is linked to Firebase Authentication via `userId`.
 * It includes indices for `fullName`, `email`, and `username` to optimize queries and lookups on these columns.
 *
 * Indices:
 * - `fullName`: Index is created to speed up searches and queries based on the user's full name.
 * - `email`: Index is created to speed up searches and queries based on the user's email address.
 * - `username`: Index is created to speed up searches and queries based on the user's app-specific username.
 *
 * @property userId The unique identifier for the user, obtained from Firebase Authentication.
 * @property fullName The full name of the user.
 * @property email The email address of the user.
 * @property username The app-specific username chosen by the user.
 * @property profilePictureUrl The URL to the user's profile picture, typically stored in Supabase Storage.
 */

/*
 	* Code Attribution
 	* Purpose: Setting up a Room Database in an Android app (based on official Android documentation)
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room
*/


@Entity(
    tableName = "user",
    indices = [Index(value = ["fullName"]), Index(value = ["email"]), Index(value = ["username"])]
)
data class User(

    @PrimaryKey(autoGenerate = false)
    val userId: String, // Unique identifier from Firebase Authentication

    val fullName: String, // Full legal or display name of the user

    val email: String, // User's email address

    val username: String, // Unique username for display and search within the app

    val profilePictureUrl: String, // URL linking to the user's profile image (hosted on Supabase Storage)

    // Sync fields (if user profile data is editable locally and needs sync)
    var isSynced: Boolean = false,
    var lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
