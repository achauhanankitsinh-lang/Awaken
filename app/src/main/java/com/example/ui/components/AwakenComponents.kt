package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.GoalStatus
import com.example.domain.model.TaskStatus
import com.example.domain.model.TrainingStatus
import com.example.domain.util.AwakenDateUtils
import com.example.ui.theme.AwakenThemeTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun attributeIcon(attribute: AwakenAttribute): ImageVector = when (attribute) {
  AwakenAttribute.PHYSICAL -> Icons.Outlined.FitnessCenter
  AwakenAttribute.MENTAL -> Icons.Outlined.Psychology
  AwakenAttribute.LEARNING -> Icons.AutoMirrored.Outlined.MenuBook
  AwakenAttribute.COMMUNICATION -> Icons.Outlined.RecordVoiceOver
  AwakenAttribute.FINANCIAL -> Icons.Outlined.AccountBalanceWallet
  AwakenAttribute.CAREER -> Icons.Outlined.WorkOutline
}

@Composable
fun taskStatusColors(status: TaskStatus): Pair<Color, Color> {
  val c = AwakenThemeTokens.colors
  return when (status) {
    TaskStatus.PLANNED -> c.statusPlanned to c.statusPlannedContainer
    TaskStatus.IN_PROGRESS -> c.statusInProgress to c.statusInProgressContainer
    TaskStatus.COMPLETED -> c.statusCompleted to c.statusCompletedContainer
    TaskStatus.CANCELLED -> c.statusCancelled to c.statusCancelledContainer
    TaskStatus.MISSED -> c.statusMissed to c.statusMissedContainer
  }
}

@Composable
fun trainingStatusColors(status: TrainingStatus): Pair<Color, Color> {
  val c = AwakenThemeTokens.colors
  return when (status) {
    TrainingStatus.PLANNED -> c.statusPlanned to c.statusPlannedContainer
    TrainingStatus.REST -> c.statusRest to c.statusRestContainer
    TrainingStatus.COMPLETED -> c.statusCompleted to c.statusCompletedContainer
    TrainingStatus.MISSED -> c.statusMissed to c.statusMissedContainer
  }
}

@Composable
fun goalStatusColors(status: GoalStatus): Pair<Color, Color> {
  val c = AwakenThemeTokens.colors
  return when (status) {
    GoalStatus.ACTIVE -> c.statusInProgress to c.statusInProgressContainer
    GoalStatus.COMPLETED -> c.statusCompleted to c.statusCompletedContainer
    GoalStatus.CANCELLED -> c.statusCancelled to c.statusCancelledContainer
  }
}

@Composable
fun AwakenCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  borderColor: Color = AwakenThemeTokens.colors.border,
  containerColor: Color = AwakenThemeTokens.colors.card,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(14.dp)
  val cardModifier = if (onClick != null) {
    modifier.clickable(onClick = onClick)
  } else {
    modifier
  }
  Surface(
    modifier = cardModifier.fillMaxWidth(),
    shape = shape,
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      content = content
    )
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = colors.accent
      )
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary
        )
      }
    }
    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = colors.accentBright
        )
      }
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  foreground: Color,
  background: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = background,
    border = BorderStroke(1.dp, foreground.copy(alpha = 0.35f))
  ) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = foreground,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun AttributeBadge(
  attribute: AwakenAttribute,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = colors.elevated,
    border = BorderStroke(1.dp, colors.border)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      Icon(
        imageVector = attributeIcon(attribute),
        contentDescription = null,
        tint = colors.accent,
        modifier = Modifier.size(13.dp)
      )
      Text(
        text = attribute.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = colors.textPrimary,
        maxLines = 1
      )
    }
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  message: String? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  AwakenCard(modifier = modifier) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(colors.accentContainer, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = colors.accent,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = colors.textPrimary
      )
      if (!message.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = colors.textSecondary
        )
      }
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onAction,
          border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentBright)
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun AttributeFilterRow(
  selectedAttribute: AwakenAttribute?,
  onSelect: (AwakenAttribute?) -> Unit,
  includeAllOption: Boolean = true,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (includeAllOption) {
      FilterChip(
        selected = selectedAttribute == null,
        onClick = { onSelect(null) },
        label = { Text("All") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = colors.accentContainer,
          selectedLabelColor = colors.accentBright
        )
      )
    }
    AwakenAttribute.entries.forEach { attr ->
      FilterChip(
        selected = selectedAttribute == attr,
        onClick = { onSelect(attr) },
        label = { Text(attr.displayName) },
        leadingIcon = {
          Icon(
            imageVector = attributeIcon(attr),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = colors.accentContainer,
          selectedLabelColor = colors.accentBright,
          selectedLeadingIconColor = colors.accent
        )
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorField(
  label: String,
  dateKey: String,
  onDateSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  allowClear: Boolean = false
) {
  val colors = AwakenThemeTokens.colors
  var showPicker by remember { mutableStateOf(false) }

  OutlinedButton(
    onClick = { showPicker = true },
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, colors.border)
  ) {
    Icon(
      imageVector = Icons.Outlined.CalendarToday,
      contentDescription = null,
      tint = colors.accent,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = if (dateKey.isBlank()) "$label: Not set" else "$label: ${AwakenDateUtils.formatDisplayDate(dateKey)}",
      color = colors.textPrimary,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (allowClear && dateKey.isNotBlank()) {
      TextButton(onClick = { onDateSelected("") }) {
        Text("Clear", color = colors.textSecondary)
      }
    }
  }

  if (showPicker) {
    val initialMillis = remember(dateKey) {
      if (AwakenDateUtils.isValidDateKey(dateKey)) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(dateKey)?.time ?: System.currentTimeMillis()
      } else {
        System.currentTimeMillis()
      }
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              val utcFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
              }
              onDateSelected(utcFmt.format(Date(millis)))
            }
            showPicker = false
          }
        ) {
          Text("Select", color = colors.accentBright)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPicker = false }) {
          Text("Cancel", color = colors.textSecondary)
        }
      }
    ) {
      DatePicker(state = pickerState)
    }
  }
}

@Composable
fun DestructiveConfirmDialog(
  title: String,
  message: String,
  confirmLabel: String = "Delete",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = colors.card,
    title = { Text(title, color = colors.textPrimary) },
    text = { Text(message, color = colors.textSecondary) },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        modifier = Modifier.testTag("confirm_delete_button")
      ) {
        Text(confirmLabel, color = colors.statusMissed, fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = colors.textPrimary)
      }
    }
  )
}
