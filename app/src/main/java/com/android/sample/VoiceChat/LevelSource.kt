package com.android.sample.VoiceChat

import kotlinx.coroutines.flow.Flow

/** Source de niveau audio normalisé [0f..1f]. */
interface LevelSource {
  val levels: Flow<Float>

  fun start()

  fun stop()
}

/**
 * Smoother (exponentiel) pour éviter les à-coups visuels. Attack plus rapide pour réagir rapidement
 * à la voix. Optimisé pour 60fps+ avec smoothing frame-by-frame.
 */
class LevelSmoother(
    private val attack: Float = 0.5f, // Plus rapide pour réactivité (était 0.4f)
    private val release: Float = 0.2f // Plus rapide pour suivre la voix (était 0.15f)
) {
  private var y = 0f

  fun step(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    val a = if (clamped > y) attack else release
    // Smoothing exponentiel optimisé pour haute fréquence
    y = y + a * (clamped - y)
    return y
  }

  fun reset() {
    y = 0f
  }
}
