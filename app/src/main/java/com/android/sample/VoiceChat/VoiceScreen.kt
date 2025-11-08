package com.android.sample.VoiceChat

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay as coroutinesDelay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Full-screen voice UI: requests microphone permission, manages mic lifecycle, monitors audio
 * levels and renders the visualizer. Provides close action.
 */
@Composable
fun VoiceScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val alreadyGranted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
          PackageManager.PERMISSION_GRANTED

  var hasMic by remember { mutableStateOf(alreadyGranted) }

  // Runtime permission
  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { granted ->
            hasMic = granted
            android.util.Log.d("VoiceScreen", "Permission RECORD_AUDIO: $granted")
            if (!granted) {
              android.util.Log.w("VoiceScreen", "Microphone permission was denied by user")
            }
          })

  LaunchedEffect(Unit) {
    if (!alreadyGranted) {
      android.util.Log.d("VoiceScreen", "Requesting RECORD_AUDIO permission...")
      launcher.launch(Manifest.permission.RECORD_AUDIO)
    } else {
      android.util.Log.d("VoiceScreen", "RECORD_AUDIO permission already granted")
    }
  }

  // State to control whether the microphone is active
  var isMicActive by remember { mutableStateOf(false) }
  val mic = remember { AndroidMicLevelSource() }
  val mockSource = remember { MockLevelSource(0f) }

  // Voice inactivity detection to automatically disable mic after silence
  var lastVoiceTime by remember { mutableStateOf(System.currentTimeMillis()) }
  val silenceThreshold = 0.05f // Threshold to consider silence
  val silenceDuration = 2500L // 2.5 seconds of silence before auto deactivation

  // Monitor current audio level to track inactivity
  var currentLevel by remember { mutableStateOf(0f) }

  // Microphone control (start/stop)
  LaunchedEffect(isMicActive, hasMic) {
    if (isMicActive && hasMic) {
      try {
        android.util.Log.d("VoiceScreen", "Starting microphone...")
        mic.start()
        lastVoiceTime = System.currentTimeMillis()
        // Small delay to let the mic initialize
        coroutinesDelay(100)
        android.util.Log.d("VoiceScreen", "Microphone started successfully")
      } catch (e: Exception) {
        android.util.Log.e("VoiceScreen", "Microphone start error", e)
        e.printStackTrace()
        isMicActive = false
      }
    } else {
      try {
        android.util.Log.d("VoiceScreen", "Stopping microphone...")
        mic.stop()
        android.util.Log.d("VoiceScreen", "Microphone stopped")
      } catch (e: Exception) {
        android.util.Log.e("VoiceScreen", "Microphone stop error", e)
      }
      currentLevel = 0f
    }
  }

  // Cleanup when leaving the screen
  DisposableEffect(Unit) {
    onDispose {
      if (isMicActive) {
        mic.stop()
        android.util.Log.d("VoiceScreen", "Microphone stopped on dispose")
      }
    }
  }

  // Collect audio levels when mic is active + silence detection
  LaunchedEffect(isMicActive, hasMic) {
    if (isMicActive && hasMic) {
      try {
        android.util.Log.d("VoiceScreen", "Begin collecting audio levels")
        var frameCount = 0
        mic.levels.collect { level ->
          currentLevel = level
          frameCount++

          // Periodic logs for debugging
          if (frameCount % 30 == 0) { // ~ every 0.5s
            android.util.Log.d(
                "VoiceScreen",
                "Audio level: ${String.format("%.3f", level)} (silence threshold: $silenceThreshold)")
          }

          // If level is above the threshold, consider it as voice activity
          val now = System.currentTimeMillis()
          lastVoiceTime = updateLastVoiceTimestamp(level, silenceThreshold, now, lastVoiceTime)
          if (level > silenceThreshold && frameCount % 10 == 0) {
            android.util.Log.d(
                "VoiceScreen", "Voice detected! Level: ${String.format("%.3f", level)}")
          }
        }
      } catch (e: Exception) {
        android.util.Log.e("VoiceScreen", "Error collecting audio levels", e)
        currentLevel = resetLevelAfterError()
      }
    } else {
      currentLevel = 0f
    }
  }

  // Periodically check whether to disable mic after continuous silence
  LaunchedEffect(isMicActive) {
    if (isMicActive && hasMic) {
      while (isMicActive) {
        coroutinesDelay(500) // Check every 500ms
        val now = System.currentTimeMillis()
        if (shouldDeactivateMic(
            now, lastVoiceTime, currentLevel, silenceThreshold, silenceDuration)) {
          android.util.Log.d(
              "VoiceScreen", "Silence detected (${now - lastVoiceTime}ms), auto deactivating mic")
          isMicActive = false
          break
        }
      }
    }
  }

  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

    // Visualizer in the center - uses real mic if active, otherwise silent mock
    // ManagedLevelSource prevents VoiceVisualizer from auto start/stop
    VoiceVisualizer(
        levelSource =
            remember(isMicActive, hasMic) {
              if (isMicActive && hasMic) {
                ManagedLevelSource(mic)
              } else {
                mockSource
              }
            },
        preset = VisualPreset.Bloom, // Bloom visualizer
        color = Color(0xFC0000),
        petals = 4,
        size = 1500.dp,
        modifier = Modifier.align(Alignment.Center).offset(y = -10.dp))

    // Bottom buttons
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(50.dp),
              verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    onClick = {
                      if (hasMic) {
                        isMicActive = !isMicActive
                        android.util.Log.d(
                            "VoiceScreen",
                            "Microphone ${if (isMicActive) "enabled" else "disabled"}")
                        if (isMicActive) {
                          lastVoiceTime = System.currentTimeMillis()
                        }
                      }
                    },
                    iconVector = Icons.Filled.Mic,
                    contentDescription = "Toggle microphone",
                    size = 100.dp,
                    background =
                        if (isMicActive) Color(0x33FF0000) else Color(0x0), // Visual indicator
                    iconTint = if (isMicActive) Color(0xFFFF0000) else Color.White)
                RoundIconButton(
                    onClick = onClose,
                    iconVector = Icons.Default.Close,
                    contentDescription = "Close voice screen",
                    size = 100.dp,
                    background = Color(0x0),
                    iconTint = Color.White)
              }
          Spacer(Modifier.height(14.dp))

          // Simple debug level indicator while mic active
          if (isMicActive && hasMic) {
            Text(
                text =
                    "Level: ${String.format("%.2f", currentLevel)} ${if (currentLevel > silenceThreshold) "✓ Voice" else "Silence"}",
                color =
                    if (currentLevel > silenceThreshold) Color(0xFF2CD510) else Color(0xFF9A9A9A),
                fontSize = 11.sp,
                modifier = Modifier.alpha(0.8f).testTag("levelIndicator"))
            Spacer(Modifier.height(8.dp))
          }

          Text(
              text = "Powered by APERTUS Swiss LLM",
              color = Color(0xFF9A9A9A),
              fontSize = 12.sp,
              modifier = Modifier.alpha(0.9f))
        }
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

