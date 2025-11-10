package com.android.sample.VoiceChat

import com.android.sample.VoiceChat.UI.AndroidMicLevelSource
import com.android.sample.VoiceChat.UI.Recorder
import com.android.sample.VoiceChat.UI.RecorderProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidMicLevelSourceTest {

  @Test
  fun stopWithoutStart_doesNotThrow() {
    val source = AndroidMicLevelSource()
    source.stop()
    source.stop()
    assertTrue(true)
  }

  @Test
  fun multipleInstances_areIndependent() {
    val source1 = AndroidMicLevelSource()
    val source2 = AndroidMicLevelSource()

    assertNotSame(source1, source2)
    assertNotNull(source1.levels)
    assertNotNull(source2.levels)
  }

  @Test
  fun start_emitsLevel_andStopsRecorder() = runBlocking {
    val recorder = EmittingRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    val level =
        withTimeout(1_000) { source.levels.first { emitted -> emitted > 0f && emitted <= 1f } }
    assertTrue(level in 0f..1f)

    source.stop()

    assertEquals(1, recorder.startCount)
    assertEquals(1, recorder.stopCount)
    assertEquals(1, recorder.releaseCount)
  }

  @Test
  fun start_handlesNotInitializedRecorder() = runBlocking {
    val source =
        AndroidMicLevelSource(
            recorderProvider = FakeRecorderProvider(recorderFactory = { NotInitializedRecorder() }))

    source.start()
    source.stop()
    assertTrue(true)
  }

  @Test
  fun start_handlesNotRecordingState() = runBlocking {
    val source =
        AndroidMicLevelSource(
            recorderProvider = FakeRecorderProvider(recorderFactory = { NotRecordingRecorder() }))

    source.start()
    source.stop()
    assertTrue(true)
  }

  @Test
  fun start_handlesSecurityException() = runBlocking {
    val source =
        AndroidMicLevelSource(
            recorderProvider =
                object : RecorderProvider {
                  override fun minBufferSize(sampleRate: Int): Int = 128

                  override fun create(sampleRate: Int, bufferSize: Int): Recorder {
                    throw SecurityException("permission missing")
                  }
                })

    source.start()
    source.stop()
    assertTrue(true)
  }

  @Test
  fun start_handlesSecurityExceptionWhenStarting() = runBlocking {
    val recorder = StartSecurityExceptionRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    assertEquals(1, recorder.startAttempts.get())
    assertEquals(1, recorder.releaseCount.get())

    source.stop()
  }

  @Test
  fun start_handlesUnexpectedExceptionAndReleases() = runBlocking {
    val recorder = ThrowingStateRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    assertEquals(1, recorder.releaseCount.get())
    assertEquals(0, recorder.startCount.get())

    source.stop()
  }

  @Test
  fun readLoop_emitsAndStopsOnZero() = runBlocking {
    val provider = FakeRecorderProvider(recorderFactory = { LoopRecorder() })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()
    delay(100)
    val level = source.levels.first()
    assertTrue(level in 0f..1f)
    source.stop()
  }

  @Test
  fun negativeRead_finishesLoopGracefully() = runBlocking {
    val recorder = NegativeReadRecorder()
    val source =
        AndroidMicLevelSource(
            recorderProvider = FakeRecorderProvider(recorderFactory = { recorder }))

    source.start()
    withTimeout(1_000) { recorder.loopFinished.first { it } }
    source.stop()

    assertEquals(1, recorder.stopCount)
    assertEquals(1, recorder.releaseCount)
  }

  @Test
  fun readLoop_logsAtIntervalAndStopsWhenRecorderStops() = runBlocking {
    val recorder = LoggingRecorder()
    val source =
        AndroidMicLevelSource(
            recorderProvider = FakeRecorderProvider(recorderFactory = { recorder }))

    source.start()

    // Wait for the recorder to report that the periodic logging interval has been reached
    withTimeout(2_000) {
      while (recorder.loggedInterval.get() < AndroidMicLevelSource.LOG_INTERVAL_FRAMES) {
        delay(10)
      }
    }

    // Wait until the recorder flips to non-recording state which should end the loop
    withTimeout(1_000) { recorder.loopFinished.first { it } }

    source.stop()

    assertTrue(recorder.loggedInterval.get() >= AndroidMicLevelSource.LOG_INTERVAL_FRAMES)
  }

  @Test
  fun start_whenAlreadyStarted_doesNotRecreateRecorder() {
    val createCount = AtomicInteger(0)
    val source =
        AndroidMicLevelSource(
            recorderProvider =
                FakeRecorderProvider(
                    recorderFactory = {
                      createCount.incrementAndGet()
                      NoopRecordingRecorder()
                    },
                    minBuffer = 16))

    source.start()
    source.start() // Should be ignored
    source.stop()

    assertEquals(1, createCount.get())
  }

  private class FakeRecorderProvider(
      private val recorderFactory: () -> Recorder,
      private val minBuffer: Int = 8
  ) : RecorderProvider {
    override fun minBufferSize(sampleRate: Int): Int = minBuffer

    override fun create(sampleRate: Int, bufferSize: Int): Recorder = recorderFactory()
  }

  private class EmittingRecorder : Recorder {
    private var recordingStateValue = Recorder.RECORDSTATE_RECORDING

    var startCount = 0
      private set

    var stopCount = 0
      private set

    var releaseCount = 0
      private set

    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = recordingStateValue

    override fun startRecording() {
      startCount++
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
      for (i in 0 until size) {
        buffer[offset + i] = 1200.toShort()
      }
      recordingStateValue = 0
      return size
    }

    override fun stop() {
      stopCount++
      recordingStateValue = 0
    }

    override fun release() {
      releaseCount++
    }
  }

  private class NotInitializedRecorder : Recorder {
    override val state: Int
      get() = 0

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {}
  }

  private class NotRecordingRecorder : Recorder {
    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = 0

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {}
  }

  private class LoopRecorder : Recorder {
    private val reads = AtomicInteger(0)

    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {}

    override fun stop() {}

    override fun release() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
      val count = reads.incrementAndGet()
      if (count > 30) return 0
      val n = kotlin.math.min(size, 128)
      for (i in 0 until n) {
        buffer[offset + i] = ((i % 32) * 100).toShort()
      }
      return n
    }
  }

  private class NegativeReadRecorder : Recorder {
    private val flow = MutableSharedFlow<Boolean>(replay = 1)
    var stopCount = 0
      private set

    var releaseCount = 0
      private set

    val loopFinished = flow

    private var recordingStateValue = Recorder.RECORDSTATE_RECORDING

    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = recordingStateValue

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
      recordingStateValue = 0
      flow.tryEmit(true)
      return -5
    }

    override fun stop() {
      stopCount++
    }

    override fun release() {
      releaseCount++
    }
  }

  private class StartSecurityExceptionRecorder : Recorder {
    val startAttempts = AtomicInteger(0)
    val releaseCount = AtomicInteger(0)

    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {
      startAttempts.incrementAndGet()
      throw SecurityException("denied")
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {
      releaseCount.incrementAndGet()
    }
  }

  private class ThrowingStateRecorder : Recorder {
    val releaseCount = AtomicInteger(0)
    val startCount = AtomicInteger(0)

    override val state: Int
      get() = throw IllegalStateException("boom")

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {
      startCount.incrementAndGet()
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {
      releaseCount.incrementAndGet()
    }
  }

  private class LoggingRecorder : Recorder {
    private val reads = AtomicInteger(0)
    private val loopFinishedFlow = MutableSharedFlow<Boolean>(replay = 1)
    private var recordingStateValue = Recorder.RECORDSTATE_RECORDING

    val loggedInterval: AtomicInteger = AtomicInteger(0)
    val loopFinished: MutableSharedFlow<Boolean>
      get() = loopFinishedFlow

    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = recordingStateValue

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
      val count = reads.incrementAndGet()
      val n = kotlin.math.min(size, 64)
      for (i in 0 until n) {
        buffer[offset + i] = 1200.toShort()
      }
      if (count % AndroidMicLevelSource.LOG_INTERVAL_FRAMES == 0) {
        loggedInterval.set(count)
      }
      if (count > AndroidMicLevelSource.LOG_INTERVAL_FRAMES) {
        recordingStateValue = 0
        loopFinishedFlow.tryEmit(true)
        return 0
      }
      return n
    }

    override fun stop() {
      recordingStateValue = 0
    }

    override fun release() {}
  }

  private class NoopRecordingRecorder : Recorder {
    override val state: Int
      get() = Recorder.STATE_INITIALIZED

    override val recordingState: Int
      get() = Recorder.RECORDSTATE_RECORDING

    override fun startRecording() {}

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

    override fun stop() {}

    override fun release() {}
  }
}
