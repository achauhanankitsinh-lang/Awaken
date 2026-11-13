package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TrainingDay
import com.example.domain.model.TrainingStatus
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AwakenCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.trainingStatusColors
import com.example.ui.theme.AwakenThemeTokens

@Composable
fun TrainingCalendarScreen(
  trainingDays: List<TrainingDay>,
  onSetTrainingStatus: (String, TrainingStatus, TrainingDay?) -> Unit,
  onClearTrainingDay: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var currentMonth by remember { mutableStateOf(AwakenDateUtils.currentMonthIst()) }
  var selectedDateKey by remember { mutableStateOf(AwakenDateUtils.todayIst()) }
  val todayKey = remember { AwakenDateUtils.todayIst() }

  val dayMap = remember(trainingDays) {
    trainingDays.associateBy { it.trainingDate }
  }
  val cells = remember(currentMonth) {
    AwakenDateUtils.monthGridMondayFirst(currentMonth)
  }
  val monthStart = "$currentMonth-01"
  val monthEnd = AwakenDateUtils.dateRangeForPeriod(monthStart, ReportPeriod.MONTHLY).endDate
  val monthSummary = remember(trainingDays, monthStart, monthEnd) {
    AwakenCalculations.calculateTrainingSummary(trainingDays, monthStart, monthEnd)
  }
  val selectedTraining = dayMap[selectedDateKey]

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "Training Calendar",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary
      )
    }

    item {
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          TrainingCountItem("Completed", monthSummary.completed, colors.statusCompleted)
          TrainingCountItem("Rest", monthSummary.rest, colors.statusRest)
          TrainingCountItem("Planned", monthSummary.planned, colors.statusPlanned)
          TrainingCountItem("Missed", monthSummary.missed, colors.statusMissed)
        }
      }
    }

    item {
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { currentMonth = AwakenDateUtils.shiftMonths(currentMonth, -1) }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.textPrimary)
          }
          Text(
            text = AwakenDateUtils.formatMonthTitle(currentMonth),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
          )
          IconButton(onClick = { currentMonth = AwakenDateUtils.shiftMonths(currentMonth, 1) }) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month", tint = colors.textPrimary)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
          listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayName ->
            Text(
              text = dayName,
              style = MaterialTheme.typography.labelSmall,
              color = colors.textSecondary,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          cells.chunked(7).forEach { week ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              week.forEach { cell ->
                if (cell == null) {
                  Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                } else {
                  val record = dayMap[cell.dateKey]
                  val isSelected = selectedDateKey == cell.dateKey
                  val isToday = todayKey == cell.dateKey
                  val (fg, bg) = if (record != null) {
                    trainingStatusColors(record.trainingStatus)
                  } else {
                    colors.textPrimary to colors.elevated
                  }
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (record != null) bg else colors.elevated.copy(alpha = 0.7f),
                    border = BorderStroke(
                      width = if (isSelected) 2.dp else 1.dp,
                      color = when {
                        isSelected -> colors.accent
                        isToday -> colors.accent.copy(alpha = 0.5f)
                        record != null -> fg.copy(alpha = 0.45f)
                        else -> colors.border
                      }
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                      .clickable { selectedDateKey = cell.dateKey }
                      .testTag("calendar_day_${cell.dateKey}")
                  ) {
                    Column(
                      modifier = Modifier.fillMaxSize(),
                      verticalArrangement = Arrangement.Center,
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Text(
                        text = cell.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (record != null) fg else colors.textPrimary
                      )
                      if (record != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                          modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(fg)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      AwakenCard {
        SectionHeader(
          title = AwakenDateUtils.formatDisplayDate(selectedDateKey),
          subtitle = selectedTraining?.trainingStatus?.label ?: "Unmarked"
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          TrainingStatus.entries.chunked(2).forEach { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              pair.forEach { status ->
                val isCurrent = selectedTraining?.trainingStatus == status
                val (fg, bg) = trainingStatusColors(status)
                Surface(
                  onClick = { onSetTrainingStatus(selectedDateKey, status, selectedTraining) },
                  shape = RoundedCornerShape(10.dp),
                  color = if (isCurrent) bg else colors.elevated,
                  border = BorderStroke(1.dp, if (isCurrent) fg else colors.border),
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("set_training_${status.wireValue}")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    StatusPill(text = status.label, foreground = fg, background = bg)
                  }
                }
              }
            }
          }
        }
        if (selectedTraining != null) {
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = { onClearTrainingDay(selectedDateKey) },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, colors.border)
          ) {
            Text("Clear Date", color = colors.textSecondary)
          }
        }
      }
    }
  }
}

@Composable
private fun TrainingCountItem(
  label: String,
  count: Int,
  color: Color
) {
  val colors = AwakenThemeTokens.colors
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      color = color
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = colors.textSecondary
    )
  }
}
