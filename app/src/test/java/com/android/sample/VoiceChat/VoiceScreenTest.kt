package com.android.sample.VoiceChat

import android.Manifest
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun voiceScreen_displays() {
    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_withModifier_displays() {
    composeTestRule.setContent { VoiceScreen(onClose = {}, modifier = Modifier.size(100.dp)) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_rendersWithoutCrash() {
    // Test that the screen can be composed without crashing
    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    // If we get here without exception, the test passes
    assertTrue(true)
  }

  @Test
  fun voiceOverlay_displays() {
    composeTestRule.setContent { VoiceOverlay(onDismiss = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceOverlay_withModifier_displays() {
    composeTestRule.setContent { VoiceOverlay(onDismiss = {}, modifier = Modifier.size(100.dp)) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreenPreviewContent_renders() {
    composeTestRule.setContent { VoiceScreenPreviewContent(level = 0.4f) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_toggleControlsInjectedMic() {
    val fakeSource = FakeMicLevelSource()
    composeTestRule.setContent {
      VoiceScreen(
          onClose = {},
          levelSourceFactory = { fakeSource },
          initialHasMicOverride = true,
          silenceThresholdOverride = 0.2f,
          silenceDurationOverride = 100L)
    }

    composeTestRule.onNodeWithContentDescription("Toggle microphone").performClick()

    composeTestRule.runOnIdle {
      fakeSource.push(0.7f)
      fakeSource.push(0.0f)
    }

    composeTestRule.waitUntilTrue(timeoutMillis = 2_000) { fakeSource.stopCount.get() > 0 }

    composeTestRule.runOnIdle {
      assertEquals(1, fakeSource.startCount.get())
      assertTrue(fakeSource.stopCount.get() >= 1)
    }
  }

  @Test
  fun voiceScreen_requestsPermissionWhenNotGranted() {
    val requested = mutableListOf<String>()
    composeTestRule.setContent {
      VoiceScreen(
          onClose = {},
          levelSourceFactory = { FakeMicLevelSource() },
          initialHasMicOverride = false,
          permissionRequester = { requested += it })
    }

    composeTestRule.waitForIdle()
    assertEquals(listOf(Manifest.permission.RECORD_AUDIO), requested)
  }

  @Test
  fun voiceScreen_toggleWithThrowingMic_revertsState() {
    val throwingSource = ThrowingMicLevelSource()
    composeTestRule.setContent {
      VoiceScreen(
          onClose = {}, levelSourceFactory = { throwingSource }, initialHasMicOverride = true)
    }

    composeTestRule.onNodeWithContentDescription("Toggle microphone").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle { assertTrue(throwingSource.startAttempts.get() >= 1) }
    composeTestRule.onNodeWithContentDescription("Toggle microphone").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle {
      assertTrue(throwingSource.startAttempts.get() >= 2)
      assertTrue(throwingSource.stopCount.get() >= 1)
    }
  }

  @Test
  fun voiceScreen_disposeStopsMic() {
    val fakeSource = FakeMicLevelSource()
    val showScreen = mutableStateOf(true)

    composeTestRule.setContent {
      if (showScreen.value) {
        VoiceScreen(
            onClose = {},
            levelSourceFactory = { fakeSource },
            initialHasMicOverride = true,
            silenceDurationOverride = Long.MAX_VALUE)
      }
    }

    composeTestRule.onNodeWithContentDescription("Toggle microphone").performClick()
    composeTestRule.runOnIdle { fakeSource.push(0.6f) }
    composeTestRule.runOnIdle { showScreen.value = false }
    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { assertTrue(fakeSource.stopCount.get() >= 1) }
  }

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

  @Test
  fun evaluateAudioLevel_logsWithoutVoiceDetection() {
    val previousTime = 4_000L
    val result =
        evaluateAudioLevel(
            level = 0.15f,
            silenceThreshold = 0.2f,
            frameCount = 60,
            currentTime = 8_000L,
            lastVoiceTime = previousTime)

    assertTrue(result.shouldLogLevel)
    assertFalse(result.voiceDetected)
    assertEquals(previousTime, result.updatedLastVoiceTime)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun startMicrophoneSafely_success() = runTest {
    val events = mutableListOf<String>()
    var started = false
    val result =
        startMicrophoneSafely(
            startAction = { started = true },
            delayMs = { _ -> events += "delay" },
            currentTime = { 1_234L },
            log = { events += it },
            errorLog = { message, _ -> events += message },
            delayDurationMs = 0L)

    assertTrue(result.success)
    assertEquals(1_234L, result.lastVoiceTime)
    assertTrue(started)
    assertEquals(
        listOf("Starting microphone...", "delay", "Microphone started successfully"), events)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun startMicrophoneSafely_handlesException() = runTest {
    val events = mutableListOf<String>()
    val result =
        startMicrophoneSafely(
            startAction = { throw IllegalStateException("boom") },
            delayMs = { _ -> events += "delay" },
            currentTime = { 0L },
            log = { events += it },
            errorLog = { message, _ -> events += message },
            delayDurationMs = 0L)

    assertFalse(result.success)
    assertNull(result.lastVoiceTime)
    assertEquals(listOf("Starting microphone...", "Microphone start error"), events)
  }

  @Test
  fun stopMicrophoneSafely_success() {
    val events = mutableListOf<String>()
    var stopped = false
    stopMicrophoneSafely(
        stopAction = { stopped = true },
        log = { events += it },
        errorLog = { message, _ -> events += message })

    assertTrue(stopped)
    assertEquals(listOf("Stopping microphone...", "Microphone stopped"), events)
  }

  @Test
  fun stopMicrophoneSafely_handlesException() {
    val events = mutableListOf<String>()
    stopMicrophoneSafely(
        stopAction = { throw IllegalArgumentException("boom") },
        log = { events += it },
        errorLog = { message, _ -> events += message })

    assertEquals(listOf("Stopping microphone...", "Microphone stop error"), events)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun monitorSilence_returnsImmediatelyWhenMicInactive() = runTest {
    var loggerCalled = false
    monitorSilence(
        isMicActiveProvider = { false },
        hasMicProvider = { true },
        delayMs = { fail("Should not delay when mic is inactive") },
        timeProvider = { 0L },
        lastVoiceTimeProvider = { 0L },
        currentLevelProvider = { 0f },
        silenceThreshold = 0.05f,
        silenceDuration = 2_500L,
        onDeactivate = { fail("Should not deactivate when inactive") },
        logger = { loggerCalled = true })

    assertFalse(loggerCalled)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun monitorSilence_returnsImmediatelyWhenMicUnavailable() = runTest {
    var loggerCalled = false
    monitorSilence(
        isMicActiveProvider = { true },
        hasMicProvider = { false },
        delayMs = { fail("Should not delay when mic unavailable") },
        timeProvider = { 0L },
        lastVoiceTimeProvider = { 0L },
        currentLevelProvider = { 0f },
        silenceThreshold = 0.05f,
        silenceDuration = 2_500L,
        onDeactivate = { fail("Should not deactivate when mic unavailable") },
        logger = { loggerCalled = true })

    assertFalse(loggerCalled)
  }
}

private class FakeMicLevelSource : LevelSource {
  private val flow = MutableSharedFlow<Float>(replay = 1)
  val startCount = AtomicInteger(0)
  val stopCount = AtomicInteger(0)

  init {
    flow.tryEmit(0f)
  }

  override val levels = flow

  override fun start() {
    startCount.incrementAndGet()
  }

  override fun stop() {
    stopCount.incrementAndGet()
  }

  fun push(value: Float) {
    flow.tryEmit(value)
  }
}

private class ThrowingMicLevelSource : LevelSource {
  val startAttempts = AtomicInteger(0)
  val stopCount = AtomicInteger(0)

  override val levels = MutableSharedFlow<Float>(replay = 1)

  override fun start() {
    startAttempts.incrementAndGet()
    throw IllegalStateException("boom")
  }

  override fun stop() {
    stopCount.incrementAndGet()
  }
}

private fun ComposeTestRule.waitUntilTrue(timeoutMillis: Long, condition: () -> Boolean) {
  val deadline = System.currentTimeMillis() + timeoutMillis
  while (!condition()) {
    if (System.currentTimeMillis() > deadline) {
      fail("Condition not met within $timeoutMillis ms")
    }
    this.waitForIdle()
  }
}
