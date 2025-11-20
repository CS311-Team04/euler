package com.android.sample.speech

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.test.core.app.ApplicationProvider
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Locale
import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextToSpeechHelperTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun audioFocusGain_setsFlag() {
    val harness = createHarness()
    val helper = harness.helper

    helper.setBooleanField("hasAudioFocus", false)
    helper.audioFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

    assertTrue(helper.getBooleanField("hasAudioFocus"))
  }

  @Test
  fun audioFocusLoss_stopsPlaybackAndNotifies() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    val errors = mutableListOf<String?>()
    helper.registerCallback("utt", onError = { errors += it?.message })
    helper.setBooleanField("hasAudioFocus", true)
    helper.setCurrentUtterance("utt")

    helper.audioFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify { harness.tts.stop() }
    assertEquals(listOf("Audio focus lost"), errors)
    assertNull(helper.currentUtterance())
    assertFalse(helper.getBooleanField("hasAudioFocus"))
    assertTrue(helper.callbacksMap().isEmpty())
  }

  @Test
  fun audioFocusLossTransient_mirrorsLossBehavior() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    val errors = mutableListOf<String?>()
    helper.registerCallback("utt", onError = { errors += it?.message })
    helper.setBooleanField("hasAudioFocus", true)
    helper.setCurrentUtterance("utt")

    helper.audioFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify { harness.tts.stop() }
    assertEquals(listOf("Audio focus lost"), errors)
    assertNull(helper.currentUtterance())
    assertFalse(helper.getBooleanField("hasAudioFocus"))
  }

  @Test
  fun audioFocusDuck_requestsFocusAgain() {
    val harness = createHarness()
    val helper = harness.helper
    helper.setBooleanField("hasAudioFocus", false)

    val audioManager = mockk<AudioManager>()
    every { audioManager.requestAudioFocus(any<AudioFocusRequest>()) } returns
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    every { audioManager.requestAudioFocus(any(), any(), any()) } returns
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    helper.setField("audioManager", audioManager)

    helper.audioFocusListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

    assertTrue(helper.getBooleanField("hasAudioFocus"))
    verify { audioManager.requestAudioFocus(any<AudioFocusRequest>()) }
  }

  @Test
  fun initializationSuccess_replaysQueuedRequests() = runTest {
    val harness = createHarness()
    val helper = harness.helper

    helper.speak("Bonjour", "utt")
    assertEquals(1, helper.pendingQueueSize())

    harness.initListener.onInit(TextToSpeech.SUCCESS)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify { harness.tts.speak("Bonjour", TextToSpeech.QUEUE_FLUSH, any(), "utt") }
    assertEquals(0, helper.pendingQueueSize())
  }

  @Test
  fun initializationFailure_notifiesQueuedCallbacks() = runTest {
    val harness = createHarness()
    val helper = harness.helper

    var errorMessage: String? = null
    helper.speak("Bonjour", "utt", onError = { errorMessage = it?.message })

    harness.initListener.onInit(TextToSpeech.ERROR)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertEquals("TextToSpeech engine is not available on this device", errorMessage)
    assertEquals(0, helper.pendingQueueSize())
    verify(exactly = 0) { harness.tts.speak(any(), any(), any(), any()) }
  }

  @Test
  fun utteranceListener_onStart_dispatchesCallback() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    var started = false
    helper.registerCallback("utt", onStart = { started = true })
    helper.setCurrentUtterance("utt")

    harness.progressListener.onStart("utt")
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertTrue(started)
  }

  @Test
  fun utteranceListener_onDone_clearsState() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    val audioManager = mockk<AudioManager>()
    every { audioManager.abandonAudioFocusRequest(any<AudioFocusRequest>()) } returns
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    every { audioManager.abandonAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    helper.setField("audioManager", audioManager)

    var finished = false
    helper.registerCallback("utt", onDone = { finished = true })
    helper.setCurrentUtterance("utt")
    helper.setBooleanField("hasAudioFocus", true)

    harness.progressListener.onDone("utt")
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertTrue(finished)
    assertNull(helper.currentUtterance())
    assertFalse(helper.getBooleanField("hasAudioFocus"))
  }

  @Test
  fun utteranceListener_onErrorDeprecated_reportsNull() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    var error: Throwable? = Throwable("placeholder")
    helper.registerCallback("utt", onError = { error = it })
    helper.setCurrentUtterance("utt")

    harness.progressListener.onError("utt")
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertNull(error)
    assertNull(helper.currentUtterance())
  }

  @Test
  fun utteranceListener_onErrorWithCode_wrapsException() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    var error: Throwable? = null
    helper.registerCallback("utt", onError = { error = it })
    helper.setCurrentUtterance("utt")

    harness.progressListener.onError("utt", 42)
    Shadows.shadowOf(Looper.getMainLooper()).idle()

    assertTrue(error is IllegalStateException)
    assertEquals("TTS error (code=42)", error?.message)
    assertNull(helper.currentUtterance())
  }

  @Test
  fun speakInternal_requestsFocus_andRegistersCallback() {
    val harness = createHarness()
    val helper = harness.helper
    harness.initListener.onInit(TextToSpeech.SUCCESS)

    val audioManager = mockk<AudioManager>()
    every { audioManager.requestAudioFocus(any<AudioFocusRequest>()) } returns
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    every { audioManager.requestAudioFocus(any(), any(), any()) } returns
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    helper.setField("audioManager", audioManager)
    helper.setBooleanField("hasAudioFocus", false)

    val pending = pendingSpeech("Bonjour", "utt")
    helper.invokeSpeakInternal(pending)

    verify { audioManager.requestAudioFocus(any<AudioFocusRequest>()) }
    verify { harness.tts.speak("Bonjour", TextToSpeech.QUEUE_FLUSH, any(), "utt") }
    assertEquals("utt", helper.currentUtterance())
    assertTrue(helper.callbacksMap().containsKey("utt"))
  }

  @Test
  fun setupEqualizer_usesFactory_andConfiguresBands() {
    val mockTts = mockk<TextToSpeech>(relaxed = true)
    every { mockTts.setOnUtteranceProgressListener(any()) } returns TextToSpeech.SUCCESS
    every { mockTts.voices } returns emptySet()
    every { mockTts.setVoice(any()) } returns TextToSpeech.SUCCESS
    val mockEq = mockk<Equalizer>(relaxed = true)
    every { mockEq.bandLevelRange } returns shortArrayOf(-600, 600)
    every { mockEq.numberOfBands } returns 3.toShort()
    every { mockEq.getCenterFreq(0) } returns 100_000
    every { mockEq.getCenterFreq(1) } returns 2_000_000
    every { mockEq.getCenterFreq(2) } returns 8_000_000

    val helper =
        TextToSpeechHelper(
            context,
            Locale.FRENCH,
            ttsFactory = { _, _ -> mockTts },
            equalizerFactory = { mockEq },
        )

    val previousEq = mockk<Equalizer>()
    var released = false
    every { previousEq.release() } answers { released = true }
    helper.setEqualizer(previousEq)

    helper.invokeSetupEqualizer(777)

    assertTrue(released)
    verify { mockEq.enabled = true }
    verify { mockEq.setBandLevel(0.toShort(), (-200).toShort()) }
    verify { mockEq.setBandLevel(1.toShort(), 0.toShort()) }
    verify { mockEq.setBandLevel(2.toShort(), (-300).toShort()) }
    assertSame(mockEq, helper.getField("equalizer"))
  }

  @Test
  fun configureEngine_appliesPremiumVoiceWhenAvailable() {
    val voice = mockk<Voice>()
    every { voice.locale } returns Locale.FRENCH
    every { voice.features } returns setOf("com.google.android.tts.feature.neural_network")
    every { voice.quality } returns Voice.QUALITY_HIGH
    every { voice.latency } returns Voice.LATENCY_NORMAL
    every { voice.name } returns "fr-fr-premium"

    val tts = mockk<TextToSpeech>(relaxed = true)
    every { tts.voices } returns setOf(voice)
    every { tts.setVoice(voice) } returns TextToSpeech.SUCCESS
    every { tts.setOnUtteranceProgressListener(any()) } returns TextToSpeech.SUCCESS
    every { tts.setLanguage(any()) } returns TextToSpeech.LANG_AVAILABLE
    every { tts.setSpeechRate(any()) } returns TextToSpeech.SUCCESS
    every { tts.setPitch(any()) } returns TextToSpeech.SUCCESS
    every { tts.setAudioAttributes(any()) } returns TextToSpeech.SUCCESS
    every { tts.stop() } returns TextToSpeech.SUCCESS
    every { tts.shutdown() } answers {}
    every { tts.speak(any(), any(), any<Bundle>(), any()) } returns TextToSpeech.SUCCESS

    val harness = createHarness(tts = tts)
    every { tts.voices } returns setOf(voice)

    harness.initListener.onInit(TextToSpeech.SUCCESS)

    verify { tts.setVoice(voice) }
  }

  @Test
  fun speak_adjustsProsodyForQuestion() {
    val harness = createHarness()
    val helper = harness.helper
    helper.updateSpeechStyle(
        TextToSpeechHelper.SpeechStyle(
            baseRate = 0.9f,
            minRate = 0.85f,
            maxRate = 1.1f,
            questionPitchBoost = 0.1f,
            questionTempoBoost = 0.05f,
        ))
    harness.initListener.onInit(TextToSpeech.SUCCESS)
    helper.setBooleanField("isReady", true)

    val rates = mutableListOf<Float>()
    val pitches = mutableListOf<Float>()
    every { harness.tts.setSpeechRate(capture(rates)) } returns TextToSpeech.SUCCESS
    every { harness.tts.setPitch(capture(pitches)) } returns TextToSpeech.SUCCESS

    val pending = pendingSpeech("Are we on track?", "utt")
    helper.invokeSpeakInternal(pending)

    assertTrue(rates.last() > 0.9f)
    assertTrue(pitches.last() > 0.9f)
  }

  @Test
  fun speak_slowsDownForLongParagraph() {
    val harness = createHarness()
    val helper = harness.helper
    helper.updateSpeechStyle(
        TextToSpeechHelper.SpeechStyle(
            baseRate = 0.95f,
            minRate = 0.8f,
            maxRate = 1.05f,
            mediumSentenceThreshold = 12,
            mediumSentenceSlowdown = 0.05f,
            longSentenceThreshold = 20,
            longSentenceSlowdown = 0.09f,
        ))
    harness.initListener.onInit(TextToSpeech.SUCCESS)
    helper.setBooleanField("isReady", true)

    val rates = mutableListOf<Float>()
    every { harness.tts.setSpeechRate(capture(rates)) } returns TextToSpeech.SUCCESS

    val text =
        "La voix doit garder un rythme naturel tout en restant claire, " +
            "même lorsque la réponse contient plusieurs propositions et nuances."
    val pending = pendingSpeech(text, "utt-long")
    helper.invokeSpeakInternal(pending)

    assertTrue(rates.last() < 0.95f)
  }

  private data class Harness(
      val helper: TextToSpeechHelper,
      val tts: TextToSpeech,
      val initListener: TextToSpeech.OnInitListener,
      val progressSlot: CapturingSlot<UtteranceProgressListener>,
  ) {
    val progressListener: UtteranceProgressListener
      get() = progressSlot.captured
  }

  private fun createHarness(
      tts: TextToSpeech = mockk(relaxed = true),
      equalizerFactory: (Int) -> Equalizer? = { null }
  ): Harness {
    val progressSlot = slot<UtteranceProgressListener>()
    every { tts.setOnUtteranceProgressListener(capture(progressSlot)) } returns TextToSpeech.SUCCESS
    every { tts.setLanguage(any()) } returns TextToSpeech.LANG_AVAILABLE
    every { tts.setSpeechRate(any()) } returns TextToSpeech.SUCCESS
    every { tts.setPitch(any()) } returns TextToSpeech.SUCCESS
    every { tts.setAudioAttributes(any()) } returns TextToSpeech.SUCCESS
    every { tts.stop() } returns TextToSpeech.SUCCESS
    every { tts.shutdown() } answers {}
    every { tts.speak(any(), any(), any<Bundle>(), any()) } returns TextToSpeech.SUCCESS
    every { tts.voices } returns emptySet()
    every { tts.setVoice(any()) } returns TextToSpeech.SUCCESS

    var listener: TextToSpeech.OnInitListener? = null
    val helper =
        TextToSpeechHelper(
            context,
            Locale.FRENCH,
            ttsFactory = { _, init ->
              listener = init
              tts
            },
            equalizerFactory = equalizerFactory,
        )
    return Harness(helper, tts, listener ?: error("Init listener missing"), progressSlot)
  }

  private fun TextToSpeechHelper.audioFocusListener(): AudioManager.OnAudioFocusChangeListener {
    val field = TextToSpeechHelper::class.java.getDeclaredField("audioFocusChangeListener")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") return field.get(this) as AudioManager.OnAudioFocusChangeListener
  }

  private fun TextToSpeechHelper.setField(name: String, value: Any?) {
    val field = TextToSpeechHelper::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun TextToSpeechHelper.getField(name: String): Any? {
    val field = TextToSpeechHelper::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this)
  }

  private fun TextToSpeechHelper.setBooleanField(name: String, value: Boolean) =
      setField(name, value)

  private fun TextToSpeechHelper.getBooleanField(name: String): Boolean = getField(name) as Boolean

  private fun TextToSpeechHelper.setCurrentUtterance(value: String?) =
      setField("currentUtteranceId", value)

  private fun TextToSpeechHelper.currentUtterance(): String? =
      getField("currentUtteranceId") as? String

  private fun TextToSpeechHelper.callbacksMap(): MutableMap<String, Any> {
    val field = TextToSpeechHelper::class.java.getDeclaredField("callbacks")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") return field.get(this) as MutableMap<String, Any>
  }

  private fun TextToSpeechHelper.pendingQueueSize(): Int {
    val field = TextToSpeechHelper::class.java.getDeclaredField("pendingQueue")
    field.isAccessible = true
    val list = field.get(this) as MutableList<*>
    return list.size
  }

  private fun TextToSpeechHelper.registerCallback(
      utteranceId: String,
      onStart: () -> Unit = {},
      onDone: () -> Unit = {},
      onError: (Throwable?) -> Unit = {},
  ) {
    callbacksMap()[utteranceId] = newCallbacks(onStart, onDone, onError)
  }

  private fun TextToSpeechHelper.invokeSpeakInternal(pending: Any) {
    val method =
        TextToSpeechHelper::class
            .java
            .getDeclaredMethod(
                "speakInternal",
                pendingSpeechClass,
            )
    method.isAccessible = true
    method.invoke(this, pending)
  }

  private fun TextToSpeechHelper.setEqualizer(equalizer: Equalizer?) =
      setField("equalizer", equalizer)

  private fun TextToSpeechHelper.invokeSetupEqualizer(sessionId: Int) {
    val method =
        TextToSpeechHelper::class
            .java
            .getDeclaredMethod("setupEqualizer", Int::class.javaPrimitiveType)
    method.isAccessible = true
    method.invoke(this, sessionId)
  }

  private fun newCallbacks(
      onStart: () -> Unit = {},
      onDone: () -> Unit = {},
      onError: (Throwable?) -> Unit = {},
  ): Any {
    val start =
        object : Function0<Unit> {
          override fun invoke() = onStart()
        }
    val done =
        object : Function0<Unit> {
          override fun invoke() = onDone()
        }
    val error =
        object : Function1<Throwable?, Unit> {
          override fun invoke(p1: Throwable?) = onError(p1)
        }
    return speechCallbacksConstructor.newInstance(start, done, error)
  }

  private fun pendingSpeech(
      text: String,
      utteranceId: String,
      onStart: () -> Unit = {},
      onDone: () -> Unit = {},
      onError: (Throwable?) -> Unit = {},
  ): Any {
    return pendingSpeechConstructor.newInstance(
        text, utteranceId, newCallbacks(onStart, onDone, onError))
  }

  companion object {
    private val speechCallbacksClass =
        Class.forName("com.android.sample.speech.TextToSpeechHelper\$SpeechCallbacks")
    private val speechCallbacksConstructor =
        speechCallbacksClass
            .getDeclaredConstructor(
                Function0::class.java,
                Function0::class.java,
                Function1::class.java,
            )
            .apply { isAccessible = true }

    private val pendingSpeechClass =
        Class.forName("com.android.sample.speech.TextToSpeechHelper\$PendingSpeech")
    private val pendingSpeechConstructor =
        pendingSpeechClass
            .getDeclaredConstructor(
                String::class.java,
                String::class.java,
                speechCallbacksClass,
            )
            .apply { isAccessible = true }
  }
}
