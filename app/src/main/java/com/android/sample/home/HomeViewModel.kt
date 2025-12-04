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
import com.android.sample.conversations.ConversationTitleFormatter
import com.android.sample.conversations.MessageDTO
import com.android.sample.llm.BotReply
import com.android.sample.llm.FirebaseFunctionsLlmClient
import com.android.sample.llm.LlmClient
import com.android.sample.network.NetworkConnectivityMonitor
import com.android.sample.profile.UserProfile
import com.android.sample.profile.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.UUID
import kotlin.getValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SourceMeta(
    val siteLabel: String, // e.g. "EPFL.ch Website" or "Your Schedule"
    val title: String, // e.g. "Projet de Semestre – Bachelor"
    val url: String?, // null for schedule sources
    val retrievedAt: Long = System.currentTimeMillis(),
    val isScheduleSource: Boolean = false // true if from user's EPFL schedule
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
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repo: ConversationRepository =
        ConversationRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()),
    private val profileRepository: com.android.sample.profile.ProfileDataSource =
        UserProfileRepository(),
    private val networkMonitor: NetworkConnectivityMonitor? = null
) : ViewModel() {
  companion object {
    private const val TAG = "HomeViewModel"
    private const val DEFAULT_USER_NAME = "Student"
    private const val ED_INTENT_POST_QUESTION = "post_question"

    // Global exception handler for uncaught coroutine exceptions
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
      Log.e(TAG, "Uncaught exception in coroutine", throwable)
    }
  }

  // Auth / Firestore handles
  private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
  private var conversationId: String? = null

  // private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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
  private var lastUserPrompt: String? = null

  // ---------------------------
  // UI mutations / state helpers
  // ---------------------------
  // ============ INIT ============

  private var dataStarted = false
  private var authListener: FirebaseAuth.AuthStateListener? = null
  private var networkMonitorJob: Job? = null

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

    // Monitor network connectivity
    networkMonitor?.let { monitor ->
      networkMonitorJob =
          viewModelScope.launch {
            monitor.isOnline.collectLatest { isOnline ->
              _uiState.update { state ->
                val wasOffline = state.isOffline
                val newState = state.copy(isOffline = !isOnline)
                // Show offline message when going offline, but only if user is signed in
                if (!isOnline && !wasOffline && auth.currentUser != null) {
                  newState.copy(showOfflineMessage = true)
                } else if (isOnline && wasOffline) {
                  // Hide offline message when back online
                  newState.copy(showOfflineMessage = false)
                } else {
                  newState
                }
              }
            }
          }
    }

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
  @OptIn(ExperimentalCoroutinesApi::class)
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
                val streamingId = _uiState.value.streamingMessageId
                if (streamingId != null) {

                  return@collect
                }

                _uiState.update { currentState ->
                  val firestoreMessages = msgs.map { it.toUi() }

                  val existingSourceCards =
                      currentState.messages.filter { it.source != null && it.text.isBlank() }

                  val finalMessages = mutableListOf<ChatUIModel>()

                  finalMessages.addAll(firestoreMessages)

                  existingSourceCards.forEach { sourceCard ->
                    val originalIndex =
                        currentState.messages.indexOfFirst { it.id == sourceCard.id }
                    if (originalIndex > 0) {

                      val precedingAssistant = currentState.messages[originalIndex - 1]
                      if (precedingAssistant.type == ChatType.AI &&
                          precedingAssistant.text.isNotBlank()) {

                        val firestoreIndex =
                            finalMessages.indexOfFirst {
                              it.type == ChatType.AI && it.text == precedingAssistant.text
                            }
                        if (firestoreIndex >= 0) {

                          finalMessages.add(firestoreIndex + 1, sourceCard)
                        } else {

                          finalMessages.add(sourceCard)
                        }
                      } else {

                        finalMessages.add(sourceCard)
                      }
                    } else {

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
    // reset UI but preserve systems list
    Log.d(TAG, "onSignedOutInternal(): reset UI state to defaults")
    val currentSystems = _uiState.value.systems
    _uiState.value =
        HomeUiState(systems = currentSystems) // preserve systems, reset everything else
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

  fun setGuestMode(isGuest: Boolean) {
    if (isGuest) {
      _uiState.value =
          _uiState.value.copy(
              isGuest = true, profile = null, userName = "guest", showGuestProfileWarning = false)
    } else {
      _uiState.value =
          _uiState.value.copy(
              isGuest = false,
              userName = _uiState.value.userName.takeIf { it.isNotBlank() } ?: DEFAULT_USER_NAME)
    }
  }

  fun refreshProfile() {
    viewModelScope.launch {
      try {
        val profile = profileRepository.loadProfile()
        _uiState.value =
            if (profile != null) {
              _uiState.value.copy(
                  profile = profile,
                  userName =
                      profile.preferredName
                          .ifBlank { profile.fullName }
                          .ifBlank { DEFAULT_USER_NAME },
                  isGuest = false)
            } else {
              _uiState.value.copy(
                  profile = null,
                  userName =
                      _uiState.value.userName.takeIf { it.isNotBlank() } ?: DEFAULT_USER_NAME,
                  isGuest = false)
            }
      } catch (t: Throwable) {
        Log.e("HomeViewModel", "Failed to load profile", t)
      }
    }
  }

  fun saveProfile(profile: UserProfile) {
    viewModelScope.launch {
      try {
        profileRepository.saveProfile(profile)
        _uiState.value =
            _uiState.value.copy(
                profile = profile,
                userName =
                    profile.preferredName
                        .ifBlank { profile.fullName }
                        .ifBlank { DEFAULT_USER_NAME },
                isGuest = false)
      } catch (t: Throwable) {
        Log.e("HomeViewModel", "Failed to save profile", t)
      }
    }
  }

  fun clearProfile() {
    _uiState.value =
        _uiState.value.copy(
            profile = null,
            userName = DEFAULT_USER_NAME,
            isGuest = false,
            showGuestProfileWarning = false)
  }

  fun showGuestProfileWarning() {
    _uiState.value = _uiState.value.copy(showGuestProfileWarning = true)
  }

  fun hideGuestProfileWarning() {
    _uiState.value = _uiState.value.copy(showGuestProfileWarning = false)
  }

  /** Control the top-right overflow menu visibility. */
  fun setTopRightOpen(open: Boolean) {
    _uiState.update { it.copy(isTopRightOpen = open) }
  }

  /** Update the current message draft (bound to the input field). */
  fun updateMessageDraft(text: String) {
    _uiState.update { it.copy(messageDraft = text) }
  }

  /** Dismiss the offline message. */
  fun dismissOfflineMessage() {
    _uiState.update { it.copy(showOfflineMessage = false) }
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
   * Helper function to handle errors when sending a message. Updates the UI to show the error
   * message and clears streaming state.
   */
  private suspend fun handleSendMessageError(error: Throwable, aiMessageId: String) {
    val details: String? = (error as? FirebaseFunctionsException)?.details as? String
    val code: String? = (error as? FirebaseFunctionsException)?.code?.name
    val errText = buildString {
      append("Error")
      if (!code.isNullOrBlank()) append(" [").append(code).append("]")
      append(": ")
      append(details ?: error.message ?: "request failed")
    }
    try {
      _uiState.update { state ->
        state.copy(
            messages =
                state.messages.map { msg ->
                  if (msg.id == aiMessageId) {
                    msg.copy(text = errText, isThinking = false)
                  } else {
                    msg
                  }
                },
            isSending = false,
            streamingMessageId = null)
      }
      clearStreamingState(aiMessageId)
    } catch (ex: Exception) {
      Log.e(TAG, "Error updating UI with error message", ex)
      _uiState.update { it.copy(isSending = false, streamingMessageId = null) }
    }
  }

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
    // Don't allow sending messages when offline - check both state and actual connectivity
    if (current.isOffline) {
      // Update state to show offline message if not already shown
      if (!current.showOfflineMessage && auth.currentUser != null) {
        _uiState.update { it.copy(showOfflineMessage = true) }
      }
      return
    }
    // Double-check actual connectivity as a safety measure
    if (networkMonitor?.isCurrentlyOnline() == false) {
      _uiState.update {
        it.copy(
            isOffline = true,
            showOfflineMessage = if (auth.currentUser != null) true else it.showOfflineMessage)
      }
      return
    }
    val msg = current.messageDraft.trim()
    if (msg.isEmpty()) return

    // Store for retry functionality
    lastUserPrompt = msg

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

    // UI optimiste : on ajoute user + placeholder, on vide l'input, on marque l'état de streaming
    _uiState.update { st ->
      st.copy(
          messages = st.messages + userMsg + placeholder,
          messageDraft = "",
          isSending = true,
          streamingMessageId = aiMessageId,
          streamingSequence = st.streamingSequence + 1)
    }

    viewModelScope.launch(exceptionHandler) {
      try {
        Log.d(TAG, "sendMessage: starting, message='${msg.take(50)}...'")
        // GUEST: no Firestore, just streaming UI
        if (isGuest()) {
          Log.d(TAG, "sendMessage: guest mode, starting streaming")
          startStreaming(
              question = msg,
              messageId = aiMessageId,
              conversationId = null,
              summary = null,
              transcript = null)
          return@launch
        }

        // CONNECTED: ensure we have a conversation and persist the USER message immediately (repo)
        val cid =
            _uiState.value.currentConversationId
                ?: run {
                  val quickTitle = ConversationTitleFormatter.localTitleFrom(msg)
                  val newId = repo.startNewConversation(quickTitle)
                  _uiState.update { it.copy(currentConversationId = newId) }
                  isInLocalNewChat = false

                  // Upgrade du titre en arrière-plan (UNE fois)
                  launch {
                    try {
                      val good = ConversationTitleFormatter.fetchTitle(functions, msg, TAG)
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
        // Persister immédiatement le message USER côté repo
        repo.appendMessage(cid, "user", msg)

        // ---------- Firestore + RAG (from second snippet) ----------

        // Note: User message is already persisted via repo.appendMessage() above
        // The direct Firestore write was removed to avoid duplicate writes and timestamp type
        // conflicts

        val uid = auth.currentUser?.uid
        val conversationId = cid // unify naming

        // Récupérer le résumé précédent (rolling summary) si disponible
        val summary: String? =
            if (uid != null && conversationId != null) {
              val prior = fetchPriorSummary(uid, conversationId, userMsg.id)
              val source = if (prior != null) "prior" else "none"
              Log.d(
                  TAG,
                  "answerWithRagFn summarySource=$source len=${prior?.length ?: 0} " +
                      "head='${prior?.take(2000) ?: ""}'")
              prior
            } else null

        // Construire un transcript récent si nécessaire
        val recentTranscript: String? =
            if (uid != null && conversationId != null) {
              buildRecentTranscript(uid, conversationId, userMsg.id)
            } else null

        // Appel RAG
        startStreaming(
            question = msg,
            messageId = aiMessageId,
            conversationId = conversationId,
            summary = summary,
            transcript = recentTranscript)
      } catch (_: AuthNotReadyException) {
        // L'auth n'est pas prête : côté UI, on signale une erreur de streaming / envoi
        try {
          setStreamingError(aiMessageId, AuthNotReadyException())
          clearStreamingState(aiMessageId)
        } catch (ex: Exception) {
          Log.e(TAG, "Error setting streaming error state", ex)
          _uiState.update { it.copy(isSending = false, streamingMessageId = null) }
        }
      } catch (e: Exception) {
        // Erreurs back-end / Functions
        Log.e(TAG, "Error sending message", e)
        handleSendMessageError(e, aiMessageId)
      } catch (t: Throwable) {
        // Catch any other Throwable (Error, etc.) to prevent crashes
        Log.e(TAG, "Unexpected error sending message", t)
        handleSendMessageError(t, aiMessageId)
      } finally {
        // Toujours arrêter l'indicateur global d'envoi
        try {
          _uiState.update { it.copy(isSending = false) }
        } catch (ex: Exception) {
          Log.e(TAG, "Error updating isSending state in finally", ex)
        }
      }
    }
  }
  /** Streaming helper using [LlmClient] and optional summary/transcript. */
  private fun startStreaming(
      question: String,
      messageId: String,
      conversationId: String?,
      summary: String?,
      transcript: String?
  ) {
    activeStreamJob?.cancel()
    userCancelledStream = false

    activeStreamJob =
        viewModelScope.launch(exceptionHandler) {
          try {
            Log.d(
                TAG,
                "startStreaming: calling llmClient.generateReply for messageId=$messageId, question='${question.take(50)}...'")
            val reply =
                try {
                  withContext(Dispatchers.IO) {
                    llmClient.generateReply(
                        prompt = question, summary = summary, transcript = transcript)
                  }
                } catch (e: FirebaseFunctionsException) {
                  Log.e(
                      TAG,
                      "Firebase Functions exception in startStreaming: code=${e.code}, message=${e.message}",
                      e)
                  throw e
                } catch (e: Exception) {
                  Log.e(TAG, "Exception in llmClient.generateReply: ${e.javaClass.simpleName}", e)
                  throw e
                } catch (t: Throwable) {
                  Log.e(
                      TAG,
                      "Unexpected throwable in llmClient.generateReply: ${t.javaClass.simpleName}",
                      t)
                  throw t
                }
            Log.d(
                TAG,
                "startStreaming: received reply, length=${reply.reply.length}, edIntentDetected=${reply.edIntent.detected}, edIntent=${reply.edIntent.intent}")

            // Handle ED intent detection - create PostOnEd pending action
            handleEdIntent(reply, question)

            // simulate stream into the placeholder AI message
            simulateStreamingFromText(messageId, reply.reply)

            // add optional source card based on source type
            val meta: SourceMeta? =
                when (reply.sourceType) {
                  com.android.sample.llm.SourceType.SCHEDULE -> {
                    // Schedule source - show a small indicator
                    SourceMeta(
                        siteLabel = "Your EPFL Schedule",
                        title = "Retrieved from your connected calendar",
                        url = null,
                        isScheduleSource = true)
                  }
                  com.android.sample.llm.SourceType.RAG -> {
                    // RAG source - show the web source card if URL exists
                    reply.url?.let { url ->
                      SourceMeta(
                          siteLabel = buildSiteLabel(url),
                          title = buildFallbackTitle(url),
                          url = url,
                          isScheduleSource = false)
                    }
                  }
                  com.android.sample.llm.SourceType.NONE -> null
                }

            meta?.let { sourceMeta ->
              _uiState.update { s ->
                s.copy(
                    messages =
                        s.messages +
                            ChatUIModel(
                                id = UUID.randomUUID().toString(),
                                text = "",
                                timestamp = System.currentTimeMillis(),
                                type = ChatType.AI,
                                source = sourceMeta),
                    streamingSequence = s.streamingSequence + 1)
              }
            }

            // Persist assistant message if we are signed in
            if (conversationId != null) {
              try {
                // Use repository method which handles timestamps correctly
                repo.appendMessage(conversationId, "assistant", reply.reply)
              } catch (e: Exception) {
                Log.w(TAG, "Failed to persist assistant message: ${e.message}")
              }
            }
          } catch (ce: CancellationException) {
            if (!userCancelledStream) {
              try {
                setStreamingError(messageId, ce)
              } catch (ex: Exception) {
                Log.e(TAG, "Error setting streaming error for cancellation", ex)
              }
            }
          } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error during streaming", t)
            if (!userCancelledStream) {
              try {
                setStreamingError(messageId, t)
              } catch (ex: Exception) {
                Log.e(TAG, "Error setting streaming error", ex)
                // Fallback: directly update UI state
                _uiState.update { state ->
                  state.copy(
                      messages =
                          state.messages.map { msg ->
                            if (msg.id == messageId) {
                              msg.copy(
                                  text = "Error: ${t.message ?: "Unknown error"}",
                                  isThinking = false)
                            } else {
                              msg
                            }
                          },
                      streamingMessageId = null,
                      isSending = false)
                }
              }
            }
          } finally {
            try {
              clearStreamingState(messageId)
            } catch (ex: Exception) {
              Log.e(TAG, "Error clearing streaming state", ex)
              _uiState.update { it.copy(streamingMessageId = null, isSending = false) }
            }
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

  /** Stop the current AI response generation. */
  fun stopGeneration() {
    val streamingId = _uiState.value.streamingMessageId ?: return
    userCancelledStream = true
    activeStreamJob?.cancel()
    activeStreamJob = null

    // Mark the message as finished with whatever content it has
    _uiState.update { state ->
      val updated =
          state.messages.map { msg ->
            if (msg.id == streamingId) {
              val text = if (msg.text.isBlank()) "Generation stopped" else msg.text
              msg.copy(text = text, isThinking = false, wasStopped = true)
            } else {
              msg
            }
          }
      state.copy(
          messages = updated,
          streamingMessageId = null,
          isSending = false,
          streamingSequence = state.streamingSequence + 1)
    }
    userCancelledStream = false
  }

  /** Retry the last user message. Removes the last AI response and re-sends. */
  fun retryLastMessage() {
    val prompt = lastUserPrompt ?: return
    val current = _uiState.value
    if (current.isSending || current.streamingMessageId != null) return

    // Find and remove the last AI message
    val messages = current.messages.toMutableList()
    val lastAiIndex = messages.indexOfLast { it.type == ChatType.AI }
    if (lastAiIndex >= 0) {
      messages.removeAt(lastAiIndex)
    }

    val aiMessageId = UUID.randomUUID().toString()
    val placeholder =
        ChatUIModel(
            id = aiMessageId,
            text = "",
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI,
            isThinking = true)

    _uiState.update { st ->
      st.copy(
          messages = messages + placeholder,
          isSending = true,
          streamingMessageId = aiMessageId,
          streamingSequence = st.streamingSequence + 1)
    }

    viewModelScope.launch(exceptionHandler) {
      try {
        if (isGuest()) {
          startStreaming(
              question = prompt,
              messageId = aiMessageId,
              conversationId = null,
              summary = null,
              transcript = null)
          return@launch
        }

        val cid = _uiState.value.currentConversationId
        val uid = auth.currentUser?.uid
        val summary: String? =
            if (uid != null && cid != null) {
              fetchPriorSummary(uid, cid, aiMessageId)
            } else null

        val recentTranscript: String? =
            if (uid != null && cid != null) {
              buildRecentTranscript(uid, cid, aiMessageId)
            } else null

        startStreaming(
            question = prompt,
            messageId = aiMessageId,
            conversationId = cid,
            summary = summary,
            transcript = recentTranscript)
      } catch (e: Exception) {
        Log.e(TAG, "Error retrying message", e)
        handleSendMessageError(e, aiMessageId)
      } finally {
        _uiState.update { it.copy(isSending = false) }
      }
    }
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

  /**
   * Delete multiple conversations from Firebase.
   *
   * This function deletes each conversation ID from Firestore and updates local state. The
   * conversationsFlow listener will automatically refresh the UI after deletion.
   *
   * @param ids List of conversation IDs to delete.
   */
  fun deleteConversations(ids: List<String>) {
    if (ids.isEmpty()) return
    if (isGuest()) {
      // Guest mode: no Firebase, just clear local state if current conversation is deleted
      val currentId = _uiState.value.currentConversationId
      if (currentId != null && ids.contains(currentId)) {
        startLocalNewChat()
      }
      return
    }
    Log.d(TAG, "deleteConversations(): deleting ${ids.size} conversations")
    viewModelScope.launch(exceptionHandler) {
      try {
        // Delete each conversation from Firebase
        ids.forEach { id ->
          try {
            repo.deleteConversation(id)
            Log.d(TAG, "deleteConversations(): deleted conversation $id")
          } catch (e: Exception) {
            Log.e(TAG, "deleteConversations(): failed to delete conversation $id", e)
            // Continue with other deletions even if one fails
          }
        }

        // If the current conversation was deleted, start a new local chat
        val currentId = _uiState.value.currentConversationId
        if (currentId != null && ids.contains(currentId)) {
          startLocalNewChat()
        }
      } catch (e: AuthNotReadyException) {
        Log.e(TAG, "deleteConversations(): auth not ready", e)
      } catch (e: Exception) {
        Log.e(TAG, "deleteConversations(): unexpected error", e)
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
    networkMonitorJob?.cancel()
    networkMonitorJob = null
    (networkMonitor as? com.android.sample.network.AndroidNetworkConnectivityMonitor)?.unregister()
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
   * Fetches the rolling summary from the most recent message that has one. The onMessageCreate
   * Cloud Function stores summaries in message documents. We look for the most recent message
   * (before the current one) that has a summary field.
   *
   * Note: The Cloud Function generates summaries asynchronously, so we may need to wait a bit for
   * the previous message's summary to be generated.
   */
  private suspend fun fetchPriorSummary(uid: String, cid: String, currentMid: String): String? {
    return try {
      val col =
          firestore
              .collection("users")
              .document(uid)
              .collection("conversations")
              .document(cid)
              .collection("messages")

      // Get recent messages ordered by createdAt
      var snap = col.orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(30).get().await()
      var docs = snap.documents

      // Look backwards from the most recent message to find one with a summary
      for (i in docs.size - 1 downTo 0) {
        val d = docs[i]
        if (d.id == currentMid) continue // Skip the current message

        val summary = d.getString("summary")?.trim()
        if (!summary.isNullOrEmpty()) {
          Log.d(
              TAG, "fetchPriorSummary: found summary in message ${d.id}, length=${summary.length}")
          return summary
        }
      }

      // If no summary found, the Cloud Function might still be processing.
      // Wait a bit and check the most recent assistant message (summaries are usually on assistant
      // messages)
      Log.d(TAG, "fetchPriorSummary: no summary found, waiting for Cloud Function to generate...")
      delay(500) // Wait 500ms for the Cloud Function to process

      // Re-fetch and check again, focusing on the most recent assistant message
      snap = col.orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(10).get().await()
      docs = snap.documents

      // Look for the most recent assistant message with a summary
      for (i in docs.size - 1 downTo 0) {
        val d = docs[i]
        if (d.id == currentMid) continue

        val role = d.getString("role") ?: ""
        // Summaries are typically on assistant messages
        if (role == "assistant") {
          val summary = d.getString("summary")?.trim()
          if (!summary.isNullOrEmpty()) {
            Log.d(
                TAG,
                "fetchPriorSummary: found summary in assistant message ${d.id} after wait, length=${summary.length}")
            return summary
          }
        }
      }

      Log.d(TAG, "fetchPriorSummary: no summary found after waiting")
      null
    } catch (e: Exception) {
      Log.w(TAG, "fetchPriorSummary: error fetching summary", e)
      null
    }
  }

  /**
   * Builds a compact transcript window containing ONLY the previous round (last user message + last
   * assistant reply), to complement the prior summary.
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
    val docs =
        snap.documents
            .filter { it.id != currentMid }
            .takeLast(2) // previous user + assistant, if present
    if (docs.isEmpty()) return null
    val lines =
        docs.map { d ->
          val role = (d.getString("role") ?: "").lowercase()
          val text = d.getString("text")?.trim().orEmpty() // Use "text" field, not "content"
          val tag = if (role == "assistant") "Assistant" else "Utilisateur"
          "$tag: $text"
        }
    val txt = lines.joinToString("\n")
    return if (txt.isBlank()) null else txt.take(600)
  }

  /**
   * Handles ED intent detection from the LLM reply. If a post_question intent is detected, creates
   * a PostOnEd pending action.
   *
   * @param reply The BotReply from the LLM
   * @param originalQuestion The original user question (used as fallback for formatted question)
   */
  private fun handleEdIntent(reply: BotReply, originalQuestion: String) {
    if (reply.edIntent.detected && reply.edIntent.intent == ED_INTENT_POST_QUESTION) {
      Log.d(TAG, "ED intent detected: ${reply.edIntent.intent} - creating PostOnEd pending action")
      val formattedQuestion = reply.edIntent.formattedQuestion ?: originalQuestion
      val formattedTitle = reply.edIntent.formattedTitle ?: ""

      _uiState.update { state ->
        state.copy(
            pendingAction =
                PendingAction.PostOnEd(draftTitle = formattedTitle, draftBody = formattedQuestion))
      }
    }
  }
}
