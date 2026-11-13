package com.example.domain.calc

import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TrainingDay
import com.example.domain.model.TrainingStatus
import com.example.domain.model.TransactionType
import com.example.domain.util.AwakenDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AwakenCalculationsUnitTest {

  @Test
  fun dateRange_weekStartsOnMonday_inIst() {
    // 2026-11-12 is Thursday; Monday is 2026-11-09, Sunday is 2026-11-15
    val weekRange = AwakenDateUtils.dateRangeForPeriod("2026-11-12", ReportPeriod.WEEKLY)
    assertEquals("2026-11-09", weekRange.startDate)
    assertEquals("2026-11-15", weekRange.endDate)

    // 2026-11-15 is Sunday; Monday is still 2026-11-09
    val sundayRange = AwakenDateUtils.dateRangeForPeriod("2026-11-15", ReportPeriod.WEEKLY)
    assertEquals("2026-11-09", sundayRange.startDate)
    assertEquals("2026-11-15", sundayRange.endDate)

    val monthRange = AwakenDateUtils.dateRangeForPeriod("2026-11-12", ReportPeriod.MONTHLY)
    assertEquals("2026-11-01", monthRange.startDate)
    assertEquals("2026-11-30", monthRange.endDate)
  }

  @Test
  fun taskSummary_excludesCancelledAndFutureDatedTasksInCurrentPeriod() {
    val today = "2026-11-12"
    val tasks = listOf(
      TaskItem(id = "1", attributeKey = "physical", title = "Run", scheduledDate = "2026-11-10", status = TaskStatus.COMPLETED.wireValue),
      TaskItem(id = "2", attributeKey = "mental", title = "Meditate", scheduledDate = "2026-11-12", status = TaskStatus.MISSED.wireValue),
      TaskItem(id = "3", attributeKey = "learning", title = "Read", scheduledDate = "2026-11-12", status = TaskStatus.CANCELLED.wireValue),
      // Future task inside current week (Friday 2026-11-13) must be excluded from completion-rate denominator
      TaskItem(id = "4", attributeKey = "career", title = "Ship PR", scheduledDate = "2026-11-13", status = TaskStatus.PLANNED.wireValue)
    )
    val summary = AwakenCalculations.calculateTaskSummary(
      tasks = tasks,
      startDate = "2026-11-09",
      endDate = "2026-11-15",
      todayIst = today
    )
    assertEquals(1, summary.completed)
    assertEquals(1, summary.missed)
    assertEquals(1, summary.cancelled)
    assertEquals(1, summary.planned)
    // Due non-cancelled = id 1 (completed) + id 2 (missed) = 2 -> completionRate = 0.5
    assertEquals(2, summary.dueNonCancelledCount)
    assertEquals(0.5, summary.completionRate!!, 0.0001)
  }

  @Test
  fun activitySummary_aggregatesOnlyMatchingUnits_andPreservesUnmeasured() {
    val logs = listOf(
      ActivityLog(id = "1", attributeKey = "physical", title = "Run", occurredDate = "2026-11-12", metricValue = 3.5, metricUnit = "km"),
      ActivityLog(id = "2", attributeKey = "physical", title = "Evening Run", occurredDate = "2026-11-12", metricValue = 2.5, metricUnit = "KM "),
      ActivityLog(id = "3", attributeKey = "physical", title = "Push-ups", occurredDate = "2026-11-12", metricValue = 40.0, metricUnit = "repetitions"),
      ActivityLog(id = "4", attributeKey = "mental", title = "Reflection", occurredDate = "2026-11-12", metricValue = null, metricUnit = null)
    )
    val summary = AwakenCalculations.calculateActivitySummary(logs, "2026-11-12", "2026-11-12")
    assertEquals(4, summary.totalSessions)
    assertEquals(3, summary.measuredSessions)
    assertEquals(1, summary.unmeasuredSessions)
    val kmTotal = summary.unitTotals.first { it.unit == "km" }
    val repTotal = summary.unitTotals.first { it.unit == "repetitions" }
    assertEquals(6.0, kmTotal.total, 0.0001)
    assertEquals(40.0, repTotal.total, 0.0001)
  }

  @Test
  fun trainingSummary_separatesRestFromMissed() {
    val days = listOf(
      TrainingDay(trainingDate = "2026-11-09", status = TrainingStatus.COMPLETED.wireValue),
      TrainingDay(trainingDate = "2026-11-10", status = TrainingStatus.REST.wireValue),
      TrainingDay(trainingDate = "2026-11-11", status = TrainingStatus.MISSED.wireValue),
      TrainingDay(trainingDate = "2026-11-12", status = TrainingStatus.PLANNED.wireValue)
    )
    val summary = AwakenCalculations.calculateTrainingSummary(days, "2026-11-09", "2026-11-15")
    assertEquals(1, summary.completed)
    assertEquals(1, summary.rest)
    assertEquals(1, summary.missed)
    assertEquals(1, summary.planned)
  }

  @Test
  fun financialSummary_calculatesInrNetBalanceAndFormatsRupees() {
    val txs = listOf(
      FinancialTransaction(transactionType = TransactionType.INCOME.wireValue, amount = 50000.0, currency = "INR", category = "Salary", transactionDate = "2026-11-01"),
      FinancialTransaction(transactionType = TransactionType.EXPENSE.wireValue, amount = 12500.0, currency = "INR", category = "Rent", transactionDate = "2026-11-05")
    )
    val summary = AwakenCalculations.calculateFinancialSummary(txs, "2026-11-01", "2026-11-30")
    assertEquals(2, summary.transactionCount)
    assertEquals(50000.0, summary.totalIncome, 0.001)
    assertEquals(12500.0, summary.totalExpenses, 0.001)
    assertEquals(37500.0, summary.netBalance, 0.001)
    assertEquals("₹37,500.00", AwakenDateUtils.formatCurrency(summary.netBalance))
  }

  @Test
  fun goalSummary_countsStatusesWithoutInventedPercentages() {
    val goals = listOf(
      GoalItem(attributeKey = AwakenAttribute.CAREER.key, title = "G1", status = GoalStatus.ACTIVE.wireValue),
      GoalItem(attributeKey = AwakenAttribute.CAREER.key, title = "G2", status = GoalStatus.COMPLETED.wireValue),
      GoalItem(attributeKey = AwakenAttribute.CAREER.key, title = "G3", status = GoalStatus.CANCELLED.wireValue)
    )
    val summary = AwakenCalculations.calculateGoalSummary(goals)
    assertEquals(1, summary.active)
    assertEquals(1, summary.completed)
    assertEquals(1, summary.cancelled)
    assertEquals(3, summary.total)
  }

  @Test
  fun taskSummary_emptyPeriod_returnsNullCompletionRate() {
    val summary = AwakenCalculations.calculateTaskSummary(emptyList(), "2026-11-12", "2026-11-12", "2026-11-12")
    assertNull(summary.completionRate)
    assertEquals(0, summary.totalScheduled)
  }
}
