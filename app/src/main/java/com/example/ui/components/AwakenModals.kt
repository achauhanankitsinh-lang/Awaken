package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.ActivityLog
import com.example.domain.model.AwakenAttribute
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.GoalItem
import com.example.domain.model.GoalStatus
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskStatus
import com.example.domain.model.TransactionType
import com.example.domain.util.AwakenDateUtils
import com.example.ui.theme.AwakenThemeTokens
import com.google.firebase.Timestamp

@Composable
fun TaskEditorDialog(
  initialTask: TaskItem?,
  defaultAttribute: AwakenAttribute = AwakenAttribute.PHYSICAL,
  defaultDate: String = AwakenDateUtils.todayIst(),
  isSaving: Boolean,
  onSave: (TaskItem) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  var title by remember { mutableStateOf(initialTask?.title ?: "") }
  var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
  var selectedAttribute by remember {
    mutableStateOf(initialTask?.attribute ?: defaultAttribute)
  }
  var scheduledDate by remember {
    mutableStateOf(initialTask?.scheduledDate?.ifBlank { defaultDate } ?: defaultDate)
  }
  var selectedStatus by remember {
    mutableStateOf(initialTask?.taskStatus ?: TaskStatus.PLANNED)
  }
  var validationError by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = colors.card,
      border = BorderStroke(1.dp, colors.border)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = if (initialTask == null) "New Task" else "Edit Task",
          style = MaterialTheme.typography.headlineSmall,
          color = colors.textPrimary
        )

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            validationError = null
          },
          label = { Text("Task Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("task_title_input")
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes (optional)") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth()
        )

        Text(
          text = "Attribute",
          style = MaterialTheme.typography.labelMedium,
          color = colors.accent
        )
        AttributeFilterRow(
          selectedAttribute = selectedAttribute,
          onSelect = { if (it != null) selectedAttribute = it },
          includeAllOption = false
        )

        DateSelectorField(
          label = "Scheduled",
          dateKey = scheduledDate,
          onDateSelected = { scheduledDate = it }
        )

        Text(
          text = "Status",
          style = MaterialTheme.typography.labelMedium,
          color = colors.accent
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TaskStatus.entries.forEach { st ->
            FilterChip(
              selected = selectedStatus == st,
              onClick = { selectedStatus = st },
              label = { Text(st.label) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright
              )
            )
          }
        }

        if (validationError != null) {
          Text(
            text = validationError!!,
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusMissed
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss, enabled = !isSaving) {
            Text("Cancel", color = colors.textSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (title.isBlank()) {
                validationError = "Please enter a task title."
                return@Button
              }
              onSave(
                TaskItem(
                  id = initialTask?.id ?: "",
                  attributeKey = selectedAttribute.key,
                  title = title.trim(),
                  notes = notes.trim().takeIf { it.isNotEmpty() },
                  scheduledDate = scheduledDate,
                  status = selectedStatus.wireValue,
                  createdAt = initialTask?.createdAt
                )
              )
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = colors.onAccent
            ),
            modifier = Modifier.testTag("save_task_button")
          ) {
            Text(if (isSaving) "Saving..." else "Save Task")
          }
        }
      }
    }
  }
}

