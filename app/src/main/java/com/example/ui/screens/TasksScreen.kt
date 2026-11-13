package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AttributeBadge
import com.example.ui.components.AttributeFilterRow
import com.example.ui.components.AwakenCard
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.taskStatusColors
import com.example.ui.theme.AwakenThemeTokens
import kotlin.math.roundToInt

@Composable
fun TasksScreen(
  tasks: List<TaskItem>,
  onCreateTask: () -> Unit,
  onEditTask: (TaskItem) -> Unit,
  onChangeStatus: (TaskItem, TaskStatus) -> Unit,
  onDeleteTask: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var selectedPeriod by remember { mutableStateOf(ReportPeriod.DAILY) }
  var anchorDate by remember { mutableStateOf(AwakenDateUtils.todayIst()) }
  var selectedAttribute by remember { mutableStateOf<AwakenAttribute?>(null) }
  var selectedStatus by remember { mutableStateOf<TaskStatus?>(null) }
  var taskPendingDelete by remember { mutableStateOf<TaskItem?>(null) }

  val todayIst = remember { AwakenDateUtils.todayIst() }
  val range = remember(anchorDate, selectedPeriod) {
    AwakenDateUtils.dateRangeForPeriod(anchorDate, selectedPeriod)
  }

  val filteredTasks = remember(tasks, range, selectedAttribute, selectedStatus) {
    tasks.filter { task ->
      task.scheduledDate in range.startDate..range.endDate &&
        (selectedAttribute == null || task.attributeKey == selectedAttribute!!.key) &&
        (selectedStatus == null || task.taskStatus == selectedStatus)
    }
  }

  val periodSummary = remember(tasks, range, todayIst) {
    AwakenCalculations.calculateTaskSummary(tasks, range.startDate, range.endDate, todayIst)
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(
          text = "Tasks",
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = when (selectedPeriod) {
                  ReportPeriod.DAILY -> AwakenDateUtils.formatDisplayDate(anchorDate)
                  ReportPeriod.WEEKLY -> "${AwakenDateUtils.formatShortDate(range.startDate)} – ${AwakenDateUtils.formatShortDate(range.endDate)}"
                  ReportPeriod.MONTHLY -> AwakenDateUtils.formatMonthTitle(anchorDate.take(7))
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
              )
              Text(
                text = "${periodSummary.completed}/${periodSummary.dueNonCancelledCount} done" +
                  (periodSummary.completionRate?.let { " (${(it * 100).roundToInt()}%)" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = colors.accentBright
              )
            }
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
        AttributeFilterRow(
          selectedAttribute = selectedAttribute,
          onSelect = { selectedAttribute = it }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedStatus == null,
            onClick = { selectedStatus = null },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.accentContainer,
              selectedLabelColor = colors.accentBright
            )
          )
          TaskStatus.entries.forEach { st ->
            val (fg, bg) = taskStatusColors(st)
            FilterChip(
              selected = selectedStatus == st,
              onClick = { selectedStatus = st },
              label = { Text(st.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = bg,
                selectedLabelColor = fg
              )
            )
          }
        }
      }

      if (filteredTasks.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Outlined.CheckCircleOutline,
            title = "No tasks found",
            actionLabel = "Create Task",
            onAction = onCreateTask
          )
        }
      } else {
        items(filteredTasks, key = { it.id }) { task ->
          TaskRowCard(
            task = task,
            onEdit = { onEditTask(task) },
            onChangeStatus = { status -> onChangeStatus(task, status) },
            onDelete = { taskPendingDelete = task }
          )
        }
      }
    }

    FloatingActionButton(
      onClick = onCreateTask,
      containerColor = colors.accent,
      contentColor = colors.onAccent,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("add_task_fab")
    ) {
      Icon(Icons.Outlined.Add, contentDescription = "Create task")
    }
  }

  taskPendingDelete?.let { task ->
    DestructiveConfirmDialog(
      title = "Delete Task",
      message = "Delete \"${task.title}\"?",
      onConfirm = {
        onDeleteTask(task.id)
        taskPendingDelete = null
      },
      onDismiss = { taskPendingDelete = null }
    )
  }
}

@Composable
private fun TaskRowCard(
  task: TaskItem,
  onEdit: () -> Unit,
  onChangeStatus: (TaskStatus) -> Unit,
  onDelete: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  var statusMenuExpanded by remember { mutableStateOf(false) }
  val (fg, bg) = taskStatusColors(task.taskStatus)

  AwakenCard(modifier = Modifier.testTag("task_item_${task.id}")) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.titleMedium,
          color = colors.textPrimary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        if (!task.notes.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = task.notes,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AttributeBadge(attribute = task.attribute)
          Text(
            text = AwakenDateUtils.formatShortDate(task.scheduledDate),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        Box {
          StatusPill(
            text = task.taskStatus.label,
            foreground = fg,
            background = bg,
            modifier = Modifier
              .padding(bottom = 4.dp)
              .testTag("status_pill_${task.id}")
          )
          TextButton(onClick = { statusMenuExpanded = true }) {
            Text("Status", style = MaterialTheme.typography.labelSmall, color = colors.accentBright)
          }
          DropdownMenu(
            expanded = statusMenuExpanded,
            onDismissRequest = { statusMenuExpanded = false },
            containerColor = colors.card
          ) {
            TaskStatus.entries.forEach { st ->
              DropdownMenuItem(
                text = { Text(st.label, color = colors.textPrimary) },
                onClick = {
                  statusMenuExpanded = false
                  onChangeStatus(st)
                }
              )
            }
          }
        }
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit task", tint = colors.textSecondary)
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete task", tint = colors.statusMissed)
          }
        }
      }
    }
  }
}
