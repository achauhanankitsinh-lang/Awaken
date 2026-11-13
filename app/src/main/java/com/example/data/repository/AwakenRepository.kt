package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.domain.model.ActivityLog
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.model.OwnerLock
import com.example.domain.model.OwnerProfile
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TrainingDay
import com.example.domain.model.TrainingStatus
import com.example.domain.util.AwakenDateUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AwakenRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = FirebaseAuth.getInstance()
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in before accessing AWAKEN data.")
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  // --- SINGLE-OWNER WORKSPACE LOCK & PROFILE ---

  suspend fun getOwnerLock(): OwnerLock? {
    requireUserId()
    val ref = db.collection("app_config").document("owner_lock")
    return try {
      val snap = ref.get().await()
      if (snap.exists()) {
        snap.toObject(OwnerLock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, ref.path)
      throw e
    }
  }

  suspend fun claimOwnerWorkspace(displayName: String, email: String?): OwnerLock {
    val uid = requireUserId()
    val lockRef = db.collection("app_config").document("owner_lock")
    val profileRef = userDoc(uid).collection("private").document("profile")
    try {
      val lockPayload = mapOf(
        "ownerUid" to uid,
        "registrationClosed" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      lockRef.set(lockPayload).await()

      val profilePayload = mapOf(
        "ownerId" to uid,
        "displayName" to displayName.ifBlank { "Owner" }.take(120),
        "email" to email?.take(200),
        "timezone" to AwakenDateUtils.TIMEZONE_ID,
        "defaultCurrency" to "INR",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      ).filterValues { it != null }.mapValues { it.value!! }
      profileRef.set(profilePayload).await()

      return OwnerLock(id = "owner_lock", ownerUid = uid, registrationClosed = true)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, lockRef.path)
      throw e
    }
  }

  suspend fun ensureOwnerProfile(displayName: String, email: String?): OwnerProfile? {
    val uid = requireUserId()
    val profileRef = userDoc(uid).collection("private").document("profile")
    return try {
      val existing = profileRef.get().await()
      if (!existing.exists()) {
        val profilePayload = mapOf(
          "ownerId" to uid,
          "displayName" to displayName.ifBlank { "Owner" }.take(120),
          "email" to email?.take(200),
          "timezone" to AwakenDateUtils.TIMEZONE_ID,
          "defaultCurrency" to "INR",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }.mapValues { it.value!! }
        profileRef.set(profilePayload).await()
      }
      profileRef.get().await()
        .toObject(OwnerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, profileRef.path)
      throw e
    }
  }

  // --- REALTIME OBSERVATION FLOWS ---

  fun observeTasks(uid: String): Flow<List<TaskItem>> = observeUserCollection(
    uid = uid,
    collectionName = "tasks",
    mapper = { snap ->
      snap.toObjects(TaskItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedWith(compareBy<TaskItem> { it.scheduledDate }.thenByDescending { it.createdAt ?: Timestamp(0, 0) })
    }
  )

  fun observeActivityLogs(uid: String): Flow<List<ActivityLog>> = observeUserCollection(
    uid = uid,
    collectionName = "activity_logs",
    mapper = { snap ->
      snap.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.occurredAt ?: it.createdAt ?: Timestamp(0, 0) }
    }
  )

  fun observeTrainingDays(uid: String): Flow<List<TrainingDay>> = observeUserCollection(
    uid = uid,
    collectionName = "training_days",
    mapper = { snap ->
      snap.toObjects(TrainingDay::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.trainingDate }
    }
  )

  fun observeGoals(uid: String): Flow<List<GoalItem>> = observeUserCollection(
    uid = uid,
    collectionName = "goals",
    mapper = { snap ->
      snap.toObjects(GoalItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    }
  )

  fun observeTransactions(uid: String): Flow<List<FinancialTransaction>> = observeUserCollection(
    uid = uid,
    collectionName = "financial_transactions",
    mapper = { snap ->
      snap.toObjects(FinancialTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedWith(compareByDescending<FinancialTransaction> { it.transactionDate }.thenByDescending { it.createdAt ?: Timestamp(0, 0) })
    }
  )

  private fun <T> observeUserCollection(
    uid: String,
    collectionName: String,
    mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>
  ): Flow<List<T>> = flow {
    val activeUid = requireUserId()
    require(activeUid == uid) { "Owner UID mismatch" }
    val colRef = userDoc(uid).collection(collectionName)
    emitAll(
      colRef.snapshots()
        .map(mapper)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
          }
          throw error
        }
    )
  }

  // --- TASKS CRUD ---

  suspend fun createTask(task: TaskItem): String {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("tasks").document()
    return try {
      docRef.set(task.toCreateMap(uid)).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateTask(task: TaskItem) {
    val uid = requireUserId()
    require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
    val docRef = userDoc(uid).collection("tasks").document(task.id)
    try {
      docRef.set(task.toUpdateMap(uid)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateTaskStatus(task: TaskItem, newStatus: TaskStatus) {
    updateTask(task.copy(status = newStatus.wireValue))
  }

  suspend fun deleteTask(taskId: String) {
    val uid = requireUserId()
    require(taskId.isNotBlank()) { "Task ID must not be blank for delete" }
    val docRef = userDoc(uid).collection("tasks").document(taskId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- ACTIVITY LOGS CRUD ---

  suspend fun createActivityLog(log: ActivityLog): String {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("activity_logs").document()
    return try {
      docRef.set(log.toCreateMap(uid)).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateActivityLog(log: ActivityLog) {
    val uid = requireUserId()
    require(log.id.isNotBlank()) { "ActivityLog ID must not be blank for update" }
    val docRef = userDoc(uid).collection("activity_logs").document(log.id)
    try {
      docRef.set(log.toUpdateMap(uid)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteActivityLog(logId: String) {
    val uid = requireUserId()
    require(logId.isNotBlank()) { "ActivityLog ID must not be blank for delete" }
    val docRef = userDoc(uid).collection("activity_logs").document(logId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- TRAINING DAYS CRUD (Unique per dateKey) ---

  suspend fun setTrainingDay(dateKey: String, status: TrainingStatus, existing: TrainingDay? = null) {
    val uid = requireUserId()
    require(AwakenDateUtils.isValidDateKey(dateKey)) { "Invalid training date key: $dateKey" }
    val docRef = userDoc(uid).collection("training_days").document(dateKey)
    try {
      val snap = if (existing == null) docRef.get().await() else null
      val existingCreatedAt = existing?.createdAt ?: snap?.getTimestamp("createdAt")
      val payload = if (existingCreatedAt != null) {
        mapOf(
          "ownerId" to uid,
          "trainingDate" to dateKey,
          "status" to status.wireValue,
          "createdAt" to existingCreatedAt,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      } else {
        mapOf(
          "ownerId" to uid,
          "trainingDate" to dateKey,
          "status" to status.wireValue,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      }
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun deleteTrainingDay(dateKey: String) {
    val uid = requireUserId()
    require(AwakenDateUtils.isValidDateKey(dateKey)) { "Invalid training date key: $dateKey" }
    val docRef = userDoc(uid).collection("training_days").document(dateKey)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- GOALS CRUD ---

  suspend fun createGoal(goal: GoalItem): String {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("goals").document()
    return try {
      docRef.set(goal.toCreateMap(uid)).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateGoal(goal: GoalItem) {
    val uid = requireUserId()
    require(goal.id.isNotBlank()) { "Goal ID must not be blank for update" }
    val docRef = userDoc(uid).collection("goals").document(goal.id)
    try {
      docRef.set(goal.toUpdateMap(uid)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun updateGoalStatus(goal: GoalItem, newStatus: GoalStatus) {
    updateGoal(goal.copy(status = newStatus.wireValue))
  }

  suspend fun deleteGoal(goalId: String) {
    val uid = requireUserId()
    require(goalId.isNotBlank()) { "Goal ID must not be blank for delete" }
    val docRef = userDoc(uid).collection("goals").document(goalId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- FINANCIAL TRANSACTIONS CRUD ---

  suspend fun createTransaction(tx: FinancialTransaction): String {
    val uid = requireUserId()
    require(tx.amount > 0.0) { "Amount must be greater than zero" }
    val docRef = userDoc(uid).collection("financial_transactions").document()
    return try {
      docRef.set(tx.toCreateMap(uid)).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateTransaction(tx: FinancialTransaction) {
    val uid = requireUserId()
    require(tx.id.isNotBlank()) { "Transaction ID must not be blank for update" }
    require(tx.amount > 0.0) { "Amount must be greater than zero" }
    val docRef = userDoc(uid).collection("financial_transactions").document(tx.id)
    try {
      docRef.set(tx.toUpdateMap(uid)).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteTransaction(txId: String) {
    val uid = requireUserId()
    require(txId.isNotBlank()) { "Transaction ID must not be blank for delete" }
    val docRef = userDoc(uid).collection("financial_transactions").document(txId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }
}
