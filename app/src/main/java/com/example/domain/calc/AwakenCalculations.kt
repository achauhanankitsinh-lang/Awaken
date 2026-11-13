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

data class TaskSummary(
  val planned: Int = 0,
  val inProgress: Int = 0,
  val completed: Int = 0,
  val cancelled: Int = 0,
  val missed: Int = 0,
  val dueNonCancelledCount: Int = 0,
  val completionRate: Double? = null
) {
  val totalScheduled: Int get() = planned + inProgress + completed + cancelled + missed
}

data class ActivityUnitTotal(
  val unit: String,
  val total: Double,
  val sessionCount: Int
)

data class ActivitySummary(
  val totalSessions: Int = 0,
  val measuredSessions: Int = 0,
  val unmeasuredSessions: Int = 0,
  val unitTotals: List<ActivityUnitTotal> = emptyList()
)

data class TrainingSummary(
  val planned: Int = 0,
  val rest: Int = 0,
  val completed: Int = 0,
  val missed: Int = 0
) {
  val totalMarked: Int get() = planned + rest + completed + missed
}

data class GoalSummary(
  val active: Int = 0,
  val completed: Int = 0,
  val cancelled: Int = 0
) {
  val total: Int get() = active + completed + cancelled
}

data class FinancialSummary(
  val totalIncome: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val netBalance: Double = 0.0,
  val transactionCount: Int = 0
)

data class DailyTrendPoint(
  val dateKey: String,
  val completedTasks: Int,
  val dueTasks: Int,
  val activityCount: Int
)

object AwakenCalculations {

  /**
   * Task reporting rules:
   * - Only completed tasks count as completed.
   * - Cancelled tasks never count as completed and are excluded from completion-rate denominators.
   * - Missed tasks are not completed.
   * - For the current period (where todayIst falls in [startDate, endDate]), exclude future-dated tasks
   *   from due completion-rate denominators.
   */
  fun calculateTaskSummary(
    tasks: List<TaskItem>,
    startDate: String,
    endDate: String,
    todayIst: String = AwakenDateUtils.todayIst()
  ): TaskSummary {
    val inPeriod = tasks.filter { it.scheduledDate in startDate..endDate }
    val isCurrentPeriod = todayIst in startDate..endDate
    val dueTasks = if (isCurrentPeriod) {
      inPeriod.filter { it.scheduledDate <= todayIst }
    } else {
      inPeriod
    }

    val planned = inPeriod.count { it.taskStatus == TaskStatus.PLANNED }
    val inProgress = inPeriod.count { it.taskStatus == TaskStatus.IN_PROGRESS }
    val completed = dueTasks.count { it.taskStatus == TaskStatus.COMPLETED }
    val cancelled = inPeriod.count { it.taskStatus == TaskStatus.CANCELLED }
    val missed = inPeriod.count { it.taskStatus == TaskStatus.MISSED }

    val dueNonCancelled = dueTasks.count { it.taskStatus != TaskStatus.CANCELLED }
    val rate = if (dueNonCancelled > 0) completed.toDouble() / dueNonCancelled.toDouble() else null

    return TaskSummary(
      planned = planned,
      inProgress = inProgress,
      completed = completed,
      cancelled = cancelled,
      missed = missed,
      dueNonCancelledCount = dueNonCancelled,
      completionRate = rate
    )
  }

  /**
   * Activity reporting rules:
   * - Count actual activity records.
   * - Aggregate metrics ONLY when units match (case-insensitive trimmed).
   * - Missing metric means unmeasured, not zero.
   */
  fun calculateActivitySummary(
    logs: List<ActivityLog>,
    startDate: String? = null,
    endDate: String? = null
  ): ActivitySummary {
    val filtered = if (startDate != null && endDate != null) {
      logs.filter { it.occurredDate in startDate..endDate }
    } else {
      logs
    }

    var measured = 0
    var unmeasured = 0
    val totalsByUnit = linkedMapOf<String, Pair<Double, Int>>()

    for (log in filtered) {
      val value = log.metricValue
      val unit = log.metricUnit?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
      if (value != null && unit != null) {
        measured++
        val current = totalsByUnit[unit] ?: (0.0 to 0)
        totalsByUnit[unit] = (current.first + value) to (current.second + 1)
      } else {
        unmeasured++
      }
    }

    val unitTotals = totalsByUnit.entries
      .sortedByDescending { it.value.second }
      .map { (unit, pair) ->
        ActivityUnitTotal(unit = unit, total = pair.first, sessionCount = pair.second)
      }

    return ActivitySummary(
      totalSessions = filtered.size,
      measuredSessions = measured,
      unmeasuredSessions = unmeasured,
      unitTotals = unitTotals
    )
  }

