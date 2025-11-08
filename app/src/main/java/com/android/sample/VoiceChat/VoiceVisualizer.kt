package com.android.sample.VoiceChat

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Visual presets for the voice visualizer. */
enum class VisualPreset {
  Bloom,
  Ripple
}

/**
 * Composable visualizer that renders audio-driven visuals using a [LevelSource]. It smooths levels
 * for fluid animation and supports different visual [preset]s.
 */
@Composable
fun VoiceVisualizer(
    levelSource: LevelSource,
    modifier: Modifier = Modifier,
    preset: VisualPreset = VisualPreset.Bloom,
    color: Color = Color(0xFFB61919),
    petals: Int = 10, // for Bloom preset
    size: Dp = 360.dp,
    onLevelUpdate: (Float) -> Unit = {}
) {
  val smoother = remember { LevelSmoother() }
  var rawLevel by remember { mutableStateOf(0f) }
  var level by remember { mutableStateOf(0f) }

  // Collect raw levels
  LaunchedEffect(levelSource) {
    levelSource.start()
    try {
      levelSource.levels.collect { rawLevel = it }
    } finally {
      levelSource.stop()
    }
  }

  // Continuous smoothing with frame-based animation for maximum fluidity (~60fps)
  LaunchedEffect(Unit) {
    while (isActive) {
      delay(8) // ~60fps
      // Smooth each frame for ultra-smooth animation
      level = smoother.step(rawLevel)
      onLevelUpdate(level)
    }
  }

  if (Build.VERSION.SDK_INT >= 33) {
    when (preset) {
      VisualPreset.Bloom -> AgslBloom(level, color, petals, size, modifier)
      VisualPreset.Ripple -> AgslRipple(level, color, size, modifier)
    }
  } else {
    // Fallback for < API 33
    LegacyPulse(level, color, size, modifier)
  }
}

/** Bloom-style visualization (API 33+) drawn with Canvas (simplified placeholder for AGSL). */
@RequiresApi(33)
@Composable
private fun AgslBloom(
    level: Float,
    color: Color,
    petals: Int,
    size: Dp,
    modifier: Modifier = Modifier
) {
  // Simplified version without shader for now - direct Canvas drawing
  var t by remember { mutableStateOf(0f) }
  var frameCount by remember { mutableStateOf(0L) }

  // Continuous animation targeting ~60fps
  LaunchedEffect(Unit) {
    val startTime = System.currentTimeMillis()
    while (isActive) {
      delay(10) // ~60fps
      t = ((System.currentTimeMillis() - startTime) / 1000f) // Time in seconds
      frameCount++ // Force recomposition each frame for smooth animation
    }
  }

  // Use frameCount to force recomposition each frame
  val r = remember { frameCount }

  Canvas(modifier.size(size)) {
    val canvasSize = this.size
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val minSize = min(canvasSize.width, canvasSize.height)

    val params = calculateBloomParameters(level, t, petals, minSize)

    drawCircle(
        color = color.copy(alpha = 0.01f + params.easeLevel * 0.05f),
        radius = params.base * (1f + params.breathing) * 0.9f * 10,
        center = center)

    val path = buildBloomPath(params.pathPoints)

    drawPath(path, color.copy(alpha = 0.75f + params.easeLevel * 0.3f), style = Fill)

    drawCircle(
        color = Color.Black.copy(alpha = 0.25f + params.easeLevel * 0.15f),
        radius = params.base * 0.2f,
        center = center)
  }
}

/** Ripple-style visualization (API 33+) drawn with Canvas (simplified placeholder for AGSL). */
@RequiresApi(33)
@Composable
private fun AgslRipple(level: Float, color: Color, size: Dp, modifier: Modifier = Modifier) {
  // Simplified version without shader for now
  var t by remember { mutableStateOf(0f) }
  var frameCount by remember { mutableStateOf(0L) }

  // Continuous animation targeting ~60fps
  LaunchedEffect(Unit) {
    val startTime = System.currentTimeMillis()
    while (isActive) {
      delay(16) // ~60fps
      t = ((System.currentTimeMillis() - startTime) / 1000f) // Time in seconds
      frameCount++ // Force recomposition each frame
    }
  }

  // Use frameCount to force recomposition each frame
  val r1 = remember { frameCount }

  Canvas(modifier.size(size)) {
    val canvasSize = this.size
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val minSize = min(canvasSize.width, canvasSize.height)

    val params = calculateRippleParameters(level, t, minSize)

    params.radii.forEachIndexed { index, ringRadius ->
      val alpha = params.alphas[index]
      drawCircle(color = color.copy(alpha = alpha), radius = ringRadius, center = center)
    }
  }
}

