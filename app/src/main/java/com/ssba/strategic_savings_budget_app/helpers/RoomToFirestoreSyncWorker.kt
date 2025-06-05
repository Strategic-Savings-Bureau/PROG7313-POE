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

/**
 * A [CoroutineWorker] that synchronizes local Room database entities with Firestore cloud database.
 *
 * This worker uploads all unsynced entities (such as savings goals, incomes, expenses, budgets, and users)
 * belonging to the currently logged-in user to Firestore, and then marks them as synced in Room.
 *
 * It uses batch writes for efficiency and handles retries if syncing fails.
 *
 * @param appContext The context of the application
 * @param workerParams Parameters for this worker
 */
class RoomToFirestoreSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Firestore database instance for syncing data to cloud
    private val firestoreDb = FirebaseFirestore.getInstance()

    // Firebase Authentication instance to get current logged-in user ID
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Singleton instance of Room database
    private val roomDatabase = AppDatabase.getInstance(applicationContext)

    companion object {
        private const val TAG = "RoomToFirestoreWorker"
    }

    /**
     * The main work function that runs asynchronously to:
     * 1. Check if user is logged in.
     * 2. Sync unsynced entities one by one.
     * 3. Update the local Room entities with `isSynced = true` flag upon success.
     * 4. Return a [Result] indicating success or retry on failure.
     */
    override suspend fun doWork(): Result {
        // Get current user's UID from FirebaseAuth
        val userId = firebaseAuth.currentUser?.uid

        // Abort if user is not logged in
        if (userId == null) {
            Log.w(TAG, "User not logged in. Sync aborted.")
            return Result.failure()
        }

        Log.d(TAG, "Starting Room to Firestore sync for user: $userId")

        return try {
            // Sync each entity type by calling the generic syncEntities function
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

            // Special handling for User profiles synchronization
            val unsyncedUsers = roomDatabase.userDao().getAllUnSyncedUsers()

            if (unsyncedUsers.isNotEmpty()) {
                // Start a Firestore batch operation for all unsynced users
                val batch = firestoreDb.batch()

                // Queue all unsynced users to be written to Firestore under "users_profile" collection
                unsyncedUsers.forEach { user ->
                    val userRef = firestoreDb.collection("users_profile").document(user.userId)
                    batch.set(userRef, user, SetOptions.merge())
                }

                // Commit batch upload and wait for completion
                batch.commit().await()

                // Mark all user entities as synced in local Room database
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
            // Retry the sync if any exception occurs
            Result.retry()
        }
    }

    /**
     * Generic suspend function to sync a list of unsynced entities from Room to Firestore.
     *
     * This function:
     * - Retrieves unsynced entities using [getUnsyncedOp].
     * - Performs a Firestore batch write to upload all entities.
     * - Marks entities as synced by setting their `isSynced` flag to true and updating in Room via [updateOp].
     *
     * @param T The type of the entity (e.g., SavingGoal, Expense, Budget)
     * @param userId The Firebase UID of the current user
     * @param collectionName Firestore sub-collection name where the entities will be stored
     * @param getUnsyncedOp Suspended lambda that returns a list of unsynced entities from Room
     * @param updateOp Suspended lambda to update an entity in Room with new values (e.g., isSynced = true)
     * @param getDocumentId Function to extract a unique document ID from an entity
     *
     * @throws IllegalArgumentException if an unsupported entity type is passed
     */
    private suspend fun <T : Any> syncEntities(
        userId: String,
        collectionName: String,
        getUnsyncedOp: suspend () -> List<T>,
        updateOp: suspend (T) -> Unit,
        getDocumentId: (T) -> String
    ) {
        // Fetch all unsynced items of type T from local database
        val unsyncedItems = getUnsyncedOp()

        if (unsyncedItems.isNotEmpty()) {
            // Start a Firestore batch operation to upload all unsynced items
            val batch = firestoreDb.batch()

            // Queue each unsynced item for upload using batch.set with merge option
            unsyncedItems.forEach { item ->
                val docId = getDocumentId(item)
                val docRef = firestoreDb.collection("users")
                    .document(userId)
                    .collection(collectionName)
                    .document(docId)
                batch.set(docRef, item, SetOptions.merge())
            }

            // Commit the batch and wait for Firestore upload completion
            batch.commit().await()

            // Update each synced item locally by setting isSynced = true
            unsyncedItems.forEach { item ->
                val updatedItem = when (item) {
                    is SavingGoal -> item.copy(isSynced = true) as T
                    is Saving -> item.copy(isSynced = true) as T
                    is Income -> item.copy(isSynced = true) as T
                    is ExpenseCategory -> item.copy(isSynced = true) as T
                    is Expense -> item.copy(isSynced = true) as T
                    is Budget -> item.copy(isSynced = true) as T
                    else -> throw IllegalArgumentException("Unsupported entity type")
                }
                updateOp(updatedItem) // Update Room database with synced item
            }

            Log.d(TAG, "Synced ${unsyncedItems.size} $collectionName to Firestore.")

        } else {
            Log.d(TAG, "No unsynced $collectionName to sync.")
        }
    }
}
