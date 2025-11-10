package com.android.sample.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.BuildConfig
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
class HomeViewModel : ViewModel() {

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
    private val endpoint = "http://10.0.2.2:5001/euler-e8edb/us-central1/answerWithRagHttp"
    private val apiKey = "db8e16080302b511c256794b26a6e80089c80e1c15b7927193e754b7fd87fc4e"

    /**
     * Firebase Functions handle for the chat backend. Uses local emulator when configured via
     * BuildConfig flags.
     */
    private val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance("us-central1").apply {
            if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
                useEmulator(BuildConfig.FUNCTIONS_HOST, BuildConfig.FUNCTIONS_PORT)
            }
        }
    }

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
     * - Guard against concurrent sends and blank drafts
     * - Append a USER message immediately
     * - Call the backend for a response
     * - Append an AI message (or an error message) on completion
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
                val answer = callAnswerWithRag(msg)
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

    /**
     * Calls the Cloud Function to get a chat reply for the given [question]. Runs on Dispatchers.IO
     * and returns either the 'reply' string or a fallback.
     *
     * @return The assistant reply, or a short fallback string on invalid payload.
     */
    private suspend fun callAnswerWithRag(question: String): String =
        withContext(Dispatchers.IO) {
            val data = hashMapOf("question" to question) // add "topK"/"model" if needed
            val result =
                withTimeoutOrNull(5_000) {
                    functions.getHttpsCallable("answerWithRagFn").call(data).await()
                } ?: return@withContext "Service unavailable"

            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any?> ?: return@withContext "Invalid response"
            (map["reply"] as? String)?.ifBlank { null } ?: "No reply"
        }
}