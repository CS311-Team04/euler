package com.android.sample.home

import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.speech.SpeechPlayback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAudioControllerTest {

  private val aiMessage =
      ChatUIModel(
          id = "ai-1",
          text = "Bonjour tout le monde",
          timestamp = 0L,
          type = ChatType.AI,
          isThinking = false)

  private val userMessage =
      ChatUIModel(id = "user-1", text = "Hello", timestamp = 0L, type = ChatType.USER)

  @Test
  fun audioStateFor_returnsNull_whenPlaybackUnavailable() {
    val controller = HomeAudioController(playback = null)

    val state = controller.audioStateFor(aiMessage, streamingMessageId = null)

    assertNull(state)
  }

  @Test
  fun audioStateFor_filtersOutNonEligibleMessages() {
    val playback = FakeSpeechPlayback()
    val controller = HomeAudioController(playback)

    assertNull(
        "User messages should not expose audio controls",
        controller.audioStateFor(userMessage, null))
    assertNull(
        "Blank assistant messages should not expose controls",
        controller.audioStateFor(aiMessage.copy(text = ""), null))
    assertNull(
        "Thinking messages should not expose controls",
        controller.audioStateFor(aiMessage.copy(isThinking = true), null))
    assertNull(
        "Currently streaming message should not expose controls",
        controller.audioStateFor(aiMessage, streamingMessageId = aiMessage.id))
  }

  @Test
  fun play_transitionsThroughLoadingAndPlayingStates() {
    val playback = FakeSpeechPlayback()
    val controller = HomeAudioController(playback)

    val initial = controller.audioStateFor(aiMessage, null)
    assertNotNull(initial)
    initial!!.onPlay()

    assertEquals("speak should be invoked once", 1, playback.speakInvocations)
    val loading = controller.audioStateFor(aiMessage, null)
    assertNotNull(loading)
    assertTrue(loading!!.isLoading)
    assertFalse(loading.isPlaying)

    playback.dispatchStart()
    val playing = controller.audioStateFor(aiMessage, null)
    assertNotNull(playing)
    assertFalse(playing!!.isLoading)
    assertTrue(playing.isPlaying)

    playback.dispatchDone()
    val finished = controller.audioStateFor(aiMessage, null)
    assertNotNull(finished)
    assertFalse(finished!!.isLoading)
    assertFalse(finished.isPlaying)
  }

  @Test
  fun stop_viaCallback_invokesUnderlyingPlaybackStop() {
    val playback = FakeSpeechPlayback()
    val controller = HomeAudioController(playback)

    val state = controller.audioStateFor(aiMessage, null)
    requireNotNull(state).onPlay()
    playback.dispatchStart()

    controller.audioStateFor(aiMessage, null)!!.onStop()

    assertEquals("Stop should be delegated to playback", 1, playback.stopInvocations)
    val idleState = controller.audioStateFor(aiMessage, null)
    assertFalse(requireNotNull(idleState).isPlaying)
  }

  @Test
  fun handleMessagesChanged_stopsWhenActiveMessageRemoved() {
    val playback = FakeSpeechPlayback()
    val controller = HomeAudioController(playback)

    controller.audioStateFor(aiMessage, null)!!.onPlay()
    playback.dispatchStart()

    controller.handleMessagesChanged(emptyList())

    assertEquals(1, playback.stopInvocations)
    assertFalse(requireNotNull(controller.audioStateFor(aiMessage, null)).isPlaying)
  }

  @Test
  fun play_newMessageStopsPreviousPlayback() {
    val playback = FakeSpeechPlayback()
    val controller = HomeAudioController(playback)
    val secondMessage = aiMessage.copy(id = "ai-2", text = "Deuxième réponse")

    controller.audioStateFor(aiMessage, null)!!.onPlay()
    controller.audioStateFor(secondMessage, null)!!.onPlay()

    assertEquals(
        "Switching messages should stop the previous playback", 1, playback.stopInvocations)
    assertEquals(
        "Last speech request should target the new message",
        secondMessage.id,
        playback.lastUtteranceId)
  }

  private class FakeSpeechPlayback : SpeechPlayback {

    data class PendingSpeak(
        val text: String,
        val utteranceId: String,
        val onStart: () -> Unit,
        val onDone: () -> Unit,
        val onError: (Throwable?) -> Unit
    )

    private var pending: PendingSpeak? = null
    var speakInvocations: Int = 0
      private set

    var stopInvocations: Int = 0
      private set

    var lastUtteranceId: String? = null
      private set

    override fun speak(
        text: String,
        utteranceId: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable?) -> Unit
    ) {
      speakInvocations += 1
      lastUtteranceId = utteranceId
      pending = PendingSpeak(text, utteranceId, onStart, onDone, onError)
    }

    override fun stop() {
      stopInvocations += 1
      pending = null
    }

    override fun shutdown() {
      // not required for tests
    }

    fun dispatchStart() {
      pending?.onStart?.invoke()
    }

    fun dispatchDone() {
      pending?.onDone?.invoke()
      pending = null
    }

    fun dispatchError() {
      pending?.onError?.invoke(RuntimeException("error"))
      pending = null
    }
  }
}
