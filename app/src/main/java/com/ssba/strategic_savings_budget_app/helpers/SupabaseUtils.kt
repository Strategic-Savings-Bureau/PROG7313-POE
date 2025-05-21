package com.ssba.strategic_savings_budget_app.helpers

import android.content.Context
import android.util.Log
import com.ssba.strategic_savings_budget_app.R
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType


/*
 	* Code Attribution
 	* Purpose:
 	*   - Setting up Supabase client in an Android app
 	*   - Uploading an image to a Supabase bucket
 	* Author: Supabase Community
 	* Sources:
 	*   - Supabase Android Client: https://supabase.com/docs/guides/with-react-native/android
*/

/**
 * `SupabaseUtils` is a singleton helper object responsible for managing interactions
 * with the Supabase backend. It provides utilities to:
 * - Initialize the Supabase client.
 * - Upload images to specific Supabase Storage buckets (profile and receipt images).
 *
 * This object abstracts and simplifies Supabase-related operations
 * throughout the application.
 */

object SupabaseUtils
{
    // Holds the initialized Supabase client instance
    private var supabaseClient: SupabaseClient? = null

    /**
     * Initializes the Supabase client using the application context to access
     * configuration values such as the Supabase URL and API key.
     *
     * This should only be called once during the app's lifecycle.
     *
     * @param context The application context used to fetch string resources.
     */

    fun init(context: Context) {
        if (supabaseClient == null) {
            val url = context.getString(R.string.supabase_url)
            val key = context.getString(R.string.supabase_api_key)

            // Create and configure the Supabase client
            supabaseClient = createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Postgrest)
                install(Storage)
            }
        }
    }

    // Constants representing the names of the Supabase storage buckets
    private const val PROFILE_BUCKET = "user-profile-pictures"
    private const val RECEIPT_BUCKET = "expense-reciepts"

    /**
     * Uploads a user's profile image to the 'user-profile' bucket in Supabase Storage.
     *
     * @param filename The user's unique identifier (used as file path).
     * @param image The profile image as a byte array.
     * @return The public URL of the uploaded image, or an empty string if the upload fails.
     */
    suspend fun uploadProfileImageToStorage(filename: String, image: ByteArray): String {
        return uploadImageToBucket(
            bucketName = PROFILE_BUCKET,
            filePath = filename,
            image = image,
            tag = "ProfileUpload"
        )
    }

    /**
     * Uploads a receipt image to the 'expense-reciepts' bucket in Supabase Storage.
     *
     * @param filename The post's unique identifier (used as file path).
     * @param image The receipt image as a byte array.
     * @return The public URL of the uploaded image, or an empty string if the upload fails.
     */
    suspend fun uploadReceiptImageToStorage(filename: String, image: ByteArray): String {
        return uploadImageToBucket(
            bucketName = RECEIPT_BUCKET,
            filePath = filename,
            image = image,
            tag = "ReceiptUpload"
        )
    }

    /**
     * Uploads an image to the specified Supabase storage bucket.
     *
     * @param bucketName The name of the Supabase bucket.
     * @param filePath The path (filename) under which the image will be stored.
     * @param image The image data as a byte array.
     * @param tag A tag used for logging purposes.
     * @return The public URL of the uploaded image, or an empty string if the upload fails.
     */
    private suspend fun uploadImageToBucket(
        bucketName: String,
        filePath: String,
        image: ByteArray,
        tag: String
    ): String
    {
        val client = supabaseClient ?: return ""

        return try {
            // Validate file path and image data
            if (filePath.isNotEmpty() && image.isNotEmpty()) {
                val bucket = client.storage.from(bucketName)

                // Upload the image file with JPEG content type
                bucket.upload(filePath, image) {
                    upsert = true // Prevent overwrite if file exists
                    contentType = ContentType.Image.JPEG
                }

                // Return the public URL to access the image
                bucket.publicUrl(filePath)
            } else {
                Log.e(tag, "File path or image is empty")
                ""
            }
        } catch (e: Exception) {
            Log.e(tag, "Error uploading image to Supabase", e)
            ""
        }
    }
}