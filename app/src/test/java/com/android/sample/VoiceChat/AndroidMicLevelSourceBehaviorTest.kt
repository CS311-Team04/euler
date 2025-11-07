package com.android.sample.VoiceChat

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
class AndroidMicLevelSourceBehaviorTest {

  @Test
  fun start_emitsLevel_andStopsRecorder() = runBlocking {
    val recorder = EmittingRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    val level =
        withTimeout(1_000) { source.levels.first { emitted -> emitted > 0f && emitted <= 1f } }
    assertTrue("expected emitted level within bounds", level in 0f..1f)

    source.stop()

    assertEquals(1, recorder.startCount)
    assertEquals(1, recorder.stopCount)
    assertEquals(1, recorder.releaseCount)
  }

  @Test
  fun start_whenRecorderNotInitialized_releasesRecorderAndSkipsStart() {
    val recorder = NotInitializedRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    assertEquals("recorder.startRecording should not be invoked", 0, recorder.startCount)
    assertEquals("recorder should be released when not initialized", 1, recorder.releaseCount)
    source.stop()
  }

  @Test
  fun start_whenStartRecordingThrows_securityExceptionHandled() {
    val recorder = ThrowingStartRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    assertTrue("recorder should attempt to start", recorder.startAttempted)
    assertEquals("recorder should be released after failure", 1, recorder.releaseCount)

    source.stop()
  }

  @Test
  fun start_handlesNegativeReadGracefully() = runBlocking {
    val recorder = NegativeReadRecorder()
    val provider = FakeRecorderProvider(recorderFactory = { recorder })
    val source = AndroidMicLevelSource(recorderProvider = provider)

    source.start()

    // No emissions expected, but ensure the coroutine exits without hanging
    withTimeout(1_000) { recorder.loopFinished.first { it } }

    source.stop()

    assertEquals(1, recorder.stopCount)
    assertEquals(1, recorder.releaseCount)
  }
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
  var startCount = 0
    private set

  var releaseCount = 0
    private set

  override val state: Int
    get() = 0

  override val recordingState: Int
    get() = Recorder.RECORDSTATE_RECORDING

  override fun startRecording() {
    startCount++
  }

  override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

  override fun stop() {}

  override fun release() {
    releaseCount++
  }
}

private class ThrowingStartRecorder : Recorder {
  var releaseCount = 0
    private set

  var startAttempted = false
    private set

  override val state: Int
    get() = Recorder.STATE_INITIALIZED

  override val recordingState: Int
    get() = Recorder.RECORDSTATE_RECORDING

  override fun startRecording() {
    startAttempted = true
    throw SecurityException("permission missing")
  }

  override fun read(buffer: ShortArray, offset: Int, size: Int): Int = 0

  override fun stop() {}

  override fun release() {
    releaseCount++
  }
}

private class NegativeReadRecorder : Recorder {
  private val flow = kotlinx.coroutines.flow.MutableSharedFlow<Boolean>(replay = 1)
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
