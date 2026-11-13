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
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.ReportPeriod
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AttributeBadge
import com.example.ui.components.AttributeFilterRow
import com.example.ui.components.AwakenCard
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.AwakenThemeTokens

enum class ActivityDateFilter(val label: String) {
  ALL("All"),
  TODAY("Today"),
  THIS_WEEK("Week"),
  THIS_MONTH("Month")
}

@Composable
fun ActivitiesScreen(
  activities: List<ActivityLog>,
  onCreateActivity: () -> Unit,
  onEditActivity: (ActivityLog) -> Unit,
  onDeleteActivity: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var selectedAttribute by remember { mutableStateOf<AwakenAttribute?>(null) }
  var dateFilter by remember { mutableStateOf(ActivityDateFilter.ALL) }
  var pendingDelete by remember { mutableStateOf<ActivityLog?>(null) }

  val todayIst = remember { AwakenDateUtils.todayIst() }
  val range = remember(dateFilter, todayIst) {
    when (dateFilter) {
      ActivityDateFilter.ALL -> null
      ActivityDateFilter.TODAY -> AwakenDateUtils.dateRangeForPeriod(todayIst, ReportPeriod.DAILY)
      ActivityDateFilter.THIS_WEEK -> AwakenDateUtils.dateRangeForPeriod(todayIst, ReportPeriod.WEEKLY)
      ActivityDateFilter.THIS_MONTH -> AwakenDateUtils.dateRangeForPeriod(todayIst, ReportPeriod.MONTHLY)
    }
  }

  val filteredLogs = remember(activities, selectedAttribute, range) {
    activities.filter { log ->
      (selectedAttribute == null || log.attributeKey == selectedAttribute!!.key) &&
        (range == null || log.occurredDate in range.startDate..range.endDate)
    }
  }

  val summary = remember(filteredLogs) {
    AwakenCalculations.calculateActivitySummary(filteredLogs)
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(
          text = "Activity Logs",
          style = MaterialTheme.typography.headlineMedium,
          color = colors.textPrimary
        )
      }

      item {
        AttributeFilterRow(
          selectedAttribute = selectedAttribute,
          onSelect = { selectedAttribute = it }
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ActivityDateFilter.entries.forEach { df ->
            FilterChip(
              selected = dateFilter == df,
              onClick = { dateFilter = df },
              label = { Text(df.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright
              )
            )
          }
        }
      }

      item {
        AwakenCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "${summary.totalSessions} sessions",
              style = MaterialTheme.typography.titleMedium,
              color = colors.textPrimary
            )
            Text(
              text = "${summary.measuredSessions} measured",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )
          }
          if (summary.unitTotals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            summary.unitTotals.forEach { ut ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${ut.unit} (${ut.sessionCount})",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
                Text(
                  text = "${AwakenDateUtils.formatMetric(ut.total)} ${ut.unit}",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.accentBright
                )
              }
            }
          }
        }
      }

      if (filteredLogs.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            title = "No activity logs",
            actionLabel = "Log Activity",
            onAction = onCreateActivity
          )
        }
      } else {
        items(filteredLogs, key = { it.id }) { log ->
          AwakenCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = log.title,
                  style = MaterialTheme.typography.titleMedium,
                  color = colors.textPrimary
                )
                if (!log.details.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = log.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  log.attribute?.let { AttributeBadge(attribute = it) }
                  Text(
                    text = AwakenDateUtils.formatDisplayDate(log.occurredDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                  )
                }
              }
              Column(horizontalAlignment = Alignment.End) {
                if (log.metricValue != null && !log.metricUnit.isNullOrBlank()) {
                  Text(
                    text = "${AwakenDateUtils.formatMetric(log.metricValue)} ${log.metricUnit}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accentBright
                  )
                }
                Row {
                  IconButton(onClick = { onEditActivity(log) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit log", tint = colors.textSecondary)
                  }
                  IconButton(onClick = { pendingDelete = log }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete log", tint = colors.statusMissed)
                  }
                }
              }
            }
          }
        }
      }
    }

    FloatingActionButton(
      onClick = onCreateActivity,
      containerColor = colors.accent,
      contentColor = colors.onAccent,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("add_activity_fab")
    ) {
      Icon(Icons.Outlined.Add, contentDescription = "Log activity")
    }
  }

  pendingDelete?.let { log ->
    DestructiveConfirmDialog(
      title = "Delete Activity Log",
      message = "Delete \"${log.title}\"?",
      onConfirm = {
        onDeleteActivity(log.id)
        pendingDelete = null
      },
      onDismiss = { pendingDelete = null }
    )
  }
}
