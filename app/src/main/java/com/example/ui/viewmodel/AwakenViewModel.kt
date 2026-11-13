package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.unwrapFirestoreRootMessage
import com.example.data.repository.AwakenRepository
import com.example.domain.model.ActivityLog
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.model.OwnerProfile
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TrainingDay
import com.example.domain.model.TrainingStatus
import com.example.domain.util.AwakenDateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

sealed interface OwnerGateState {
  data object Checking : OwnerGateState
  data object NeedsInitialClaim : OwnerGateState
  data class Authorized(val profile: OwnerProfile?) : OwnerGateState
  data object LockedToOtherOwner : OwnerGateState
  data class Error(val message: String) : OwnerGateState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AwakenViewModel(
  private val repository: AwakenRepository,
  val currentUserId: String,
  val userDisplayName: String,
  val userEmail: String?
) : ViewModel() {

  private val _gateState = MutableStateFlow<OwnerGateState>(OwnerGateState.Checking)
  val gateState: StateFlow<OwnerGateState> = _gateState.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _feedbackMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val feedbackMessages = _feedbackMessages.asSharedFlow()

  private val isAuthorizedFlow = _gateState.map { it is OwnerGateState.Authorized }

  val tasksState: StateFlow<UiState<List<TaskItem>>> = isAuthorizedFlow
    .flatMapLatest { authorized ->
      if (!authorized) {
        flowOf(UiState.Loading)
      } else {
        repository.observeTasks(currentUserId)
          .map<List<TaskItem>, UiState<List<TaskItem>>> { UiState.Success(it) }
          .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val msg = unwrapFirestoreRootMessage(error, "Failed to load tasks")
            Log.w(TAG, "observeTasks error: $msg", error)
            emit(UiState.Error(msg))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val activitiesState: StateFlow<UiState<List<ActivityLog>>> = isAuthorizedFlow
    .flatMapLatest { authorized ->
      if (!authorized) {
        flowOf(UiState.Loading)
      } else {
        repository.observeActivityLogs(currentUserId)
          .map<List<ActivityLog>, UiState<List<ActivityLog>>> { UiState.Success(it) }
          .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val msg = unwrapFirestoreRootMessage(error, "Failed to load activities")
            Log.w(TAG, "observeActivityLogs error: $msg", error)
            emit(UiState.Error(msg))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val trainingDaysState: StateFlow<UiState<List<TrainingDay>>> = isAuthorizedFlow
    .flatMapLatest { authorized ->
      if (!authorized) {
        flowOf(UiState.Loading)
      } else {
        repository.observeTrainingDays(currentUserId)
          .map<List<TrainingDay>, UiState<List<TrainingDay>>> { UiState.Success(it) }
          .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val msg = unwrapFirestoreRootMessage(error, "Failed to load training calendar")
            Log.w(TAG, "observeTrainingDays error: $msg", error)
            emit(UiState.Error(msg))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val goalsState: StateFlow<UiState<List<GoalItem>>> = isAuthorizedFlow
    .flatMapLatest { authorized ->
      if (!authorized) {
        flowOf(UiState.Loading)
      } else {
        repository.observeGoals(currentUserId)
          .map<List<GoalItem>, UiState<List<GoalItem>>> { UiState.Success(it) }
          .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val msg = unwrapFirestoreRootMessage(error, "Failed to load goals")
            Log.w(TAG, "observeGoals error: $msg", error)
            emit(UiState.Error(msg))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val transactionsState: StateFlow<UiState<List<FinancialTransaction>>> = isAuthorizedFlow
    .flatMapLatest { authorized ->
      if (!authorized) {
        flowOf(UiState.Loading)
      } else {
        repository.observeTransactions(currentUserId)
          .map<List<FinancialTransaction>, UiState<List<FinancialTransaction>>> { UiState.Success(it) }
          .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val msg = unwrapFirestoreRootMessage(error, "Failed to load transactions")
            Log.w(TAG, "observeTransactions error: $msg", error)
            emit(UiState.Error(msg))
          }
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  init {
    verifyOwnerAccess()
  }

  fun verifyOwnerAccess() {
    viewModelScope.launch {
      _gateState.value = OwnerGateState.Checking
      try {
        val lock = repository.getOwnerLock()
        when {
          lock == null -> _gateState.value = OwnerGateState.NeedsInitialClaim
          lock.ownerUid == currentUserId -> {
            val profile = repository.ensureOwnerProfile(userDisplayName, userEmail)
            _gateState.value = OwnerGateState.Authorized(profile)
          }
          else -> _gateState.value = OwnerGateState.LockedToOtherOwner
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _gateState.value = OwnerGateState.Error(
          unwrapFirestoreRootMessage(e, "Failed to verify workspace ownership.")
        )
      }
    }
  }

  fun claimOwnerWorkspace() {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        repository.claimOwnerWorkspace(userDisplayName, userEmail)
        val profile = repository.ensureOwnerProfile(userDisplayName, userEmail)
        _gateState.value = OwnerGateState.Authorized(profile)
        _feedbackMessages.tryEmit("Owner workspace initialized. Public registration closed.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val msg = unwrapFirestoreRootMessage(e, "Unable to initialize owner workspace.")
        _gateState.value = OwnerGateState.Error(msg)
        _feedbackMessages.tryEmit(msg)
      } finally {
        _isSaving.value = false
      }
    }
  }

  // --- TASK MUTATIONS ---

  fun saveTask(existing: TaskItem?, task: TaskItem, onComplete: () -> Unit = {}) {
    if (task.title.isBlank()) {
      _feedbackMessages.tryEmit("Task title is required.")
      return
    }
    if (!AwakenDateUtils.isValidDateKey(task.scheduledDate)) {
      _feedbackMessages.tryEmit("Enter a valid YYYY-MM-DD date.")
      return
    }
    launchSave(
      successMessage = if (existing == null) "Task created." else "Task updated.",
      onComplete = onComplete
    ) {
      if (existing == null) {
        repository.createTask(task)
      } else {
        repository.updateTask(
          existing.copy(
            attributeKey = task.attributeKey,
            title = task.title,
            notes = task.notes,
            scheduledDate = task.scheduledDate,
            status = task.status
          )
        )
      }
    }
  }

  fun updateTaskStatus(task: TaskItem, status: TaskStatus) {
    launchSave(successMessage = "Task marked ${status.label}.") {
      repository.updateTaskStatus(task, status)
    }
  }

  fun deleteTask(taskId: String) {
    launchSave(successMessage = "Task deleted.") {
      repository.deleteTask(taskId)
    }
  }

  // --- ACTIVITY LOG MUTATIONS ---

  fun saveActivityLog(existing: ActivityLog?, log: ActivityLog, onComplete: () -> Unit = {}) {
    if (log.title.isBlank()) {
      _feedbackMessages.tryEmit("Activity title is required.")
      return
    }
    if (!AwakenDateUtils.isValidDateKey(log.occurredDate)) {
      _feedbackMessages.tryEmit("Enter a valid YYYY-MM-DD date.")
      return
    }
    if (log.metricValue != null && log.metricValue < 0.0) {
      _feedbackMessages.tryEmit("Metric value cannot be negative.")
      return
    }
    if (log.metricValue != null && log.metricUnit.isNullOrBlank()) {
      _feedbackMessages.tryEmit("Metric unit is required when entering a measurement.")
      return
    }
    launchSave(
      successMessage = if (existing == null) "Activity logged." else "Activity updated.",
      onComplete = onComplete
    ) {
      if (existing == null) {
        repository.createActivityLog(log)
      } else {
        repository.updateActivityLog(
          existing.copy(
            attributeKey = log.attributeKey,
            title = log.title,
            details = log.details,
            occurredDate = log.occurredDate,
            occurredAt = log.occurredAt ?: existing.occurredAt,
            metricValue = log.metricValue,
            metricUnit = log.metricUnit,
            categoryTag = log.categoryTag
          )
        )
      }
    }
  }

  fun deleteActivityLog(logId: String) {
    launchSave(successMessage = "Activity log deleted.") {
      repository.deleteActivityLog(logId)
    }
  }

  // --- TRAINING DAY MUTATIONS ---

  fun setTrainingDayStatus(dateKey: String, status: TrainingStatus, existing: TrainingDay?) {
    launchSave(successMessage = "${AwakenDateUtils.formatShortDate(dateKey)} set to ${status.label}.") {
      repository.setTrainingDay(dateKey, status, existing)
    }
  }

  fun clearTrainingDay(dateKey: String) {
    launchSave(successMessage = "Training status cleared for ${AwakenDateUtils.formatShortDate(dateKey)}.") {
      repository.deleteTrainingDay(dateKey)
    }
  }

  // --- GOAL MUTATIONS ---

  fun saveGoal(existing: GoalItem?, goal: GoalItem, onComplete: () -> Unit = {}) {
    if (goal.title.isBlank()) {
      _feedbackMessages.tryEmit("Goal title is required.")
      return
    }
    if (!goal.targetDate.isNullOrBlank() && !AwakenDateUtils.isValidDateKey(goal.targetDate)) {
      _feedbackMessages.tryEmit("Target date must be YYYY-MM-DD.")
      return
    }
    launchSave(
      successMessage = if (existing == null) "Goal created." else "Goal updated.",
      onComplete = onComplete
    ) {
      if (existing == null) {
        repository.createGoal(goal)
      } else {
        repository.updateGoal(
          existing.copy(
            attributeKey = goal.attributeKey,
            title = goal.title,
            description = goal.description,
            targetDate = goal.targetDate,
            status = goal.status
          )
        )
      }
    }
  }

  fun updateGoalStatus(goal: GoalItem, status: GoalStatus) {
    launchSave(successMessage = "Goal marked ${status.label}.") {
      repository.updateGoalStatus(goal, status)
    }
  }

  fun deleteGoal(goalId: String) {
    launchSave(successMessage = "Goal deleted.") {
      repository.deleteGoal(goalId)
    }
  }

  // --- FINANCIAL TRANSACTION MUTATIONS ---

  fun saveTransaction(
    existing: FinancialTransaction?,
    tx: FinancialTransaction,
    onComplete: () -> Unit = {}
  ) {
    if (tx.amount <= 0.0) {
      _feedbackMessages.tryEmit("Amount must be greater than zero.")
      return
    }
    if (tx.category.isBlank()) {
      _feedbackMessages.tryEmit("Category is required.")
      return
    }
    if (!AwakenDateUtils.isValidDateKey(tx.transactionDate)) {
      _feedbackMessages.tryEmit("Transaction date must be YYYY-MM-DD.")
      return
    }
    val normalized = tx.copy(currency = "INR")
    launchSave(
      successMessage = if (existing == null) "Transaction recorded." else "Transaction updated.",
      onComplete = onComplete
    ) {
      if (existing == null) {
        repository.createTransaction(normalized)
      } else {
        repository.updateTransaction(
          existing.copy(
            transactionType = normalized.transactionType,
            amount = normalized.amount,
            currency = "INR",
            category = normalized.category,
            description = normalized.description,
            transactionDate = normalized.transactionDate
          )
        )
      }
    }
  }

  fun deleteTransaction(txId: String) {
    launchSave(successMessage = "Transaction deleted.") {
      repository.deleteTransaction(txId)
    }
  }

  private fun launchSave(
    successMessage: String,
    onComplete: () -> Unit = {},
    block: suspend () -> Unit
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      try {
        block()
        _feedbackMessages.tryEmit(successMessage)
        onComplete()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _feedbackMessages.tryEmit(unwrapFirestoreRootMessage(e, "Operation failed."))
      } finally {
        _isSaving.value = false
      }
    }
  }

  private companion object {
    const val TAG = "AwakenViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
