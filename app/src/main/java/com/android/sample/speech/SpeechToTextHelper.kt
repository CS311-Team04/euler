package com.android.sample.speech

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Helper class to encapsulate Android speech-to-text using RecognizerIntent.
 *
 * This class:
 * - Requests RECORD_AUDIO permission at runtime if needed
 * - Launches the native speech recognizer UI via RecognizerIntent
 * - Delivers the best recognized text to the provided callback
 * - Shows a small Toast prompting the user to speak
 * - Uses Locale.getDefault() for recognition language
 *
 * Usage from an Activity (ComponentActivity) or Fragment:
 *
 * val speechHelper = SpeechToTextHelper(requireContext(), this) button.setOnClickListener {
 * speechHelper.startListening { recognizedText -> // Use recognizedText (e.g., set it in a
 * TextField) } }
 */
class SpeechToTextHelper(private val context: Context, caller: ActivityResultCaller) {

  private var onResultCallback: ((String) -> Unit)? = null

  // Launcher to request microphone permission at runtime
  private val requestPermissionLauncher =
      caller.registerForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
          launchRecognizer()
        } else {
          notifyError("Microphone permission denied")
        }
      }

  // Launcher to start the RecognizerIntent and receive the result
  private val recognizerLauncher =
      caller.registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
        handleRecognizerResult(result)
      }

  /**
   * Starts listening for user speech and invokes [onResult] with the best recognized text. If the
   * RECORD_AUDIO permission is not granted, it will be requested first.
   */
  fun startListening(onResult: (String) -> Unit) {
    onResultCallback = onResult
    val permissionGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    if (permissionGranted) {
      launchRecognizer()
    } else {
      requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  // Builds and launches the RecognizerIntent if supported on the device
  private fun launchRecognizer() {
    val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
          // Prompt helps some implementations show a hint to the user
          putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak…")
        }

    // Verify there is an Activity to handle the intent
    val handler = intent.resolveActivity(context.packageManager)
    if (handler == null) {
      notifyError("Speech recognition not supported on this device")
      return
    }

    Toast.makeText(context, "Speak now…", Toast.LENGTH_SHORT).show()
    recognizerLauncher.launch(intent)
  }

  // Handles the result returned by the RecognizerIntent
  private fun handleRecognizerResult(result: ActivityResult) {
    if (result.resultCode != Activity.RESULT_OK) {
      // RESULT_CANCELED or other non-OK codes
      notifyError("Speech recognition cancelled or failed (code=${result.resultCode})")
      return
    }

    val data = result.data
    val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
    val bestMatch = matches?.firstOrNull()?.takeIf { it.isNotBlank() }

    if (bestMatch != null) {
      onResultCallback?.invoke(bestMatch)
    } else {
      notifyError("No speech recognized")
    }
  }

  // Delivers a brief message to the user and clears any pending callback
  private fun notifyError(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    // Do not clear the callback permanently; allow caller to retry with same instance
  }
}
