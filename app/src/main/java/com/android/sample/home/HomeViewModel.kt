package com.android.sample.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.BuildConfig
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
class HomeViewModel : ViewModel() {
  companion object {
    private const val TAG = "HomeViewModel"
  }

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
  // private val endpoint = "http://10.0.2.2:5001/euler-e8edb/us-central1/answerWithRagHttp"
  // private val apiKey = "db8e16080302b511c256794b26a6e80089c80e1c15b7927193e754b7fd87fc4e"

  // Auth / Firestore handles
  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
  private var conversationId: String? = null

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

    // optimistic UI update
    _uiState.value =
        current.copy(messages = current.messages + userMsg, messageDraft = "", isSending = true)

    viewModelScope.launch {
      try {
        // Write message to Firestore and await rolling summary
        val uid = auth.currentUser?.uid
        val cid = ensureConversation(uid)
        val mid = userMsg.id
        if (uid != null && cid != null) {
          val messagesCol =
              firestore
                  .collection("users")
                  .document(uid)
                  .collection("conversations")
                  .document(cid)
                  .collection("messages")
          val payload =
              hashMapOf(
                  "content" to msg,
                  "role" to "user",
                  "createdAt" to now,
              )
          messagesCol.document(mid).set(payload).await()
        }

        val summary: String? =
            if (uid != null && conversationId != null) {
              val prior = fetchPriorSummary(uid, conversationId!!, mid)
              val source = if (prior != null) "prior" else "none"
              Log.d(
                  TAG,
                  "answerWithRagFn summarySource=$source len=${prior?.length ?: 0} " +
                      "head='${prior?.take(2000) ?: ""}'")
              prior
            } else null

        val recentTranscript: String? =
            if (uid != null && conversationId != null) buildRecentTranscript(uid, conversationId!!, mid)
            else null

        val answer = callAnswerWithRag(msg, summary, recentTranscript)
        val aiMsg =
            ChatUIModel(
                id = UUID.randomUUID().toString(),
                text = answer,
                timestamp = System.currentTimeMillis(),
                type = ChatType.AI)
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + aiMsg)

        // Persist assistant message (enables summary to include assistant replies too)
        val uid2 = auth.currentUser?.uid
        val cid2 = conversationId
        if (uid2 != null && cid2 != null) {
          val messagesCol =
              firestore
                  .collection("users")
                  .document(uid2)
                  .collection("conversations")
                  .document(cid2)
                  .collection("messages")
          val payload =
              hashMapOf(
                  "content" to answer,
                  "role" to "assistant",
                  "createdAt" to System.currentTimeMillis(),
              )
          messagesCol.document(aiMsg.id).set(payload).await()
        }
      } catch (e: Exception) {
        val details: String? =
            (e as? FirebaseFunctionsException)?.details as? String
        val code: String? =
            (e as? FirebaseFunctionsException)?.code?.name
        val errText =
            buildString {
              append("Error")
              if (!code.isNullOrBlank()) append(" [").append(code).append("]")
              append(": ")
              append(details ?: e.message ?: "request failed")
            }
        val errMsg =
            ChatUIModel(
                id = UUID.randomUUID().toString(),
                text = errText,
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
  private suspend fun callAnswerWithRag(question: String, summary: String?, transcript: String?): String =
      withContext(Dispatchers.IO) {
        val data =
            hashMapOf<String, Any>("question" to question).apply {
              summary?.let { put("summary", it) }
              transcript?.let { put("recentTranscript", it) }
            } // add "topK"/"model" if needed
        val result = functions.getHttpsCallable("answerWithRagFn").call(data).await()

        @Suppress("UNCHECKED_CAST")
        val map = result.getData() as? Map<String, Any?> ?: return@withContext "Invalid response"
        (map["reply"] as? String)?.ifBlank { null } ?: "No reply"
      }

  /**
   * Ensure a conversation document exists for the current session. Creates one if needed and caches
   * its ID locally.
   */
  private suspend fun ensureConversation(uid: String?): String? {
    val userId = uid ?: return null
    conversationId?.let {
      return it
    }
    val newId = UUID.randomUUID().toString()
    val ref =
        firestore.collection("users").document(userId).collection("conversations").document(newId)
    val data = hashMapOf("createdAt" to System.currentTimeMillis())
    ref.set(data).await()
    conversationId = newId
    return newId
  }

  /**
   * Fallback: returns the most recent available summary from previous messages
   * (excludes the current [mid]). Useful when the new summary isn't ready yet.
   */
  private suspend fun fetchPriorSummary(uid: String, cid: String, currentMid: String): String? {
    val col =
        firestore
            .collection("users")
            .document(uid)
            .collection("conversations")
            .document(cid)
            .collection("messages")
    val snap = col.orderBy("createdAt").limitToLast(30).get().await()
    val docs = snap.documents
    for (i in docs.size - 1 downTo 0) {
      val d = docs[i]
      if (d.id == currentMid) continue
      val s = d.getString("summary")?.trim()
      if (!s.isNullOrEmpty()) return s
    }
    return null
  }

  /**
   * Builds a compact transcript window containing ONLY the previous round
   * (last user message + last assistant reply), to complement the prior summary.
   */
  private suspend fun buildRecentTranscript(uid: String, cid: String, currentMid: String): String? {
    val col =
        firestore
            .collection("users")
            .document(uid)
            .collection("conversations")
            .document(cid)
            .collection("messages")
    // Fetch a small tail and then pick the two messages immediately preceding currentMid
    val snap = col.orderBy("createdAt").limitToLast(4).get().await()
    if (snap.isEmpty) return null
    val docs = snap.documents
      .filter { it.id != currentMid }
      .takeLast(2) // previous user + assistant, if present
    if (docs.isEmpty()) return null
    val lines =
        docs.map { d ->
          val role = (d.getString("role") ?: "").lowercase()
          val content = d.getString("content")?.trim().orEmpty()
          val tag = if (role == "assistant") "Assistant" else "Utilisateur"
          "$tag: $content"
        }
    val txt = lines.joinToString("\n")
    return if (txt.isBlank()) null else txt.take(600)
  }
}
