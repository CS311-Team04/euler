package com.android.sample.VoiceChat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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

  @Test
  fun handlePermissionResult_logsWarningWhenDenied() {
    val debugs = mutableListOf<String>()
    val warns = mutableListOf<String>()

    val result = handlePermissionResult(false, { debugs += it }, { warns += it })

    assertFalse(result)
    assertEquals(listOf("Permission RECORD_AUDIO: false"), debugs)
    assertEquals(listOf("Microphone permission was denied by user"), warns)
  }

  @Test
  fun handlePermissionResult_returnsTrueWhenGranted() {
    val debugs = mutableListOf<String>()
    val warns = mutableListOf<String>()

    val result = handlePermissionResult(true, { debugs += it }, { warns += it })

    assertTrue(result)
    assertEquals(listOf("Permission RECORD_AUDIO: true"), debugs)
    assertTrue(warns.isEmpty())
  }

  @Test
  fun logInitialPermissionState_emitsCorrectMessage() {
    val messages = mutableListOf<String>()
    val shouldRequest =
        logInitialPermissionState(alreadyGranted = false) { message -> messages += message }
    assertTrue(shouldRequest)
    assertEquals(listOf("Requesting RECORD_AUDIO permission..."), messages)

    messages.clear()
    val shouldRequestWhenGranted =
        logInitialPermissionState(alreadyGranted = true) { message -> messages += message }
    assertFalse(shouldRequestWhenGranted)
    assertEquals(listOf("RECORD_AUDIO permission already granted"), messages)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun monitorSilence_deactivatesAfterTimeout() = runTest {
    var active = true
    val logs = mutableListOf<String>()

    monitorSilence(
        isMicActiveProvider = { active },
        hasMicProvider = { true },
        delayMs = { /* no-op for tests */},
        timeProvider = { 5_500L },
        lastVoiceTimeProvider = { 2_000L },
        currentLevelProvider = { 0.01f },
        silenceThreshold = 0.05f,
        silenceDuration = 2_500L,
        onDeactivate = { active = false },
        logger = { logs += it })

    assertFalse(active)
    assertEquals(listOf("Silence detected (3500ms), auto deactivating mic"), logs)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun monitorSilence_noActionWhenInactive() = runTest {
    var active = false

    monitorSilence(
        isMicActiveProvider = { active },
        hasMicProvider = { true },
        delayMs = { /* no-op */},
        timeProvider = { 0L },
        lastVoiceTimeProvider = { 0L },
        currentLevelProvider = { 0f },
        silenceThreshold = 0.05f,
        silenceDuration = 2_500L,
        onDeactivate = { active = true })

    assertFalse(active)
  }

  @Test
  fun evaluateAudioLevel_detectsVoiceAndLogs() {
    val previousTime = 1_000L
    val result =
        evaluateAudioLevel(
            level = 0.6f,
            silenceThreshold = 0.2f,
            frameCount = 30,
            currentTime = 5_000L,
            lastVoiceTime = previousTime)

    assertTrue(result.shouldLogLevel)
    assertTrue(result.voiceDetected)
    assertEquals(5_000L, result.updatedLastVoiceTime)
  }

  @Test
  fun evaluateAudioLevel_silenceDoesNotUpdateTimestamp() {
    val previousTime = 2_000L
    val result =
        evaluateAudioLevel(
            level = 0.01f,
            silenceThreshold = 0.2f,
            frameCount = 7,
            currentTime = 6_000L,
            lastVoiceTime = previousTime)

    assertFalse(result.shouldLogLevel)
    assertFalse(result.voiceDetected)
    assertEquals(previousTime, result.updatedLastVoiceTime)
  }
}
