package com.android.sample.VoiceChat

import com.android.sample.VoiceChat.UI.LevelSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Test double for [com.android.sample.VoiceChat.UI.LevelSource] that records lifecycle events and
 * allows emitting arbitrary levels.
 */
class RecordingLevelSource(initial: Float = 0f) : LevelSource {
  private val levelsFlow = MutableSharedFlow<Float>(replay = 1)
  override val levels = levelsFlow.asSharedFlow()

  var startCount = 0
    private set

  var stopCount = 0
    private set

  private val _emittedValues = mutableListOf<Float>()
  val emittedValues: List<Float>
    get() = _emittedValues

  init {
    levelsFlow.tryEmit(initial)
    _emittedValues += initial.coerceIn(0f, 1f)
  }

  fun push(value: Float) {
    val clamped = value.coerceIn(0f, 1f)
    levelsFlow.tryEmit(clamped)
    _emittedValues += clamped
  }

  override fun start() {
    startCount++
  }

  override fun stop() {
    stopCount++
  }
}