@Composable
fun ActivityEditorDialog(
  initialLog: ActivityLog?,
  presetTitle: String = "",
  presetAttribute: AwakenAttribute? = AwakenAttribute.PHYSICAL,
  presetUnit: String = "",
  presetCategoryTag: String? = null,
  isSaving: Boolean,
  onSave: (ActivityLog) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  var title by remember { mutableStateOf(initialLog?.title ?: presetTitle) }
  var details by remember { mutableStateOf(initialLog?.details ?: "") }
  var selectedAttribute by remember { mutableStateOf(initialLog?.attribute ?: presetAttribute) }
  var occurredDate by remember {
    mutableStateOf(initialLog?.occurredDate?.ifBlank { AwakenDateUtils.todayIst() } ?: AwakenDateUtils.todayIst())
  }
  var metricValueText by remember {
    mutableStateOf(initialLog?.metricValue?.let { AwakenDateUtils.formatMetric(it) } ?: "")
  }
  var metricUnit by remember {
    mutableStateOf(initialLog?.metricUnit ?: presetUnit)
  }
  var validationError by remember { mutableStateOf<String?>(null) }

  val unitSuggestions = listOf("km", "minutes", "repetitions", "hours", "sessions", "pages")

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = colors.card,
      border = BorderStroke(1.dp, colors.border)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = if (initialLog == null) "Log Activity" else "Edit Activity",
          style = MaterialTheme.typography.headlineSmall,
          color = colors.textPrimary
        )

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            validationError = null
          },
          label = { Text("Activity Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_title_input")
        )

        Text(
          text = "Attribute",
          style = MaterialTheme.typography.labelMedium,
          color = colors.accent
        )
        AttributeFilterRow(
          selectedAttribute = selectedAttribute,
          onSelect = { selectedAttribute = it },
          includeAllOption = true
        )

        DateSelectorField(
          label = "Date",
          dateKey = occurredDate,
          onDateSelected = { occurredDate = it }
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = metricValueText,
            onValueChange = {
              metricValueText = it
              validationError = null
            },
            label = { Text("Metric (opt)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("activity_metric_input")
          )
          OutlinedTextField(
            value = metricUnit,
            onValueChange = {
              metricUnit = it
              validationError = null
            },
            label = { Text("Unit") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("activity_unit_input")
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          unitSuggestions.forEach { u ->
            FilterChip(
              selected = metricUnit.equals(u, ignoreCase = true),
              onClick = { metricUnit = u },
              label = { Text(u) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright
              )
            )
          }
        }

        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text("Notes (optional)") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth()
        )

        if (validationError != null) {
          Text(
            text = validationError!!,
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusMissed
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss, enabled = !isSaving) {
            Text("Cancel", color = colors.textSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (title.isBlank()) {
                validationError = "Activity title is required."
                return@Button
              }
              val parsedMetric: Double? = if (metricValueText.isBlank()) {
                null
              } else {
                val num = metricValueText.trim().toDoubleOrNull()
                if (num == null || num < 0.0) {
                  validationError = "Metric must be a non-negative number."
                  return@Button
                }
                num
              }
              if (parsedMetric != null && metricUnit.isBlank()) {
                validationError = "Specify a unit for the measurement."
                return@Button
              }
              onSave(
                ActivityLog(
                  id = initialLog?.id ?: "",
                  attributeKey = selectedAttribute?.key,
                  title = title.trim(),
                  details = details.trim().takeIf { it.isNotEmpty() },
                  occurredAt = initialLog?.occurredAt ?: Timestamp.now(),
                  occurredDate = occurredDate,
                  metricValue = parsedMetric,
                  metricUnit = if (parsedMetric != null) metricUnit.trim().lowercase() else null,
                  categoryTag = initialLog?.categoryTag ?: presetCategoryTag,
                  createdAt = initialLog?.createdAt
                )
              )
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = colors.onAccent
            ),
            modifier = Modifier.testTag("save_activity_button")
          ) {
            Text(if (isSaving) "Saving..." else "Save Log")
          }
        }
      }
    }
  }
}

@Composable
fun GoalEditorDialog(
  initialGoal: GoalItem?,
  defaultAttribute: AwakenAttribute = AwakenAttribute.CAREER,
  isSaving: Boolean,
  onSave: (GoalItem) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  var title by remember { mutableStateOf(initialGoal?.title ?: "") }
  var description by remember { mutableStateOf(initialGoal?.description ?: "") }
  var selectedAttribute by remember { mutableStateOf(initialGoal?.attribute ?: defaultAttribute) }
  var targetDate by remember { mutableStateOf(initialGoal?.targetDate ?: "") }
  var selectedStatus by remember { mutableStateOf(initialGoal?.goalStatus ?: GoalStatus.ACTIVE) }
  var validationError by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = colors.card,
      border = BorderStroke(1.dp, colors.border)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = if (initialGoal == null) "New Goal" else "Edit Goal",
          style = MaterialTheme.typography.headlineSmall,
          color = colors.textPrimary
        )

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            validationError = null
          },
          label = { Text("Goal Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_title_input")
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (optional)") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth()
        )

        Text(
          text = "Attribute",
          style = MaterialTheme.typography.labelMedium,
          color = colors.accent
        )
        AttributeFilterRow(
          selectedAttribute = selectedAttribute,
          onSelect = { if (it != null) selectedAttribute = it },
          includeAllOption = false
        )

        DateSelectorField(
          label = "Target Date",
          dateKey = targetDate,
          onDateSelected = { targetDate = it },
          allowClear = true
        )

        if (initialGoal != null) {
          Text(
            text = "Status",
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoalStatus.entries.forEach { st ->
              FilterChip(
                selected = selectedStatus == st,
                onClick = { selectedStatus = st },
                label = { Text(st.label) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = colors.accentContainer,
                  selectedLabelColor = colors.accentBright
                )
              )
            }
          }
        }

        if (validationError != null) {
          Text(
            text = validationError!!,
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusMissed
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss, enabled = !isSaving) {
            Text("Cancel", color = colors.textSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (title.isBlank()) {
                validationError = "Goal title is required."
                return@Button
              }
              onSave(
                GoalItem(
                  id = initialGoal?.id ?: "",
                  attributeKey = selectedAttribute.key,
                  title = title.trim(),
                  description = description.trim().takeIf { it.isNotEmpty() },
                  targetDate = targetDate.trim().takeIf { it.isNotEmpty() },
                  status = selectedStatus.wireValue,
                  createdAt = initialGoal?.createdAt
                )
              )
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = colors.onAccent
            ),
            modifier = Modifier.testTag("save_goal_button")
          ) {
            Text(if (isSaving) "Saving..." else "Save Goal")
          }
        }
      }
    }
  }
}

