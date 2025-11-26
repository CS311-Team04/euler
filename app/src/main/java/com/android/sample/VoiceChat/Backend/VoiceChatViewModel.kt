package com.android.sample.VoiceChat.Backend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.BuildConfig
import com.android.sample.conversations.ConversationRepository
import com.android.sample.conversations.ConversationTitleFormatter
import com.android.sample.llm.FirebaseFunctionsLlmClient
import com.android.sample.llm.LlmClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Coordinates speech-to-text transcripts with the custom LLM and delegates playback to the UI.
 *
 * Responsibilities:
 * - Manage the conversational state (transcripts, replies, errors).
 * - Call the LLM asynchronously when the user finishes speaking.
 * - Emit `SpeechRequest` events that the UI layer can render via any `SpeechPlayback`.
 * - Provide synthesized audio level samples so the visualizer stays animated while TTS runs.
 * - Create and manage conversations in Firestore (same as text mode).
 * - Persist voice interactions to maintain context and memory.
 */
class VoiceChatViewModel(
    private val llmClient: LlmClient = FirebaseFunctionsLlmClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : ViewModel() {

  private val repo = ConversationRepository(auth, db)
  private var currentConversationId: String? = null

  /**
   * Firebase Functions handle for title generation. Uses local emulator when configured via
   * BuildConfig flags.
   */
  private val functions: FirebaseFunctions by lazy {
    FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION).apply {
      if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
        useEmulator(BuildConfig.FUNCTIONS_HOST, BuildConfig.FUNCTIONS_PORT)
      }
    }
  }

  private val _uiState = MutableStateFlow(VoiceChatUiState())
  val uiState: StateFlow<VoiceChatUiState> = _uiState.asStateFlow()

  private val _audioLevels = MutableSharedFlow<Float>(replay = 0, extraBufferCapacity = 32)
  val audioLevels: SharedFlow<Float> = _audioLevels.asSharedFlow()

  private val _speechRequests =
      MutableSharedFlow<SpeechRequest>(replay = 0, extraBufferCapacity = 8)
  val speechRequests: SharedFlow<SpeechRequest> = _speechRequests.asSharedFlow()

  private var currentGenerationJob: Job? = null
  private var levelAnimationJob: Job? = null
  private var lastEmittedLevel: Float = 0f

  /**
   * Returns true when the user is **not** authenticated. Guest mode disables Firestore persistence
   * and keeps all messages in memory only.
   */
  private fun isGuest(): Boolean = auth.currentUser == null

  /**
   * Prepares for voice mode activation. Resets the current conversation so that a new one will be
   * created automatically on the first message (same pattern as text mode).
   *
   * This ensures that each voice mode session starts with a fresh conversation, maintaining context
   * and memory within that session.
   *
   * This method is called when the voice screen is opened. In guest mode, this is a no-op.
   */
  fun initializeConversation() {
    // Reset conversation ID so a new conversation is created on first message
    // This ensures each voice mode activation starts a fresh conversation
    currentConversationId = null
    if (isGuest()) {
      Log.d(TAG, "Guest mode: conversation will not be persisted")
    } else {
      Log.d(TAG, "Ready to create new conversation on first voice message")
    }
  }

  /** Receives live partial transcripts from the STT layer so the UI can reflect them instantly. */
  fun onUserTranscript(transcript: String) {
    _uiState.update { it.copy(lastTranscript = transcript, lastError = null) }
  }

  /**
   * Handles the final transcript of a user utterance by launching an LLM request and, once
   * completed, emitting a [SpeechRequest] for playback.
   *
   * Creates a conversation on the first message (if signed in) and persists all messages to
   * Firestore to maintain context and memory, just like text mode.
   */
  fun handleUserUtterance(transcript: String) {
    val cleaned = transcript.trim()
    if (cleaned.isEmpty()) return

    currentGenerationJob?.cancel()
    currentGenerationJob =
        viewModelScope.launch {
          updateStateForNewUtterance(cleaned)
          try {
            val cid = ensureConversationExists(cleaned)
            persistUserMessage(cid, cleaned)
            val reply = generateAiReply(cleaned)
            updateStateWithReply(reply.reply)
            persistAiMessage(cid, reply.reply)
            emitSpeechRequest(reply.reply)
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (t: Throwable) {
            handleGenerationError(t)
          }
        }
  }

  /** Updates UI state when starting to process a new user utterance. */
  private fun updateStateForNewUtterance(cleaned: String) {
    _uiState.update {
      it.copy(
          lastTranscript = cleaned,
          isGenerating = true,
          isSpeaking = false,
          lastError = null,
      )
    }
  }

  /**
   * Ensures a conversation exists for the current session. Returns conversation ID if signed in,
   * null if guest mode.
   *
   * Creates a conversation with a quick title, then upgrades it in the background using
   * generateTitleFn (same pattern as text mode).
   */
  private suspend fun ensureConversationExists(cleaned: String): String? {
    if (isGuest()) {
      return null // Guest mode: no Firestore persistence
    }
    return currentConversationId
        ?: run {
          // Create new conversation on first message (same pattern as text mode)
          val quickTitle = ConversationTitleFormatter.localTitleFrom(cleaned)
          val newId = repo.startNewConversation(quickTitle)
          currentConversationId = newId
          Log.d(TAG, "Created new voice conversation: $newId with title: $quickTitle")

          // Upgrade title in background (once)
          viewModelScope.launch {
            try {
              val good = ConversationTitleFormatter.fetchTitle(functions, cleaned, TAG)
              if (good.isNotBlank() && good != quickTitle) {
                repo.updateConversationTitle(newId, good)
                Log.d(TAG, "Upgraded conversation title: $newId -> $good")
              }
            } catch (e: Exception) {
              // Keep provisional title on error
              Log.d(TAG, "Failed to upgrade title, keeping provisional: ${e.message}")
            }
          }

          newId
        }
  }

  /** Persists user message to Firestore if conversation ID exists. */
  private suspend fun persistUserMessage(cid: String?, message: String) {
    if (cid == null) return
    try {
      repo.appendMessage(cid, "user", message)
      Log.d(TAG, "Persisted user message to conversation: $cid")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to persist user message: ${e.message}")
    }
  }

  /** Generates AI reply using the LLM client. */
  private suspend fun generateAiReply(cleaned: String) = llmClient.generateReply(cleaned)

  /** Updates UI state with the AI reply. */
  private fun updateStateWithReply(reply: String) {
    _uiState.update { it.copy(lastAiReply = reply, isGenerating = false, lastError = null) }
  }

  /** Persists AI message to Firestore if conversation ID exists. */
  private suspend fun persistAiMessage(cid: String?, reply: String) {
    if (cid == null) return
    try {
      repo.appendMessage(cid, "assistant", reply)
      Log.d(TAG, "Persisted assistant message to conversation: $cid")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to persist assistant message: ${e.message}")
    }
  }

  /** Emits a speech request for TTS playback. */
  private suspend fun emitSpeechRequest(reply: String) {
    val spokenText = sanitizeForSpeech(reply)
    val request = SpeechRequest(text = spokenText, utteranceId = UUID.randomUUID().toString())
    if (!_speechRequests.tryEmit(request)) {
      _speechRequests.emit(request)
    }
  }

  /** Handles errors during LLM generation and updates UI state accordingly. */
  private fun handleGenerationError(t: Throwable) {
    Log.e(TAG, "LLM generation failed", t)
    _uiState.update {
      it.copy(
          isGenerating = false,
          isSpeaking = false,
          lastError = t.message ?: "Unable to generate response",
      )
    }
  }

  /** Allows other components (STT/TTS) to bubble a recoverable error into the UI banner. */
  fun reportError(message: String) {
    _uiState.update { it.copy(lastError = message) }
  }

  /** Marks the beginning of playback and kicks off the fallback visualizer animation. */
  fun onSpeechStarted() {
    _uiState.update { it.copy(isSpeaking = true, lastError = null) }
    startLevelAnimation()
  }

  /** Resets animation and state once TTS playback finishes normally. */
  fun onSpeechFinished() {
    stopLevelAnimation()
    _uiState.update { it.copy(isSpeaking = false) }
  }

  /** Stops animation and records a playback failure, preserving the message for the UI. */
  fun onSpeechError(throwable: Throwable?) {
    stopLevelAnimation()
    _uiState.update {
      it.copy(
          isSpeaking = false,
          lastError = throwable?.message ?: "Playback is unavailable",
      )
    }
  }

  /** Cancels outstanding work and resets the UI. Invoked when the voice screen is dismissed. */
  fun stopAll() {
    currentGenerationJob?.cancel()
    currentGenerationJob = null
    stopLevelAnimation()
    _uiState.update { it.copy(isGenerating = false, isSpeaking = false) }
    // Note: We keep currentConversationId so that if the user returns to voice mode,
    // they can continue the same conversation. To start a new conversation, the user
    // should use the text mode or we could add a "new conversation" action.
  }

  override fun onCleared() {
    super.onCleared()
    stopAll()
  }

  /**
   * Emits a looping sequence of levels so the visualizer stays alive while TextToSpeech is running.
   */
  private fun startLevelAnimation() {
    levelAnimationJob?.cancel()
    lastEmittedLevel = 0f
    levelAnimationJob =
        viewModelScope.launch {
          var index = 0
          while (isActive) {
            val level = LEVEL_PATTERN[index % LEVEL_PATTERN.size]
            emitLevel(level)
            index++
            delay(LEVEL_FRAME_DELAY_MS)
          }
        }
  }

  /** Stops the visualizer pattern and ensures the next emission starts from zero. */
  private fun stopLevelAnimation() {
    levelAnimationJob?.cancel()
    levelAnimationJob = null
    lastEmittedLevel = 0f
    emitLevel(0f)
  }

  /** Pushes a level sample with exponential smoothing to avoid abrupt jumps between frames. */
  private fun emitLevel(level: Float) {
    val smoothed =
        (lastEmittedLevel * LEVEL_SMOOTHING_ALPHA) + (level * (1 - LEVEL_SMOOTHING_ALPHA))
    lastEmittedLevel = smoothed
    if (!_audioLevels.tryEmit(smoothed)) {
      viewModelScope.launch { _audioLevels.emit(smoothed) }
    }
  }

  data class VoiceChatUiState(
      val isSpeaking: Boolean = false,
      val isGenerating: Boolean = false,
      val lastTranscript: String? = null,
      val lastAiReply: String? = null,
      val lastError: String? = null,
  )

  data class SpeechRequest(val text: String, val utteranceId: String)

  private companion object {
    private const val TAG = "VoiceChatViewModel"
    private const val LEVEL_FRAME_DELAY_MS = 120L
    private const val LEVEL_SMOOTHING_ALPHA = 0.72f
    private val LEVEL_PATTERN =
        floatArrayOf(
            0.16f,
            0.22f,
            0.27f,
            0.24f,
            0.20f,
            0.25f,
        )
    private val EMOJI_REGEX =
        Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]|[\\uFE0F\\uFE0E]")
    private val MARKDOWN_REGEX = Regex("[*_`~]")
    private val CODE_BLOCK_REGEX = Regex("```.*?```", RegexOption.DOT_MATCHES_ALL)
    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val MULTI_PUNCTUATION_REGEX = Regex("([!?]){2,}")
    private val WHITESPACE_REGEX = Regex("\\s+")

    /**
     * Removes markdown, emojis, HTML tags, and extra whitespace so TextToSpeech receives clean
     * text.
     */
    private fun sanitizeForSpeech(raw: String): String {
      val withoutCodeBlocks = CODE_BLOCK_REGEX.replace(raw, " ")
      val noHtml = HTML_TAG_REGEX.replace(withoutCodeBlocks, " ")
      val noEmojis = EMOJI_REGEX.replace(noHtml, " ")
      val noMarkdown = MARKDOWN_REGEX.replace(noEmojis, " ")
      val collapsedPunctuation =
          MULTI_PUNCTUATION_REGEX.replace(noMarkdown) { matchResult ->
            matchResult.value.first().toString()
          }
      val sentenceFriendly =
          collapsedPunctuation.replace("\n{2,}".toRegex(), ". ").replace("\n", ", ")
      val normalizedWhitespace = WHITESPACE_REGEX.replace(sentenceFriendly, " ").trim()
      return normalizedWhitespace.ifBlank { raw.trim() }
    }
  }
}
