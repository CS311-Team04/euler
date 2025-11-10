package com.android.sample.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.BuildConfig
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.conversations.AuthNotReadyException
import com.android.sample.conversations.ConversationRepository
import com.android.sample.conversations.MessageDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * HomeViewModel
 *
 * Drives the chat screen.
 * - Keeps all UI state in [uiState] (drawer, draft text, messages, loading).
 * - Two modes: • Signed in → conversations/messages are read & written in Firestore. • Guest →
 *   everything stays local (no Firestore).
 * - Sends user prompts to the Cloud Function `answerWithRagFn` and shows the reply.
 * - Creates a conversation only on the FIRST message; shows a local placeholder before that.
 * - Generates a quick local title, then upgrades it once via `generateTitleFn`.
 *
 * Lifecycle:
 * - Listens to Firebase auth changes. • On sign-out → stop Firestore, clear UI, start a local empty
 *   chat. • On sign-in → attach Firestore, still show a local empty chat until first send.
 */
class HomeViewModel : ViewModel() {

  companion object {
    private const val TAG = "HomeViewModel"
  }

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
  private val repo = ConversationRepository(auth, db)
  private var isInLocalNewChat = false
  private var conversationsJob: kotlinx.coroutines.Job? = null
  private var messagesJob: kotlinx.coroutines.Job? = null
  private var lastUid: String? = null

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
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  private val functions: FirebaseFunctions by lazy {
    FirebaseFunctions.getInstance("us-central1").apply {
      if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
        useEmulator(BuildConfig.FUNCTIONS_HOST, BuildConfig.FUNCTIONS_PORT)
      }
    }
  }

  // ============ INIT ============

  private var dataStarted = false
  private var authListener: FirebaseAuth.AuthStateListener? = null

  init {
    val current = auth.currentUser?.uid
    lastUid = current

    authListener =
        FirebaseAuth.AuthStateListener { a ->
          val uid = a.currentUser?.uid
          if (uid != lastUid) {
            if (uid == null) {
              // SIGN-OUT → guest
              onSignedOutInternal() // reset UI
              startLocalNewChat() // local blank screen
            } else {
              // SIGN-IN
              startLocalNewChat() // local placeholder until the first send
              startData() // attach Firestore
            }
            lastUid = uid
          }
        }
    auth.addAuthStateListener(requireNotNull(authListener))

    if (current != null) {
      // already signed in
      startLocalNewChat()
      startData()
    } else {
      // guest at startup
      onSignedOutInternal()
      startLocalNewChat()
    }
  }

  /**
   * Returns true when the user is **not** authenticated. Guest mode disables Firestore listeners
   * and keeps all messages in memory only.
   */
  private fun isGuest(): Boolean = auth.currentUser == null

  /**
   * Start Firestore listeners (conversations + messages) **only when signed in**. No-op in guest
   * mode or if already attached.
   *
   * Selection logic:
   * - Keeps current selection if still present.
   * - If nothing is selected and we're not in a local placeholder, auto-select the most recent.
   * - If the list is empty, keep `currentConversationId = null`.
   */
  private fun startData() {
    if (dataStarted || isGuest()) return
    dataStarted = true
    Log.d(TAG, "startData(): attaching listeners (isInLocalNewChat=$isInLocalNewChat)")

    // 1) Conversation list
    conversationsJob?.cancel()
    conversationsJob =
        viewModelScope.launch {
          repo.conversationsFlow().collect { list ->
            Log.d(
                TAG,
                "conversationsFlow -> size=${list.size}, current=${_uiState.value.currentConversationId}, isInLocalNewChat=$isInLocalNewChat")
            _uiState.update { it.copy(conversations = list) }

            val currentId = _uiState.value.currentConversationId
            val hasCurrent = currentId != null && list.any { it.id == currentId }

            when {
              hasCurrent -> Log.d(TAG, "Keeping current conversation $currentId")
              currentId == null && list.isNotEmpty() && !isInLocalNewChat -> {
                val next = list.first().id
                Log.d(TAG, "Auto-selecting conversation $next (not in local placeholder mode)")
                isInLocalNewChat = false
                _uiState.update { it.copy(currentConversationId = next) }
              }
              list.isEmpty() -> {
                Log.d(TAG, "No remote conversations available; staying with null selection")
                _uiState.update { it.copy(currentConversationId = null) }
              }
              else -> Log.d(TAG, "Local new chat active; leaving selection as null")
            }
          }
        }

    // 2) Messages for the selected conversation (flatMapLatest strategy)
    messagesJob?.cancel()
    messagesJob =
        viewModelScope.launch {
          uiState
              .map { it.currentConversationId }
              .distinctUntilChanged()
              .flatMapLatest { cid ->
                if (cid == null) kotlinx.coroutines.flow.flowOf(emptyList())
                else repo.messagesFlow(cid)
              }
              .collect { msgs ->
                _uiState.update { st -> st.copy(messages = msgs.map { it.toUi() }) }
              }
        }
  }

  /**
   * Handles **sign-out** transitions:
   * - Cancels Firestore jobs, clears flags, resets [uiState] to defaults. Used when auth listener
   *   reports `uid == null`.
   */
  private fun onSignedOutInternal() {
    // cancel Firestore listeners
    conversationsJob?.cancel()
    conversationsJob = null
    messagesJob?.cancel()
    messagesJob = null
    dataStarted = false
    isInLocalNewChat = false
    // reset UI
    Log.d(TAG, "onSignedOutInternal(): reset UI state to defaults")
    _uiState.value = HomeUiState() // vide messages, currentConversationId=null, etc.
  }

  /**
   * Enters a **local, unsaved** new chat state:
   * - Clears selection and message list, resets draft and UI flags.
   * - Prevents auto-selection of a remote conversation until the user sends the first message.
   */
  fun startLocalNewChat() {
    isInLocalNewChat = true
    Log.d(TAG, "startLocalNewChat(): clearing current conversation selection")
    _uiState.update {
      it.copy(
          currentConversationId = null,
          messages = emptyList(),
          messageDraft = "",
          isSending = false,
          showDeleteConfirmation = false)
    }
  }

  // ============ UI BASICS (unchanged) ============

  fun toggleDrawer() {
    _uiState.update { it.copy(isDrawerOpen = !it.isDrawerOpen) }
  }

  fun setTopRightOpen(open: Boolean) {
    _uiState.update { it.copy(isTopRightOpen = open) }
  }

  fun updateMessageDraft(text: String) {
    _uiState.update { it.copy(messageDraft = text) }
  }

  fun setLoading(loading: Boolean) {
    _uiState.update { it.copy(isLoading = loading) }
  }

  /** Select a conversation by id and exit the local placeholder state. */
  fun selectConversation(id: String) {
    Log.d(TAG, "selectConversation(): $id")
    isInLocalNewChat = false
    _uiState.update { it.copy(currentConversationId = id) }
  }

  fun showDeleteConfirmation() {
    _uiState.update { it.copy(showDeleteConfirmation = true) }
  }

  fun hideDeleteConfirmation() {
    _uiState.update { it.copy(showDeleteConfirmation = false) }
  }

  // ============ SENDING MESSAGE -> Firestore ============

  /**
   * Send flow. Guest: append user + AI messages locally. Signed-in: create convo on first send
   * (with quick title), then persist messages; upgrade title once.
   */
  fun sendMessage() {
    val draft = _uiState.value.messageDraft.trim()
    if (draft.isEmpty() || _uiState.value.isSending) return
    Log.d(
        TAG,
        "sendMessage(): draft='${draft.take(50)}', current=${_uiState.value.currentConversationId}")

    // Optimistic UI: clear the input and show spinner
    _uiState.update { it.copy(messageDraft = "", isSending = true) }

    viewModelScope.launch {
      try {
        if (isGuest()) {
          // -------- GUEST: fully local, no Firestore ----------
          // append local USER message
          _uiState.update { st ->
            st.copy(
                messages =
                    st.messages +
                        ChatUIModel(
                            id = UUID.randomUUID().toString(),
                            text = draft,
                            timestamp = System.currentTimeMillis(),
                            type = ChatType.USER))
          }
          // call backend function and append local AI message
          val answer = callAnswerWithRag(draft)
          _uiState.update { st ->
            st.copy(
                messages =
                    st.messages +
                        ChatUIModel(
                            id = UUID.randomUUID().toString(),
                            text = answer,
                            timestamp = System.currentTimeMillis(),
                            type = ChatType.AI))
          }
          return@launch
        }

        // -------- SIGNED-IN: Firestore flow ----------
        val cid =
            _uiState.value.currentConversationId
                ?: run {
                  val quickTitle = localTitleFrom(draft)
                  Log.d(
                      TAG,
                      "sendMessage(): creating conversation with provisional title '$quickTitle'")
                  val newId = repo.startNewConversation(quickTitle)
                  _uiState.update { it.copy(currentConversationId = newId) }
                  isInLocalNewChat = false

                  // smart title update in the background (once)
                  launch {
                    try {
                      val good = fetchTitle(draft)
                      if (good.isNotBlank() && good != quickTitle) {
                        Log.d(TAG, "sendMessage(): updating title to '$good' for $newId")
                        repo.updateConversationTitle(newId, good)
                      }
                    } catch (_: Exception) {
                      Log.d(TAG, "sendMessage(): title generation failed, keeping provisional")
                    }
                  }
                  newId
                }
        isInLocalNewChat = false
        Log.d(TAG, "sendMessage(): using conversationId=$cid")

        repo.appendMessage(cid, "user", draft)
        val answer = callAnswerWithRag(draft)
        repo.appendMessage(cid, "assistant", answer)
      } catch (_: AuthNotReadyException) {
        Log.d(TAG, "sendMessage(): auth not ready, deferring")
      } catch (e: Exception) {
        Log.e(TAG, "sendMessage(): failed ${e.message}")
        if (isGuest()) {
          _uiState.update { st ->
            st.copy(
                messages =
                    st.messages +
                        ChatUIModel(
                            id = UUID.randomUUID().toString(),
                            text = "Error: ${e.message ?: "request failed"}",
                            timestamp = System.currentTimeMillis(),
                            type = ChatType.AI))
          }
        } else {
          _uiState.value.currentConversationId?.let { cid ->
            repo.appendMessage(cid, "assistant", "Error: ${e.message ?: "request failed"}")
          }
        }
      } finally {
        _uiState.update { it.copy(isSending = false) }
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

  // ============ DELETING THE CURRENT CONVERSATION ============

  /** Delete current conversation (guest: just clear local; signed-in: remove from Firestore). */
  fun deleteCurrentConversation() {
    if (isGuest()) {
      // guest: simply reset locally
      startLocalNewChat()
      return
    }
    val cid = _uiState.value.currentConversationId ?: return
    Log.d(TAG, "deleteCurrentConversation(): deleting $cid")
    viewModelScope.launch {
      try {
        startLocalNewChat()
        repo.deleteConversation(cid)
      } catch (_: AuthNotReadyException) {} catch (_: Exception) {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
      }
    }
  }

  // ============ BACKEND CHAT ============

  /** Call Cloud Function `answerWithRagFn` and return the reply text. */
  private suspend fun callAnswerWithRag(question: String): String =
      withContext(Dispatchers.IO) {
        val data = hashMapOf("question" to question)
        val result = functions.getHttpsCallable("answerWithRagFn").call(data).await()
        @Suppress("UNCHECKED_CAST")
        val map = result.getData() as? Map<String, Any?> ?: return@withContext "Invalid response"
        (map["reply"] as? String)?.ifBlank { null } ?: "No reply"
      }

  // mapping MessageDTO -> UI
  private fun MessageDTO.toUi(): ChatUIModel =
      ChatUIModel(
          id = UUID.randomUUID().toString(),
          text = this.text,
          timestamp = this.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
          type = if (this.role == "user") ChatType.USER else ChatType.AI)

  /** Make a short provisional title from the first prompt. */
  private fun localTitleFrom(question: String, maxLen: Int = 60, maxWords: Int = 8): String {
    val cleaned = question.replace(Regex("\\s+"), " ").trim()
    val head = cleaned.split(" ").filter { it.isNotBlank() }.take(maxWords).joinToString(" ")
    return (if (head.length <= maxLen) head else head.take(maxLen)).trim()
  }

  /** Ask `generateTitleFn` for a better title; fallback to [localTitleFrom] on errors. */
  private suspend fun fetchTitle(question: String): String {
    return try {
      val res =
          functions.getHttpsCallable("generateTitleFn").call(mapOf("question" to question)).await()
      val t = (res.getData() as? Map<*, *>)?.get("title") as? String
      (t?.takeIf { it.isNotBlank() } ?: localTitleFrom(question)).also {
        Log.d(TAG, "fetchTitle(): generated='$it'")
      }
    } catch (_: Exception) {
      Log.d(TAG, "fetchTitle(): fallback to local extraction")
      localTitleFrom(question)
    }
  }
}
