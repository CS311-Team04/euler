package com.android.sample.VoiceChat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceVisualizerApi33Test {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun voiceVisualizer_api33_bloom_displays() {
    val source = Api33TestLevelSource(flowOf(0.6f))
    composeRule.setContent {
      VoiceVisualizer(
          levelSource = source,
          preset = VisualPreset.Bloom,
          color = Color(0xFFB61919),
          petals = 8,
          size = 120.dp)
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_api33_ripple_displays() {
    val source = Api33TestLevelSource(flowOf(0.4f))
    composeRule.setContent {
      VoiceVisualizer(
          levelSource = source, preset = VisualPreset.Ripple, color = Color.Cyan, size = 160.dp)
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_api33_ripple_reactsToLevels() {
    val source = RecordingLevelSource(initial = 0f)
    val observed = mutableListOf<Float>()
    composeRule.mainClock.autoAdvance = false

    try {
      composeRule.setContent {
        VoiceVisualizer(
            levelSource = source,
            preset = VisualPreset.Ripple,
            color = Color.Magenta,
            size = 180.dp,
            onLevelUpdate = { observed.add(it) })
      }
      composeRule.waitForIdle()

      repeat(5) { composeRule.mainClock.advanceTimeByFrame() }

      composeRule.runOnIdle { source.push(0.85f) }
      repeat(25) { composeRule.mainClock.advanceTimeByFrame() }

      composeRule.runOnIdle { source.push(0.1f) }
      repeat(25) { composeRule.mainClock.advanceTimeByFrame() }

      composeRule.waitForIdle()
    } finally {
      composeRule.mainClock.autoAdvance = true
    }

    assertTrue("expected ripple to react to level input", observed.any { it > 0.5f })
  }
}

private class Api33TestLevelSource(private val flow: Flow<Float>) : LevelSource {
  override val levels: Flow<Float> = flow

  override fun start() {}

  override fun stop() {}
}
