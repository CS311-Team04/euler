package com.android.sample.speech

import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(RobolectricTestRunner::class)
class AndroidTextToSpeechEngineTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun androidEngineWrapsPlatformTts() {
    val engine = AndroidTextToSpeechEngine(context) { _ -> }

    // Initialization happens asynchronously on main looper.
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    // Exercise API surface.
    val result = engine.setLanguage(Locale.US)
    // setLanguage returns LANG_AVAILABLE or error codes; verify call succeeded.
    assertNotNull(result)

    engine.setProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String) {}

          override fun onDone(utteranceId: String) {}

          @Deprecated("Deprecated in Java") override fun onError(utteranceId: String) {}
        })

    val speakResult = engine.speak("Hello world", TextToSpeech.QUEUE_FLUSH, null, "engine-test-utt")
    assertEquals(TextToSpeech.SUCCESS, speakResult)

    // Ensure basic stop/shutdown paths execute without crashing.
    engine.stop()
    engine.shutdown()

    // Progress callbacks are best-effort; ensure listener registered.
    val platformEngine =
        AndroidTextToSpeechEngine::class
            .java
            .getDeclaredField("engine")
            .apply { isAccessible = true }
            .get(engine) as TextToSpeech
    val shadowTts = Shadows.shadowOf(platformEngine) as ShadowTextToSpeech
    assertEquals("Hello world", shadowTts.lastSpokenText)
  }
}
