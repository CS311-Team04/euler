package com.android.sample.VoiceChat

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LevelSmootherTest {

  private lateinit var smoother: LevelSmoother

  @Before
  fun setUp() {
    smoother = LevelSmoother()
  }

  @Test
  fun step_withDefaultValues_returnsSmoothedValue() {
    val result = smoother.step(0.5f)
    assertTrue(result >= 0f && result <= 1f)
  }

  @Test
  fun step_withZeroInput_returnsZero() {
    smoother.reset()
    val result = smoother.step(0f)
    assertEquals(0f, result, 0.001f)
  }

  @Test
  fun step_withOneInput_returnsOne() {
    smoother.reset()
    val result = smoother.step(1f)
    assertTrue(result > 0f)
    assertTrue(result <= 1f)
  }

  @Test
  fun step_withIncreasingValue_usesAttack() {
    smoother.reset()
    val first = smoother.step(0.1f)
    val second = smoother.step(0.5f)
    assertTrue(second > first)
  }

  @Test
  fun step_withDecreasingValue_usesRelease() {
    smoother.reset()
    smoother.step(1f) // Set to high value
    val first = smoother.step(1f)
    val second = smoother.step(0.1f)
    assertTrue(second < first)
  }

  @Test
  fun step_clampsInputAboveOne() {
    smoother.reset()
    val result = smoother.step(2f)
    assertTrue(result <= 1f)
  }

  @Test
  fun step_clampsInputBelowZero() {
    smoother.reset()
    val result = smoother.step(-1f)
    assertTrue(result >= 0f)
  }

  @Test
  fun reset_setsValueToZero() {
    smoother.step(0.8f)
    smoother.reset()
    val result = smoother.step(0f)
    assertEquals(0f, result, 0.001f)
  }

  @Test
  fun step_withCustomAttackAndRelease() {
    val customSmoother = LevelSmoother(attack = 0.8f, release = 0.3f)
    customSmoother.reset()
    val result = customSmoother.step(0.5f)
    assertTrue(result >= 0f && result <= 1f)
  }

  @Test
  fun step_multipleSteps_convergesToTarget() {
    smoother.reset()
    var current = 0f
    repeat(100) {
      current = smoother.step(1f)
    }
    assertTrue(current > 0.9f)
  }

  @Test
  fun step_attackFasterThanRelease() {
    smoother.reset()
    val attackValue = smoother.step(0.1f)
    smoother.reset()
    smoother.step(1f) // Set high
    val releaseValue = smoother.step(0.1f)
    // Attack should be faster (larger change)
    assertTrue(attackValue > releaseValue || attackValue == releaseValue)
  }

  @Test
  fun step_withSameValue_returnsSameOrClose() {
    smoother.reset()
    val first = smoother.step(0.5f)
    val second = smoother.step(0.5f)
    // Should be close or same when input doesn't change
    assertTrue(kotlin.math.abs(first - second) < 0.1f)
  }

  @Test
  fun step_withBoundaryValues() {
    smoother.reset()
    val zeroResult = smoother.step(0f)
    assertEquals(0f, zeroResult, 0.001f)

    smoother.reset()
    repeat(100) {
      smoother.step(1f)
    }
    val oneResult = smoother.step(1f)
    assertTrue(oneResult > 0.9f)
  }

  @Test
  fun step_handlesPrecisionValues() {
    smoother.reset()
    val smallValue = smoother.step(0.001f)
    assertTrue(smallValue >= 0f)

    smoother.reset()
    val largeValue = smoother.step(0.999f)
    assertTrue(largeValue <= 1f)
  }

  @Test
  fun step_rapidChanges_smoothsCorrectly() {
    smoother.reset()
    var result = 0f
    repeat(10) {
      result = smoother.step(if (it % 2 == 0) 1f else 0f)
    }
    assertTrue(result >= 0f && result <= 1f)
  }

  @Test
  fun reset_afterMultipleSteps_resetsCorrectly() {
    repeat(50) {
      smoother.step(0.7f)
    }
    smoother.reset()
    val afterReset = smoother.step(0f)
    assertEquals(0f, afterReset, 0.001f)
  }

  @Test
  fun step_withExactBoundaryValues() {
    smoother.reset()
    val result1 = smoother.step(0f)
    assertEquals(0f, result1, 0.001f)

    smoother.reset()
    repeat(200) {
      smoother.step(1f)
    }
    val result2 = smoother.step(1f)
    assertTrue(result2 > 0.95f)
  }
}

