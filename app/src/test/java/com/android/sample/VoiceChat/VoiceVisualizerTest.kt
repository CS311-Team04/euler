package com.android.sample.VoiceChat

import android.graphics.PathMeasure
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.android.sample.VoiceChat.UI.LevelSource
import com.android.sample.VoiceChat.UI.VisualPreset
import com.android.sample.VoiceChat.UI.VoiceVisualizer
import com.android.sample.VoiceChat.UI.buildBloomPath
import com.android.sample.VoiceChat.UI.calculateBloomParameters
import com.android.sample.VoiceChat.UI.calculateCanvasMetrics
import com.android.sample.VoiceChat.UI.calculateLegacyPulseMetrics
import com.android.sample.VoiceChat.UI.calculateRippleParameters
import com.android.sample.VoiceChat.UI.createBloomDrawInstructions
import com.android.sample.VoiceChat.UI.createRippleCircles
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
  fun voiceVisualizer_withDefaultPreset_displays() {
    val testSource = TestLevelSource(flowOf(0.4f))
    composeTestRule.setContent {
      VoiceVisualizer(
          levelSource = testSource,
          preset = VisualPreset.Bloom,
          color = Color(0xFFB61919),
          petals = 10,
          size = 100.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_pre33_ripplePresetFallsBack() {
    val testSource = TestLevelSource(flowOf(0.2f))
    composeTestRule.setContent {
      VoiceVisualizer(
          levelSource = testSource,
          preset = VisualPreset.Ripple,
          color = Color(0xFFB61919),
          petals = 10,
          size = 96.dp)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceVisualizer_updatesSmoothedLevel_onSamples() {
    val source = RecordingLevelSource(initial = 0f)
    val observed = mutableListOf<Float>()
    composeTestRule.mainClock.autoAdvance = false

    try {
      composeTestRule.setContent {
        VoiceVisualizer(
            levelSource = source,
            preset = VisualPreset.Bloom,
            color = Color(0xFFB61919),
            petals = 10,
            size = 160.dp,
            onLevelUpdate = { observed.add(it) })
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
  fun voiceVisualizer_emitsUpdatesOnEachFrame() {
    val source = RecordingLevelSource(initial = 0.2f)
    val observed = mutableListOf<Float>()
    composeTestRule.mainClock.autoAdvance = false

    try {
      composeTestRule.setContent {
        VoiceVisualizer(
            levelSource = source,
            preset = VisualPreset.Bloom,
            color = Color(0xFFB61919),
            petals = 10,
            size = 120.dp,
            onLevelUpdate = { observed.add(it) })
      }
      composeTestRule.waitForIdle()

      repeat(40) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.waitForIdle()
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }

    assertTrue("expected multiple updates", observed.size >= 20)
    assertTrue("values stay within bounds", observed.all { it in 0f..1f })
  }

  @Test
  @Config(sdk = [33])
  fun voiceVisualizer_ripple_reactsToLevels_onApi33() {
    val source = RecordingLevelSource(initial = 0f)
    val observed = mutableListOf<Float>()
    composeTestRule.mainClock.autoAdvance = false

    try {
      composeTestRule.setContent {
        VoiceVisualizer(
            levelSource = source,
            preset = VisualPreset.Ripple,
            petals = 10,
            color = Color.Magenta,
            size = 180.dp,
            onLevelUpdate = { observed.add(it) })
      }
      composeTestRule.waitForIdle()

      repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.runOnIdle { source.push(0.85f) }
      repeat(25) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.runOnIdle { source.push(0.1f) }
      repeat(25) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.waitForIdle()
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }

    assertTrue("expected ripple to react to level input", observed.any { it > 0.5f })
  }

  @Test
  @Config(sdk = [33])
  fun voiceVisualizer_bloom_reactsToLevels_onApi33() {
    val source = RecordingLevelSource(initial = 0f)
    val observed = mutableListOf<Float>()
    composeTestRule.mainClock.autoAdvance = false

    try {
      composeTestRule.setContent {
        VoiceVisualizer(
            levelSource = source,
            preset = VisualPreset.Bloom,
            color = Color.Green,
            petals = 6,
            size = 200.dp,
            onLevelUpdate = { observed.add(it) })
      }
      composeTestRule.waitForIdle()

      repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.runOnIdle { source.push(0.9f) }
      repeat(25) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.runOnIdle { source.push(0.2f) }
      repeat(25) { composeTestRule.mainClock.advanceTimeByFrame() }

      composeTestRule.waitForIdle()
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }

    assertTrue("expected bloom to react to level input", observed.any { it > 0.55f })
    assertTrue("expected bloom to decay", observed.any { it < 0.35f })
  }

  @Test
  fun voiceVisualizer_lifecycle_startsAndStopsLevelSource() {
    val source = RecordingLevelSource()
    lateinit var showVisualizer: MutableState<Boolean>

    composeTestRule.setContent {
      showVisualizer = remember { mutableStateOf(true) }
      if (showVisualizer.value) {
        VoiceVisualizer(
            levelSource = source,
            preset = VisualPreset.Bloom,
            color = Color(0xFFB61919),
            petals = 10,
            size = 120.dp)
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
        VoiceVisualizer(
            levelSource = levelSourceState.value,
            preset = VisualPreset.Bloom,
            color = Color(0xFFB61919),
            petals = 10,
            size = 140.dp)
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

  @Test
  fun bloomParameters_result_scalesWithLevel() {
    val low = calculateBloomParameters(level = 0.2f, t = 0.1f, petals = 4, minSize = 200f)
    val high = calculateBloomParameters(level = 0.8f, t = 0.1f, petals = 4, minSize = 200f)

    assertTrue(high.base > low.base)
    assertEquals(low.pathPoints.size, high.pathPoints.size)
    assertTrue(low.pathPoints.all { it.x.isFinite() && it.y.isFinite() })
    assertNotEquals(Offset.Zero, low.pathPoints.first())
    assertEquals(low.pathPoints.first().x, low.pathPoints.last().x, 1e-2f)
    assertEquals(low.pathPoints.first().y, low.pathPoints.last().y, 1e-2f)
  }

  @Test
  fun rippleParameters_produces_expected_rings() {
    val paramsLow = calculateRippleParameters(0.1f, t = 0.2f, minSize = 250f)
    val paramsHigh = calculateRippleParameters(0.9f, t = 0.2f, minSize = 250f)

    assertEquals(4, paramsLow.radii.size)
    assertEquals(4, paramsLow.alphas.size)
    assertTrue(paramsLow.alphas.all { it in 0f..1f })
    assertTrue(paramsHigh.alphas.all { it in 0f..1f })
    assertTrue(paramsHigh.radii.first() > paramsLow.radii.first())
    assertTrue(paramsHigh.radii.zipWithNext().all { (a, b) -> b >= a })
    assertTrue(paramsLow.alphas.zipWithNext().all { (a, b) -> a >= b })
  }

  @Test
  fun bloomParameters_respectPetalCount() {
    val params = calculateBloomParameters(level = 0.6f, t = 0.3f, petals = 6, minSize = 160f)
    assertTrue(params.pathPoints.size >= 6 * 40)
  }

  @Test
  fun buildBloomPath_closesShape() {
    val params = calculateBloomParameters(level = 0.5f, t = 0.2f, petals = 5, minSize = 220f)
    val path = buildBloomPath(params.pathPoints)
    val measure = PathMeasure(path.asAndroidPath(), false)
    assertTrue(measure.length > 0)
    val forcedClosed = PathMeasure(path.asAndroidPath(), true)
    assertEquals(forcedClosed.length, measure.length, 1e-3f)
  }

  @Test
  fun createRippleCircles_pairsRadiusAndAlpha() {
    val params = calculateRippleParameters(level = 0.6f, t = 0.4f, minSize = 210f)
    val circles = createRippleCircles(params)
    assertEquals(params.radii.size, circles.size)
    circles.forEachIndexed { index, circle ->
      assertEquals(params.radii[index], circle.radius, 1e-4f)
      assertEquals(params.alphas[index], circle.alpha, 1e-4f)
    }
  }

  @Test
  fun legacyPulseMetrics_scalesWithLevel() {
    val low = calculateLegacyPulseMetrics(level = 0.1f, minSize = 300f)
    val high = calculateLegacyPulseMetrics(level = 0.9f, minSize = 300f)
    assertEquals(low.baseRadius, high.baseRadius, 1e-4f)
    assertEquals(low.maxRadius, high.maxRadius, 1e-4f)
    assertTrue("radius should increase with level", high.radius > low.radius)
    assertTrue("radius stays within bounds", high.radius in low.baseRadius..high.maxRadius)
  }

  @Test
  fun calculateCanvasMetrics_returnsCenterAndMinSize() {
    val size = androidx.compose.ui.geometry.Size(width = 200f, height = 120f)
    val metrics = calculateCanvasMetrics(size)
    assertEquals(size, metrics.size)
    assertEquals(100f, metrics.center.x, 1e-4f)
    assertEquals(60f, metrics.center.y, 1e-4f)
    assertEquals(120f, metrics.minSize, 1e-4f)
  }

  @Test
  fun createBloomDrawInstructions_scalesWithParameters() {
    val params = calculateBloomParameters(level = 0.7f, t = 0.5f, petals = 4, minSize = 180f)
    val instructions = createBloomDrawInstructions(params)
    assertTrue(instructions.outerCircleAlpha > 0f)
    assertTrue(instructions.pathAlpha > 0f)
    assertTrue(instructions.innerCircleAlpha > 0f)
    assertTrue(instructions.outerCircleRadius > instructions.innerCircleRadius)
    assertEquals(params.base * 0.25f, instructions.innerCircleRadius, 1e-4f)
    val measure = PathMeasure(instructions.path.asAndroidPath(), false)
    assertTrue(measure.length > 0)
    assertNotEquals(Offset.Zero, instructions.pathCenter)
  }

  @Test
  fun rippleParameters_waveInfluenceAlpha() {
    val t1 = calculateRippleParameters(0.5f, t = 0.0f, minSize = 200f)
    val t2 = calculateRippleParameters(0.5f, t = 0.5f, minSize = 200f)
    assertNotEquals(t1.alphas, t2.alphas)
    assertTrue(t1.alphas.zip(t2.alphas).any { (a, b) -> a != b })
  }

  @Test
  fun rippleParameters_clampsValues() {
    val params = calculateRippleParameters(level = 1.5f, t = 3.2f, minSize = 220f)
    val expectedFirstRadius = 220f * 0.45f
    assertEquals(expectedFirstRadius, params.radii.first(), 1e-4f)
    assertTrue(params.radii.zipWithNext().all { (a, b) -> b > a })
    assertTrue(params.alphas.all { it in 0f..1f })
    assertTrue(params.alphas.last() >= 0f)
  }

  @Test
  fun legacyPulseMetrics_clampsNegativeLevel() {
    val metrics = calculateLegacyPulseMetrics(level = -0.5f, minSize = 360f)
    assertEquals(metrics.baseRadius, metrics.radius, 1e-4f)
  }

  @Test
  fun bloomParameters_bounds() {
    val params = calculateBloomParameters(level = 1.4f, t = 1.1f, petals = 6, minSize = 210f)
    assertTrue(params.easeLevel in 0f..1f)
    assertEquals(6 * 50 + 1, params.pathPoints.size)
    assertTrue(params.pathPoints.all { it.x.isFinite() && it.y.isFinite() })
  }
}

// Helper class for testing
private class TestLevelSource(private val flow: Flow<Float>) : LevelSource {
  override val levels: Flow<Float> = flow

  override fun start() {}

  override fun stop() {}
}
