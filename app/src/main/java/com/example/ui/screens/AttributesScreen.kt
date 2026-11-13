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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.AwakenLevel
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AwakenCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.attributeIcon
import com.example.ui.components.goalStatusColors
import com.example.ui.components.taskStatusColors
import com.example.ui.theme.AwakenThemeTokens

@Composable
fun AttributesScreen(
  tasks: List<TaskItem>,
  activities: List<ActivityLog>,
  goals: List<GoalItem>,
  onSelectAttribute: (AwakenAttribute) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  val level1 = remember { AwakenAttribute.entries.filter { it.level == AwakenLevel.CORE_DEVELOPMENT } }
  val level2 = remember { AwakenAttribute.entries.filter { it.level == AwakenLevel.LIFE_ADVANCEMENT } }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "Attributes",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary
      )
    }

    item {
      SectionHeader(title = "Level 01 — Core Development")
    }
    items(level1, key = { it.key }) { attr ->
      AttributeOverviewCard(
        attribute = attr,
        tasks = tasks,
        activities = activities,
        goals = goals,
        onClick = { onSelectAttribute(attr) }
      )
    }

    item {
      Spacer(modifier = Modifier.height(6.dp))
      SectionHeader(title = "Level 02 — Life Advancement")
    }
    items(level2, key = { it.key }) { attr ->
      AttributeOverviewCard(
        attribute = attr,
        tasks = tasks,
        activities = activities,
        goals = goals,
        onClick = { onSelectAttribute(attr) }
      )
    }
  }
}

@Composable
private fun AttributeOverviewCard(
  attribute: AwakenAttribute,
  tasks: List<TaskItem>,
  activities: List<ActivityLog>,
  goals: List<GoalItem>,
  onClick: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  val attrTasks = remember(tasks, attribute) { AwakenCalculations.filterTasksByAttribute(tasks, attribute) }
  val attrLogs = remember(activities, attribute) { AwakenCalculations.filterLogsByAttribute(activities, attribute) }
  val attrGoals = remember(goals, attribute) { AwakenCalculations.filterGoalsByAttribute(goals, attribute) }
  val activitySummary = remember(attrLogs) { AwakenCalculations.calculateActivitySummary(attrLogs) }

  AwakenCard(
    onClick = onClick,
    modifier = Modifier.testTag("attribute_card_${attribute.key}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Icon(
        imageVector = attributeIcon(attribute),
        contentDescription = null,
        tint = colors.accent,
        modifier = Modifier.size(22.dp)
      )
      Text(
        text = attribute.displayName,
        style = MaterialTheme.typography.titleLarge,
        color = colors.textPrimary
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = "${attrTasks.count { it.taskStatus == TaskStatus.COMPLETED }}/${attrTasks.count { it.taskStatus != TaskStatus.CANCELLED }} tasks",
        style = MaterialTheme.typography.labelMedium,
        color = colors.textPrimary
      )
      Text(
        text = "${activitySummary.totalSessions} logs",
        style = MaterialTheme.typography.labelMedium,
        color = colors.accentBright
      )
      Text(
        text = "${attrGoals.count { it.goalStatus == GoalStatus.ACTIVE }} goals",
        style = MaterialTheme.typography.labelMedium,
        color = colors.textSecondary
      )
    }

    if (activitySummary.unitTotals.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      val unitsText = activitySummary.unitTotals.take(3).joinToString(" · ") {
        "${AwakenDateUtils.formatMetric(it.total)} ${it.unit}"
      }
      Text(
        text = unitsText,
        style = MaterialTheme.typography.bodySmall,
        color = colors.accentBright
      )
    }
  }
}

