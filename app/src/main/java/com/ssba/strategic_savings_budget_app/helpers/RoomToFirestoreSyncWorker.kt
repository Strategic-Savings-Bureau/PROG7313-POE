package com.ssba.strategic_savings_budget_app.helpers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.*
import kotlinx.coroutines.tasks.await

/*
 * Code Attribution
 *
 * Purpose:
 * This worker handles background synchronization of unsynced Room database entities
 * (such as savings goals, incomes, expenses, budgets, and user profiles) to Firebase Firestore.
 * It:
 *   - Authenticates the current user with FirebaseAuth
 *   - Retrieves unsynced local Room entities
 *   - Performs batch writes to Firestore collections under the authenticated user's ID
 *   - Updates local Room entities to mark them as synced after successful upload
 *   - Supports automatic retries on failure
 *
 * Authors/Technologies Used:
 *   - Firebase Authentication & Firestore: Google Firebase Team
 *   - Android Jetpack WorkManager & Room Persistence Library: Android Developers
 *   - Kotlin Coroutines for asynchronous background processing: Kotlin Team
 *
 * Date Accessed: 6 June 2025
 *
 * References:
 *   - Firebase Auth & Firestore Batch Writes: https://firebase.google.com/docs/firestore/manage-data/transactions
 *   - Android WorkManager Documentation: https://developer.android.com/topic/libraries/architecture/workmanager
 *   - Room Database: https://developer.android.com/training/data-storage/room
 *   - Kotlin Coroutines with WorkManager: https://developer.android.com/kotlin/coroutines/coroutines-adv-workmanager
 */

/**
 * [RoomToFirestoreSyncWorker] is a [CoroutineWorker] that uploads unsynced Room database entities
 * to Firebase Firestore for backup and cross-device availability.
 *
 * The worker:
 * - Runs as a background job.
 * - Checks for user authentication.
 * - Uploads all unsynced entities (e.g., savings goals, incomes, expenses).
 * - Marks each successfully synced entity in Room as `isSynced = true`.
 * - Retries automatically on failure.
 *
 * @param appContext The application context used by the worker.
 * @param workerParams Parameters passed to the worker.
 */
class RoomToFirestoreSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Firebase Firestore instance for cloud storage
    private val firestoreDb = FirebaseFirestore.getInstance()

    // FirebaseAuth instance for retrieving current user ID
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Singleton instance of the Room database
    private val roomDatabase = AppDatabase.getInstance(applicationContext)

    companion object {
        private const val TAG = "RoomToFirestoreWorker"
    }

    /**
     * The entry point of the worker, executed asynchronously in the background.
     *
     * Steps:
     * 1. Verify if a user is logged in.
     * 2. Call [syncEntities] for each entity type to sync unsynced Room entries to Firestore.
     * 3. Handle user profile syncing separately.
     * 4. Mark each synced item in Room with `isSynced = true`.
     *
     * @return [Result.success] if syncing is successful,
     *         [Result.failure] if no user is logged in,
     *         [Result.retry] on exception/failure.
     */
    override suspend fun doWork(): Result {
        val userId = firebaseAuth.currentUser?.uid

        // Exit early if no user is authenticated
        if (userId == null) {
            Log.w(TAG, "User not logged in. Sync aborted.")
            return Result.failure()
        }

        Log.d(TAG, "Starting Room to Firestore sync for user: $userId")

        return try {
            // Sequentially sync all Room entity types
            syncEntities(
                userId,
                "saving_goals",
                { roomDatabase.savingsGoalDao().getUnSyncedSavingGoals(userId) },
                { entity -> roomDatabase.savingsGoalDao().upsertSavingGoal(entity) },
                { it.savingGoalId.toString() }
            )

            syncEntities(
                userId,
                "savings",
                { roomDatabase.savingDao().getUnSyncedSavings(userId) },
                { entity -> roomDatabase.savingDao().upsertSaving(entity) },
                { it.savingId.toString() }
            )

            syncEntities(
                userId,
                "income_entries",
                { roomDatabase.incomeDao().getUnSyncedIncomes(userId) },
                { entity -> roomDatabase.incomeDao().upsertIncome(entity) },
                { it.incomeId.toString() }
            )

            syncEntities(
                userId,
                "expense_categories",
                { roomDatabase.expenseCategoryDao().getUnSyncedExpenseCategories(userId) },
                { entity -> roomDatabase.expenseCategoryDao().upsertExpenseCategory(entity) },
                { it.categoryId.toString() }
            )

            syncEntities(
                userId,
                "expenses",
                { roomDatabase.expenseDao().getUnSyncedExpenses(userId) },
                { entity -> roomDatabase.expenseDao().upsertExpense(entity) },
                { it.expenseId.toString() }
            )

            syncEntities(
                userId,
                "budgets",
                { roomDatabase.budgetDao().getUnSyncedBudgets(userId) },
                { entity -> roomDatabase.budgetDao().upsertBudget(entity) },
                { it.budgetId.toString() }
            )

            // Sync user profile data independently
            val unsyncedUsers = roomDatabase.userDao().getAllUnSyncedUsers()

            if (unsyncedUsers.isNotEmpty()) {
                val batch = firestoreDb.batch()

                // Add each unsynced user to batch write
                unsyncedUsers.forEach { user ->
                    val userRef = firestoreDb.collection("users_profile").document(user.userId)
                    batch.set(userRef, user, SetOptions.merge())
                }

                // Commit all user writes to Firestore
                batch.commit().await()

                // Mark users as synced in Room
                unsyncedUsers.forEach { user ->
                    val updatedUser = user.copy(isSynced = true)
                    roomDatabase.userDao().upsertUser(updatedUser)
                }

                Log.d(TAG, "Synced ${unsyncedUsers.size} user profiles to Firestore.")
            } else {
                Log.d(TAG, "No unsynced user profiles to sync.")
            }

            Log.d(TAG, "Room to Firestore sync completed successfully for user: $userId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error during Room to Firestore sync for user $userId", e)
            Result.retry()
        }
    }

    /**
     * Generic suspend function to sync Room entities with Firestore in batch.
     *
     * It:
     * - Fetches unsynced items from Room via [getUnsyncedOp].
     * - Writes them to Firestore in a batch under the appropriate collection.
     * - Marks each entity as `isSynced = true` and updates Room via [updateOp].
     *
     * @param T The entity type to sync.
     * @param userId Firebase user ID.
     * @param collectionName Firestore collection name (e.g., "expenses", "savings").
     * @param getUnsyncedOp Function to fetch unsynced Room entities.
     * @param updateOp Function to update an entity in Room after syncing.
     * @param getDocumentId Function to extract the Firestore document ID from an entity.
     *
     * @throws IllegalArgumentException If the entity type is unsupported.
     */
    private suspend fun <T : Any> syncEntities(
        userId: String,
        collectionName: String,
        getUnsyncedOp: suspend () -> List<T>,
        updateOp: suspend (T) -> Unit,
        getDocumentId: (T) -> String
    ) {
        // Get all unsynced entities of type T
        val unsyncedItems = getUnsyncedOp()

        if (unsyncedItems.isNotEmpty()) {
            val batch = firestoreDb.batch()

            // Add each item to the Firestore batch
            unsyncedItems.forEach { item ->
                val docId = getDocumentId(item)
                val docRef = firestoreDb.collection("users")
                    .document(userId)
                    .collection(collectionName)
                    .document(docId)

                batch.set(docRef, item, SetOptions.merge())
            }

            // Commit batch write to Firestore
            batch.commit().await()

            // Update each item as synced in Room
            unsyncedItems.forEach { item ->
                @Suppress("UNCHECKED_CAST")
                val updatedItem = when (item) {
                    is SavingGoal -> item.copy(isSynced = true) as T
                    is Saving -> item.copy(isSynced = true) as T
                    is Income -> item.copy(isSynced = true) as T
                    is ExpenseCategory -> item.copy(isSynced = true) as T
                    is Expense -> item.copy(isSynced = true) as T
                    is Budget -> item.copy(isSynced = true) as T
                    else -> throw IllegalArgumentException("Unsupported entity type")
                }

                updateOp(updatedItem)
            }

            Log.d(TAG, "Synced ${unsyncedItems.size} $collectionName to Firestore.")
        } else {
            Log.d(TAG, "No unsynced $collectionName to sync.")
        }
    }
}