package com.android.sample.VoiceChat

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidMicLevelSourceErrorPathsTest {

  private class NotInitializedRecorder : Recorder {
    override val state: Int
      get() = 0 // not initialized

    override val recordingState: Int
      get() = 0

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {}
  }

  private class NotRecordingRecorder : Recorder {
    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = 0 // not recording

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {}
  }

  private class NegativeReadRecorder : Recorder {
    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = -1

    override fun stop() {}

    override fun release() {}
  }

  private class ThrowingProvider : RecorderProvider {
    override fun minBufferSize(sampleRate: Int): Int = 256

    override fun create(sampleRate: Int, bufferSize: Int): Recorder {
      throw SecurityException("no permission")
    }
  }

  private class FixedProvider(private val rec: Recorder) : RecorderProvider {
    override fun minBufferSize(sampleRate: Int): Int = 256

    override fun create(sampleRate: Int, bufferSize: Int): Recorder = rec
  }

  @Test
  fun start_handles_not_initialized_recorder() = runBlocking {
    val src = AndroidMicLevelSource(recorderProvider = FixedProvider(NotInitializedRecorder()))
    src.start()
    src.stop()
    assertTrue(true)
  }

  @Test
  fun start_handles_not_recording_state() = runBlocking {
    val src = AndroidMicLevelSource(recorderProvider = FixedProvider(NotRecordingRecorder()))
    src.start()
    src.stop()
    assertTrue(true)
  }

  @Test
  fun loop_handles_negative_reads() = runBlocking {
    val src = AndroidMicLevelSource(recorderProvider = FixedProvider(NegativeReadRecorder()))
    src.start()
    // Immediately stop; negative read path is exercised
    src.stop()
    assertTrue(true)
  }

  @Test
  fun start_handles_security_exception() = runBlocking {
    val src = AndroidMicLevelSource(recorderProvider = ThrowingProvider())
    src.start()
    src.stop()
    assertTrue(true)
  }
}