@Composable
fun TransactionEditorDialog(
  initialTx: FinancialTransaction?,
  defaultType: TransactionType = TransactionType.EXPENSE,
  isSaving: Boolean,
  onSave: (FinancialTransaction) -> Unit,
  onDismiss: () -> Unit
) {
  val colors = AwakenThemeTokens.colors
  var txType by remember { mutableStateOf(initialTx?.type ?: defaultType) }
  var amountText by remember {
    mutableStateOf(initialTx?.amount?.let { AwakenDateUtils.formatMetric(it) } ?: "")
  }
  var category by remember { mutableStateOf(initialTx?.category ?: "") }
  var description by remember { mutableStateOf(initialTx?.description ?: "") }
  var txDate by remember {
    mutableStateOf(initialTx?.transactionDate?.ifBlank { AwakenDateUtils.todayIst() } ?: AwakenDateUtils.todayIst())
  }
  var validationError by remember { mutableStateOf<String?>(null) }

  val quickCategories = if (txType == TransactionType.INCOME) {
    listOf("Salary", "Freelance", "Consulting", "Business", "Dividend")
  } else {
    listOf("Housing", "Food", "Transport", "Health", "Education", "Utilities")
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = colors.card,
      border = BorderStroke(1.dp, colors.border)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = if (initialTx == null) "Record Transaction" else "Edit Transaction",
          style = MaterialTheme.typography.headlineSmall,
          color = colors.textPrimary
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TransactionType.entries.forEach { t ->
            val selected = txType == t
            FilterChip(
              selected = selected,
              onClick = { txType = t },
              label = { Text(t.label) },
              modifier = Modifier.weight(1f),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (t == TransactionType.INCOME) colors.statusCompletedContainer else colors.statusMissedContainer,
                selectedLabelColor = if (t == TransactionType.INCOME) colors.statusCompleted else colors.statusMissed
              )
            )
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            validationError = null
          },
          label = { Text("Amount (₹ INR)") },
          prefix = { Text("₹ ") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_amount_input")
        )

        OutlinedTextField(
          value = category,
          onValueChange = {
            category = it
            validationError = null
          },
          label = { Text("Category") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tx_category_input")
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickCategories.forEach { cat ->
            FilterChip(
              selected = category.equals(cat, ignoreCase = true),
              onClick = { category = cat },
              label = { Text(cat) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright
              )
            )
          }
        }

        DateSelectorField(
          label = "Date",
          dateKey = txDate,
          onDateSelected = { txDate = it }
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Note (optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (validationError != null) {
          Text(
            text = validationError!!,
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusMissed
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss, enabled = !isSaving) {
            Text("Cancel", color = colors.textSecondary)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val parsedAmount = amountText.trim().toDoubleOrNull()
              if (parsedAmount == null || parsedAmount <= 0.0) {
                validationError = "Amount must be greater than zero."
                return@Button
              }
              if (category.isBlank()) {
                validationError = "Category is required."
                return@Button
              }
              onSave(
                FinancialTransaction(
                  id = initialTx?.id ?: "",
                  transactionType = txType.wireValue,
                  amount = parsedAmount,
                  currency = "INR",
                  category = category.trim(),
                  description = description.trim().takeIf { it.isNotEmpty() },
                  transactionDate = txDate,
                  createdAt = initialTx?.createdAt
                )
              )
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
              containerColor = colors.accent,
              contentColor = colors.onAccent
            ),
            modifier = Modifier.testTag("save_tx_button")
          ) {
            Text(if (isSaving) "Saving..." else "Save")
          }
        }
      }
    }
  }
}
