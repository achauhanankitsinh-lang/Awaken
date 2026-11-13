package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TaskItem
import com.example.domain.model.TrainingDay
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AwakenCard
import com.example.ui.components.IncomeExpenseChart
import com.example.ui.components.SectionHeader
import com.example.ui.components.WeeklyConsistencyChart
import com.example.ui.components.attributeIcon
import com.example.ui.theme.AwakenThemeTokens
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
  tasks: List<TaskItem>,
  activities: List<ActivityLog>,
  trainingDays: List<TrainingDay>,
  goals: List<GoalItem>,
  transactions: List<FinancialTransaction>,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var selectedPeriod by remember { mutableStateOf(ReportPeriod.WEEKLY) }
  var anchorDate by remember { mutableStateOf(AwakenDateUtils.todayIst()) }

  val todayIst = remember { AwakenDateUtils.todayIst() }
  val range = remember(anchorDate, selectedPeriod) {
    AwakenDateUtils.dateRangeForPeriod(anchorDate, selectedPeriod)
  }

  val taskSummary = remember(tasks, range, todayIst) {
    AwakenCalculations.calculateTaskSummary(tasks, range.startDate, range.endDate, todayIst)
  }
  val trainingSummary = remember(trainingDays, range) {
    AwakenCalculations.calculateTrainingSummary(trainingDays, range.startDate, range.endDate)
  }
  val goalSummary = remember(goals) { AwakenCalculations.calculateGoalSummary(goals) }
  val financialSummary = remember(transactions, range) {
    AwakenCalculations.calculateFinancialSummary(transactions, range.startDate, range.endDate)
  }
  val weeklyTrend = remember(tasks, activities, anchorDate) {
    AwakenCalculations.buildWeeklyTrend(tasks, activities, anchorDate)
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "Progress Reports",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary
      )
    }

    item {
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ReportPeriod.entries.forEach { period ->
            FilterChip(
              selected = selectedPeriod == period,
              onClick = { selectedPeriod = period },
              label = { Text(period.label) },
              modifier = Modifier.weight(1f),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright
              )
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = {
            anchorDate = when (selectedPeriod) {
              ReportPeriod.DAILY -> AwakenDateUtils.shiftDays(anchorDate, -1)
              ReportPeriod.WEEKLY -> AwakenDateUtils.shiftDays(anchorDate, -7)
              ReportPeriod.MONTHLY -> "${AwakenDateUtils.shiftMonths(anchorDate.take(7), -1)}-01"
            }
          }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous", tint = colors.textPrimary)
          }
          Text(
            text = when (selectedPeriod) {
              ReportPeriod.DAILY -> AwakenDateUtils.formatDisplayDate(anchorDate)
              ReportPeriod.WEEKLY -> "${AwakenDateUtils.formatShortDate(range.startDate)} – ${AwakenDateUtils.formatShortDate(range.endDate)}"
              ReportPeriod.MONTHLY -> AwakenDateUtils.formatMonthTitle(anchorDate.take(7))
            },
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
          )
          Row {
            if (anchorDate != todayIst) {
              TextButton(onClick = { anchorDate = todayIst }) {
                Text("Today", color = colors.accentBright)
              }
            }
            IconButton(onClick = {
              anchorDate = when (selectedPeriod) {
                ReportPeriod.DAILY -> AwakenDateUtils.shiftDays(anchorDate, 1)
                ReportPeriod.WEEKLY -> AwakenDateUtils.shiftDays(anchorDate, 7)
                ReportPeriod.MONTHLY -> "${AwakenDateUtils.shiftMonths(anchorDate.take(7), 1)}-01"
              }
            }) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next", tint = colors.textPrimary)
            }
          }
        }
      }
    }

    item {
      SectionHeader(title = "Tasks")
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Completion Rate", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
          Text(
            text = taskSummary.completionRate?.let {
              "${(it * 100).roundToInt()}% (${taskSummary.completed}/${taskSummary.dueNonCancelledCount})"
            } ?: "—",
            style = MaterialTheme.typography.titleMedium,
            color = colors.accentBright
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          ReportStat("Planned", taskSummary.planned, colors.statusPlanned)
          ReportStat("Active", taskSummary.inProgress, colors.statusInProgress)
          ReportStat("Done", taskSummary.completed, colors.statusCompleted)
          ReportStat("Missed", taskSummary.missed, colors.statusMissed)
          ReportStat("Cancelled", taskSummary.cancelled, colors.textSecondary)
        }
      }
    }

    if (selectedPeriod == ReportPeriod.WEEKLY) {
      item {
        AwakenCard {
          Text(
            text = "Weekly Rhythm",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
          )
          Spacer(modifier = Modifier.height(10.dp))
          WeeklyConsistencyChart(points = weeklyTrend)
        }
      }
    }

    item {
      SectionHeader(title = "Training")
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          ReportStat("Completed", trainingSummary.completed, colors.statusCompleted)
          ReportStat("Rest", trainingSummary.rest, colors.statusRest)
          ReportStat("Planned", trainingSummary.planned, colors.statusPlanned)
          ReportStat("Missed", trainingSummary.missed, colors.statusMissed)
        }
      }
    }

    item {
      SectionHeader(title = "Goals")
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          ReportStat("Active", goalSummary.active, colors.statusInProgress)
          ReportStat("Completed", goalSummary.completed, colors.statusCompleted)
          ReportStat("Cancelled", goalSummary.cancelled, colors.textSecondary)
        }
      }
    }

    item {
      SectionHeader(title = "Finances (${selectedPeriod.label})")
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard {
        IncomeExpenseChart(summary = financialSummary)
      }
    }

    item {
      SectionHeader(title = "By Attribute")
    }
    items(AwakenAttribute.entries, key = { it.key }) { attr ->
      val attrLogs = remember(activities, attr, range) {
        AwakenCalculations.filterLogsByAttribute(activities, attr)
          .filter { it.occurredDate in range.startDate..range.endDate }
      }
      val attrTasks = remember(tasks, attr) { AwakenCalculations.filterTasksByAttribute(tasks, attr) }
      val attrTaskSummary = remember(attrTasks, range, todayIst) {
        AwakenCalculations.calculateTaskSummary(attrTasks, range.startDate, range.endDate, todayIst)
      }
      val attrActivitySummary = remember(attrLogs) {
        AwakenCalculations.calculateActivitySummary(attrLogs)
      }

      AwakenCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = attributeIcon(attr),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = attr.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Tasks: ${attrTaskSummary.completed}/${attrTaskSummary.dueNonCancelledCount} · Logs: ${attrActivitySummary.totalSessions}",
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary
        )
        if (attrActivitySummary.unitTotals.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          attrActivitySummary.unitTotals.forEach { ut ->
            Text(
              text = "• ${AwakenDateUtils.formatMetric(ut.total)} ${ut.unit} (${ut.sessionCount})",
              style = MaterialTheme.typography.labelMedium,
              color = colors.accentBright
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ReportStat(
  label: String,
  value: Int,
  color: Color
) {
  val colors = AwakenThemeTokens.colors
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value.toString(),
      style = MaterialTheme.typography.titleLarge,
      color = color
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = colors.textSecondary
    )
  }
}
