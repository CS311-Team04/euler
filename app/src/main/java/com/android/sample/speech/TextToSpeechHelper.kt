package com.android.sample.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/** Abstraction so UI/test code can provide custom playback implementations if needed. */
interface SpeechPlayback {
  fun speak(
      text: String,
      utteranceId: String,
      onStart: () -> Unit = {},
      onDone: () -> Unit = {},
      onError: (Throwable?) -> Unit = {}
  )

  fun stop()

  fun shutdown()
}

/**
 * Lightweight wrapper around [TextToSpeech] to provide easy one-shot playback for chat messages.
 *
 * Responsibilities:
 * - Initialize TTS with the device locale (falling back gracefully when unavailable)
 * - Expose a simple [speak] method with callbacks for start/done/error events
 * - Allow clients to stop playback or release resources when no longer needed
 */
/**
 * Facade around [TextToSpeech] tailored for short chat responses.
 *
 * Key features:
 * - Lazy initialization with locale selection and queuing for early calls to [speak].
 * - Audio focus management that immediately reacquires focus when the system tries to duck.
 * - Loudness and EQ processing to boost volume while attenuating hiss/crackle.
 * - Main-thread callback guarantees for start/done/error observers.
 */
class TextToSpeechHelper(
    context: Context,
    private val preferredLocale: Locale = Locale.ENGLISH,
    private val ttsFactory: (Context, TextToSpeech.OnInitListener) -> TextToSpeech =
        { ctx, listener ->
          TextToSpeech(ctx, listener)
        },
    private val equalizerFactory: (Int) -> Equalizer? = { sessionId ->
      runCatching { Equalizer(0, sessionId) }.getOrNull()
    },
) : SpeechPlayback {

  private val appContext = context.applicationContext
  private val mainHandler = Handler(Looper.getMainLooper())
  private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val audioFocusChangeListener =
      AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
          AudioManager.AUDIOFOCUS_GAIN -> {
            hasAudioFocus = true
          }
          AudioManager.AUDIOFOCUS_LOSS,
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
            hasAudioFocus = false
            currentUtteranceId?.let { id ->
              callbacks.remove(id)?.let { cb ->
                dispatchOnMain { cb.onError(IllegalStateException("Audio focus lost")) }
              }
            }
            currentUtteranceId = null
            textToSpeech.stop()
          }
          AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
            // Immediately try to regain exclusive focus so Android stops ducking our speech.
            requestAudioFocus(force = true)
          }
        }
      }
  private var audioFocusRequest: AudioFocusRequest? = null
  private val audioAttributes: AudioAttributes by lazy {
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
  }
  @Volatile private var hasAudioFocus: Boolean = false
  @Volatile private var currentUtteranceId: String? = null
  private var loudnessEnhancer: LoudnessEnhancer? = null
  private var enhancerSessionId: Int = AudioManager.ERROR
  private var equalizer: Equalizer? = null
  private val speechStyle = AtomicReference(SpeechStyle())

  private data class PendingSpeech(
      val text: String,
      val utteranceId: String,
      val callbacks: SpeechCallbacks
  )

  private data class SpeechCallbacks(
      val onStart: () -> Unit,
      val onDone: () -> Unit,
      val onError: (Throwable?) -> Unit
  )

  private val pendingQueue = mutableListOf<PendingSpeech>()
  private val callbacks = ConcurrentHashMap<String, SpeechCallbacks>()

  @Volatile private var isReady: Boolean = false

  private lateinit var textToSpeech: TextToSpeech

  init {
    textToSpeech =
        ttsFactory(appContext) { status ->
          isReady =
              if (status == TextToSpeech.SUCCESS) {
                val languageReady = selectLanguage()
                if (languageReady) {
                  configureEngine()
                }
                languageReady
              } else {
                false
              }

          synchronized(pendingQueue) {
            if (isReady) {
              // Replay queued requests now that initialization succeeded.
              val requests = pendingQueue.toList()
              pendingQueue.clear()
              requests.forEach { speakInternal(it) }
            } else {
              // Notify queued callbacks that TTS is unavailable.
              val failure =
                  IllegalStateException("TextToSpeech engine is not available on this device")
              pendingQueue.forEach { pending ->
                dispatchOnMain { pending.callbacks.onError(failure) }
              }
              pendingQueue.clear()
            }
          }
        }

    textToSpeech.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String) {
            enableEnhancer()
            callbacks[utteranceId]?.let { cb -> dispatchOnMain { cb.onStart() } }
          }

          override fun onDone(utteranceId: String) {
            callbacks.remove(utteranceId)?.let { cb ->
              currentUtteranceId = null
              disableEnhancer()
              abandonAudioFocus()
              dispatchOnMain { cb.onDone() }
            }
          }

          @Deprecated("Deprecated in API 21")
          override fun onError(utteranceId: String) {
            callbacks.remove(utteranceId)?.let { cb ->
              currentUtteranceId = null
              disableEnhancer()
              abandonAudioFocus()
              dispatchOnMain { cb.onError(null) }
            }
          }

          override fun onError(utteranceId: String, errorCode: Int) {
            callbacks.remove(utteranceId)?.let { cb ->
              val error = IllegalStateException("TTS error (code=$errorCode)")
              currentUtteranceId = null
              disableEnhancer()
              abandonAudioFocus()
              dispatchOnMain { cb.onError(error) }
            }
          }
        })
  }

  /**
   * Speaks the given [text]. If the TTS engine is still warming up, the request is queued and
   * played once initialization finishes.
   *
   * @param text Message that should be read aloud.
   * @param utteranceId Unique ID to track callbacks (use message ID when available).
   * @param onStart Invoked on the main thread when playback begins.
   * @param onDone Invoked on the main thread when playback completes.
   * @param onError Invoked on the main thread if playback fails (initialization or runtime).
   */
  /**
   * Speaks the provided text once. If the engine is still warming up, the request is enqueued.
   *
   * @param text sentence or paragraph to vocalise.
   * @param utteranceId unique identifier so clients can correlate callbacks.
   * @param onStart invoked on the main thread as soon as playback begins.
   * @param onDone invoked after successful completion (main thread).
   * @param onError invoked if the engine fails to enqueue or speak the utterance.
   */
  override fun speak(
      text: String,
      utteranceId: String,
      onStart: () -> Unit,
      onDone: () -> Unit,
      onError: (Throwable?) -> Unit
  ) {
    val callbacks =
        SpeechCallbacks(
            onStart = onStart,
            onDone = onDone,
            onError = onError,
        )
    val request = PendingSpeech(text = text, utteranceId = utteranceId, callbacks = callbacks)

    if (!isReady) {
      synchronized(pendingQueue) { pendingQueue.add(request) }
      return
    }

    speakInternal(request)
  }

  /** Stops any ongoing playback immediately. */
  /**
   * Stops any ongoing playback and clears callbacks.
   *
   * Does not release engine resources; use [shutdown] when finished with the helper entirely.
   */
  override fun stop() {
    callbacks.clear()
    currentUtteranceId = null
    disableEnhancer()
    abandonAudioFocus()
    textToSpeech.stop()
  }

  /** Releases the underlying TTS engine. Call when you no longer need speech output. */
  /** Fully tears down TextToSpeech and audio effects. Call this from `onDestroy`. */
  override fun shutdown() {
    callbacks.clear()
    currentUtteranceId = null
    releaseEnhancer()
    abandonAudioFocus()
    textToSpeech.stop()
    textToSpeech.shutdown()
  }

  /**
   * Enqueues speech on the underlying engine.
   *
   * Audio focus is requested immediately; failure results in a callback error rather than silent
   * loss.
   */
  private fun speakInternal(request: PendingSpeech) {
    if (!requestAudioFocus()) {
      callbacks.remove(request.utteranceId)
      val failure = IllegalStateException("Audio focus not granted for text-to-speech")
      dispatchOnMain { request.callbacks.onError(failure) }
      return
    }
    currentUtteranceId = request.utteranceId
    callbacks[request.utteranceId] = request.callbacks
    val prosody = computeProsody(request.text)
    textToSpeech.setSpeechRate(prosody.rate)
    textToSpeech.setPitch(prosody.pitch)
    val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f) }
    val result =
        textToSpeech.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, request.utteranceId)
    if (result != TextToSpeech.SUCCESS) {
      callbacks.remove(request.utteranceId)
      currentUtteranceId = null
      disableEnhancer()
      abandonAudioFocus()
      val failure = IllegalStateException("Failed to enqueue text-to-speech (code=$result)")
      dispatchOnMain { request.callbacks.onError(failure) }
    }
  }

  /** Ensures callbacks run on the main thread even when triggered from background coroutines. */
  private fun dispatchOnMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      block()
    } else {
      mainHandler.post(block)
    }
  }

  /**
   * Iterates through preferred locales, picking the first one supported by the device TTS engine.
   */
  private fun selectLanguage(): Boolean {
    val candidates = buildList {
      add(preferredLocale)
      if (preferredLocale != Locale.ENGLISH) add(Locale.ENGLISH)
      val device = Locale.getDefault()
      if (!contains(device)) add(device)
    }

    for (locale in candidates) {
      val result = textToSpeech.setLanguage(locale)
      if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
        return true
      }
    }
    return false
  }

  /** Applies common attributes (usage, speech rate, pitch) and primes audio effects. */
  private fun configureEngine() {
    textToSpeech.setAudioAttributes(audioAttributes)
    val style = speechStyle.get()
    textToSpeech.setSpeechRate(style.baseRate)
    textToSpeech.setPitch(style.basePitch)
    applyPreferredVoice()
    prepareEnhancer()
  }

  private fun applyPreferredVoice() {
    val availableVoices =
        runCatching { textToSpeech.voices?.toSet() }
            .onFailure { error -> Log.w(TAG, "Unable to query available voices", error) }
            .getOrNull() ?: return
    val selected = selectPreferredVoice(availableVoices) ?: return
    val result = textToSpeech.setVoice(selected)
    if (result != TextToSpeech.SUCCESS) {
      Log.w(TAG, "Failed to apply preferred voice ${selected.name} (code=$result)")
    } else {
      Log.d(TAG, "Applied premium voice ${selected.name} (${selected.locale})")
    }
  }

  private fun selectPreferredVoice(voices: Set<Voice>): Voice? {
    if (voices.isEmpty()) return null
    val languageMatches =
        voices.filter { voice -> voice.locale?.language == preferredLocale.language }
    val candidates = if (languageMatches.isNotEmpty()) languageMatches else voices.toList()

    val prioritized =
        candidates.sortedWith(
            compareByDescending<Voice> { voice ->
                  val features = voice.features ?: emptySet()
                  if (features.contains(FEATURE_NEURAL_NETWORK)) 2 else 0
                }
                .thenByDescending { it.quality }
                .thenBy { it.latency })
    return prioritized.firstOrNull()
  }

  /** Requests exclusive audio focus so system ducking does not reduce TTS volume mid-utterance. */
  private fun requestAudioFocus(force: Boolean = false): Boolean {
    if (hasAudioFocus && !force) return true
    val request =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
            .setWillPauseWhenDucked(false)
            .build()
    audioFocusRequest = request
    val granted = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    hasAudioFocus = granted
    return granted
  }

  /** Releases audio focus (if held) once playback and callbacks conclude. */
  private fun abandonAudioFocus() {
    hasAudioFocus = false
    audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    audioFocusRequest = null
  }

  /** Lazily configures `LoudnessEnhancer` and the companion equalizer for the active session ID. */
  private fun prepareEnhancer() {
    val sessionId = resolveAudioSessionId()
    if (sessionId == AudioManager.ERROR || sessionId <= 0) return
    if (enhancerSessionId == sessionId) return
    releaseEnhancer()
    loudnessEnhancer =
        LoudnessEnhancer(sessionId).apply {
          setTargetGain(ENHANCER_GAIN_MB)
          enabled = false
        }
    enhancerSessionId = sessionId
    setupEqualizer(sessionId)
  }

  private fun enableEnhancer() {
    prepareEnhancer()
    loudnessEnhancer?.apply {
      setTargetGain(ENHANCER_GAIN_MB)
      enabled = true
    }
    equalizer?.enabled = true
  }

  private fun disableEnhancer() {
    loudnessEnhancer?.enabled = false
    equalizer?.enabled = false
  }

  private fun releaseEnhancer() {
    loudnessEnhancer?.release()
    loudnessEnhancer = null
    enhancerSessionId = AudioManager.ERROR
    releaseEqualizer()
  }

  /** Applies a gentle high-frequency cut and a small low-end reduction to soften artefacts. */
  private fun setupEqualizer(sessionId: Int) {
    releaseEqualizer()
    val newEqualizer = equalizerFactory(sessionId) ?: return
    equalizer =
        newEqualizer.apply {
          enabled = true
          val bandRange = bandLevelRange
          val trebleCut = (bandRange[0] / 2).coerceAtMost(0).toShort()
          val bassCut = (bandRange[0] / 3).coerceAtMost(0).toShort()
          val bands = numberOfBands.toInt()
          for (band in 0 until bands) {
            val centerHz = getCenterFreq(band.toShort()) / 1000
            when {
              centerHz >= TREBLE_CUTOFF_HZ -> setBandLevel(band.toShort(), trebleCut)
              centerHz <= BASS_CUTOFF_HZ -> setBandLevel(band.toShort(), bassCut)
              else -> setBandLevel(band.toShort(), 0)
            }
          }
        }
  }

  /** Disposes the equalizer instance if one is currently attached to the audio session. */
  private fun releaseEqualizer() {
    equalizer?.release()
    equalizer = null
  }

  /** Attempts to read the internal TTS audio session ID via reflection (API-dependent). */
  private fun resolveAudioSessionId(): Int {
    return runCatching {
          TextToSpeech::class.java.getMethod("getAudioSessionId").invoke(textToSpeech) as? Int
        }
        .getOrNull() ?: AudioManager.ERROR
  }

  companion object {
    private const val TAG = "TextToSpeechHelper"
    private const val ENHANCER_GAIN_MB = 1800
    private const val TREBLE_CUTOFF_HZ = 6_500
    private const val BASS_CUTOFF_HZ = 180
    private const val FEATURE_NEURAL_NETWORK = "com.google.android.tts.feature.neural_network"
  }

  data class SpeechStyle(
      // Base rate of 1.4f (40% faster than normal) chosen for improved speech clarity
      // and reduced robotic feel. Can be adjusted via updateSpeechStyle() if needed.
      val baseRate: Float = 1.4f,
      val minRate: Float = 1.0f,
      val maxRate: Float = 1.5f,
      val shortSentenceThreshold: Int = 8,
      val mediumSentenceThreshold: Int = 18,
      val longSentenceThreshold: Int = 40,
      val shortSentenceBoost: Float = 0.03f,
      val mediumSentenceSlowdown: Float = 0.04f,
      val longSentenceSlowdown: Float = 0.08f,
      val commaSlowdown: Float = 0.02f,
      val questionTempoBoost: Float = 0.02f,
      val basePitch: Float = 1.03f,
      val minPitch: Float = 0.92f,
      val maxPitch: Float = 1.18f,
      val questionPitchBoost: Float = 0.08f,
      val exclamationPitchBoost: Float = 0.05f,
  )

  fun updateSpeechStyle(style: SpeechStyle) {
    speechStyle.set(style)
    textToSpeech.setSpeechRate(style.baseRate)
    textToSpeech.setPitch(style.basePitch)
  }

  private data class Prosody(val rate: Float, val pitch: Float)

  private fun computeProsody(text: String): Prosody {
    val style = speechStyle.get()
    val trimmed = text.trim()
    if (trimmed.isEmpty()) {
      return Prosody(style.baseRate, style.basePitch)
    }

    val wordCount = trimmed.split(Regex("\\s+")).count { it.isNotBlank() }
    var rate = style.baseRate
    var pitch = style.basePitch

    when {
      wordCount >= style.longSentenceThreshold -> {
        rate -= style.longSentenceSlowdown
      }
      wordCount >= style.mediumSentenceThreshold -> {
        rate -= style.mediumSentenceSlowdown
      }
      wordCount <= style.shortSentenceThreshold -> {
        rate += style.shortSentenceBoost
      }
    }

    if (trimmed.contains(",")) {
      rate -= style.commaSlowdown
    }

    when {
      trimmed.endsWith("?") -> {
        pitch += style.questionPitchBoost
        rate += style.questionTempoBoost
      }
      trimmed.endsWith("!") -> {
        pitch += style.exclamationPitchBoost
      }
    }

    rate = rate.coerceIn(style.minRate, style.maxRate)
    val boundedPitch = pitch.coerceIn(style.minPitch, style.maxPitch)

    return Prosody(rate = rate, pitch = boundedPitch)
  }
}
