package com.android.sample.VoiceChat

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
  fun visualPreset_enumValues_areCorrect() {
    assertEquals(VisualPreset.Bloom, VisualPreset.valueOf("Bloom"))
    assertEquals(VisualPreset.Ripple, VisualPreset.valueOf("Ripple"))
  }

  @Test
  fun visualPreset_enumValues_count() {
    val values = VisualPreset.values()
    assertEquals(2, values.size)
  }
}

// Helper class for testing
private class TestLevelSource(private val flow: Flow<Float>) : LevelSource {
  override val levels: Flow<Float> = flow

  override fun start() {}

  override fun stop() {}
}
