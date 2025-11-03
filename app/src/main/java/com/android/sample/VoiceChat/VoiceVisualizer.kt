package com.android.sample.VoiceChat

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class VisualPreset {
  Bloom,
  Ripple
}

@Composable
fun VoiceVisualizer(
    levelSource: LevelSource,
    modifier: Modifier = Modifier,
    preset: VisualPreset = VisualPreset.Bloom,
    color: Color = Color(0xFFB61919),
    petals: Int = 10, // pour Bloom
    size: Dp = 360.dp
) {
  val smoother = remember { LevelSmoother() }
  var rawLevel by remember { mutableStateOf(0f) }
  var level by remember { mutableStateOf(0f) }

  // Collecter les niveaux bruts
  LaunchedEffect(levelSource) {
    levelSource.start()
    try {
      levelSource.levels.collect { rawLevel = it }
    } finally {
      levelSource.stop()
    }
  }

  // Smoothing continu avec animation frame pour fluidité maximale (~60fps)
  LaunchedEffect(Unit) {
    while (isActive) {
      delay(8) // ~60fps (16ms par frame)
      // Smoothing à chaque frame pour une animation ultra fluide
      level = smoother.step(rawLevel)
    }
  }

  if (Build.VERSION.SDK_INT >= 33) {
    when (preset) {
      VisualPreset.Bloom -> AgslBloom(level, color, petals, size, modifier)
      VisualPreset.Ripple -> AgslRipple(level, color, size, modifier)
    }
  } else {
    // fallback < 33
    LegacyPulse(level, color, size, modifier)
  }
}

@RequiresApi(33)
@Composable
private fun AgslBloom(
    level: Float,
    color: Color,
    petals: Int,
    size: Dp,
    modifier: Modifier = Modifier
) {
  // Version simplifiée sans shader pour l'instant - dessin Canvas direct
  var t by remember { mutableStateOf(0f) }
  var frameCount by remember { mutableStateOf(0L) }

  // Animation continue pour 60fps+ fluide
  LaunchedEffect(Unit) {
    val startTime = System.currentTimeMillis()
    while (isActive) {
      delay(10) // ~60fps (16ms par frame)
      t = ((System.currentTimeMillis() - startTime) / 1000f) // Temps en secondes
      frameCount++ // Force la recomposition à chaque frame pour animation fluide
    }
  }

  // Utiliser frameCount pour forcer la recomposition à chaque frame
  val r = remember { frameCount }

  Canvas(modifier.size(size)) {
    val canvasSize = this.size
    val center = androidx.compose.ui.geometry.Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val minSize = minOf(canvasSize.width, canvasSize.height)

    // Animation continue avec réaction plus proportionnelle
    val breathing = sin(t * 2.0f) * 0.015f
    val clamped = level.coerceIn(0f, 1f)
    val oneMinusClamped = (1.0 - clamped.toDouble())
    val easeLevel =
        1.0f - (oneMinusClamped * oneMinusClamped * oneMinusClamped).toFloat() // pow(x, 3)

    // La taille de base de TOUTE la forme du bloom grossit avec le niveau audio
    val baseScale = 1.0f + easeLevel * 1.0f // La forme peut grossir jusqu'à 2x sa taille de base
    val base = minSize * 0.16f * baseScale

    // Amplitude des pétales pour l'animation (reste constante pour garder la forme)
    val amp = 0.12f + 0.45f * sin(t * 2) * cos(2 * t) * 2

    // Cercle derriere le bloom
    drawCircle(
        color = color.copy(alpha = 0.01f + easeLevel * 0.05f),
        radius = base * (1f + breathing) * 0.9f * 10, // Grossit avec base
        center = center)

    // Petals avec path - réaction proportionnelle
    val path = Path()
    val steps = petals * 50
    val phase = t * 1.5f

    for (i in 0..steps) {
      val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
      val edge = base * (1f + amp * sin(petals.toFloat() * angle + phase) / 5)
      val x = center.x + edge * cos(angle) * 1.2f
      val y = center.y + edge * sin(angle) * 1.2f

      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    // Alpha plus visible proportionnellement au niveau
    drawPath(path, color.copy(alpha = 0.75f + easeLevel * 0.3f), style = Fill) // Augmenté

    // Core - reste proportionnel à la taille de base (grossit avec toute la forme)
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f + easeLevel * 0.15f),
        radius = base * 0.2f, // Proportionnel à base, donc grossit avec toute la forme
        center = center)
  }
}

@RequiresApi(33)
@Composable
private fun AgslRipple(level: Float, color: Color, size: Dp, modifier: Modifier = Modifier) {
  // Version simplifiée sans shader pour l'instant
  var t by remember { mutableStateOf(0f) }
  var frameCount by remember { mutableStateOf(0L) }

  // Animation continue pour 60fps+ fluide
  LaunchedEffect(Unit) {
    val startTime = System.currentTimeMillis()
    while (isActive) {
      delay(16) // ~60fps (16ms par frame)
      t = ((System.currentTimeMillis() - startTime) / 1000f) // Temps en secondes
      frameCount++ // Force la recomposition à chaque frame
    }
  }

  // Utiliser frameCount pour forcer la recomposition à chaque frame
  val r1 = remember { frameCount }

  Canvas(modifier.size(size)) {
    val canvasSize = this.size
    val center = androidx.compose.ui.geometry.Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val minSize = minOf(canvasSize.width, canvasSize.height)

    // Réaction plus proportionnelle : base plus petite, amplitude plus grande
    val baseRadius = minSize * 0.08f
    val maxRadius = minSize * 0.45f // Augmenté de 0.43f (0.08 + 0.35)
    val radius = baseRadius + (maxRadius - baseRadius) * level.coerceIn(0f, 1f)

    val waveTime = t * 1.7f

    // Ripple rings - plus visibles avec la voix
    for (i in 0..3) {
      val ringRadius = radius + i * minSize * 0.06f // Légèrement augmenté
      val wave = 0.5f + 0.5f * sin(18f * (ringRadius / minSize) - waveTime)
      val alpha = (0.6f - i * 0.15f).coerceIn(0f, 1f)

      // Alpha plus visible quand il y a de la voix
      val baseAlpha = 0.35f + 0.3f * level.coerceIn(0f, 1f) // Augmenté pour plus de visibilité
      val finalAlpha = alpha * (baseAlpha + 0.45f * wave)

      drawCircle(
          color = color.copy(alpha = finalAlpha.coerceIn(0f, 1f)),
          radius = ringRadius,
          center = center)
    }
  }
}

@Composable
private fun LegacyPulse(level: Float, color: Color, size: Dp, modifier: Modifier = Modifier) {
  val px = with(LocalDensity.current) { size.toPx() }

  Canvas(modifier.size(size)) {
    val canvasSize = this.size
    val c = androidx.compose.ui.geometry.Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val minSize = minOf(canvasSize.width, canvasSize.height)

    // Réaction plus proportionnelle : base plus petite, amplitude plus grande
    val baseRadius = minSize * 0.25f
    val maxRadius = minSize * 0.42f
    val r = baseRadius + (maxRadius - baseRadius) * level.coerceIn(0f, 1f)

    // Glow plus visible avec la voix
    drawCircle(color.copy(alpha = 0.12f + level * 0.2f), r * 1.8f, c)
    // Cercle principal plus visible
    drawCircle(color.copy(alpha = 0.8f + level * 0.15f), r, c)
    drawCircle(Color.Black.copy(alpha = 0.22f + level * 0.1f), r * 0.22f, c)
  }
}
