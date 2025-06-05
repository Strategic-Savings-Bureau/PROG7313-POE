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

/**
 * CoroutineWorker responsible for synchronizing user data from Firestore
 * to the local Room database when local data does not yet exist.
 *
 * This worker:
 * - Checks if the authenticated user exists in the local Room DB.
 * - If not, fetches the user's profile and subcollections from Firestore.
 * - Converts documents into Room entities and inserts them locally with isSynced = true.
 *
 * Firestore structure assumed:
 * /users/{userId} -> user document
 * /users/{userId}/saving_goals
 * /users/{userId}/savings
 * /users/{userId}/income_entries
 * /users/{userId}/expense_categories
 * /users/{userId}/expenses
 * /users/{userId}/budgets
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
     * Entry point for background syncing work.
     * This checks if the Room DB is empty for the current user,
     * and if so, pulls data from Firestore and inserts it.
     */
    override suspend fun doWork(): Result
    {
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            Log.w(TAG, "User not logged in. Sync aborted.")
            return Result.failure()
        }

        try {
            // Check if user already exists in local Room database
            val localUser = roomDatabase.userDao().getUserById(userId)
            if (localUser != null) {
                Log.d(TAG, "User $userId already exists in local DB. Skipping sync.")
                return Result.success()
            }

            Log.d(TAG, "No local data found for user $userId. Starting sync from Firestore.")

            // --- Sync main user document from 'users_profile' instead of 'users' ---
            val userDoc = firestoreDb.collection("users_profile").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)?.copy(isSynced = true)
            if (user != null) {
                roomDatabase.userDao().upsertUser(user)
                Log.d(TAG, "User profile synced from users_profile.")
            } else {
                Log.w(TAG, "User document could not be parsed. Aborting sync.")
                return Result.failure()
            }

            // --- Sync subcollections ---
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
     * Generic function to synchronize a subcollection from Firestore into Room.
     *
     * @param userId The authenticated user's Firebase UID
     * @param collectionName The name of the Firestore subcollection under /users/{userId}/
     * @param mapDocToEntity Function to convert Firestore DocumentSnapshot to Room entity (with isSynced = true)
     * @param insertOp Room DAO function to insert a list of parsed entities
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

        // Fetch all documents from the subcollection
        val querySnapshot = collectionRef.get().await()

        if (querySnapshot.isEmpty) {
            Log.d(TAG, "No documents found in $collectionName.")
            return
        }

        // Convert Firestore documents to Room entities (filtering nulls)
        val entities = querySnapshot.documents.mapNotNull { mapDocToEntity(it) }

        if (entities.isNotEmpty()) {
            insertOp(entities)
            Log.d(TAG, "Inserted ${entities.size} from $collectionName into Room.")
        } else {
            Log.w(TAG, "All documents in $collectionName were null or failed to parse.")
        }
    }
}
