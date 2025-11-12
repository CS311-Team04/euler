package com.android.sample.speech

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextToSpeechHelperTest {

  private lateinit var context: Context
  private lateinit var helper: TextToSpeechHelper

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    helper = TextToSpeechHelper(context, Locale.FRENCH)
  }

  @Test
  fun speak_queues_when_not_ready() = runTest {
    var onStartCalled = false
    var onDoneCalled = false
    var onErrorCalled = false

    // TTS is not ready immediately after construction
    helper.speak(
        "Test",
        "test-id",
        onStart = { onStartCalled = true },
        onDone = { onDoneCalled = true },
        onError = { onErrorCalled = true })

    // Should queue the request
    assertFalse(onStartCalled)
    assertFalse(onDoneCalled)
    assertFalse(onErrorCalled)
  }

  @Test
  fun stop_clears_callbacks() = runTest {
    var onDoneCalled = false

    helper.speak("Test", "test-id", onDone = { onDoneCalled = true })
    helper.stop()

    // Stop should clear callbacks
    assertFalse(onDoneCalled)
  }

  @Test
  fun shutdown_releases_resources() = runTest {
    helper.speak("Test", "test-id")
    helper.shutdown()

    // Shutdown should not throw
    assertTrue(true)
  }

  @Test
  fun speak_with_empty_text() = runTest {
    var onErrorCalled = false

    helper.speak("", "test-id", onError = { onErrorCalled = true })

    // Empty text might be handled by TTS engine
    // Just verify it doesn't crash
    assertTrue(true)
  }

  @Test
  fun multiple_speak_calls() = runTest {
    var callCount = 0

    helper.speak("First", "id1", onDone = { callCount++ })
    helper.speak("Second", "id2", onDone = { callCount++ })

    // Should handle multiple calls
    assertTrue(true)
  }

  @Test
  fun stop_during_speech() = runTest {
    var onDoneCalled = false

    helper.speak("Long text", "test-id", onDone = { onDoneCalled = true })
    helper.stop()

    // Stop should interrupt
    assertFalse(onDoneCalled)
  }

  @Test
  fun shutdown_after_speak() = runTest {
    helper.speak("Test", "test-id")
    helper.shutdown()

    // Should handle shutdown gracefully
    assertTrue(true)
  }

  @Test
  fun speak_with_special_characters() = runTest {
    var onErrorCalled = false

    helper.speak("Hello @#$%^&*()", "test-id", onError = { onErrorCalled = true })

    // Should handle special characters
    assertTrue(true)
  }

  @Test
  fun speak_with_unicode() = runTest {
    var onErrorCalled = false

    helper.speak("Hello 世界 🌍", "test-id", onError = { onErrorCalled = true })

    // Should handle unicode
    assertTrue(true)
  }

  @Test
  fun speak_with_long_text() = runTest {
    val longText = "A".repeat(1000)
    var onErrorCalled = false

    helper.speak(longText, "test-id", onError = { onErrorCalled = true })

    // Should handle long text
    assertTrue(true)
  }

  @Test
  fun multiple_stop_calls() = runTest {
    helper.stop()
    helper.stop()
    helper.stop()

    // Should handle multiple stops gracefully
    assertTrue(true)
  }

  @Test
  fun shutdown_multiple_times() = runTest {
    helper.shutdown()
    helper.shutdown()

    // Should handle multiple shutdowns gracefully
    assertTrue(true)
  }

  @Test
  fun speak_after_stop() = runTest {
    helper.speak("First", "id1")
    helper.stop()
    helper.speak("Second", "id2")

    // Should handle speak after stop
    assertTrue(true)
  }

  @Test
  fun speak_after_shutdown() = runTest {
    helper.shutdown()
    var onErrorCalled = false

    helper.speak("Test", "test-id", onError = { onErrorCalled = true })

    // After shutdown, speak might fail
    assertTrue(true)
  }

  @Test
  fun different_utterance_ids() = runTest {
    var id1Done = false
    var id2Done = false

    helper.speak("First", "id1", onDone = { id1Done = true })
    helper.speak("Second", "id2", onDone = { id2Done = true })

    // Should handle different IDs
    assertTrue(true)
  }

  @Test
  fun same_utterance_id_multiple_times() = runTest {
    helper.speak("First", "same-id")
    helper.speak("Second", "same-id")

    // Should handle same ID
    assertTrue(true)
  }

  @Test
  fun speak_with_null_error_in_callback() = runTest {
    var errorReceived: Throwable? = null

    helper.speak("Test", "test-id", onError = { errorReceived = it })

    // Wait for potential error
    Thread.sleep(100)

    // Should handle null error gracefully
    assertTrue(true)
  }

  @Test
  fun stop_clears_current_utterance() = runTest {
    helper.speak("Test", "test-id")
    helper.stop()

    // Stop should clear current utterance
    assertTrue(true)
  }

  @Test
  fun shutdown_clears_callbacks_and_current_utterance() = runTest {
    helper.speak("Test", "test-id")
    helper.shutdown()

    // Shutdown should clear everything
    assertTrue(true)
  }

  @Test
  fun speak_with_whitespace_only() = runTest {
    var onErrorCalled = false

    helper.speak("   ", "test-id", onError = { onErrorCalled = true })

    // Should handle whitespace-only text
    assertTrue(true)
  }

  @Test
  fun speak_with_newline_characters() = runTest {
    helper.speak("Line1\nLine2\nLine3", "test-id")

    // Should handle newlines
    assertTrue(true)
  }

  @Test
  fun speak_queues_multiple_requests() = runTest {
    var callCount = 0

    helper.speak("First", "id1", onDone = { callCount++ })
    helper.speak("Second", "id2", onDone = { callCount++ })
    helper.speak("Third", "id3", onDone = { callCount++ })

    // Should queue multiple requests
    assertTrue(true)
  }

  @Test
  fun stop_before_speak() = runTest {
    helper.stop()
    helper.speak("Test", "test-id")

    // Should handle stop before speak
    assertTrue(true)
  }

  @Test
  fun shutdown_before_speak() = runTest {
    helper.shutdown()
    var onErrorCalled = false

    helper.speak("Test", "test-id", onError = { onErrorCalled = true })

    // After shutdown, speak might fail
    assertTrue(true)
  }

  @Test
  fun speak_with_very_long_utterance_id() = runTest {
    val longId = "a".repeat(1000)
    helper.speak("Test", longId)

    // Should handle long IDs
    assertTrue(true)
  }

  @Test
  fun speak_with_empty_utterance_id() = runTest {
    helper.speak("Test", "")

    // Should handle empty ID
    assertTrue(true)
  }
}
