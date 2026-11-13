package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.components.AwakenCard
import com.example.ui.theme.AwakenThemeTokens
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.ChatSender
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.CoachRole

@Composable
fun ChatScreen(
  uiState: ChatUiState,
  onSelectRole: (CoachRole) -> Unit,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit,
  onRetry: () -> Unit,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier
) {
  val colors = AwakenThemeTokens.colors
  var inputText by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  val starterPrompts = when (uiState.role) {
    CoachRole.GENERAL -> listOf(
      "Plan my daily routine",
      "Build consistency across 6 areas",
      "Evening review checklist"
    )
    CoachRole.FITNESS -> listOf(
      "30-min strength workout",
      "Balance workout & rest days",
      "5km endurance progression"
    )
    CoachRole.FOCUS -> listOf(
      "90-min deep work protocol",
      "10-min meditation guide",
      "English speaking drill"
    )
    CoachRole.FINANCE_CAREER -> listOf(
      "Simple INR monthly budget rule",
      "Portfolio project roadmap",
      "Freelance client outreach plan"
    )
  }

  LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.lastIndex)
    }
  }

  fun submitInput() {
    val trimmed = inputText.trim()
    if (trimmed.isNotEmpty() && !uiState.isSending) {
      inputText = ""
      onSendMessage(trimmed)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(colors.background)
  ) {
    // Header & Role Selector
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = "AWAKEN Coach",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary
          )
        }
        if (uiState.messages.isNotEmpty()) {
          IconButton(
            onClick = onClearChat,
            enabled = !uiState.isSending,
            modifier = Modifier.testTag("chat_clear_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.DeleteSweep,
              contentDescription = "Clear conversation",
              tint = colors.textSecondary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        CoachRole.entries.forEach { role ->
          FilterChip(
            selected = uiState.role == role,
            onClick = { onSelectRole(role) },
            enabled = !uiState.isSending,
            label = { Text(role.label) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = colors.accentContainer,
              selectedLabelColor = colors.accentBright
            ),
            modifier = Modifier.testTag("chat_role_${role.name.lowercase()}")
          )
        }
      }
    }

    // Error Banner
    if (uiState.errorMessage != null) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.statusMissedContainer,
        border = BorderStroke(1.dp, colors.statusMissed.copy(alpha = 0.45f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = uiState.errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = colors.statusMissed,
            modifier = Modifier.weight(1f)
          )
          Row {
            if (uiState.lastFailedPrompt != null) {
              IconButton(onClick = onRetry) {
                Icon(
                  imageVector = Icons.Outlined.Refresh,
                  contentDescription = "Retry",
                  tint = colors.statusMissed
                )
              }
            }
            TextButton(onClick = onDismissError) {
              Text("Dismiss", color = colors.textPrimary)
            }
          }
        }
      }
    }

    // Message Thread
    if (uiState.messages.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        AwakenCard {
          Text(
            text = "${uiState.role.label} Coach",
            style = MaterialTheme.typography.titleMedium,
            color = colors.accentBright
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Ask for routines, training blocks, focus plans, or INR budgeting advice.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
          )
          Spacer(modifier = Modifier.height(12.dp))
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            starterPrompts.forEach { suggestion ->
              OutlinedButton(
                onClick = { onSendMessage(suggestion) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, colors.border)
              ) {
                Text(
                  text = suggestion,
                  color = colors.textPrimary,
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }
        }
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("chat_messages_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(uiState.messages, key = { it.id }) { message ->
          ChatBubble(message = message)
        }
      }
    }

    // Composer Bar
    Surface(
      color = colors.surface,
      border = BorderStroke(1.dp, colors.border),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Ask ${uiState.role.label} Coach...") },
          maxLines = 4,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = { submitInput() }),
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input")
        )
        Surface(
          onClick = { submitInput() },
          enabled = inputText.isNotBlank() && !uiState.isSending,
          shape = RoundedCornerShape(12.dp),
          color = if (inputText.isNotBlank() && !uiState.isSending) colors.accent else colors.elevated,
          modifier = Modifier
            .size(48.dp)
            .testTag("chat_send_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            if (uiState.isSending) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.accent,
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Send message",
                tint = if (inputText.isNotBlank()) colors.onAccent else colors.textSecondary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
  val colors = AwakenThemeTokens.colors
  val isUser = message.sender == ChatSender.USER

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 14.dp,
        bottomStart = if (isUser) 14.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 14.dp
      ),
      color = if (isUser) colors.accentContainer else colors.card,
      border = BorderStroke(
        width = 1.dp,
        color = if (isUser) colors.accent.copy(alpha = 0.45f) else colors.border
      ),
      modifier = Modifier.widthIn(max = 560.dp)
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (!isUser) {
          Text(
            text = "AWAKEN COACH",
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent
          )
          Spacer(modifier = Modifier.height(4.dp))
        }
        if (message.text.isBlank() && message.isStreaming) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(14.dp),
              color = colors.accent,
              strokeWidth = 2.dp
            )
            Text(
              text = "Thinking...",
              style = MaterialTheme.typography.bodySmall,
              color = colors.textSecondary
            )
          }
        } else {
          Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
          )
        }
      }
    }
  }
}
