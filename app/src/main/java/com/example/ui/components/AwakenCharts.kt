package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.example.domain.calc.DailyTrendPoint
import com.example.domain.calc.FinancialSummary
import com.example.domain.util.AwakenDateUtils
import com.example.ui.theme.AwakenThemeTokens
import kotlin.math.max

@Composable
fun WeeklyConsistencyChart(
  points: List<DailyTrendPoint>,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
  val maxValue = max(
    1,
    points.maxOfOrNull { max(it.dueTasks, it.activityCount) } ?: 1
  )

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(124.dp)
    ) {
      val count = points.size.coerceAtLeast(1)
      val slotWidth = size.width / count
      val barWidth = (slotWidth * 0.24f).coerceIn(8f, 22f)
      val baselineY = size.height - 8f
      val chartHeight = (size.height - 20f).coerceAtLeast(16f)

      drawLine(
        color = colors.border,
        start = Offset(0f, baselineY),
        end = Offset(size.width, baselineY),
        strokeWidth = 1.5f
      )

      points.forEachIndexed { idx, pt ->
        val centerX = idx * slotWidth + slotWidth / 2f

        if (pt.dueTasks > 0) {
          val dueH = (pt.dueTasks.toFloat() / maxValue) * chartHeight
          drawRoundRect(
            color = colors.border,
            topLeft = Offset(centerX - barWidth - 2f, baselineY - dueH),
            size = Size(barWidth, dueH),
            cornerRadius = CornerRadius(4f, 4f)
          )
        }

        if (pt.completedTasks > 0) {
          val compH = (pt.completedTasks.toFloat() / maxValue) * chartHeight
          drawRoundRect(
            color = colors.accent,
            topLeft = Offset(centerX - barWidth - 2f, baselineY - compH),
            size = Size(barWidth, compH),
            cornerRadius = CornerRadius(4f, 4f)
          )
        }

        if (pt.activityCount > 0) {
          val actH = (pt.activityCount.toFloat() / maxValue) * chartHeight
          drawRoundRect(
            color = colors.statusInProgress,
            topLeft = Offset(centerX + 2f, baselineY - actH),
            size = Size(barWidth, actH),
            cornerRadius = CornerRadius(4f, 4f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      points.forEachIndexed { index, _ ->
        Text(
          text = dayLabels.getOrElse(index) { "" },
          style = MaterialTheme.typography.labelSmall,
          color = colors.textSecondary
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = "■ Completed Tasks",
        style = MaterialTheme.typography.labelSmall,
        color = colors.accent
      )
      Text(
        text = "■ Activity Logs",
        style = MaterialTheme.typography.labelSmall,
        color = colors.statusInProgress
      )
    }
  }
}

@Composable
fun IncomeExpenseChart(
  summary: FinancialSummary,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  val maxVal = max(1.0, max(summary.totalIncome, summary.totalExpenses))

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Income vs Expenses",
        style = MaterialTheme.typography.titleSmall,
        color = colors.textPrimary
      )
      Text(
        text = "Net ${AwakenDateUtils.formatCurrency(summary.netBalance)}",
        style = MaterialTheme.typography.labelMedium,
        color = if (summary.netBalance >= 0) colors.statusCompleted else colors.statusMissed
      )
    }
    Spacer(modifier = Modifier.height(10.dp))
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
    ) {
      val rowHeight = 18f
      val maxBarWidth = size.width
      val incomeW = ((summary.totalIncome / maxVal) * maxBarWidth).toFloat()
      val expenseW = ((summary.totalExpenses / maxVal) * maxBarWidth).toFloat()

      drawRoundRect(
        color = colors.border.copy(alpha = 0.5f),
        topLeft = Offset(0f, 0f),
        size = Size(maxBarWidth, rowHeight),
        cornerRadius = CornerRadius(6f, 6f)
      )
      if (incomeW > 0f) {
        drawRoundRect(
          color = colors.statusCompleted,
          topLeft = Offset(0f, 0f),
          size = Size(incomeW.coerceAtLeast(8f), rowHeight),
          cornerRadius = CornerRadius(6f, 6f)
        )
      }

      val track2Y = rowHeight + 14f
      drawRoundRect(
        color = colors.border.copy(alpha = 0.5f),
        topLeft = Offset(0f, track2Y),
        size = Size(maxBarWidth, rowHeight),
        cornerRadius = CornerRadius(6f, 6f)
      )
      if (expenseW > 0f) {
        drawRoundRect(
          color = colors.statusMissed,
          topLeft = Offset(0f, track2Y),
          size = Size(expenseW.coerceAtLeast(8f), rowHeight),
          cornerRadius = CornerRadius(6f, 6f)
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Income: ${AwakenDateUtils.formatCurrency(summary.totalIncome)}",
        style = MaterialTheme.typography.labelSmall,
        color = colors.statusCompleted
      )
      Text(
        text = "Expense: ${AwakenDateUtils.formatCurrency(summary.totalExpenses)}",
        style = MaterialTheme.typography.labelSmall,
        color = colors.statusMissed
      )
    }
  }
}