@Composable
fun AttributeDetailScreen(
  attribute: AwakenAttribute,
  tasks: List<TaskItem>,
  activities: List<ActivityLog>,
  goals: List<GoalItem>,
  onBack: () -> Unit,
  onCreateTaskForAttribute: (AwakenAttribute) -> Unit,
  onLogActivityForAttribute: (AwakenAttribute) -> Unit,
  onToggleTaskStatus: (TaskItem, TaskStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  val attrTasks = remember(tasks, attribute) { AwakenCalculations.filterTasksByAttribute(tasks, attribute) }
  val attrLogs = remember(activities, attribute) { AwakenCalculations.filterLogsByAttribute(activities, attribute) }
  val attrGoals = remember(goals, attribute) { AwakenCalculations.filterGoalsByAttribute(goals, attribute) }
  val activitySummary = remember(attrLogs) { AwakenCalculations.calculateActivitySummary(attrLogs) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = colors.textPrimary
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = attributeIcon(attribute),
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "${attribute.level.code} · ${attribute.level.title}",
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent
          )
          Text(
            text = attribute.displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
          )
        }
      }
    }

    item {
      AwakenCard {
        Text(
          text = attribute.purpose,
          style = MaterialTheme.typography.bodyMedium,
          color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { onCreateTaskForAttribute(attribute) },
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = colors.onAccent
            ),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Task")
          }
          OutlinedButton(
            onClick = { onLogActivityForAttribute(attribute) },
            modifier = Modifier.weight(1f)
          ) {
            Text("Log Activity", color = colors.accentBright)
          }
        }
      }
    }

    item {
      SectionHeader(title = "Statistics (${activitySummary.totalSessions} sessions)")
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard {
        if (activitySummary.unitTotals.isEmpty()) {
          Text(
            text = if (activitySummary.unmeasuredSessions > 0) {
              "${activitySummary.unmeasuredSessions} unmeasured session(s)."
            } else {
              "No measured records yet."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activitySummary.unitTotals.forEach { ut ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${ut.unit} (${ut.sessionCount})",
                  style = MaterialTheme.typography.bodyMedium,
                  color = colors.textSecondary
                )
                Text(
                  text = "${AwakenDateUtils.formatMetric(ut.total)} ${ut.unit}",
                  style = MaterialTheme.typography.titleMedium,
                  color = colors.accentBright
                )
              }
            }
          }
        }
      }
    }

    item {
      SectionHeader(title = "Tasks (${attrTasks.size})")
    }
    if (attrTasks.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Outlined.CheckCircleOutline,
          title = "No ${attribute.displayName} tasks"
        )
      }
    } else {
      items(attrTasks.take(10), key = { it.id }) { task ->
        val (fg, bg) = taskStatusColors(task.taskStatus)
        AwakenCard(
          onClick = {
            val next = if (task.taskStatus == TaskStatus.COMPLETED) TaskStatus.PLANNED else TaskStatus.COMPLETED
            onToggleTaskStatus(task, next)
          }
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(task.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
              Text(
                AwakenDateUtils.formatDisplayDate(task.scheduledDate),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
              )
            }
            StatusPill(text = task.taskStatus.label, foreground = fg, background = bg)
          }
        }
      }
    }

    item {
      SectionHeader(title = "Activities (${attrLogs.size})")
    }
    if (attrLogs.isEmpty()) {
      item {
        AwakenCard {
          Text("No activity logs yet.", color = colors.textSecondary)
        }
      }
    } else {
      items(attrLogs.take(10), key = { it.id }) { log ->
        AwakenCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(log.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
              Text(
                AwakenDateUtils.formatDisplayDate(log.occurredDate),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
              )
            }
            if (log.metricValue != null && !log.metricUnit.isNullOrBlank()) {
              Text(
                "${AwakenDateUtils.formatMetric(log.metricValue)} ${log.metricUnit}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.accentBright
              )
            }
          }
        }
      }
    }

    item {
      SectionHeader(title = "Goals (${attrGoals.size})")
    }
    if (attrGoals.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Outlined.Flag,
          title = "No ${attribute.displayName} goals"
        )
      }
    } else {
      items(attrGoals, key = { it.id }) { goal ->
        val (fg, bg) = goalStatusColors(goal.goalStatus)
        AwakenCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(goal.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
              goal.targetDate?.let {
                Text(
                  AwakenDateUtils.formatDisplayDate(it),
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
              }
            }
            StatusPill(text = goal.goalStatus.label, foreground = fg, background = bg)
          }
        }
      }
    }
  }
}
