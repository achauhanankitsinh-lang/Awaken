package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.calc.AwakenCalculations
import com.example.domain.model.FinancialTransaction
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TransactionType
import com.example.domain.util.AwakenDateUtils
import com.example.ui.components.AwakenCard
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IncomeExpenseChart
import com.example.ui.components.StatusPill
import com.example.ui.theme.AwakenThemeTokens

enum class FinanceScope(val label: String) {
  MONTHLY("Monthly"),
  ALL("All Time")
}

@Composable
fun FinancesScreen(
  transactions: List<FinancialTransaction>,
  onAddIncome: () -> Unit,
  onAddExpense: () -> Unit,
  onEditTransaction: (FinancialTransaction) -> Unit,
  onDeleteTransaction: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var scope by remember { mutableStateOf(FinanceScope.MONTHLY) }
  var selectedMonth by remember { mutableStateOf(AwakenDateUtils.currentMonthIst()) }
  var selectedType by remember { mutableStateOf<TransactionType?>(null) }
  var pendingDelete by remember { mutableStateOf<FinancialTransaction?>(null) }

  val monthRange = remember(selectedMonth) {
    AwakenDateUtils.dateRangeForPeriod("$selectedMonth-01", ReportPeriod.MONTHLY)
  }

  val scopedTransactions = remember(transactions, scope, monthRange) {
    if (scope == FinanceScope.MONTHLY) {
      transactions.filter { it.transactionDate in monthRange.startDate..monthRange.endDate }
    } else {
      transactions
    }
  }

  val financialSummary = remember(scopedTransactions) {
    AwakenCalculations.calculateFinancialSummary(scopedTransactions)
  }

  val displayedTransactions = remember(scopedTransactions, selectedType) {
    scopedTransactions.filter { tx ->
      selectedType == null || tx.type == selectedType
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "Finances (INR)",
        style = MaterialTheme.typography.headlineMedium,
        color = colors.textPrimary
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onAddIncome,
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("finances_add_income_button")
        ) {
          Icon(Icons.Outlined.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Income")
        }
        OutlinedButton(
          onClick = onAddExpense,
          modifier = Modifier
            .weight(1f)
            .testTag("finances_add_expense_button")
        ) {
          Text("Add Expense", color = colors.statusMissed)
        }
      }
    }

    item {
      AwakenCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FinanceScope.entries.forEach { sc ->
            FilterChip(
              selected = scope == sc,
              onClick = { scope = sc },
              label = { Text(sc.label) },
              modifier = Modifier.weight(1f),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentContainer,
                selectedLabelColor = colors.accentBright
              )
            )
          }
        }
        if (scope == FinanceScope.MONTHLY) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { selectedMonth = AwakenDateUtils.shiftMonths(selectedMonth, -1) }) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month", tint = colors.textPrimary)
            }
            Text(
              text = AwakenDateUtils.formatMonthTitle(selectedMonth),
              style = MaterialTheme.typography.titleMedium,
              color = colors.textPrimary
            )
            IconButton(onClick = { selectedMonth = AwakenDateUtils.shiftMonths(selectedMonth, 1) }) {
              Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month", tint = colors.textPrimary)
            }
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedType == null,
          onClick = { selectedType = null },
          label = { Text("All") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.accentContainer,
            selectedLabelColor = colors.accentBright
          )
        )
        TransactionType.entries.forEach { t ->
          FilterChip(
            selected = selectedType == t,
            onClick = { selectedType = t },
            label = { Text(t.label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = if (t == TransactionType.INCOME) colors.statusCompletedContainer else colors.statusMissedContainer,
              selectedLabelColor = if (t == TransactionType.INCOME) colors.statusCompleted else colors.statusMissed
            )
          )
        }
      }
    }

    item {
      AwakenCard {
        IncomeExpenseChart(summary = financialSummary)
      }
    }

    if (displayedTransactions.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Outlined.AccountBalanceWallet,
          title = "No transactions",
          actionLabel = "Add Expense",
          onAction = onAddExpense
        )
      }
    } else {
      items(displayedTransactions, key = { it.id }) { tx ->
        val isIncome = tx.type == TransactionType.INCOME
        AwakenCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  text = tx.category,
                  style = MaterialTheme.typography.titleMedium,
                  color = colors.textPrimary
                )
                StatusPill(
                  text = tx.type.label,
                  foreground = if (isIncome) colors.statusCompleted else colors.statusMissed,
                  background = if (isIncome) colors.statusCompletedContainer else colors.statusMissedContainer
                )
              }
              if (!tx.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = tx.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = colors.textSecondary
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = AwakenDateUtils.formatDisplayDate(tx.transactionDate),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = (if (isIncome) "+" else "-") + AwakenDateUtils.formatCurrency(tx.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isIncome) colors.statusCompleted else colors.statusMissed
              )
              Row {
                IconButton(onClick = { onEditTransaction(tx) }) {
                  Icon(Icons.Outlined.Edit, contentDescription = "Edit transaction", tint = colors.textSecondary)
                }
                IconButton(onClick = { pendingDelete = tx }) {
                  Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete transaction", tint = colors.statusMissed)
                }
              }
            }
          }
        }
      }
    }
  }

  pendingDelete?.let { tx ->
    DestructiveConfirmDialog(
      title = "Delete Transaction",
      message = "Delete ${tx.category} (${AwakenDateUtils.formatCurrency(tx.amount)})?",
      onConfirm = {
        onDeleteTransaction(tx.id)
        pendingDelete = null
      },
      onDismiss = { pendingDelete = null }
    )
  }
}
