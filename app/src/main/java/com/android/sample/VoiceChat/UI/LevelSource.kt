package com.android.sample.VoiceChat.UI

import kotlinx.coroutines.flow.Flow

/** Source of normalized audio levels in the range [0f..1f]. */
interface LevelSource {
  /** Stream of normalized audio levels. */
  val levels: Flow<Float>

  /** Starts the audio level source. */
  fun start()

  /** Stops the audio level source and releases resources if any. */
  fun stop()
}

/**
 * Exponential smoother to avoid visual jitter. Uses a faster attack to react quickly to voice and a
 * slightly slower release to decay smoothly. Optimized for frame-by-frame updates (60fps+).
 */
class LevelSmoother(
    private val attack: Float = 0.5f, // Faster attack for reactivity (was 0.4f)
    private val release: Float = 0.2f // Faster release to follow voice (was 0.15f)
) {
  private var y = 0f

  /**
   * Advances the smoother towards input [x], clamping to [0f..1f]. Faster when increasing (attack)
   * and slower when decreasing (release). Returns the smoothed value.
   */
  fun step(x: Float): Float {
    val clamped = x.coerceIn(0f, 1f)
    val a = if (clamped > y) attack else release
    // Exponential smoothing optimized for high frequency updates
    y = y + a * (clamped - y)
    return y
  }

  /** Resets the smoother state back to 0. */
  fun reset() {
    y = 0f
  }
}