// Mock LevelSource for previews
private class MockLevelSource(private val level: Float) : LevelSource {
  override val levels = flow {
    while (true) {
      emit(level.coerceIn(0f, 1f))
      kotlinx.coroutines.delay(16)
    }
  }

  /** No-op in preview. */
  override fun start() {}

  /** No-op in preview. */
  override fun stop() {}
}

// Wrapper that prevents VoiceVisualizer from starting/stopping the mic automatically
// but still forwards the flow so VoiceVisualizer can collect levels
private class ManagedLevelSource(private val delegate: LevelSource) : LevelSource {
  override val levels: Flow<Float> = delegate.levels

  /** Intentionally does nothing; lifecycle is managed by VoiceScreen. */
  override fun start() {
    android.util.Log.d("ManagedLevelSource", "start() called but ignored (managed by VoiceScreen)")
  }

  /** Intentionally does nothing; lifecycle is managed by VoiceScreen. */
  override fun stop() {
    android.util.Log.d("ManagedLevelSource", "stop() called but ignored (managed by VoiceScreen)")
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
internal fun VoiceScreenPreviewContent(level: Float) {
  val mockSource = remember { MockLevelSource(level) }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
    // Visualizer centered with mock source
    VoiceVisualizer(
        levelSource = mockSource,
        preset = VisualPreset.Bloom,
        color = Color(0xFC0000),
        petals = 4,
        size = 1500.dp,
        modifier = Modifier.align(Alignment.Center).offset(y = -10.dp))

    // Bottom buttons
    Column(
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(50.dp),
              verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    onClick = {},
                    iconVector = Icons.Filled.Mic,
                    contentDescription = "Toggle microphone",
                    size = 100.dp,
                    background = Color(0x0),
                    iconTint = Color.White)
                RoundIconButton(
                    onClick = {},
                    iconVector = Icons.Default.Close,
                    contentDescription = "Close voice screen",
                    size = 100.dp,
                    background = Color(0x0),
                    iconTint = Color.White)
              }
          Spacer(Modifier.height(14.dp))
          Text(
              text = "Powered by APERTUS Swiss LLM",
              color = Color(0xFF9A9A9A),
              fontSize = 12.sp,
              modifier = Modifier.alpha(0.9f))
        }
  }
}
