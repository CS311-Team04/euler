package com.android.sample.VoiceChat

import org.junit.Assert.*
import org.junit.Test

class VoiceScreenLogicTest {

  @Test
  fun updateLastVoiceTimestamp_updatesWhenLevelAboveThreshold() {
    val previous = 100L
    val now = 200L

    val updated =
        updateLastVoiceTimestamp(
            level = 0.2f, silenceThreshold = 0.1f, currentTime = now, lastVoiceTime = previous)
    assertEquals(now, updated)

    val unchanged =
        updateLastVoiceTimestamp(
            level = 0.05f, silenceThreshold = 0.1f, currentTime = now, lastVoiceTime = previous)
    assertEquals(previous, unchanged)
  }

  @Test
  fun shouldDeactivateMic_trueOnlyWhenSilenceExceedsThreshold() {
    val now = 5_000L
    val lastVoice = 2_000L
    val silenceDuration = 2_500L

    assertTrue(
        shouldDeactivateMic(
            currentTime = now,
            lastVoiceTime = lastVoice,
            currentLevel = 0.01f,
            silenceThreshold = 0.05f,
            silenceDuration = silenceDuration))

    assertFalse(
        shouldDeactivateMic(
            currentTime = now,
            lastVoiceTime = lastVoice,
            currentLevel = 0.2f,
            silenceThreshold = 0.05f,
            silenceDuration = silenceDuration))

    assertFalse(
        shouldDeactivateMic(
            currentTime = 4_000L,
            lastVoiceTime = lastVoice,
            currentLevel = 0.01f,
            silenceThreshold = 0.05f,
            silenceDuration = silenceDuration))
  }

  @Test
  fun resetLevelAfterError_returnsZero() {
    assertEquals(0f, resetLevelAfterError(), 0f)
  }
}
