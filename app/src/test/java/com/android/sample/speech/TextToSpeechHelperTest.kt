package com.android.sample.speech

import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.core.app.ApplicationProvider
import java.util.ArrayDeque
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class TextToSpeechHelperTest {

  private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

  @Before
  fun setUp() {
    Shadows.shadowOf(Looper.getMainLooper()).pause()
  }

  @After
  fun tearDown() {
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun speak_queues_until_engine_ready() {
    val engine = FakeEngine()
    val helper =
        TextToSpeechHelper(context, Locale.FRANCE) { _, init ->
          engine.initListener = init
          engine
        }

    val events = mutableListOf<String>()
    helper.speak(
        text = "Bonjour",
        utteranceId = "utt-1",
        onStart = { events += "start" },
        onDone = { events += "done" },
        onError = { events += "error" })

    assertTrue(engine.speakCalls.isEmpty())

    engine.languageResults += TextToSpeech.LANG_AVAILABLE
    engine.triggerInit(TextToSpeech.SUCCESS)
    engine.triggerStart("utt-1")
    engine.triggerDone("utt-1")
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertEquals(listOf("utt-1"), engine.speakCalls)
    assertEquals(listOf("start", "done"), events)
  }

  @Test
  fun speak_failure_invokes_onError() {
    val engine = FakeEngine()
    engine.speakResult = TextToSpeech.ERROR
    engine.languageResults += TextToSpeech.LANG_AVAILABLE
    val helper =
        TextToSpeechHelper(context) { _, init ->
          engine.initListener = init
          engine
        }
    engine.triggerInit(TextToSpeech.SUCCESS)

    var error: Throwable? = null
    helper.speak(
        text = "Bonjour",
        utteranceId = "utt-error",
        onStart = {},
        onDone = {},
        onError = { error = it })

    assertTrue(error is IllegalStateException)
  }

  @Test
  fun stop_clears_callbacks_and_stops_engine() {
    val engine = FakeEngine()
    engine.languageResults += TextToSpeech.LANG_AVAILABLE
    val helper =
        TextToSpeechHelper(context) { _, init ->
          engine.initListener = init
          engine
        }
    engine.triggerInit(TextToSpeech.SUCCESS)

    helper.speak(
        text = "Bonjour", utteranceId = "utt-stop", onStart = {}, onDone = {}, onError = {})
    engine.triggerStart("utt-stop")
    engine.triggerDone("utt-stop")
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    helper.stop()

    assertEquals(1, engine.stopCalls)
    assertTrue(engine.callbacksCleared)
  }

  @Test
  fun shutdown_stops_and_releases_engine() {
    val engine = FakeEngine()
    engine.languageResults += TextToSpeech.LANG_AVAILABLE
    val helper =
        TextToSpeechHelper(context) { _, init ->
          engine.initListener = init
          engine
        }
    engine.triggerInit(TextToSpeech.SUCCESS)

    helper.shutdown()

    assertEquals(1, engine.stopCalls)
    assertEquals(1, engine.shutdownCalls)
  }

  @Test
  fun initialization_failure_notifies_pending_requests() {
    val engine = FakeEngine()
    val helper =
        TextToSpeechHelper(context) { _, init ->
          engine.initListener = init
          engine
        }

    var error: Throwable? = null
    helper.speak(
        text = "Bonjour", utteranceId = "utt", onStart = {}, onDone = {}, onError = { error = it })

    engine.triggerInit(TextToSpeech.ERROR)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertTrue(error is IllegalStateException)
    assertTrue(engine.speakCalls.isEmpty())
  }

  @Test
  fun selectLanguage_tries_multiple_candidates() {
    val engine = FakeEngine()
    engine.languageResults += TextToSpeech.LANG_NOT_SUPPORTED
    engine.languageResults += TextToSpeech.LANG_MISSING_DATA
    engine.languageResults += TextToSpeech.LANG_AVAILABLE
    val helper =
        TextToSpeechHelper(context, Locale("it", "IT")) { _, init ->
          engine.initListener = init
          engine
        }

    engine.triggerInit(TextToSpeech.SUCCESS)

    assertEquals(
        listOf(Locale("it", "IT"), Locale.FRANCE, Locale.getDefault()), engine.languageCalls)
  }

  private class FakeEngine : TextToSpeechEngine {
    lateinit var initListener: (Int) -> Unit
    val speakCalls = mutableListOf<String>()
    val languageCalls = mutableListOf<Locale>()
    val languageResults: ArrayDeque<Int> = ArrayDeque()
    var speakResult: Int = TextToSpeech.SUCCESS
    var stopCalls: Int = 0
    var shutdownCalls: Int = 0
    var callbacksCleared: Boolean = false
    private var listener: UtteranceProgressListener? = null

    fun triggerInit(status: Int) {
      initListener(status)
    }

    fun triggerStart(utteranceId: String) {
      listener?.onStart(utteranceId)
    }

    fun triggerDone(utteranceId: String) {
      listener?.onDone(utteranceId)
    }

    fun triggerError(utteranceId: String, code: Int? = null) {
      if (code != null) {
        listener?.onError(utteranceId, code)
      } else {
        listener?.onError(utteranceId)
      }
    }

    override fun setLanguage(locale: Locale): Int {
      languageCalls += locale
      return if (languageResults.isEmpty()) {
        TextToSpeech.LANG_AVAILABLE
      } else {
        languageResults.removeFirst()
      }
    }

    override fun speak(
        text: String,
        queueMode: Int,
        params: android.os.Bundle?,
        utteranceId: String
    ): Int {
      speakCalls += utteranceId
      return speakResult
    }

    override fun stop() {
      stopCalls++
      callbacksCleared = true
    }

    override fun shutdown() {
      shutdownCalls++
    }

    override fun setProgressListener(listener: UtteranceProgressListener) {
      this.listener = listener
    }
  }
}
