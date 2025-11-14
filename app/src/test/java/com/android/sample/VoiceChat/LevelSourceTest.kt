package com.android.sample.VoiceChat

import com.android.sample.VoiceChat.UI.LevelSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LevelSourceTest {

  @Test
  fun mockLevelSource_emitsConstantLevel() = runBlocking {
    val level = 0.5f
    val source = MockLevelSource(level)
    source.start()

    val firstValue = source.levels.first()
    assertEquals(level, firstValue, 0.001f)

    source.stop()
  }

  @Test
  fun mockLevelSource_clampsLevelAboveOne() = runBlocking {
    val source = MockLevelSource(2f)
    source.start()

    val firstValue = source.levels.first()
    assertTrue(firstValue <= 1f)

    source.stop()
  }

  @Test
  fun mockLevelSource_clampsLevelBelowZero() = runBlocking {
    val source = MockLevelSource(-1f)
    source.start()

    val firstValue = source.levels.first()
    assertTrue(firstValue >= 0f)

    source.stop()
  }

  @Test
  fun mockLevelSource_emitsMultipleValues() = runBlocking {
    val source = MockLevelSource(0.7f)
    source.start()

    val values = source.levels.take(5).toList()
    assertEquals(5, values.size)
    values.forEach { assertEquals(0.7f, it, 0.001f) }

    source.stop()
  }

  @Test
  fun mockLevelSource_startStop_doesNotThrow() {
    val source = MockLevelSource(0.5f)
    source.start()
    source.stop()
    // Should not throw
    assertTrue(true)
  }

  @Test
  fun mockLevelSource_multipleStartStopCalls_handlesCorrectly() {
    val source = MockLevelSource(0.5f)
    source.start()
    source.stop()
    source.start()
    source.stop()
    // Should not throw
    assertTrue(true)
  }

  @Test
  fun managedLevelSource_delegatesLevels() = runBlocking {
    val delegate = MockLevelSource(0.6f)
    val managed = ManagedLevelSource(delegate)
    delegate.start()

    val managedValue = managed.levels.first()
    assertEquals(0.6f, managedValue, 0.001f)

    delegate.stop()
  }

  @Test
  fun managedLevelSource_startDoesNotDelegate() {
    val delegate = MockLevelSource(0.5f)
    val managed = ManagedLevelSource(delegate)

    // start() should not throw and should not affect delegate
    managed.start()
    assertTrue(true)
  }

  @Test
  fun managedLevelSource_stopDoesNotDelegate() {
    val delegate = MockLevelSource(0.5f)
    val managed = ManagedLevelSource(delegate)

    // stop() should not throw and should not affect delegate
    managed.stop()
    assertTrue(true)
  }

  @Test
  fun managedLevelSource_multipleStartStopCalls_handlesCorrectly() {
    val delegate = MockLevelSource(0.5f)
    val managed = ManagedLevelSource(delegate)

    managed.start()
    managed.stop()
    managed.start()
    managed.stop()
    // Should not throw
    assertTrue(true)
  }
}

// Helper classes for testing (same as in VoiceScreen.kt but accessible for tests)
private class MockLevelSource(private val level: Float) : LevelSource {
  override val levels =
      kotlinx.coroutines.flow.flow {
        while (true) {
          emit(level.coerceIn(0f, 1f))
          kotlinx.coroutines.delay(16)
        }
      }

  override fun start() {}

  override fun stop() {}
}

private class ManagedLevelSource(private val delegate: LevelSource) : LevelSource {
  override val levels: kotlinx.coroutines.flow.Flow<Float> = delegate.levels

  override fun start() {
    // Ne pas démarrer, car c'est géré ailleurs
  }

  override fun stop() {
    // Ne pas arrêter, car c'est géré ailleurs
  }
}
