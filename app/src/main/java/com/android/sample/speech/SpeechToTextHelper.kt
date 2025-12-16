package com.android.sample.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Helper class to encapsulate Android speech-to-text using [SpeechRecognizer] directly (no Google
 * UI).
 *
 * Responsibilities:
 * - Request RECORD_AUDIO permission on demand.
 * - Start/stop speech recognition via [SpeechRecognizer].
 * - Surface partial and final results through callbacks.
 * - Provide basic error reporting through callbacks and Toasts.
 */
class SpeechToTextHelper(
    private val context: Context,
    caller: ActivityResultCaller,
    private val language: Locale = Locale.ENGLISH,
) {

  // Consumer-provided callbacks wired on each call to startListening.
  private var onResultCallback: ((String) -> Unit)? = null
  private var onErrorCallback: ((String) -> Unit)? = null
  private var onCompleteCallback: (() -> Unit)? = null
  private var onPartialCallback: ((String) -> Unit)? = null
  private var onRmsCallback: ((Float) -> Unit)? = null

  // Handler used to ensure callbacks are dispatched on the main thread.
  private val mainHandler = Handler(Looper.getMainLooper())

  // Lazily instantiated recognizer (created once permission/availability confirmed).
  private var speechRecognizer: SpeechRecognizer? = null

  private var isListening = false

  // Launcher to request microphone permission at runtime
  private val requestPermissionLauncher =
      caller.registerForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
          startInternalListening()
        } else {
          notifyError("Microphone permission denied")
        }
      }

  /**
   * Starts listening for user speech and invokes [onResult] with the best recognized text. If the
   * RECORD_AUDIO permission is not granted, it will be requested first.
   */
  fun startListening(
      onResult: (String) -> Unit,
      onError: ((String) -> Unit)? = null,
      onComplete: (() -> Unit)? = null,
      onPartial: ((String) -> Unit)? = null,
      onRms: ((Float) -> Unit)? = null,
  ) {
    onResultCallback = onResult
    onErrorCallback = onError
    onCompleteCallback = onComplete
    onPartialCallback = onPartial
    onRmsCallback = onRms

    if (isListening) {
      return // Already in a listening session.
    }

    if (!ensureSpeechRecognizer()) {
      return
    }

    val permissionGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    if (permissionGranted) {
      startInternalListening()
    } else {
      requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  /**
   * Stops an active recognition session, if any.
   *
   * Cancel is called to ensure pending callbacks are dropped.
   */
  fun stopListening() {
    if (!isListening) return
    speechRecognizer?.stopListening()
    speechRecognizer?.cancel()
    isListening = false
  }

  /**
   * Must be called when the hosting component is destroyed to release resources and avoid leaks.
   */
  fun destroy() {
    speechRecognizer?.destroy()
    speechRecognizer = null
  }

  /**
   * Instantiates the platform speech recognizer only once and registers our listener. Returns
   * `false` if the device does not support recognition.
   */
  private fun ensureSpeechRecognizer(): Boolean {
    if (speechRecognizer != null) return true
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      notifyError("Speech recognition not available on this device")
      return false
    }
    speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(recognitionListener)
        }
    return true
  }

  /**
   * Prepares and fires a recognition session (no UI). We request partial results and rely on our
   * listener to stream updates back to the UI layer.
   */
  private fun startInternalListening() {
    if (!ensureSpeechRecognizer()) {
      return
    }

    val languageTag = language.toLanguageTag()

    val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
          putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageTag)
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
          putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    Toast.makeText(context, "Speak now…", Toast.LENGTH_SHORT).show()
    isListening = true
    speechRecognizer?.startListening(intent)
  }

  private val recognitionListener =
      object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          // No-op: the mic prompt toast already informs the user.
        }

        override fun onBeginningOfSpeech() {
          // No-op: we do not need to update state here; RMS updates drive the visualizer.
        }

        override fun onRmsChanged(rmsdB: Float) {
          onRmsCallback?.let { callback ->
            val clamped = rmsdB.coerceIn(MIN_RMS_DB, MAX_RMS_DB)
            mainHandler.post { callback(clamped) }
          }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
          // Keep listening until onResults/onError arrives so we capture the final transcript.
        }

        override fun onError(error: Int) {
          isListening = false
          Log.w(TAG, "Speech recognition error: $error")
          notifyError(errorMessageForCode(error))
          onCompleteCallback?.invoke()
        }

        override fun onResults(results: Bundle?) {
          isListening = false
          deliverFinalResult(results)
        }

        override fun onPartialResults(partialResults: Bundle?) {
          val bestMatch = extractBestMatch(partialResults)
          if (!bestMatch.isNullOrBlank()) {
            mainHandler.post { onPartialCallback?.invoke(bestMatch) }
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
          // No custom events emitted by the platform for our use-case.
        }
      }

  /**
   * Emits the final transcription. If none is provided we surface a soft error so the caller can
   * prompt the user to retry.
   */
  private fun deliverFinalResult(results: Bundle?) {
    val bestMatch = extractBestMatch(results)
    if (!bestMatch.isNullOrBlank()) {
      mainHandler.post { onResultCallback?.invoke(bestMatch) }
      onCompleteCallback?.let { mainHandler.post(it) }
    } else {
      notifyError("No speech recognized")
    }
  }

  /**
   * Pulls the highest-confidence transcription from the speech recognizer bundle.
   *
   * Returns `null` if the recognizer did not produce any meaningful text.
   */
  private fun extractBestMatch(bundle: Bundle?): String? =
      bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf {
        it.isNotBlank()
      }

  /** Maps platform error codes to short user-friendly messages. */
  private fun errorMessageForCode(error: Int): String =
      when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Recognition service error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Unknown recognition error ($error)"
      }

  /**
   * Emits an error message to both the UI (Toast) and the consumer callbacks, then signals
   * completion so the caller can reset its state.
   */
  private fun notifyError(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    onErrorCallback?.invoke(message)
    onCompleteCallback?.invoke()
  }

  companion object {
    private const val TAG = "SpeechToTextHelper"
    private const val MIN_RMS_DB = -5f
    private const val MAX_RMS_DB = 10f
  }
}
