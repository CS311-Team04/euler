package com.android.sample.VoiceChat.UI

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel
import com.android.sample.speech.SpeechPlayback
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.speech.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay as coroutinesDelay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private const val TAG = "VoiceScreen"
private const val RMS_MIN_DB = -5f
private const val RMS_MAX_DB = 10f

/**
 * Full-screen voice UI: requests microphone permission, manages mic lifecycle, monitors audio
 * levels and renders the visualizer. Provides close action.
 */
@Composable
fun VoiceScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    speechHelper: SpeechToTextHelper? = null,
    speechPlayback: SpeechPlayback? = null,
    levelSourceFactory: () -> LevelSource = { AndroidMicLevelSource() },
    initialHasMicOverride: Boolean? = null,
    permissionRequester: ((String) -> Unit)? = null,
    silenceThresholdOverride: Float? = null,
    silenceDurationOverride: Long? = null,
    voiceChatViewModel: VoiceChatViewModel = viewModel()
) {
  val context = LocalContext.current
  // Used for async shutdown when the user presses back or closes the sheet.
  val coroutineScope = rememberCoroutineScope()
  val playback = remember(context, speechPlayback) { speechPlayback ?: TextToSpeechHelper(context) }
  // Collect the real-time voice UI state; this drives the status banner.
  val voiceUiState by voiceChatViewModel.uiState.collectAsStateWithLifecycle()

  val alreadyGranted =
      initialHasMicOverride
          ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
              PackageManager.PERMISSION_GRANTED)

  var hasMic by remember { mutableStateOf(alreadyGranted) }

  // Runtime permission
  val launcher =
      if (permissionRequester == null) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted -> hasMic = handlePermissionResult(granted) })
      } else {
        null
      }

  val requestPermission =
      remember(permissionRequester, launcher) {
        permissionRequester ?: { permission: String -> launcher?.launch(permission) }
      }

  LaunchedEffect(Unit) {
    if (logInitialPermissionState(alreadyGranted)) {
      requestPermission(Manifest.permission.RECORD_AUDIO)
    }
    // Note: Conversation is now managed automatically in handleUserUtterance
    // It will reuse existing conversation or create new one as needed
  }

  // Ensure the back button tears down the websocket/audio before leaving the screen.
  BackHandler { safeShutdown(onClose, voiceChatViewModel, playback, coroutineScope) }

  LaunchedEffect(voiceChatViewModel, playback) {
    voiceChatViewModel.speechRequests.collect { request ->
      playback.speak(
          text = request.text,
          utteranceId = request.utteranceId,
          onStart = { voiceChatViewModel.onSpeechStarted() },
          onDone = { voiceChatViewModel.onSpeechFinished() },
          onError = { throwable -> voiceChatViewModel.onSpeechError(throwable) })
    }
  }

  // State to control whether the microphone is active
  var isMicActive by remember { mutableStateOf(false) }
  // Visualizer level sources:
  // - `mockSource` is silent and used when microphone is inactive.
  // - `speechLevelSource` streams levels derived from SpeechRecognizer RMS callbacks.
  val mockSource = remember { MockLevelSource(0f) }
  val speechLevelSource = remember { MutableLevelSource() }
  val mic =
      remember(levelSourceFactory, speechHelper) {
        if (speechHelper == null) {
          levelSourceFactory()
        } else {
          speechLevelSource
        }
      }

  // Voice inactivity detection to automatically disable mic after silence
  var lastVoiceTime by remember { mutableStateOf(System.currentTimeMillis()) }
  val silenceThreshold = silenceThresholdOverride ?: 0.05f // Threshold to consider silence
  val silenceDuration = silenceDurationOverride ?: 2500L // 2.5 seconds of silence before auto...

  // Monitor current audio level to track inactivity
  var currentLevel by remember { mutableStateOf(0f) }

  // Microphone control (start/stop)
  if (speechHelper == null) {
    LaunchedEffect(isMicActive, hasMic) {
      if (isMicActive && hasMic) {
        val startResult =
            startMicrophoneSafely(
                startAction = { mic.start() },
                delayMs = { millis -> coroutinesDelay(millis) },
                currentTime = { System.currentTimeMillis() },
                log = { message -> Log.d(TAG, message) },
                errorLog = { message, throwable -> Log.e(TAG, message, throwable) })
        if (startResult.success) {
          lastVoiceTime = startResult.lastVoiceTime ?: System.currentTimeMillis()
        } else {
          isMicActive = false
        }
      } else {
        stopMicrophoneSafely(
            stopAction = { mic.stop() },
            log = { message -> Log.d(TAG, message) },
            errorLog = { message, throwable -> Log.e(TAG, message, throwable) })
        currentLevel = 0f
      }
    }
  } else {
    // When using external speech helper, ensure levels stay reset when not recording.
    LaunchedEffect(isMicActive) {
      if (!isMicActive) {
        currentLevel = 0f
        speechLevelSource.update(0f)
        speechHelper.stopListening()
      }
    }
    LaunchedEffect(Unit) {
      voiceChatViewModel.audioLevels.collect { level ->
        if (!isMicActive) {
          currentLevel = level
          speechLevelSource.update(level)
        }
      }
    }
  }

  // Cleanup when leaving the screen
  DisposableEffect(Unit) {
    onDispose {
      if (isMicActive) {
        mic.stop()
        Log.d("VoiceScreen", "Microphone stopped on dispose")
      }
      voiceChatViewModel.stopAll()
      speechHelper?.stopListening()
      playback.shutdown()
    }
  }

  // Collect audio levels when mic is active + silence detection
  if (speechHelper == null) {
    LaunchedEffect(isMicActive, hasMic) {
      if (isMicActive && hasMic) {
        try {
          Log.d("VoiceScreen", "Begin collecting audio levels")
          var frameCount = 0
          mic.levels.collect { level ->
            currentLevel = level
            frameCount++

            val evaluation =
                evaluateAudioLevel(
                    level = level,
                    silenceThreshold = silenceThreshold,
                    frameCount = frameCount,
                    currentTime = System.currentTimeMillis(),
                    lastVoiceTime = lastVoiceTime)

            lastVoiceTime = evaluation.updatedLastVoiceTime

            if (evaluation.shouldLogLevel) {
              Log.d(
                  TAG,
                  "Audio level: ${String.format("%.3f", level)} (silence threshold: $silenceThreshold)")
            }

            if (evaluation.voiceDetected) {
              Log.d(TAG, "Voice detected! Level: ${String.format("%.3f", level)}")
            }
          }
        } catch (e: Exception) {
          Log.e("VoiceScreen", "Error collecting audio levels", e)
          currentLevel = resetLevelAfterError()
        }
      } else {
        currentLevel = 0f
      }
    }

    // Periodically check whether to disable mic after continuous silence
    LaunchedEffect(isMicActive, hasMic) {
      monitorSilence(
          isMicActiveProvider = { isMicActive },
          hasMicProvider = { hasMic },
          delayMs = { millis -> coroutinesDelay(millis) },
          timeProvider = { System.currentTimeMillis() },
          lastVoiceTimeProvider = { lastVoiceTime },
          currentLevelProvider = { currentLevel },
          silenceThreshold = silenceThreshold,
          silenceDuration = silenceDuration,
          onDeactivate = { isMicActive = false },
          logger = { message -> Log.d(TAG, message) })
    }
  }

  val visualizerColor =
      when {
        voiceUiState.isSpeaking -> Color(0xFF097345)
        else -> Color(0xFC0000)
      }
  val backgroundColor = MaterialTheme.colorScheme.background
  val foregroundColor = MaterialTheme.colorScheme.onBackground

  Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {

    // Visualizer in the center - uses real mic if active, otherwise silent mock
    // ManagedLevelSource prevents VoiceVisualizer from auto start/stop
    VoiceVisualizer(
        levelSource =
            remember(isMicActive, hasMic, speechHelper) {
              when {
                isMicActive && hasMic -> ManagedLevelSource(mic)
                speechHelper != null -> ManagedLevelSource(speechLevelSource)
                else -> mockSource
              }
            },
        preset = VisualPreset.Bloom, // Bloom visualizer
        color = visualizerColor,
        petals = 4,
        size = 1400.dp,
        modifier = Modifier.align(Alignment.Center).offset(y = 180.dp))

    // Bottom buttons
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(50.dp),
              verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    onClick = {
                      if (!hasMic && speechHelper == null) {
                        return@RoundIconButton
                      }
                      if (speechHelper != null) {
                        if (isMicActive) {
                          Log.d(TAG, "Already listening via speech helper; ignoring toggle")
                          return@RoundIconButton
                        }
                        isMicActive = true
                        lastVoiceTime = System.currentTimeMillis()
                        speechHelper.startListening(
                            onResult = { recognized ->
                              voiceChatViewModel.handleUserUtterance(recognized)
                            },
                            onError = { message -> voiceChatViewModel.reportError(message) },
                            onComplete = {
                              isMicActive = false
                              speechLevelSource.update(0f)
                            },
                            onPartial = { partial -> voiceChatViewModel.onUserTranscript(partial) },
                            onRms = { rms ->
                              val normalized =
                                  ((rms - RMS_MIN_DB) / (RMS_MAX_DB - RMS_MIN_DB)).coerceIn(0f, 1f)
                              currentLevel = normalized
                              speechLevelSource.update(normalized)
                              lastVoiceTime = System.currentTimeMillis()
                            })
                      } else {
                        isMicActive = !isMicActive
                        Log.d(TAG, "Microphone ${if (isMicActive) "enabled" else "disabled"}")
                        if (isMicActive) {
                          lastVoiceTime = System.currentTimeMillis()
                        }
                      }
                    },
                    iconVector = Icons.Filled.Mic,
                    contentDescription = "Toggle microphone",
                    size = 100.dp,
                    background =
                        if (isMicActive) Color(0x33000000) else Color(0x0), // Visual indicator
                    iconTint = if (isMicActive) Color(0xFFFF0000) else foregroundColor)
                RoundIconButton(
                    onClick = {
                      safeShutdown(onClose, voiceChatViewModel, playback, coroutineScope)
                    },
                    iconVector = Icons.Default.Close,
                    contentDescription = "Close voice screen",
                    size = 100.dp,
                    background = Color(0x0),
                    iconTint = foregroundColor)
              }
          Spacer(Modifier.height(14.dp))

          StatusCard(uiState = voiceUiState)

          Text(
              text = "Powered by APERTUS Swiss LLM",
              color = Color(0xFF9A9A9A),
              fontSize = 12.sp,
              modifier = Modifier.alpha(0.9f))
        }
  }
}

