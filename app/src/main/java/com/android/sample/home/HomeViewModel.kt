package com.android.sample.home

import android.net.Uri
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.BuildConfig
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.conversations.AuthNotReadyException
import com.android.sample.conversations.ConversationRepository
import com.android.sample.conversations.MessageDTO
import com.android.sample.llm.FirebaseFunctionsLlmClient
import com.android.sample.llm.LlmClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

data class SourceMeta(
    val siteLabel: String, // e.g. "EPFL.ch Website"
    val title: String, // e.g. "Projet de Semestre – Bachelor"
    val url: String,
    val retrievedAt: Long = System.currentTimeMillis()
)
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
class HomeViewModel(
    private val llmClient: LlmClient = FirebaseFunctionsLlmClient(),
) : ViewModel() {

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
  /** Public, read-only UI state. */
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  /**
   * Firebase Functions handle for the chat backend. Uses local emulator when configured via
   * BuildConfig flags.
   */
  private val functions: FirebaseFunctions by lazy {
    FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION).apply {
      if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
        useEmulator(BuildConfig.FUNCTIONS_HOST, BuildConfig.FUNCTIONS_PORT)
      }
    }
  }

  private var activeStreamJob: Job? = null
  @Volatile private var userCancelledStream: Boolean = false

  // ---------------------------
  // UI mutations / state helpers
  // ---------------------------
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
                if (cid == null) flowOf(emptyList()) else repo.messagesFlow(cid)
              }
              .collect { msgs ->
                // IMPORTANT: si on est en train de streamer (placeholder + chunks),
                // on NE REMPLACE PAS la liste par Firestore, sinon on perd le streaming visuel.
                val streamingId = _uiState.value.streamingMessageId
                if (streamingId != null) {
                  // On ignore cette emission Firestore pendant le streaming
                  return@collect
                }

                // Pas de streaming en cours → on peut refléter Firestore tel quel
                // MAIS on préserve TOUS les source cards existants (ils ne sont pas dans Firestore)
                _uiState.update { currentState ->
                  val firestoreMessages = msgs.map { it.toUi() }
                  
                  // Collecter TOUS les source cards existants de l'UI (messages avec source mais sans text)
                  val existingSourceCards = currentState.messages.filter { 
                    it.source != null && it.text.isBlank() 
                  }
                  
                  // Construire la liste finale: messages Firestore + source cards préservés
                  val finalMessages = mutableListOf<ChatUIModel>()
                  
                  // D'abord, ajouter tous les messages Firestore
                  finalMessages.addAll(firestoreMessages)
                  
                  // Ensuite, préserver TOUS les source cards existants
                  // On les ajoute après leur message assistant correspondant (si possible)
                  existingSourceCards.forEach { sourceCard ->
                    // Chercher le message assistant qui précède ce source card dans l'UI originale
                    val originalIndex = currentState.messages.indexOfFirst { it.id == sourceCard.id }
                    if (originalIndex > 0) {
                      // Trouver le message assistant qui précède dans l'UI originale
                      val precedingAssistant = currentState.messages[originalIndex - 1]
                      if (precedingAssistant.type == ChatType.AI && precedingAssistant.text.isNotBlank()) {
                        // Chercher ce message dans la nouvelle liste Firestore
                        val firestoreIndex = finalMessages.indexOfFirst { 
                          it.type == ChatType.AI && 
                          it.text == precedingAssistant.text 
                        }
                        if (firestoreIndex >= 0) {
                          // Insérer le source card après ce message assistant
                          finalMessages.add(firestoreIndex + 1, sourceCard)
                        } else {
                          // Le message assistant n'est pas encore dans Firestore, ajouter à la fin
                          finalMessages.add(sourceCard)
                        }
                      } else {
                        // Pas de message assistant précédent, ajouter à la fin
                        finalMessages.add(sourceCard)
                      }
                    } else {
                      // Pas d'index valide, ajouter à la fin
                      finalMessages.add(sourceCard)
                    }
                  }
                  
                  currentState.copy(messages = finalMessages)
                }
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

  /** Control the top-right overflow menu visibility. */
  fun setTopRightOpen(open: Boolean) {
    _uiState.update { it.copy(isTopRightOpen = open) }
  }

  /** Update the current message draft (bound to the input field). */
  fun updateMessageDraft(text: String) {
    _uiState.update { it.copy(messageDraft = text) }
  }

  /** Select a conversation by id and exit the local placeholder state. */
  fun selectConversation(id: String) {
    Log.d(TAG, "selectConversation(): $id")
    isInLocalNewChat = false
    _uiState.update { it.copy(currentConversationId = id) }
  }

  fun hideDeleteConfirmation() {
    _uiState.update { it.copy(showDeleteConfirmation = false) }
  }

  // ============ SENDING MESSAGE -> Firestore ============

  /**
   * Send flow. Guest: append user + AI messages locally. Signed-in: create convo on first send
   * (with quick title), then persist messages; upgrade title once. Send the current draft:
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

    // UI optimiste : on ajoute user + placeholder, on vide l’input, on marque l’état de streaming
    _uiState.update { st ->
      st.copy(
          messages = st.messages + userMsg + placeholder,
          messageDraft = "",
          isSending = true,
          streamingMessageId = aiMessageId,
          streamingSequence = st.streamingSequence + 1)
    }

    viewModelScope.launch {
      try {
        if (isGuest()) {
          // INVITÉ : rien dans Firestore, uniquement streaming UI
          startStreaming(question = msg, messageId = aiMessageId, conversationId = null)
          return@launch
        }

        // CONNECTÉ : s’assurer d’avoir une conversation et persister le message USER tout de suite
        val cid =
            _uiState.value.currentConversationId
                ?: run {
                  val quickTitle = localTitleFrom(msg)
                  val newId = repo.startNewConversation(quickTitle)
                  _uiState.update { it.copy(currentConversationId = newId) }
                  isInLocalNewChat = false

                  // Upgrade du titre en arrière-plan (UNE fois)
                  launch {
                    try {
                      val good = fetchTitle(msg)
                      if (good.isNotBlank() && good != quickTitle) {
                        repo.updateConversationTitle(newId, good)
                      }
                    } catch (_: Exception) {
                      /* keep provisional */
                    }
                  }
                  newId
                }

        isInLocalNewChat = false
        // persister immédiatement le message USER
        repo.appendMessage(cid, "user", msg)

        // démarrer le streaming; à la fin on persiste le message AI
        startStreaming(question = msg, messageId = aiMessageId, conversationId = cid)
      } catch (_: AuthNotReadyException) {
        // rien : l’auth se (re)stabilisera
        setStreamingError(aiMessageId, AuthNotReadyException())
        clearStreamingState(aiMessageId)
      } catch (t: Throwable) {
        setStreamingError(aiMessageId, t)
        clearStreamingState(aiMessageId)
      }
    }
  }

  private fun startStreaming(question: String, messageId: String, conversationId: String?) {
    activeStreamJob?.cancel()
    userCancelledStream = false

    activeStreamJob =
        viewModelScope.launch {
          try {
            val reply = withContext(Dispatchers.IO) { llmClient.generateReply(question) }
            simulateStreamingFromText(messageId, reply.reply)
            reply.url?.let { url ->
              val meta =
                  SourceMeta(
                      siteLabel = buildSiteLabel(url),
                      title = /* if you return primary_title from backend, use it here */
                          buildFallbackTitle(url),
                      url = url)
              _uiState.update { s ->
                s.copy(
                    messages =
                        s.messages +
                            ChatUIModel(
                                id = UUID.randomUUID().toString(),
                                text = "", // card has no body text
                                timestamp = System.currentTimeMillis(),
                                type = ChatType.AI,
                                source = meta // <—— drives the card UI
                                ),
                    streamingSequence = s.streamingSequence + 1)
              }
            }
            // si connecté → persister le message AI une fois le texte complet connu
            if (conversationId != null) {
              try {
                repo.appendMessage(conversationId, "assistant", reply.reply)
              } catch (e: Exception) {
                // on a déjà montré la réponse côté UI; on loggue seulement
                Log.w(TAG, "Failed to persist assistant message: ${e.message}")
              }
            }
          } catch (ce: CancellationException) {
            if (!userCancelledStream) setStreamingError(messageId, ce)
          } catch (t: Throwable) {
            if (!userCancelledStream) setStreamingError(messageId, t)
          } finally {
            clearStreamingState(messageId)
            activeStreamJob = null
            userCancelledStream = false
          }
        }
  }

  private fun buildSiteLabel(url: String): String {
    val host = runCatching { Uri.parse(url).host ?: "" }.getOrNull().orEmpty()
    val clean = host.removePrefix("www.")
    return if (clean.endsWith("epfl.ch")) "EPFL.ch Website" else "$clean Website"
  }

  private fun buildFallbackTitle(url: String): String {
    // Simple fallback: last non-empty path segment or the domain
    val uri = runCatching { Uri.parse(url) }.getOrNull()
    val seg = uri?.pathSegments?.lastOrNull { it.isNotBlank() } ?: ""
    return if (seg.isNotBlank())
        seg.replace('-', ' ').replace('_', ' ').replaceFirstChar { it.uppercase() }
    else (uri?.host?.removePrefix("www.") ?: url)
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

  /** Show the delete confirmation modal. */
  fun showDeleteConfirmation() {
    _uiState.value = _uiState.value.copy(showDeleteConfirmation = true)
  }

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

  override fun onCleared() {
    super.onCleared()
    authListener?.let { auth.removeAuthStateListener(it) }
    authListener = null
    conversationsJob?.cancel()
    conversationsJob = null
    messagesJob?.cancel()
    messagesJob = null
    activeStreamJob?.cancel()
    activeStreamJob = null
  }
}
