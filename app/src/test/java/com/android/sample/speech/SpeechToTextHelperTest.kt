package com.android.sample.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowToast

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SpeechToTextHelperTest {

  private lateinit var context: Context
  private val controllers = mutableListOf<ActivityController<out ComponentActivity>>()

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ShadowToast.reset()
  }

  @Test
  fun startListening_withPermission_startsRecognizerWithConfiguredIntent() {
    val recognizer = mockk<SpeechRecognizer>(relaxed = true)
    val intentSlot = slot<Intent>()
    every { recognizer.startListening(capture(intentSlot)) } returns Unit
    val helper = createHelper(recognizer)
    ShadowApplication.getInstance().grantPermissions(Manifest.permission.RECORD_AUDIO)

    helper.startListening(onResult = {})

    assertTrue(helper.isListening())
    assertTrue(intentSlot.isCaptured)
    val intent = intentSlot.captured
    assertEquals(
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
    assertEquals(
        Locale.FRANCE.toLanguageTag(), intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
    assertEquals(true, intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
    assertEquals(3, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0))
    assertEquals("Speak now…", ShadowToast.getTextOfLatestToast())
  }

  @Test
  fun startListening_withoutPermission_requestsPermission() {
    val recognizer = mockk<SpeechRecognizer>(relaxed = true)
    val intentSlot = slot<Intent>()
    every { recognizer.startListening(capture(intentSlot)) } returns Unit
    val helper = createHelper(recognizer)

    helper.startListening(onResult = {})

    assertFalse(intentSlot.isCaptured)
    assertFalse(helper.isListening())
  }

  @Test
  fun stopListening_cancelsRecognizerAndResetsFlag() {
    val recognizer = mockk<SpeechRecognizer>(relaxed = true)
    val helper = createHelper(recognizer)
    helper.setBooleanField("isListening", true)

    helper.stopListening()

    verify { recognizer.stopListening() }
    verify { recognizer.cancel() }
    assertFalse(helper.isListening())
  }

  @Test
  fun recognitionListener_onRmsChanged_clampsAndDispatches() {
    val helper = createHelper()
    val values = mutableListOf<Float>()
    helper.setField("onRmsCallback", { value: Float -> values += value })

    helper.recognitionListener().onRmsChanged(42f)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertEquals(listOf(10f), values)
  }

  @Test
  fun recognitionListener_onError_reportsMessageAndCompletion() {
    val helper = createHelper()
    var errorMessage: String? = null
    var completed = false
    helper.setField("onErrorCallback", { msg: String -> errorMessage = msg })
    helper.setField("onCompleteCallback", { completed = true })
    helper.setBooleanField("isListening", true)

    helper.recognitionListener().onError(SpeechRecognizer.ERROR_NETWORK)

    assertEquals("Network error", errorMessage)
    assertTrue(completed)
    assertFalse(helper.isListening())
  }

  @Test
  fun recognitionListener_onResults_deliversBestMatch() {
    val helper = createHelper()
    var result: String? = null
    var completed = false
    helper.setField("onResultCallback", { text: String -> result = text })
    helper.setField("onCompleteCallback", { completed = true })
    helper.setBooleanField("isListening", true)

    val bundle =
        Bundle().apply {
          putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("bonjour", "salut"))
        }

    helper.recognitionListener().onResults(bundle)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertEquals("bonjour", result)
    assertTrue(completed)
    assertFalse(helper.isListening())
  }

  @Test
  fun recognitionListener_onPartialResults_emitsNonBlankMatch() {
    val helper = createHelper()
    val partials = mutableListOf<String>()
    helper.setField("onPartialCallback", { text: String -> partials += text })

    val bundle =
        Bundle().apply {
          putStringArrayList(
              SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("en cours", "suivant"))
        }

    helper.recognitionListener().onPartialResults(bundle)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertEquals(listOf("en cours"), partials)
  }

  @Test
  fun recognitionListener_onPartialResults_ignoresBlankResult() {
    val helper = createHelper()
    helper.setField("onPartialCallback", { _: String -> fail("Should not be called") })

    val bundle =
        Bundle().apply {
          putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(" "))
        }

    helper.recognitionListener().onPartialResults(bundle)
    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  private fun createHelper(
      recognizer: SpeechRecognizer = mockk(relaxed = true)
  ): SpeechToTextHelper {
    val controller = buildActivity(ComponentActivity::class.java).create()
    controllers += controller
    val activity = controller.get()
    val helper = SpeechToTextHelper(context, activity, Locale.FRANCE)
    controller.start().resume()
    helper.setSpeechRecognizer(recognizer)
    return helper
  }

  private fun SpeechToTextHelper.recognitionListener(): RecognitionListener {
    val field = SpeechToTextHelper::class.java.getDeclaredField("recognitionListener")
    field.isAccessible = true
    return field.get(this) as RecognitionListener
  }

  private fun SpeechToTextHelper.setSpeechRecognizer(recognizer: SpeechRecognizer) {
    setField("speechRecognizer", recognizer)
  }

  private fun SpeechToTextHelper.setField(name: String, value: Any?) {
    val field = SpeechToTextHelper::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun SpeechToTextHelper.getField(name: String): Any? {
    val field = SpeechToTextHelper::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this)
  }

  private fun SpeechToTextHelper.setBooleanField(name: String, value: Boolean) =
      setField(name, value)

  private fun SpeechToTextHelper.isListening(): Boolean = getField("isListening") as Boolean

  @Test
  fun startListening_whenAlreadyListening_returnsEarly() {
    val recognizer = mockk<SpeechRecognizer>(relaxed = true)
    val helper = createHelper(recognizer)
    helper.setBooleanField("isListening", true)

    helper.startListening(onResult = {})

    verify(exactly = 0) { recognizer.startListening(any()) }
    assertTrue(helper.isListening())
  }

  @Test
  fun ensureSpeechRecognizer_handlesUnavailableRecognizer() {
    mockkStatic(SpeechRecognizer::class)
    try {
      every { SpeechRecognizer.isRecognitionAvailable(context) } returns false
      val helper = createHelper()
      helper.setField("speechRecognizer", null)
      var error: String? = null
      var completed = false
      helper.setField("onErrorCallback", { msg: String -> error = msg })
      helper.setField("onCompleteCallback", { completed = true })

      val result = helper.invokeEnsureRecognizer()

      assertFalse(result)
      assertEquals("Speech recognition not available on this device", error)
      assertTrue(completed)
    } finally {
      unmockkStatic(SpeechRecognizer::class)
    }
  }

  @Test
  fun ensureSpeechRecognizer_initializesRecognizerWhenAvailable() {
    mockkStatic(SpeechRecognizer::class)
    try {
      val recognizer = mockk<SpeechRecognizer>(relaxed = true)
      every { SpeechRecognizer.isRecognitionAvailable(context) } returns true
      every { SpeechRecognizer.createSpeechRecognizer(context) } returns recognizer
      every { recognizer.setRecognitionListener(any()) } returns Unit
      val helper = createHelper()
      helper.setField("speechRecognizer", null)

      val result = helper.invokeEnsureRecognizer()

      assertTrue(result)
      assertSame(recognizer, helper.getField("speechRecognizer"))
    } finally {
      unmockkStatic(SpeechRecognizer::class)
    }
  }

  @Test
  fun startListening_doesNotProceedWhenEnsureFails() {
    mockkStatic(SpeechRecognizer::class)
    try {
      every { SpeechRecognizer.isRecognitionAvailable(context) } returns false
      val helper = createHelper()
      helper.setField("speechRecognizer", null)

      helper.startListening(onResult = {})

      assertFalse(helper.isListening())
    } finally {
      unmockkStatic(SpeechRecognizer::class)
    }
  }

  @Test
  fun errorMessageForCode_mapsAllErrors() {
    val helper = createHelper()
    val messages =
        listOf(
            SpeechRecognizer.ERROR_AUDIO to "Audio recording error",
            SpeechRecognizer.ERROR_CLIENT to "Client error",
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to "Microphone permission denied",
            SpeechRecognizer.ERROR_NETWORK to "Network error",
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT to "Network timeout",
            SpeechRecognizer.ERROR_NO_MATCH to "No speech recognized",
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY to "Speech recognizer busy",
            SpeechRecognizer.ERROR_SERVER to "Recognition service error",
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT to "No speech detected",
            999 to "Unknown recognition error (999)",
        )

    messages.forEach { (code, expected) ->
      assertEquals(expected, helper.invokeErrorMessageForCode(code))
    }
  }

  @Test
  fun destroy_releasesRecognizer() {
    val recognizer = mockk<SpeechRecognizer>(relaxed = true)
    val helper = createHelper(recognizer)

    helper.destroy()

    verify { recognizer.destroy() }
    assertNull(helper.getField("speechRecognizer"))
  }

  private fun SpeechToTextHelper.invokeEnsureRecognizer(): Boolean {
    val method = SpeechToTextHelper::class.java.getDeclaredMethod("ensureSpeechRecognizer")
    method.isAccessible = true
    return method.invoke(this) as Boolean
  }

  private fun SpeechToTextHelper.invokeDeliverFinalResult(bundle: Bundle?) {
    val method =
        SpeechToTextHelper::class.java.getDeclaredMethod("deliverFinalResult", Bundle::class.java)
    method.isAccessible = true
    method.invoke(this, bundle)
  }

  private fun SpeechToTextHelper.invokeErrorMessageForCode(code: Int): String {
    val method =
        SpeechToTextHelper::class
            .java
            .getDeclaredMethod("errorMessageForCode", Int::class.javaPrimitiveType)
    method.isAccessible = true
    return method.invoke(this, code) as String
  }
}
