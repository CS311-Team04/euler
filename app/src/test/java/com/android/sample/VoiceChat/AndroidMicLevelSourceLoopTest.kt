package com.android.sample.VoiceChat

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidMicLevelSourceLoopTest {

  private class FakeRecorder : Recorder {
    private val reads = AtomicInteger(0)
    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {}

    override fun stop() {}

    override fun release() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
      // Produce some deterministic waveform for a few iterations
      val count = reads.incrementAndGet()
      if (count > 30) return 0 // stop producing data
      val n = kotlin.math.min(size, 128)
      for (i in 0 until n) {
        buffer[i] = ((i % 32) * 100).toShort()
      }
      return n
    }
  }

  private class FakeRecorderProvider : RecorderProvider {
    override fun minBufferSize(sampleRate: Int): Int = 512

    override fun create(sampleRate: Int, bufferSize: Int): Recorder = FakeRecorder()
  }

  @Test
  fun start_emits_levels_then_stop() = runBlocking {
    val source =
        AndroidMicLevelSource(
            sampleRate = AndroidMicLevelSource.DEFAULT_SAMPLE_RATE,
            recorderProvider = FakeRecorderProvider())
    source.start()

    // Allow background loop to emit at least one value
    delay(100)
    val level = source.levels.first()
    assertTrue(level in 0f..1f)

    source.stop()
  }
}
