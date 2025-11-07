package com.android.sample.VoiceChat

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceVisualizerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun voiceVisualizer_withBloomPreset_displays() {
    val testSource = TestLevelSource(flowOf(0.5f))
    composeTestRule.setContent {
      VoiceVisualizer(
          levelSource = testSource, preset = VisualPreset.Bloom, petals = 10, size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withRipplePreset_displays() {
    val testSource = TestLevelSource(flowOf(0.3f))
    composeTestRule.setContent {
      VoiceVisualizer(levelSource = testSource, preset = VisualPreset.Ripple, size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withDefaultPreset_displays() {
    val testSource = TestLevelSource(flowOf(0.4f))
    composeTestRule.setContent { VoiceVisualizer(levelSource = testSource, size = 100.dp) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withZeroLevel_displays() {
    val testSource = TestLevelSource(flowOf(0f))
    composeTestRule.setContent {
      VoiceVisualizer(levelSource = testSource, preset = VisualPreset.Bloom, size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withMaxLevel_displays() {
    val testSource = TestLevelSource(flowOf(1f))
    composeTestRule.setContent {
      VoiceVisualizer(levelSource = testSource, preset = VisualPreset.Bloom, size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withCustomColor_displays() {
    val testSource = TestLevelSource(flowOf(0.5f))
    composeTestRule.setContent {
      VoiceVisualizer(levelSource = testSource, color = Color.Blue, size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withCustomPetals_displays() {
    val testSource = TestLevelSource(flowOf(0.5f))
    composeTestRule.setContent {
      VoiceVisualizer(
          levelSource = testSource, preset = VisualPreset.Bloom, petals = 5, size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_withCustomSize_displays() {
    val testSource = TestLevelSource(flowOf(0.5f))
    composeTestRule.setContent { VoiceVisualizer(levelSource = testSource, size = 200.dp) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_updatesSmoothedLevel_onSamples() {
    val source = RecordingLevelSource(initial = 0f)
    val observed = mutableListOf<Float>()
    composeTestRule.mainClock.autoAdvance = false

    try {
      composeTestRule.setContent {
        VoiceVisualizer(levelSource = source, size = 160.dp, onLevelUpdate = { observed.add(it) })
      }
      composeTestRule.waitForIdle()

      repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.runOnIdle { source.push(1f) }
      repeat(25) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.runOnIdle { source.push(0f) }
      repeat(25) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.waitForIdle()
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }

    assertTrue("expected samples to be recorded", observed.isNotEmpty())
    assertTrue("expected peak level above 0.6", (observed.maxOrNull() ?: 0f) > 0.6f)
    assertTrue("expected trough level below 0.4", (observed.minOrNull() ?: 1f) < 0.4f)
  }

  @Test
  fun voiceVisualizer_lifecycle_startsAndStopsLevelSource() {
    val source = RecordingLevelSource()
    lateinit var showVisualizer: MutableState<Boolean>

    composeTestRule.setContent {
      showVisualizer = remember { mutableStateOf(true) }
      if (showVisualizer.value) {
        VoiceVisualizer(levelSource = source, size = 120.dp)
      }
    }
    composeTestRule.waitForIdle()

    assertEquals(1, source.startCount)
    assertEquals(0, source.stopCount)

    composeTestRule.runOnIdle { showVisualizer.value = false }
    composeTestRule.waitUntil(timeoutMillis = 2_000) { source.stopCount == 1 }

    assertEquals(1, source.startCount)
    assertEquals(1, source.stopCount)
  }

  @Test
  fun voiceVisualizer_replacingSource_stopsPreviousAndStartsNewOne() {
    val first = RecordingLevelSource()
    val second = RecordingLevelSource()
    lateinit var levelSourceState: MutableState<LevelSource>
    lateinit var showVisualizer: MutableState<Boolean>

    composeTestRule.setContent {
      showVisualizer = remember { mutableStateOf(true) }
      levelSourceState = remember { mutableStateOf<LevelSource>(first) }
      if (showVisualizer.value) {
        VoiceVisualizer(levelSource = levelSourceState.value, size = 140.dp)
      }
    }

    composeTestRule.waitForIdle()
    assertEquals(1, first.startCount)
    assertEquals(0, first.stopCount)

    composeTestRule.runOnIdle { levelSourceState.value = second }
    composeTestRule.waitUntil(timeoutMillis = 2_000) { first.stopCount == 1 }
    assertEquals(1, second.startCount)
    assertEquals(0, second.stopCount)

    composeTestRule.runOnIdle { showVisualizer.value = false }
    composeTestRule.waitUntil(timeoutMillis = 2_000) { second.stopCount == 1 }
  }

  @Test
  fun visualPreset_enumValues_areCorrect() {
    assertEquals(VisualPreset.Bloom, VisualPreset.valueOf("Bloom"))
    assertEquals(VisualPreset.Ripple, VisualPreset.valueOf("Ripple"))
  }

  @Test
  fun visualPreset_enumValues_count() {
    val values = VisualPreset.values()
    assertEquals(2, values.size)
  }

  @Test
  fun recordingLevelSource_clampsValuesAndTracksLifecycle() {
    val source = RecordingLevelSource(initial = 0.5f)

    source.start()
    source.push(2f)
    source.push(-1f)
    source.stop()

    assertEquals(1, source.startCount)
    assertEquals(1, source.stopCount)
    assertTrue("expected emissions recorded", source.emittedValues.isNotEmpty())
    assertTrue("values should be clamped", source.emittedValues.all { it in 0f..1f })
  }
}

// Helper class for testing
private class TestLevelSource(private val flow: Flow<Float>) : LevelSource {
  override val levels: Flow<Float> = flow

  override fun start() {}

  override fun stop() {}
}
