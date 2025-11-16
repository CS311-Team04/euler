package com.android.sample.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.Chat.MessageAudioState
import com.android.sample.speech.SpeechPlayback

/**
 * Encapsulates the audio playback state for assistant messages so the composable remains lean and
 * testable.
 */
@Stable
class HomeAudioController(private val playback: SpeechPlayback?) {

  private var playingMessageId by mutableStateOf<String?>(null)
  private var visualState by mutableStateOf(AudioPlaybackVisualState.Idle)

  /** Start speaking the provided message text using the injected [SpeechPlayback]. */
  fun play(message: ChatUIModel) {
    if (playback == null) return
    if (message.text.isBlank()) return

    if (playingMessageId != null && playingMessageId != message.id) {
      playback.stop()
    }

    playingMessageId = message.id
    visualState = AudioPlaybackVisualState.Starting
    playback.speak(
        text = message.text,
        utteranceId = message.id,
        onStart = {
          if (playingMessageId == message.id) {
            visualState = AudioPlaybackVisualState.Playing
          }
        },
        onDone = {
          if (playingMessageId == message.id) {
            resetState()
          }
        },
        onError = {
          if (playingMessageId == message.id) {
            resetState()
          }
        })
  }

  /** Stop any ongoing playback immediately. */
  fun stop() {
    if (playingMessageId != null || visualState != AudioPlaybackVisualState.Idle) {
      playback?.stop()
      resetState()
    }
  }

  /** Called when the currently displayed message list changes. */
  fun handleMessagesChanged(messages: List<ChatUIModel>) {
    val activeId = playingMessageId ?: return
    if (messages.none { it.id == activeId }) {
      stop()
    }
  }

  /**
   * Build the [MessageAudioState] to drive the UI for a given message, returning null when the
   * button should remain hidden.
   */
  fun audioStateFor(message: ChatUIModel, streamingMessageId: String?): MessageAudioState? {
    if (playback == null) return null
    if (message.type != ChatType.AI) return null
    if (message.text.isBlank()) return null
    if (message.isThinking) return null
    if (streamingMessageId == message.id) return null

    val isLoading =
        playingMessageId == message.id && visualState == AudioPlaybackVisualState.Starting
    val isPlaying =
        playingMessageId == message.id && visualState == AudioPlaybackVisualState.Playing

    return MessageAudioState(
        isLoading = isLoading,
        isPlaying = isPlaying,
        onPlay = { play(message) },
        onStop = { stop() })
  }

  private fun resetState() {
    playingMessageId = null
    visualState = AudioPlaybackVisualState.Idle
  }
}

enum class AudioPlaybackVisualState {
  Idle,
  Starting,
  Playing
}
