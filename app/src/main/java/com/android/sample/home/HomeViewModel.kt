package com.android.sample.home

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.llm.FirebaseFunctionsLlmClient
import com.android.sample.llm.LlmClient
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

  private var activeStreamJob: Job? = null
  @Volatile private var userCancelledStream: Boolean = false

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
    if (current.isSending || current.streamingMessageId != null) return
    val msg = current.messageDraft.trim()
    if (msg.isEmpty()) return

    val now = System.currentTimeMillis()
    val userMsg =
        ChatUIModel(
            id = UUID.randomUUID().toString(), text = msg, timestamp = now, type = ChatType.USER)
    val aiMessageId = UUID.randomUUID().toString()
    val placeholder =
        ChatUIModel(
            id = aiMessageId,
            text = "",
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI,
            isThinking = true)

    _uiState.value =
        current.copy(
            messages = current.messages + userMsg + placeholder,
            messageDraft = "",
            isSending = true,
            streamingMessageId = aiMessageId,
            streamingSequence = current.streamingSequence + 1)

    startStreaming(question = msg, messageId = aiMessageId)
  }

  private fun startStreaming(question: String, messageId: String) {
    activeStreamJob?.cancel()
    userCancelledStream = false

    activeStreamJob =
        viewModelScope.launch {
          try {
            val reply = withContext(Dispatchers.IO) { llmClient.generateReply(question) }
            simulateStreamingFromText(messageId, reply)
          } catch (ce: CancellationException) {
            if (!userCancelledStream) {
              setStreamingError(messageId, ce)
            }
          } catch (t: Throwable) {
            if (userCancelledStream) return@launch
            setStreamingError(messageId, t)
          } finally {
            clearStreamingState(messageId)
            activeStreamJob = null
            userCancelledStream = false
          }
        }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  internal suspend fun simulateStreamingForTest(messageId: String, fullText: String) {
    simulateStreamingFromText(messageId, fullText)
    clearStreamingState(messageId)
  }

  private suspend fun appendStreamingChunk(messageId: String, chunk: String) =
      withContext(Dispatchers.Main) {
        _uiState.update { state ->
          val updated =
              state.messages.map { message ->
                if (message.id == messageId) {
                  message.copy(text = message.text + chunk, isThinking = false)
                } else {
                  message
                }
              }
          state.copy(
              messages = updated,
              streamingMessageId = messageId,
              streamingSequence = state.streamingSequence + 1)
        }
      }

  private suspend fun markMessageFinished(messageId: String) =
      withContext(Dispatchers.Main) {
        _uiState.update { state ->
          val updated =
              state.messages.map { message ->
                if (message.id == messageId) message.copy(isThinking = false) else message
              }
          state.copy(messages = updated, streamingSequence = state.streamingSequence + 1)
        }
      }

  private suspend fun setStreamingText(messageId: String, text: String) =
      withContext(Dispatchers.Main) {
        _uiState.update { state ->
          val updated =
              state.messages.map { message ->
                if (message.id == messageId) message.copy(text = text, isThinking = false)
                else message
              }
          state.copy(messages = updated, streamingSequence = state.streamingSequence + 1)
        }
      }

  private suspend fun setStreamingError(messageId: String, error: Throwable) {
    val message = error.message?.takeIf { it.isNotBlank() } ?: "request failed"
    setStreamingText(messageId, "Erreur: $message")
  }

  private suspend fun simulateStreamingFromText(messageId: String, fullText: String) {
    // Use current dispatcher instead of Dispatchers.Default to allow test control
    val pattern = Regex("\\S+\\s*")
    val parts = pattern.findAll(fullText).map { it.value }.toList().ifEmpty { listOf(fullText) }
    for (chunk in parts) {
      // delay() already checks for cancellation, so ensureActive() is not strictly necessary
      appendStreamingChunk(messageId, chunk)
      delay(60)
    }
    markMessageFinished(messageId)
  }

  private suspend fun clearStreamingState(messageId: String) =
      withContext(Dispatchers.Main) {
        _uiState.update { state ->
          if (state.streamingMessageId == messageId) {
            state.copy(streamingMessageId = null, isSending = false)
          } else {
            state.copy(isSending = false)
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
    userCancelledStream = true
    activeStreamJob?.cancel()
    activeStreamJob = null
    _uiState.update { state ->
      state.copy(
          messages = emptyList(),
          streamingMessageId = null,
          isSending = false,
          streamingSequence = state.streamingSequence + 1)
    }
    userCancelledStream = false
  }

  /** Show/Hide the delete confirmation modal. */
  fun showDeleteConfirmation() {
    _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
  }

  fun hideDeleteConfirmation() {
    _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
  }
}