/** Fallback pulse visualization for devices below API 33. */
@Composable
private fun LegacyPulse(level: Float, color: Color, size: Dp, modifier: Modifier = Modifier) {
  val px = with(LocalDensity.current) { size.toPx() }

  Canvas(modifier.size(size)) {
    val canvasSize = this.size
    val c = androidx.compose.ui.geometry.Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val minSize = minOf(canvasSize.width, canvasSize.height)

    // More proportional reaction: smaller base, larger amplitude
    val baseRadius = minSize * 0.25f
    val maxRadius = minSize * 0.42f
    val r = baseRadius + (maxRadius - baseRadius) * level.coerceIn(0f, 1f)

    // Slight glow becomes more visible with voice
    drawCircle(color.copy(alpha = 0.12f + level * 0.2f), r * 1.8f, c)
    // Main circle
    drawCircle(color.copy(alpha = 0.8f + level * 0.15f), r, c)
    drawCircle(Color.Black.copy(alpha = 0.22f + level * 0.1f), r * 0.22f, c)
  }
}

@VisibleForTesting
internal data class BloomParameters(
    val breathing: Float,
    val easeLevel: Float,
    val base: Float,
    val pathPoints: List<Offset>
)

@VisibleForTesting
internal fun buildBloomPath(points: List<Offset>): Path {
  val path = Path()
  points.forEachIndexed { index, offset ->
    if (index == 0) {
      path.moveTo(offset.x, offset.y)
    } else {
      path.lineTo(offset.x, offset.y)
    }
  }
  if (points.isNotEmpty()) {
    path.close()
  }
  return path
}

@VisibleForTesting
internal fun calculateBloomParameters(
    level: Float,
    t: Float,
    petals: Int,
    minSize: Float
): BloomParameters {
  val breathing = sin(t * 2.0f) * 0.015f
  val clamped = level.coerceIn(0f, 1f)
  val oneMinusClamped = (1.0 - clamped.toDouble())
  val easeLevel =
      1.0f - (oneMinusClamped * oneMinusClamped * oneMinusClamped).toFloat() // pow(x, 3)
  val baseScale = 1.0f + easeLevel * 1.0f
  val base = minSize * 0.16f * baseScale
  val amp = 0.12f + 0.45f * sin(t * 2) * cos(2 * t) * 2
  val steps = petals * 50
  val phase = t * 1.5f
  val points =
      (0..steps).map { index ->
        val angle = (index.toFloat() / steps) * 2f * PI.toFloat()
        val edge = base * (1f + amp * sin(petals.toFloat() * angle + phase) / 5)
        val x = (minSize / 2f) + edge * cos(angle) * 1.2f
        val y = (minSize / 2f) + edge * sin(angle) * 1.2f
        Offset(x, y)
      }
  return BloomParameters(
      breathing = breathing, easeLevel = easeLevel, base = base, pathPoints = points)
}

@VisibleForTesting
internal data class RippleParameters(val radii: List<Float>, val alphas: List<Float>)

@VisibleForTesting
internal fun calculateRippleParameters(level: Float, t: Float, minSize: Float): RippleParameters {
  val baseRadius = minSize * 0.08f
  val maxRadius = minSize * 0.45f
  val radius = baseRadius + (maxRadius - baseRadius) * level.coerceIn(0f, 1f)
  val waveTime = t * 1.7f
  val baseAlpha = 0.35f + 0.3f * level.coerceIn(0f, 1f)

  val radii = mutableListOf<Float>()
  val alphas = mutableListOf<Float>()

  for (i in 0..3) {
    val ringRadius = radius + i * minSize * 0.06f
    val wave = 0.5f + 0.5f * sin(18f * (ringRadius / minSize) - waveTime)
    val alpha = (0.6f - i * 0.15f).coerceIn(0f, 1f)
    val finalAlpha = (alpha * (baseAlpha + 0.45f * wave)).coerceIn(0f, 1f)
    radii += ringRadius
    alphas += finalAlpha
  }

  return RippleParameters(radii = radii, alphas = alphas)
}
