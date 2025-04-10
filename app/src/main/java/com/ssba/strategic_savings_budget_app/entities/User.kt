package com.ssba.strategic_savings_budget_app.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user in the Strategic Savings Budget App.
 *
 * This entity stores user-specific details and is linked to Firebase Authentication via `userId`.
 *
 * @property userId The unique identifier for the user, obtained from Firebase Authentication.
 * @property fullName The full name of the user.
 * @property email The email address of the user.
 * @property username The app-specific username chosen by the user.
 * @property profilePictureUrl The URL to the user's profile picture, typically stored in Supabase Storage.
 */
@Entity(tableName = "user")
data class User(

    @PrimaryKey(autoGenerate = false)
    val userId: String, // Unique identifier from Firebase Authentication

    val fullName: String, // Full legal or display name of the user

    val email: String, // User's email address

    val username: String, // Unique username for display and search within the app

    val profilePictureUrl: String // URL linking to the user's profile image (hosted on Supabase Storage)
)
