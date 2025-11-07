package com.android.sample.VoiceChat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Test double for [LevelSource] that records lifecycle events and allows emitting arbitrary levels.
 */
class RecordingLevelSource(initial: Float = 0f) : LevelSource {
  private val levelsFlow = MutableSharedFlow<Float>(replay = 1)
  override val levels = levelsFlow.asSharedFlow()

  var startCount = 0
    private set

  var stopCount = 0
    private set

  init {
    levelsFlow.tryEmit(initial)
  }

  fun push(value: Float) {
    levelsFlow.tryEmit(value.coerceIn(0f, 1f))
  }

  override fun start() {
    startCount++
  }

  override fun stop() {
    stopCount++
  }
}