@Composable
internal fun StatusCard(uiState: VoiceChatViewModel.VoiceChatUiState) {
  val errorMessage = uiState.lastError?.takeIf { it.isNotBlank() } ?: return
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 24.dp)
              .background(Color(0x33000000), shape = CircleShape)
              .padding(vertical = 12.dp)) {
        Text(text = errorMessage, color = Color(0xFFFF6F61), fontSize = 11.sp)
      }
}

private fun String.clip(maxLength: Int): String {
  if (length <= maxLength) return this
  return take(maxLength - 1).trimEnd() + "…"
}

/**
 * Helper that guarantees we disconnect the streaming session before closing the screen.
 *
 * `onClose` might pop the navigation back stack immediately, so the disconnect call is wrapped in a
 * coroutine to avoid blocking the UI thread during socket shutdown.
 */
private fun safeShutdown(
    onClose: () -> Unit,
    viewModel: VoiceChatViewModel,
    speechPlayback: SpeechPlayback,
    coroutineScope: CoroutineScope
) {
  coroutineScope.launch {
    viewModel.stopAll()
    speechPlayback.stop()
    onClose()
  }
}

/** Circular icon button used in the voice UI for mic/close actions. */
@Composable
private fun RoundIconButton(
    onClick: () -> Unit,
    iconVector: ImageVector,
    contentDescription: String,
    size: Dp,
    background: Color,
    iconTint: Color
) {
  Surface(
      color = background,
      shape = CircleShape,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = Modifier.size(size).clickable { onClick() }) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Icon(
              imageVector = iconVector,
              contentDescription = contentDescription,
              tint = iconTint,
              modifier = Modifier.size(size * 0.5f))
        }
      }
}

