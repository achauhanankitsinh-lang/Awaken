package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.calc.AwakenCalculations
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
import com.example.ui.components.AttributeBadge
import com.example.ui.components.AwakenCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.WeeklyConsistencyChart
import com.example.ui.components.attributeIcon
import com.example.ui.components.taskStatusColors
import com.example.ui.components.trainingStatusColors
import com.example.ui.theme.AwakenThemeMode
import com.example.ui.theme.AwakenThemeTokens
import kotlin.math.roundToInt

data class QuickActionPreset(
  val label: String,
  val icon: ImageVector,
  val actionType: QuickActionType
)

enum class QuickActionType {
  LOG_WORKOUT,
  LOG_MEDITATION,
  LOG_LEARNING,
  LOG_COMMUNICATION,
  ADD_INCOME,
  ADD_EXPENSE,
  LOG_CAREER,
  LOG_REFLECTION
}

@Composable
fun DashboardScreen(
  userDisplayName: String,
  themeMode: AwakenThemeMode = AwakenThemeMode.DARK,
  onToggleTheme: () -> Unit = {},
  tasks: List<TaskItem>,
  activities: List<ActivityLog>,
  trainingDays: List<TrainingDay>,
  goals: List<GoalItem>,
  transactions: List<FinancialTransaction>,
  onQuickAction: (QuickActionType) -> Unit,
  onSetTodayTrainingStatus: (TrainingStatus, TrainingDay?) -> Unit,
  onToggleTaskStatus: (TaskItem, TaskStatus) -> Unit,
  onNewTask: () -> Unit,
  onOpenAttribute: (AwakenAttribute) -> Unit,
  onNavigateTasks: () -> Unit,
  onNavigateActivities: () -> Unit,
  onNavigateTraining: () -> Unit,
  onNavigateGoals: () -> Unit,
  onNavigateFinances: () -> Unit,
  onNavigateProgress: () -> Unit,
  onNavigateCoach: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  val todayKey = remember { AwakenDateUtils.todayIst() }
  val weekRange = remember(todayKey) { AwakenDateUtils.dateRangeForPeriod(todayKey, ReportPeriod.WEEKLY) }
  val monthRange = remember(todayKey) { AwakenDateUtils.dateRangeForPeriod(todayKey, ReportPeriod.MONTHLY) }

  val todayTasks = remember(tasks, todayKey) { tasks.filter { it.scheduledDate == todayKey } }
  val todayTaskSummary = remember(tasks, todayKey) {
    AwakenCalculations.calculateTaskSummary(tasks, todayKey, todayKey, todayKey)
  }
  val weekTaskSummary = remember(tasks, weekRange, todayKey) {
    AwakenCalculations.calculateTaskSummary(tasks, weekRange.startDate, weekRange.endDate, todayKey)
  }
  val todayTraining = remember(trainingDays, todayKey) {
    trainingDays.firstOrNull { it.trainingDate == todayKey }
  }
  val weeklyTrend = remember(tasks, activities, todayKey) {
    AwakenCalculations.buildWeeklyTrend(tasks, activities, todayKey)
  }
  val activeGoals = remember(goals) { goals.filter { it.goalStatus == GoalStatus.ACTIVE } }
  val recentActivities = remember(activities) { activities.take(5) }
  val monthlyFinance = remember(transactions, monthRange) {
    AwakenCalculations.calculateFinancialSummary(transactions, monthRange.startDate, monthRange.endDate)
  }

  val quickActions = remember {
    listOf(
      QuickActionPreset("Log Workout", Icons.Outlined.FitnessCenter, QuickActionType.LOG_WORKOUT),
      QuickActionPreset("Log Meditation", Icons.Outlined.Psychology, QuickActionType.LOG_MEDITATION),
      QuickActionPreset("Log Learning", Icons.AutoMirrored.Outlined.MenuBook, QuickActionType.LOG_LEARNING),
      QuickActionPreset("Log Speaking", Icons.Outlined.RecordVoiceOver, QuickActionType.LOG_COMMUNICATION),
      QuickActionPreset("Add Income", Icons.Outlined.Add, QuickActionType.ADD_INCOME),
      QuickActionPreset("Add Expense", Icons.Outlined.RemoveCircleOutline, QuickActionType.ADD_EXPENSE),
      QuickActionPreset("Career Win", Icons.Outlined.WorkOutline, QuickActionType.LOG_CAREER),
      QuickActionPreset("Reflection", Icons.Outlined.EditNote, QuickActionType.LOG_REFLECTION)
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("dashboard_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Command Center Header
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.elevated,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .matchParentSize()
              .alpha(if (colors.isDark) 0.22f else 0.08f)
          )
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "AWAKEN COMMAND",
                  style = MaterialTheme.typography.labelMedium,
                  color = colors.accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = AwakenDateUtils.formatDisplayDate(todayKey),
                  style = MaterialTheme.typography.headlineSmall,
                  color = colors.textPrimary
                )
                Text(
                  text = userDisplayName.ifBlank { "Owner" },
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = onNavigateCoach,
                  modifier = Modifier.testTag("dashboard_coach_button")
                ) {
                  Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "Open AWAKEN Coach",
                    tint = colors.accent
                  )
                }
                IconButton(
                  onClick = onToggleTheme,
                  modifier = Modifier.testTag("dashboard_theme_toggle")
                ) {
                  Icon(
                    imageVector = if (themeMode == AwakenThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Switch theme",
                    tint = colors.accent
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              val ratePct = todayTaskSummary.completionRate?.let { "${(it * 100).roundToInt()}%" } ?: "—"
              CommandMetricBox(
                label = "Tasks",
                value = "${todayTaskSummary.completed}/${todayTaskSummary.dueNonCancelledCount}",
                sub = ratePct,
                modifier = Modifier.weight(1f)
              )
              CommandMetricBox(
                label = "Training",
                value = todayTraining?.trainingStatus?.label ?: "Unmarked",
                sub = "Today",
                modifier = Modifier
                  .weight(1f)
                  .clickable { onNavigateTraining() }
              )
              CommandMetricBox(
                label = "Goals",
                value = activeGoals.size.toString(),
                sub = "Active",
                modifier = Modifier
                  .weight(1f)
                  .clickable { onNavigateGoals() }
              )
            }
          }
        }
      }
    }

    // 2. Daily Quote
    item {
      val quoteBrush = if (colors.isDark) {
        Brush.linearGradient(
          colors = listOf(
            colors.gradientStart,
            colors.accentContainer.copy(alpha = 0.65f),
            colors.card
          )
        )
      } else {
        Brush.linearGradient(
          colors = listOf(
            colors.accentContainer.copy(alpha = 0.65f),
            colors.card
          )
        )
      }
      val borderBrush = Brush.linearGradient(
        colors = listOf(
          colors.accent.copy(alpha = 0.85f),
          colors.gradientEnd.copy(alpha = if (colors.isDark) 0.55f else 0.45f)
        )
      )
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, borderBrush),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dashboard_quote_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(quoteBrush)
            .padding(horizontal = 20.dp, vertical = 18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = stringResource(R.string.quote_line_one),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.quote_line_connector),
            style = MaterialTheme.typography.labelLarge,
            fontStyle = FontStyle.Italic,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.quote_line_two),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.accentBright,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    // 3. Today's Training-Day Status
    item {
      AwakenCard {
        SectionHeader(
          title = "Training Today",
          actionLabel = "Calendar",
          onAction = onNavigateTraining
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TrainingStatus.entries.forEach { st ->
            val selected = todayTraining?.trainingStatus == st
            val (fg, bg) = trainingStatusColors(st)
            FilterChip(
              selected = selected,
              onClick = { onSetTodayTrainingStatus(st, todayTraining) },
              label = { Text(st.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = bg,
                selectedLabelColor = fg
              ),
              modifier = Modifier.testTag("today_training_${st.wireValue}")
            )
          }
        }
      }
    }

    // 3. Quick Actions
    item {
      SectionHeader(title = "Quick Actions")
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        quickActions.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            pair.forEach { qa ->
              Surface(
                onClick = { onQuickAction(qa.actionType) },
                shape = RoundedCornerShape(12.dp),
                color = colors.elevated,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("qa_${qa.actionType.name.lowercase()}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = qa.icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = qa.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. Today's Tasks
    item {
      SectionHeader(
        title = "Today's Tasks",
        actionLabel = "All",
        onAction = onNavigateTasks
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (todayTasks.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Outlined.CheckCircleOutline,
          title = "No tasks scheduled today",
          actionLabel = "Create Task",
          onAction = onNewTask
        )
      } else {
        AwakenCard {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            todayTasks.forEach { task ->
              val (fg, bg) = taskStatusColors(task.taskStatus)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    val next = if (task.taskStatus == TaskStatus.COMPLETED) {
                      TaskStatus.PLANNED
                    } else {
                      TaskStatus.COMPLETED
                    }
                    onToggleTaskStatus(task, next)
                  }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  AttributeBadge(attribute = task.attribute)
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(text = task.taskStatus.label, foreground = fg, background = bg)
              }
            }
          }
        }
      }
    }

    // 5. Weekly Overview
    item {
      val pctText = weekTaskSummary.completionRate?.let { " · ${(it * 100).roundToInt()}%" } ?: ""
      SectionHeader(
        title = "Weekly Overview",
        subtitle = "${AwakenDateUtils.formatShortDate(weekRange.startDate)} – ${AwakenDateUtils.formatShortDate(weekRange.endDate)}$pctText",
        actionLabel = "Reports",
        onAction = onNavigateProgress
      )
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard {
        WeeklyConsistencyChart(points = weeklyTrend)
      }
    }

    // 6. Six Attributes
    item {
      SectionHeader(title = "Attributes")
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AwakenAttribute.entries.chunked(2).forEach { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            pair.forEach { attr ->
              val attrTasks = tasks.filter { it.attributeKey == attr.key }
              val attrLogs = activities.filter { it.attributeKey == attr.key }
              val attrGoals = activeGoals.filter { it.attributeKey == attr.key }
              AwakenCard(
                modifier = Modifier.weight(1f),
                onClick = { onOpenAttribute(attr) }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = attributeIcon(attr),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = attr.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "${attrLogs.size} logs · ${attrTasks.count { it.taskStatus == TaskStatus.COMPLETED }} done",
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
                Text(
                  text = "${attrGoals.size} goals",
                  style = MaterialTheme.typography.labelSmall,
                  color = colors.accentBright
                )
              }
            }
          }
        }
      }
    }

    // 7. Monthly Financial Summary (INR)
    item {
      SectionHeader(
        title = "Monthly Finances",
        subtitle = AwakenDateUtils.formatMonthTitle(AwakenDateUtils.currentMonthIst()),
        actionLabel = "Ledger",
        onAction = onNavigateFinances
      )
      Spacer(modifier = Modifier.height(8.dp))
      AwakenCard(onClick = onNavigateFinances) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Net Balance",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary
          )
          Text(
            text = AwakenDateUtils.formatCurrency(monthlyFinance.netBalance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (monthlyFinance.netBalance >= 0) colors.statusCompleted else colors.statusMissed
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Income: ${AwakenDateUtils.formatCurrency(monthlyFinance.totalIncome)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusCompleted
          )
          Text(
            text = "Expenses: ${AwakenDateUtils.formatCurrency(monthlyFinance.totalExpenses)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusMissed
          )
        }
      }
    }

    // 8. Recent Activities
    item {
      SectionHeader(
        title = "Recent Logs",
        actionLabel = "All",
        onAction = onNavigateActivities
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (recentActivities.isEmpty()) {
        EmptyStateCard(
          icon = Icons.AutoMirrored.Outlined.TrendingUp,
          title = "No activities yet"
        )
      } else {
        AwakenCard {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            recentActivities.forEach { log ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = log.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${AwakenDateUtils.formatShortDate(log.occurredDate)}${log.attribute?.let { " · ${it.displayName}" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                  )
                }
                if (log.metricValue != null && !log.metricUnit.isNullOrBlank()) {
                  Text(
                    text = "${AwakenDateUtils.formatMetric(log.metricValue)} ${log.metricUnit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentBright
                  )
                }
              }
            }
          }
        }
      }
    }

    // 9. Active Goals
    item {
      SectionHeader(
        title = "Active Goals",
        actionLabel = "Manage",
        onAction = onNavigateGoals
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (activeGoals.isEmpty()) {
        EmptyStateCard(
          icon = Icons.Outlined.Flag,
          title = "No active goals",
          actionLabel = "Open Goals",
          onAction = onNavigateGoals
        )
      } else {
        AwakenCard {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            activeGoals.take(4).forEach { goal ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${goal.attribute.displayName}${goal.targetDate?.let { " · ${AwakenDateUtils.formatShortDate(it)}" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                  )
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun CommandMetricBox(
  label: String,
  value: String,
  sub: String,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(10.dp),
    color = colors.accentContainer.copy(alpha = if (colors.isDark) 0.40f else 0.65f),
    border = BorderStroke(1.dp, colors.border)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = sub,
        style = MaterialTheme.typography.labelSmall,
        color = colors.accentBright,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
