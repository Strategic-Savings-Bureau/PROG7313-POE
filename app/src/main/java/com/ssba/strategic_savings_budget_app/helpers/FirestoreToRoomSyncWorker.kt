package com.ssba.strategic_savings_budget_app.helpers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.*
import kotlinx.coroutines.tasks.await

/*
 * Code Attribution
 *
 * Purpose:
 * This helper class implements a background synchronization worker for the Strategic Savings Budget App.
 * It syncs user profile and related financial data from Firebase Firestore to the local Room database,
 * ensuring offline access and data consistency. Key functions include:
 *   - Checking if user data exists locally before syncing
 *   - Fetching user profile and subcollections (saving goals, savings, income, expenses, budgets)
 *   - Mapping Firestore documents to Room entities with sync status
 *   - Handling asynchronous work with Kotlin Coroutines and WorkManager
 *   - Robust error handling with retries
 *
 * Authors/Technologies Used:
 *   - Firebase Authentication & Firestore: Google Firebase Team
 *   - Android Jetpack (WorkManager, CoroutineWorker, Room Persistence Library): Android Developers
 *   - Kotlin Coroutines: JetBrains & Kotlin Team
 *
 * Date Accessed: 6 June 2025
 *
 * References:
 *   - WorkManager: https://developer.android.com/topic/libraries/architecture/workmanager
 *   - Firebase Firestore: https://firebase.google.com/docs/firestore/query-data/get-data
 *   - Room Persistence Library: https://developer.android.com/training/data-storage/room
 *   - Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
 */

/**
 * A background [CoroutineWorker] that synchronizes user data from Firestore to the local Room database.
 *
 * This worker runs when local data does not exist for the authenticated user.
 * It:
 * - Verifies if the user already exists in the local Room DB.
 * - If not found, fetches the user's profile from the Firestore `users_profile` collection.
 * - Also pulls subcollections under `/users/{userId}` including savings, goals, budgets, etc.
 * - Converts Firestore documents into Room entities and inserts them with `isSynced = true`.
 *
 * Firestore structure expected:
 * - /users_profile/{userId}             -> User profile document
 * - /users/{userId}/saving_goals       -> List of saving goals
 * - /users/{userId}/savings            -> List of savings
 * - /users/{userId}/income_entries     -> List of income entries
 * - /users/{userId}/expense_categories -> Expense category definitions
 * - /users/{userId}/expenses           -> List of expense records
 * - /users/{userId}/budgets            -> Monthly or custom budgets
 */
class FirestoreToRoomSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "FirestoreToRoomSyncWorker"
    }

    private val firestoreDb = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val roomDatabase = AppDatabase.getInstance(applicationContext)

    /**
     * Main logic executed when the WorkManager triggers this worker.
     *
     * @return Result indicating success, failure, or retry depending on sync outcome.
     */
    override suspend fun doWork(): Result {
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            Log.w(TAG, "User not logged in. Sync aborted.")
            return Result.failure()
        }

        try {
            // Check if user data already exists in the local Room database
            val localUser = roomDatabase.userDao().getUserById(userId)
            if (localUser != null) {
                Log.d(TAG, "User $userId already exists in local DB. Skipping sync.")
                return Result.success()
            }

            Log.d(TAG, "No local data found for user $userId. Starting sync from Firestore.")

            // Fetch main user document from Firestore `users_profile` collection
            val userDoc = firestoreDb.collection("users_profile").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)?.copy(isSynced = true)

            if (user != null) {
                roomDatabase.userDao().upsertUser(user)
                Log.d(TAG, "User profile synced from users_profile.")
            } else {
                Log.w(TAG, "User document could not be parsed. Aborting sync.")
                return Result.failure()
            }

            // Sync each subcollection under /users/{userId}/
            syncCollectionFromFirestore(
                userId,
                "saving_goals",
                { it.toObject(SavingGoal::class.java)?.copy(isSynced = true) },
                { roomDatabase.savingsGoalDao().insertSavingGoals(it) }
            )

            syncCollectionFromFirestore(
                userId,
                "savings",
                { it.toObject(Saving::class.java)?.copy(isSynced = true) },
                { roomDatabase.savingDao().insertSavings(it) }
            )

            syncCollectionFromFirestore(
                userId,
                "income_entries",
                { it.toObject(Income::class.java)?.copy(isSynced = true) },
                { roomDatabase.incomeDao().insertIncomes(it) }
            )

            syncCollectionFromFirestore(
                userId,
                "expense_categories",
                { it.toObject(ExpenseCategory::class.java)?.copy(isSynced = true) },
                { roomDatabase.expenseCategoryDao().insertExpenseCategories(it) }
            )

            syncCollectionFromFirestore(
                userId,
                "expenses",
                { it.toObject(Expense::class.java)?.copy(isSynced = true) },
                { roomDatabase.expenseDao().insertExpenses(it) }
            )

            syncCollectionFromFirestore(
                userId,
                "budgets",
                { it.toObject(Budget::class.java)?.copy(isSynced = true) },
                { roomDatabase.budgetDao().insertBudgets(it) }
            )

            Log.d(TAG, "All Firestore data successfully synced for user: $userId")
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for user $userId", e)
            return Result.retry()
        }
    }

    /**
     * Generic helper function to sync a Firestore subcollection into the local Room database.
     *
     * @param userId Firebase Auth UID of the user.
     * @param collectionName Name of the subcollection under `/users/{userId}/`.
     * @param mapDocToEntity Lambda to map a Firestore document to a Room entity.
     * @param insertOp Suspend function to insert the list of mapped entities into Room.
     */
    private suspend fun <T> syncCollectionFromFirestore(
        userId: String,
        collectionName: String,
        mapDocToEntity: (com.google.firebase.firestore.DocumentSnapshot) -> T?,
        insertOp: suspend (List<T>) -> Unit
    ) {
        val collectionRef = firestoreDb.collection("users")
            .document(userId)
            .collection(collectionName)

        // Fetch all documents from the specified subcollection
        val querySnapshot = collectionRef.get().await()

        if (querySnapshot.isEmpty) {
            Log.d(TAG, "No documents found in $collectionName.")
            return
        }

        // Map each Firestore document to a Room entity, ignoring null results
        val entities = querySnapshot.documents.mapNotNull { mapDocToEntity(it) }

        if (entities.isNotEmpty()) {
            insertOp(entities)
            Log.d(TAG, "Inserted ${entities.size} records from $collectionName into Room.")
        } else {
            Log.w(TAG, "All documents in $collectionName failed to parse or were null.")
        }
    }
}