@VisibleForTesting
internal fun updateLastVoiceTimestamp(
    level: Float,
    silenceThreshold: Float,
    currentTime: Long,
    lastVoiceTime: Long
): Long {
  return if (level > silenceThreshold) currentTime else lastVoiceTime
}

@VisibleForTesting
internal fun shouldDeactivateMic(
    currentTime: Long,
    lastVoiceTime: Long,
    currentLevel: Float,
    silenceThreshold: Float,
    silenceDuration: Long
): Boolean {
  val timeSinceLastVoice = currentTime - lastVoiceTime
  return timeSinceLastVoice > silenceDuration && currentLevel <= silenceThreshold
}

@VisibleForTesting internal fun resetLevelAfterError(): Float = 0f

@VisibleForTesting
internal data class AudioLevelEvaluation(
    val updatedLastVoiceTime: Long,
    val shouldLogLevel: Boolean,
    val voiceDetected: Boolean
)

@VisibleForTesting
internal fun evaluateAudioLevel(
    level: Float,
    silenceThreshold: Float,
    frameCount: Int,
    currentTime: Long,
    lastVoiceTime: Long
): AudioLevelEvaluation {
  val updatedLastVoice =
      updateLastVoiceTimestamp(level, silenceThreshold, currentTime, lastVoiceTime)
  val shouldLogLevel = frameCount % 30 == 0
  val voiceDetected = level > silenceThreshold && frameCount % 10 == 0
  return AudioLevelEvaluation(
      updatedLastVoiceTime = updatedLastVoice,
      shouldLogLevel = shouldLogLevel,
      voiceDetected = voiceDetected)
}

