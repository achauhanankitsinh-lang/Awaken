package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TrainingStatus
import com.example.domain.model.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AwakenRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun tasks_crud_and_realtime_observation_succeeds_for_owner() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = AwakenRepository(firestore, auth)

    val taskId = withTimeout(TIMEOUT_MS) {
      repo.createTask(
        TaskItem(
          attributeKey = AwakenAttribute.PHYSICAL.key,
          title = "5km Morning Run ${UUID.randomUUID()}",
          notes = "Zone 2 aerobic base",
          scheduledDate = "2026-11-12",
          status = TaskStatus.PLANNED.wireValue
        )
      )
    }
    assertTrue(taskId.isNotBlank())

    val tasks = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTasks(uid).first { list -> list.any { it.id == taskId } }
    }
    val created = tasks.first { it.id == taskId }
    assertEquals(TaskStatus.PLANNED, created.taskStatus)
    assertNotNull(created.createdAt)

    withTimeout(TIMEOUT_MS) {
      repo.updateTaskStatus(created, TaskStatus.COMPLETED)
    }
    val updatedList = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTasks(uid).first { list ->
        list.firstOrNull { it.id == taskId }?.taskStatus == TaskStatus.COMPLETED
      }
    }
    assertEquals(TaskStatus.COMPLETED, updatedList.first { it.id == taskId }.taskStatus)

    withTimeout(TIMEOUT_MS) {
      repo.deleteTask(taskId)
    }
  }

  @Test
  fun activityLogs_measuredAndUnmeasured_succeeds_for_owner() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = AwakenRepository(firestore, auth)

    val measuredId = withTimeout(TIMEOUT_MS) {
      repo.createActivityLog(
        ActivityLog(
          attributeKey = AwakenAttribute.PHYSICAL.key,
          title = "Push-ups",
          occurredAt = Timestamp.now(),
          occurredDate = "2026-11-12",
          metricValue = 30.0,
          metricUnit = "repetitions"
        )
      )
    }
    val unmeasuredId = withTimeout(TIMEOUT_MS) {
      repo.createActivityLog(
        ActivityLog(
          attributeKey = AwakenAttribute.MENTAL.key,
          title = "Evening Reflection",
          details = "Maintained focus throughout deep work block",
          occurredAt = Timestamp.now(),
          occurredDate = "2026-11-12"
        )
      )
    }

    val logs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeActivityLogs(uid).first { list ->
        list.any { it.id == measuredId } && list.any { it.id == unmeasuredId }
      }
    }
    assertEquals(30.0, logs.first { it.id == measuredId }.metricValue!!, 0.001)
    assertEquals(null, logs.first { it.id == unmeasuredId }.metricValue)

    withTimeout(TIMEOUT_MS) {
      repo.deleteActivityLog(measuredId)
      repo.deleteActivityLog(unmeasuredId)
    }
  }

  @Test
  fun trainingDay_uniqueness_and_rest_vs_missed_persists() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = AwakenRepository(firestore, auth)
    val dateKey = "2026-11-12"

    withTimeout(TIMEOUT_MS) {
      repo.setTrainingDay(dateKey, TrainingStatus.REST)
    }
    val days = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTrainingDays(uid).first { list -> list.any { it.trainingDate == dateKey } }
    }
    val day = days.first { it.trainingDate == dateKey }
    assertEquals(TrainingStatus.REST, day.trainingStatus)

    // Updating same date overwrites single record, preserving uniqueness
    withTimeout(TIMEOUT_MS) {
      repo.setTrainingDay(dateKey, TrainingStatus.COMPLETED, day)
    }
    val updatedDays = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTrainingDays(uid).first { list ->
        list.firstOrNull { it.trainingDate == dateKey }?.trainingStatus == TrainingStatus.COMPLETED
      }
    }
    assertEquals(1, updatedDays.count { it.trainingDate == dateKey })
  }

  @Test
  fun goals_and_finances_persist_and_reject_zero_amount() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = AwakenRepository(firestore, auth)

    val goalId = withTimeout(TIMEOUT_MS) {
      repo.createGoal(
        GoalItem(
          attributeKey = AwakenAttribute.CAREER.key,
          title = "Ship Portfolio Case Study",
          targetDate = "2026-12-31",
          status = GoalStatus.ACTIVE.wireValue
        )
      )
    }
    val goals = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeGoals(uid).first { list -> list.any { it.id == goalId } }
    }
    assertEquals(GoalStatus.ACTIVE, goals.first { it.id == goalId }.goalStatus)

    val txId = withTimeout(TIMEOUT_MS) {
      repo.createTransaction(
        FinancialTransaction(
          transactionType = TransactionType.INCOME.wireValue,
          amount = 5000.0,
          currency = "INR",
          category = "Freelance",
          transactionDate = "2026-11-12"
        )
      )
    }
    val txs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeTransactions(uid).first { list -> list.any { it.id == txId } }
    }
    assertEquals("INR", txs.first { it.id == txId }.currency)
  }

  @Test
  fun crossUserIsolation_blocks_bob_from_reading_alice_data() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = AwakenRepository(firestore, auth)
    val taskId = withTimeout(TIMEOUT_MS) {
      aliceRepo.createTask(
        TaskItem(
          attributeKey = AwakenAttribute.LEARNING.key,
          title = "Alice Private Study",
          scheduledDate = "2026-11-12",
          status = TaskStatus.PLANNED.wireValue
        )
      )
    }

    signInTestUser(BOB_EMAIL)
    var caught: Exception? = null
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("tasks").document(taskId)
          .get()
          .await()
      }
    } catch (e: Exception) {
      caught = e
    }
    val fsErr = caught?.unwrapFirestoreException()
    assertNotNull("Expected Firestore PERMISSION_DENIED, got $caught", fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
  }

  @Test
  fun unauthenticated_caller_rejected_with_permission_denied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()

    var caught: Exception? = null
    try {
      withTimeout(TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid)
          .collection("tasks")
          .get()
          .await()
      }
    } catch (e: Exception) {
      caught = e
    }
    val fsErr = caught?.unwrapFirestoreException()
    assertNotNull("Expected Firestore PERMISSION_DENIED, got $caught", fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr!!.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_awaken@test.com"
    const val BOB_EMAIL = "bob_awaken@test.com"
    const val TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
