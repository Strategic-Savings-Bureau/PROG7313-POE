package com.ssba.strategic_savings_budget_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ssba.strategic_savings_budget_app.daos.*
import com.ssba.strategic_savings_budget_app.entities.*

/**
 * The Room database for the Strategic Savings Budget App.
 *
 * This class serves as the main access point to the local database using Room. It defines all
 * entities used in the database, declares abstract DAO accessors for each entity, and provides
 * a singleton instance of the database to avoid memory leaks or redundant initializations.
 *
 * Room handles the creation and versioning of the underlying SQLite database.
 *
 * @see UserDao
 * @see BudgetDao
 * @see SavingGoalDao
 * @see SavingDao
 * @see IncomeDao
 * @see ExpenseCategoryDao
 * @see ExpenseDao
 */
@Database(
    entities = [
        User::class,
        Budget::class,
        SavingGoal::class,
        Saving::class,
        Income::class,
        ExpenseCategory::class,
        Expense::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract DAO accessors for Room to generate DAO implementations
    abstract val userDao: UserDao
    abstract val budgetDao: BudgetDao
    abstract val savingsGoalDao: SavingGoalDao
    abstract val savingDao: SavingDao
    abstract val incomeDao: IncomeDao
    abstract val expenseCategoryDao: ExpenseCategoryDao
    abstract val expenseDao: ExpenseDao

    companion object {

        /**
         * Singleton instance of the Room database.
         * Ensures that only one instance of the database is created across the entire app lifecycle.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of the database.
         *
         * If the instance is null, it will initialize the database using the application context.
         * The [synchronized] block ensures thread safety.
         *
         * @param context The context used to access the filesystem and application lifecycle.
         * @return A singleton instance of [AppDatabase].
         */
        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // Optional: Wipes and rebuilds DB if migration isn't handled
                    .fallbackToDestructiveMigration()
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