@VisibleForTesting
data class MicrophoneStartResult(val success: Boolean, val lastVoiceTime: Long? = null)

@VisibleForTesting
suspend fun startMicrophoneSafely(
    startAction: () -> Unit,
    delayMs: suspend (Long) -> Unit,
    currentTime: () -> Long,
    log: (String) -> Unit,
    errorLog: (String, Throwable) -> Unit,
    delayDurationMs: Long = 100L
): MicrophoneStartResult {
  return try {
    log("Starting microphone...")
    startAction()
    val now = currentTime()
    delayMs(delayDurationMs)
    log("Microphone started successfully")
    MicrophoneStartResult(success = true, lastVoiceTime = now)
  } catch (e: Throwable) {
    errorLog("Microphone start error", e)
    MicrophoneStartResult(success = false)
  }
}

@VisibleForTesting
fun stopMicrophoneSafely(
    stopAction: () -> Unit,
    log: (String) -> Unit,
    errorLog: (String, Throwable) -> Unit
) {
  try {
    log("Stopping microphone...")
    stopAction()
    log("Microphone stopped")
  } catch (e: Throwable) {
    errorLog("Microphone stop error", e)
  }
}

@VisibleForTesting
internal fun handlePermissionResult(
    granted: Boolean,
    debugLog: (String) -> Unit = { message -> Log.d(TAG, message) },
    warnLog: (String) -> Unit = { message -> Log.w(TAG, message) }
): Boolean {
  debugLog("Permission RECORD_AUDIO: $granted")
  if (!granted) {
    warnLog("Microphone permission was denied by user")
  }
  return granted
}

@VisibleForTesting
internal fun logInitialPermissionState(
    alreadyGranted: Boolean,
    debugLog: (String) -> Unit = { message -> Log.d(TAG, message) }
): Boolean {
  return if (!alreadyGranted) {
    debugLog("Requesting RECORD_AUDIO permission...")
    true
  } else {
    debugLog("RECORD_AUDIO permission already granted")
    false
  }
}

