package com.android.sample.VoiceChat.Backend

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.llm.FirebaseFunctionsLlmClient
import com.android.sample.llm.LlmClient
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
import kotlinx.coroutines.withContext

/**
 * Coordinates speech-to-text transcripts with the custom LLM and delegates playback to the UI.
 *
 * Responsibilities:
 * - Manage the conversational state (transcripts, replies, errors).
 * - Call the LLM asynchronously when the user finishes speaking.
 * - Emit `SpeechRequest` events that the UI layer can render via any `SpeechPlayback`.
 * - Provide synthesized audio level samples so the visualizer stays animated while TTS runs.
 */
class VoiceChatViewModel(
    private val llmClient: LlmClient = FirebaseFunctionsLlmClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

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

  /** Receives live partial transcripts from the STT layer so the UI can reflect them instantly. */
  fun onUserTranscript(transcript: String) {
    _uiState.update { it.copy(lastTranscript = transcript, lastError = null) }
  }

  /**
   * Handles the final transcript of a user utterance by launching an LLM request and, once
   * completed, emitting a [SpeechRequest] for playback.
   */
  fun handleUserUtterance(transcript: String) {
    val cleaned = transcript.trim()
    if (cleaned.isEmpty()) return

    currentGenerationJob?.cancel()
    currentGenerationJob =
        viewModelScope.launch {
          _uiState.update {
            it.copy(
                lastTranscript = cleaned,
                isGenerating = true,
                isSpeaking = false,
                lastError = null,
            )
          }

          try {
            val reply = withContext(ioDispatcher) { llmClient.generateReply(cleaned) }
            val spokenText = sanitizeForSpeech(reply)
            val request =
                SpeechRequest(text = spokenText, utteranceId = UUID.randomUUID().toString())

            _uiState.update { it.copy(lastAiReply = reply, isGenerating = false, lastError = null) }

            if (!_speechRequests.tryEmit(request)) {
              _speechRequests.emit(request)
            }
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (t: Throwable) {
            Log.e(TAG, "LLM generation failed", t)
            _uiState.update {
              it.copy(
                  isGenerating = false,
                  isSpeaking = false,
                  lastError = t.message ?: "Unable to generate response",
              )
            }
          }
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
