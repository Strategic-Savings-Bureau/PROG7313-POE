package com.ssba.strategic_savings_budget_app.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Converters class for Room Database.
 *
 * Room doesn't support storing complex types like java.util.Date directly in the database.
 * This class provides the necessary type converters to convert Date objects to Long (epoch time)
 * and vice versa, allowing Room to persist Date values by storing them as timestamps.
 */

/*
 	* Code Attribution
 	* Purpose: Creating a Type Converter to store java.util.Date in Room Database
 	* Author: Android Developers
 	* Date Accessed: 10 April 2025
 	* Source: Developer Guide - Android Developers
 	* URL: https://developer.android.com/training/data-storage/room/defining-data#type-converters
*/

class Converters {

    /**
     * Converts a Long timestamp value from the database to a java.util.Date object.
     *
     * @param value The Long value representing time in milliseconds since epoch.
     * @return A Date object or null if the input value is null.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a java.util.Date object to a Long value for database storage.
     *
     * @param date The Date object to convert.
     * @return A Long value representing time in milliseconds since epoch, or null if the input is null.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
