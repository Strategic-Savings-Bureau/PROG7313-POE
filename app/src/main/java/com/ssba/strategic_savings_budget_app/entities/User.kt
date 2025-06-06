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
 * @property isSynced Indicates whether the local user data is synchronized with the backend.
 * @property lastUpdatedTimestamp The last time the user data was updated locally (used for conflict resolution).
 */

/*
    * Code Attribution
    * Purpose: Setting up a Room Database and enabling Firestore compatibility in a Kotlin Android app.
    * Authors: Android Developers & Firebase Documentation Team
    * Date Accessed: 10 April 2025
    * Sources:
    * - Room: https://developer.android.com/training/data-storage/room
    * - Firestore Kotlin Model Classes: https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects
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

    var isSynced: Boolean = false, // Flag indicating if the local data is synced with remote backend

    var lastUpdatedTimestamp: Long = System.currentTimeMillis() // Timestamp of last update
) {
    /**
     * No-argument constructor required by Firebase Firestore for deserialization.
     * This constructor is used by Firestore to instantiate the class via reflection.
     * It does not affect Room since Room uses the primary constructor.
     */
    constructor() : this(
        userId = "",
        fullName = "",
        email = "",
        username = "",
        profilePictureUrl = "",
        isSynced = false,
        lastUpdatedTimestamp = 0L
    )
}