@VisibleForTesting
internal suspend fun monitorSilence(
    isMicActiveProvider: () -> Boolean,
    hasMicProvider: () -> Boolean,
    delayMs: suspend (Long) -> Unit,
    timeProvider: () -> Long,
    lastVoiceTimeProvider: () -> Long,
    currentLevelProvider: () -> Float,
    silenceThreshold: Float,
    silenceDuration: Long,
    onDeactivate: () -> Unit,
    logger: (String) -> Unit = {}
) {
  if (!isMicActiveProvider() || !hasMicProvider()) return
  while (isMicActiveProvider()) {
    delayMs(500)
    val now = timeProvider()
    val lastVoice = lastVoiceTimeProvider()
    val level = currentLevelProvider()
    if (shouldDeactivateMic(now, lastVoice, level, silenceThreshold, silenceDuration)) {
      logger("Silence detected (${now - lastVoice}ms), auto deactivating mic")
      onDeactivate()
      break
    }
  }
}

/**
 * Lightweight [LevelSource] implementation that mirrors a single floating-point level. VoiceScreen
 * updates it from the SpeechRecognizer RMS callback so the visualizer can animate even though we
 * are not streaming raw PCM samples.
 */
private class MutableLevelSource : LevelSource {
  private val levelsState = MutableStateFlow(0f)
  override val levels: Flow<Float> = levelsState

  fun update(value: Float) {
    levelsState.value = value.coerceIn(0f, 1f)
  }

  override fun start() {}

  override fun stop() {}
}

// Mock LevelSource for previews
private class MockLevelSource(private val level: Float) : LevelSource {
  override val levels = flow {
    while (true) {
      emit(level.coerceIn(0f, 1f))
      coroutinesDelay(16)
    }
  }

  /** No-op in preview. */
  override fun start() {}

  /** No-op in preview. */
  override fun stop() {}
}

private class PreviewSpeechPlayback : SpeechPlayback {
  override fun speak(
      text: String,
      utteranceId: String,
      onStart: () -> Unit,
      onDone: () -> Unit,
      onError: (Throwable?) -> Unit
  ) {
    onStart()
    onDone()
  }

  override fun stop() {}

  override fun shutdown() {}
}

// Wrapper that prevents VoiceVisualizer from starting/stopping the mic automatically
// but still forwards the flow so VoiceVisualizer can collect levels
private class ManagedLevelSource(private val delegate: LevelSource) : LevelSource {
  override val levels: Flow<Float> = delegate.levels

  /** Intentionally does nothing; lifecycle is managed by VoiceScreen. */
  override fun start() {
    Log.d("ManagedLevelSource", "start() called but ignored (managed by VoiceScreen)")
  }

  /** Intentionally does nothing; lifecycle is managed by VoiceScreen. */
  override fun stop() {
    Log.d("ManagedLevelSource", "stop() called but ignored (managed by VoiceScreen)")
  }
}

/** Preview provider for different fixed audio levels. */
private class VoiceLevelProvider : PreviewParameterProvider<Float> {
  override val values: Sequence<Float>
    get() = sequenceOf(0f, 0.3f, 0.6f, 0.9f)
}

@Preview(
    name = "VoiceScreen - Silent",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true)
@Composable
private fun VoiceScreenPreviewSilent() {
  VoiceScreenPreviewContent(level = 0f)
}

@Preview(
    name = "VoiceScreen - Active",
    showBackground = true,
    backgroundColor = 0xFF000000,
    showSystemUi = true)
@Composable
private fun VoiceScreenPreviewActive() {
  VoiceScreenPreviewContent(level = 0.7f)
}

@Preview(
    name = "VoiceScreen - Multiple Levels", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun VoiceScreenPreviewMultiple(@PreviewParameter(VoiceLevelProvider::class) level: Float) {
  VoiceScreenPreviewContent(level = level)
}

/** Preview content used by the previews above. */
@Composable
@VisibleForTesting
internal fun VoiceScreenPreviewContent(
    level: Float,
    voiceChatViewModel: VoiceChatViewModel = viewModel()
) {
  MaterialTheme {
    VoiceScreen(
        onClose = {},
        modifier = Modifier.fillMaxSize(),
        levelSourceFactory = { MockLevelSource(level) },
        speechPlayback = PreviewSpeechPlayback(),
        voiceChatViewModel = voiceChatViewModel)
  }
}
