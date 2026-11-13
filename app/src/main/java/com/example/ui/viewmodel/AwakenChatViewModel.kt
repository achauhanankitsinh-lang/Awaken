package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

enum class ChatSender {
  USER,
  MODEL
}

enum class CoachRole(
  val label: String,
  val systemInstruction: String
) {
  GENERAL(
    label = "General",
    systemInstruction = "You are AWAKEN Coach, a concise, disciplined personal development strategist. " +
      "You help the owner improve across six core areas: Physical, Mental, Learning, Communication, " +
      "Financial (always in INR / ₹), and Career. Give direct, structured, actionable guidance without filler."
  ),
  FITNESS(
    label = "Fitness",
    systemInstruction = "You are AWAKEN Physical Coach, an evidence-based strength and conditioning trainer. " +
      "Help plan workouts, progressive overload, recovery/rest days, and consistency. Keep responses concise and practical."
  ),
  FOCUS(
    label = "Focus",
    systemInstruction = "You are AWAKEN Focus Mentor, specializing in meditation, deep work, deliberate learning, " +
      "and clear speaking/writing practice. Provide crisp, realistic protocols."
  ),
  FINANCE_CAREER(
    label = "Career & ₹",
    systemInstruction = "You are AWAKEN Career & Finance Advisor. Help plan portfolio projects, career milestones, " +
      "and personal cashflow/budgeting strictly in Indian Rupees (INR / ₹). Be concise and practical."
  )
}

data class ChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val sender: ChatSender,
  val text: String,
  val isStreaming: Boolean = false
)

data class ChatUiState(
  val role: CoachRole = CoachRole.GENERAL,
  val messages: List<ChatMessage> = emptyList(),
  val isSending: Boolean = false,
  val errorMessage: String? = null,
  val lastFailedPrompt: String? = null
)

class AwakenChatViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(ChatUiState())
  val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

  private val defaultModel: GenerativeModel by lazy {
    createModel(CoachRole.GENERAL)
  }

  private val roleModels = mutableMapOf<CoachRole, GenerativeModel>()
  private var activeChat: Chat? = null
  private var activeChatRole: CoachRole? = null

  private fun createModel(role: CoachRole): GenerativeModel {
    return Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      systemInstruction = content { text(role.systemInstruction) }
    )
  }

  private fun getOrCreateChat(role: CoachRole): Chat {
    val existing = activeChat
    if (existing != null && activeChatRole == role) {
      return existing
    }
    val model = if (role == CoachRole.GENERAL) {
      defaultModel
    } else {
      roleModels.getOrPut(role) { createModel(role) }
    }
    val history = _uiState.value.messages
      .filter { !it.isStreaming && it.text.isNotBlank() }
      .map { msg ->
        content(if (msg.sender == ChatSender.USER) "user" else "model") {
          text(msg.text)
        }
      }
    return model.startChat(history = history).also {
      activeChat = it
      activeChatRole = role
    }
  }

  fun selectRole(role: CoachRole) {
    if (_uiState.value.isSending || _uiState.value.role == role) return
    activeChat = null
    activeChatRole = null
    _uiState.update {
      it.copy(
        role = role,
        messages = emptyList(),
        errorMessage = null,
        lastFailedPrompt = null
      )
    }
  }

  fun clearConversation() {
    if (_uiState.value.isSending) return
    activeChat = null
    activeChatRole = null
    _uiState.update {
      it.copy(
        messages = emptyList(),
        errorMessage = null,
        lastFailedPrompt = null
      )
    }
  }

  fun dismissError() {
    _uiState.update { it.copy(errorMessage = null, lastFailedPrompt = null) }
  }

  fun sendMessage(rawText: String) {
    val prompt = rawText.trim()
    if (prompt.isEmpty() || _uiState.value.isSending) return

    val currentRole = _uiState.value.role
    val userMessage = ChatMessage(sender = ChatSender.USER, text = prompt)
    val assistantMessageId = UUID.randomUUID().toString()
    val placeholderAssistant = ChatMessage(
      id = assistantMessageId,
      sender = ChatSender.MODEL,
      text = "",
      isStreaming = true
    )

    _uiState.update { state ->
      state.copy(
        messages = state.messages + userMessage + placeholderAssistant,
        isSending = true,
        errorMessage = null,
        lastFailedPrompt = null
      )
    }

    viewModelScope.launch {
      val accumulated = StringBuilder()
      try {
        val chat = getOrCreateChat(currentRole)
        withTimeout(REQUEST_TIMEOUT_MS) {
          chat.sendMessageStream(prompt).collect { chunk ->
            val piece = chunk.text.orEmpty()
            if (piece.isNotEmpty()) {
              accumulated.append(piece)
              val snapshot = accumulated.toString()
              _uiState.update { state ->
                state.copy(
                  messages = state.messages.map { msg ->
                    if (msg.id == assistantMessageId) msg.copy(text = snapshot) else msg
                  }
                )
              }
            }
          }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isEmpty()) {
          throw IllegalStateException("Model returned an empty response.")
        }

        _uiState.update { state ->
          state.copy(
            messages = state.messages.map { msg ->
              if (msg.id == assistantMessageId) {
                msg.copy(text = finalText, isStreaming = false)
              } else {
                msg
              }
            },
            isSending = false
          )
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini chat request timed out", e)
        handleFailure(
          assistantMessageId = assistantMessageId,
          partialText = accumulated.toString(),
          prompt = prompt,
          errorMessage = "Request timed out after 60 seconds. Please try again."
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI chat failed", e)
        handleFailure(
          assistantMessageId = assistantMessageId,
          partialText = accumulated.toString(),
          prompt = prompt,
          errorMessage = e.localizedMessage ?: "Unable to get response from Gemini."
        )
      }
    }
  }

  fun retryLastFailed() {
    val failed = _uiState.value.lastFailedPrompt ?: return
    // Remove the previous failed user prompt before resending to avoid duplicates
    _uiState.update { state ->
      val trimmed = if (state.messages.lastOrNull()?.sender == ChatSender.USER &&
        state.messages.lastOrNull()?.text == failed
      ) {
        state.messages.dropLast(1)
      } else {
        state.messages
      }
      state.copy(messages = trimmed, errorMessage = null, lastFailedPrompt = null)
    }
    sendMessage(failed)
  }

  private fun handleFailure(
    assistantMessageId: String,
    partialText: String,
    prompt: String,
    errorMessage: String
  ) {
    activeChat = null
    activeChatRole = null
    _uiState.update { state ->
      val updatedMessages = if (partialText.isBlank()) {
        state.messages.filterNot { it.id == assistantMessageId }
      } else {
        state.messages.map { msg ->
          if (msg.id == assistantMessageId) msg.copy(text = partialText, isStreaming = false) else msg
        }
      }
      state.copy(
        messages = updatedMessages,
        isSending = false,
        errorMessage = errorMessage,
        lastFailedPrompt = prompt
      )
    }
  }

  private companion object {
    const val TAG = "AwakenChatViewModel"
    const val MODEL_NAME = "gemini-3.8-flash"
    const val REQUEST_TIMEOUT_MS = 60_000L
  }
}