  /**
   * Training reporting rules:
   * - Planned, rest, completed, and missed shown separately.
   * - Rest day is NOT a missed day.
   */
  fun calculateTrainingSummary(
    trainingDays: List<TrainingDay>,
    startDate: String? = null,
    endDate: String? = null
  ): TrainingSummary {
    val filtered = if (startDate != null && endDate != null) {
      trainingDays.filter { it.trainingDate in startDate..endDate }
    } else {
      trainingDays
    }
    return TrainingSummary(
      planned = filtered.count { it.trainingStatus == TrainingStatus.PLANNED },
      rest = filtered.count { it.trainingStatus == TrainingStatus.REST },
      completed = filtered.count { it.trainingStatus == TrainingStatus.COMPLETED },
      missed = filtered.count { it.trainingStatus == TrainingStatus.MISSED }
    )
  }

  fun calculateGoalSummary(goals: List<GoalItem>): GoalSummary {
    return GoalSummary(
      active = goals.count { it.goalStatus == GoalStatus.ACTIVE },
      completed = goals.count { it.goalStatus == GoalStatus.COMPLETED },
      cancelled = goals.count { it.goalStatus == GoalStatus.CANCELLED }
    )
  }

  /**
   * Financial reporting rules (INR):
   * - Net balance = Total income - Total expenses.
   */
  fun calculateFinancialSummary(
    transactions: List<FinancialTransaction>,
    startDate: String? = null,
    endDate: String? = null
  ): FinancialSummary {
    val filtered = if (startDate != null && endDate != null) {
      transactions.filter { it.transactionDate in startDate..endDate }
    } else {
      transactions
    }
    val income = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val expenses = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    return FinancialSummary(
      totalIncome = income,
      totalExpenses = expenses,
      netBalance = income - expenses,
      transactionCount = filtered.size
    )
  }

  fun buildWeeklyTrend(
    tasks: List<TaskItem>,
    logs: List<ActivityLog>,
    anchorDate: String
  ): List<DailyTrendPoint> {
    val range = AwakenDateUtils.dateRangeForPeriod(anchorDate, ReportPeriod.WEEKLY)
    val points = mutableListOf<DailyTrendPoint>()
    var cursor = range.startDate
    while (cursor <= range.endDate) {
      val dayTasks = tasks.filter { it.scheduledDate == cursor }
      val due = dayTasks.count { it.taskStatus != TaskStatus.CANCELLED }
      val completed = dayTasks.count { it.taskStatus == TaskStatus.COMPLETED }
      val dayLogs = logs.count { it.occurredDate == cursor }
      points.add(
        DailyTrendPoint(
          dateKey = cursor,
          completedTasks = completed,
          dueTasks = due,
          activityCount = dayLogs
        )
      )
      cursor = AwakenDateUtils.shiftDays(cursor, 1)
    }
    return points
  }

  fun filterTasksByAttribute(tasks: List<TaskItem>, attribute: AwakenAttribute): List<TaskItem> {
    return tasks.filter { it.attributeKey == attribute.key }
  }

  fun filterLogsByAttribute(logs: List<ActivityLog>, attribute: AwakenAttribute): List<ActivityLog> {
    return logs.filter { it.attributeKey == attribute.key }
  }

  fun filterGoalsByAttribute(goals: List<GoalItem>, attribute: AwakenAttribute): List<GoalItem> {
    return goals.filter { it.attributeKey == attribute.key }
  }
}
