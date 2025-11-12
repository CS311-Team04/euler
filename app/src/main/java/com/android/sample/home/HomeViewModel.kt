package com.android.sample.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.llm.FirebaseFunctionsLlmClient
import com.android.sample.llm.LlmClient
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HomeViewModel
 *
 * Holds the UI state for the HomeScreen and exposes mutations. The Compose UI collects [uiState]
 * and reacts to updates.
 *
 * Responsibilities:
 * - Manage drawer/menu state
 * - Manage the message draft and the send flow
 * - Append USER/AI messages to the conversation
 * - Call the backend Cloud Function for chat responses
 */
class HomeViewModel(
    private val llmClient: LlmClient = FirebaseFunctionsLlmClient(),
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          HomeUiState(
              systems =
                  listOf(
                      SystemItem(id = "isa", name = "IS-Academia", isConnected = true),
                      SystemItem(id = "moodle", name = "Moodle", isConnected = true),
                      SystemItem(id = "ed", name = "Ed Discussion", isConnected = true),
                      SystemItem(id = "camipro", name = "Camipro", isConnected = false),
                      SystemItem(id = "mail", name = "EPFL Mail", isConnected = false),
                      SystemItem(id = "drive", name = "EPFL Drive", isConnected = true),
                  ),
              messages = emptyList()))
  /** Public, read-only UI state. */
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
  // ---------------------------
  // UI mutations / state helpers
  // ---------------------------

  /** Toggle the navigation drawer state. */
  fun toggleDrawer() {
    _uiState.value = _uiState.value.copy(isDrawerOpen = !_uiState.value.isDrawerOpen)
  }

  /** Control the top-right overflow menu visibility. */
  fun setTopRightOpen(open: Boolean) {
    _uiState.value = _uiState.value.copy(isTopRightOpen = open)
  }

  /** Update the current message draft (bound to the input field). */
  fun updateMessageDraft(text: String) {
    _uiState.value = _uiState.value.copy(messageDraft = text)
  }

  /**
   * Send the current draft:
   * - Guard against concurrent sends and blank drafts.
   * - Append a USER message immediately so the UI feels responsive.
   * - Call the shared [LlmClient] (Firebase/HTTP) on a background coroutine.
   * - Append an AI message (or an error bubble) on completion.
   */
  fun sendMessage() {
    val current = _uiState.value
    if (current.isSending) return
    val msg = current.messageDraft.trim()
    if (msg.isEmpty()) return

    val now = System.currentTimeMillis()
    val userMsg =
        ChatUIModel(
            id = UUID.randomUUID().toString(), text = msg, timestamp = now, type = ChatType.USER)

    _uiState.value =
        current.copy(messages = current.messages + userMsg, messageDraft = "", isSending = true)

    viewModelScope.launch {
      try {
        val answer = llmClient.generateReply(msg) // Suspends off the main thread.
        val aiMsg =
            ChatUIModel(
                id = UUID.randomUUID().toString(),
                text = answer,
                timestamp = System.currentTimeMillis(),
                type = ChatType.AI)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + aiMsg)
      } catch (e: Exception) {
        val errMsg =
            ChatUIModel(
                id = UUID.randomUUID().toString(),
                text = "Error: ${e.message ?: "request failed"}",
                timestamp = System.currentTimeMillis(),
                type = ChatType.AI)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + errMsg)
      } finally {
        // Always stop the global thinking indicator.
        _uiState.value = _uiState.value.copy(isSending = false)
      }
    }
  }

  /** Toggle the connection state of a given EPFL system. */
  fun toggleSystemConnection(systemId: String) {
    val updated =
        _uiState.value.systems.map { s ->
          if (s.id == systemId) s.copy(isConnected = !s.isConnected) else s
        }
    _uiState.value = _uiState.value.copy(systems = updated)
  }

  /** Generic loading flag (unrelated to message sending). */
  fun setLoading(loading: Boolean) {
    _uiState.value = _uiState.value.copy(isLoading = loading)
  }

  /** Clear the conversation. */
  fun clearChat() {
    _uiState.value = _uiState.value.copy(messages = emptyList())
  }

  /** Show/Hide the delete confirmation modal. */
  fun showDeleteConfirmation() {
    _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
  }

  fun hideDeleteConfirmation() {
    _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
  }
}
