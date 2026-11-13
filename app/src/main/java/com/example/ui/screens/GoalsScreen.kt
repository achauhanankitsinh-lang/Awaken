package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AttributeBadge
import com.example.ui.components.AttributeFilterRow
import com.example.ui.components.AwakenCard
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.goalStatusColors
import com.example.ui.theme.AwakenThemeTokens

@Composable
fun GoalsScreen(
  goals: List<GoalItem>,
  onCreateGoal: () -> Unit,
  onEditGoal: (GoalItem) -> Unit,
  onUpdateGoalStatus: (GoalItem, GoalStatus) -> Unit,
  onDeleteGoal: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var selectedStatus by remember { mutableStateOf<GoalStatus?>(GoalStatus.ACTIVE) }
  var selectedAttribute by remember { mutableStateOf<AwakenAttribute?>(null) }
  var pendingDelete by remember { mutableStateOf<GoalItem?>(null) }

  val summary = remember(goals) { AwakenCalculations.calculateGoalSummary(goals) }
  val filteredGoals = remember(goals, selectedStatus, selectedAttribute) {
    goals.filter { goal ->
      (selectedStatus == null || goal.goalStatus == selectedStatus) &&
        (selectedAttribute == null || goal.attributeKey == selectedAttribute!!.key)
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(
          text = "Goals",
          style = MaterialTheme.typography.headlineMedium,
          color = colors.textPrimary
        )
      }

      item {
        AwakenCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            GoalCount("Active", summary.active, colors.statusInProgress)
            GoalCount("Completed", summary.completed, colors.statusCompleted)
            GoalCount("Cancelled", summary.cancelled, colors.textSecondary)
          }
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
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
          GoalStatus.entries.forEach { st ->
            val (fg, bg) = goalStatusColors(st)
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
        Spacer(modifier = Modifier.height(6.dp))
        AttributeFilterRow(
          selectedAttribute = selectedAttribute,
          onSelect = { selectedAttribute = it }
        )
      }

      if (filteredGoals.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Outlined.Flag,
            title = "No goals found",
            actionLabel = "Create Goal",
            onAction = onCreateGoal
          )
        }
      } else {
        items(filteredGoals, key = { it.id }) { goal ->
          val (fg, bg) = goalStatusColors(goal.goalStatus)
          AwakenCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = goal.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = colors.textPrimary
                )
                if (!goal.description.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  AttributeBadge(attribute = goal.attribute)
                  goal.targetDate?.let {
                    Text(
                      text = AwakenDateUtils.formatDisplayDate(it),
                      style = MaterialTheme.typography.labelSmall,
                      color = colors.accentBright
                    )
                  }
                }
              }
              StatusPill(text = goal.goalStatus.label, foreground = fg, background = bg)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (goal.goalStatus != GoalStatus.COMPLETED) {
                  TextButton(onClick = { onUpdateGoalStatus(goal, GoalStatus.COMPLETED) }) {
                    Text("Complete", color = colors.statusCompleted)
                  }
                }
                if (goal.goalStatus == GoalStatus.ACTIVE) {
                  TextButton(onClick = { onUpdateGoalStatus(goal, GoalStatus.CANCELLED) }) {
                    Text("Cancel", color = colors.textSecondary)
                  }
                }
                if (goal.goalStatus != GoalStatus.ACTIVE) {
                  TextButton(onClick = { onUpdateGoalStatus(goal, GoalStatus.ACTIVE) }) {
                    Text("Reopen", color = colors.accentBright)
                  }
                }
              }
              Row {
                IconButton(onClick = { onEditGoal(goal) }) {
                  Icon(Icons.Outlined.Edit, contentDescription = "Edit goal", tint = colors.textSecondary)
                }
                IconButton(onClick = { pendingDelete = goal }) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete goal", tint = colors.statusMissed)
                }
              }
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = onCreateGoal,
      containerColor = colors.accent,
      contentColor = colors.onAccent,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("add_goal_fab")
    ) {
      Icon(Icons.Outlined.Add, contentDescription = "Create goal")
    }
  }

  pendingDelete?.let { goal ->
    DestructiveConfirmDialog(
      title = "Delete Goal",
      message = "Delete \"${goal.title}\"?",
      onConfirm = {
        onDeleteGoal(goal.id)
        pendingDelete = null
      },
      onDismiss = { pendingDelete = null }
    )
  }
}

@Composable
private fun GoalCount(
  label: String,
  count: Int,
  color: Color
) {
  val colors = AwakenThemeTokens.colors
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.headlineSmall,
      color = color
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = colors.textSecondary
    )
  }
}
