package com.android.sample.VoiceChat.UI

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Wrapper interface around AudioRecord to enable deterministic testing. */
interface Recorder {
  val state: Int
  val recordingState: Int

  fun startRecording()

  fun read(buffer: ShortArray, offset: Int, size: Int): Int

  fun stop()

  fun release()

  companion object {
    // Mirror AudioRecord constants for comparison without coupling in clients
    const val STATE_INITIALIZED = 1
    const val RECORDSTATE_RECORDING = 3
  }
}

/** Provider for creating Recorder instances and computing min buffer size. */
interface RecorderProvider {
  fun minBufferSize(sampleRate: Int): Int

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  fun create(sampleRate: Int, bufferSize: Int): Recorder
}

private class DefaultRecorderProvider : RecorderProvider {
  override fun minBufferSize(sampleRate: Int): Int {
    return AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        .coerceAtLeast(AndroidMicLevelSource.MIN_AUDIO_BUFFER_SIZE)
  }

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  override fun create(sampleRate: Int, bufferSize: Int): Recorder {
    val ar =
        AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize)
    return object : Recorder {
      override val state: Int
        get() = ar.state

      override val recordingState: Int
        get() = ar.recordingState

      override fun startRecording() = ar.startRecording()

      override fun read(buffer: ShortArray, offset: Int, size: Int): Int =
          ar.read(buffer, offset, size)

      override fun stop() = ar.stop()

      override fun release() = ar.release()
    }
  }
}

/**
 * LevelSource implementation that reads microphone input using AudioRecord and emits a normalized
 * audio level [0f..1f] suitable for visualization.
 */
class AndroidMicLevelSource(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val recorderProvider: RecorderProvider = DefaultRecorderProvider(),
) : LevelSource {

  private var recorder: Recorder? = null
  private var job: Job? = null
  private val _levels = MutableSharedFlow<Float>(replay = 1)
  override val levels: Flow<Float> = _levels

  /**
   * Starts audio capture if not already started. Requires RECORD_AUDIO permission. Initializes
   * AudioRecord, begins reading audio frames, computes RMS, normalizes and emits levels.
   */
  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  override fun start() {
    if (recorder != null) return

    val min = recorderProvider.minBufferSize(sampleRate)

    try {
      try {
        recorder = recorderProvider.create(sampleRate, min)
      } catch (se: SecurityException) {
        Log.e(TAG, "SecurityException creating AudioRecord (missing permission)", se)
        recorder = null
        return
      }

      if (recorder?.state != Recorder.STATE_INITIALIZED) {
        Log.e(TAG, "Error: AudioRecord is not initialized correctly, state: ${recorder?.state}")
        recorder?.release()
        recorder = null
        return
      }

      Log.d(TAG, "AudioRecord initialized successfully")

      try {
        recorder?.startRecording()
      } catch (se: SecurityException) {
        Log.e(TAG, "SecurityException starting AudioRecord", se)
        try {
          recorder?.release()
        } catch (_: Throwable) {}
        recorder = null
        return
      }

      val recordingState = recorder?.recordingState
      Log.d(
          TAG,
          "AudioRecord started, state: $recordingState (RECORDSTATE_RECORDING=${Recorder.RECORDSTATE_RECORDING})")

      if (recordingState != Recorder.RECORDSTATE_RECORDING) {
        Log.e(
            TAG,
            "Error: AudioRecord is not in RECORDING state after startRecording(), state: $recordingState")
        try {
          recorder?.stop()
        } catch (_: Throwable) {}
        recorder?.release()
        recorder = null
        return
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception while initializing AudioRecord", e)
      try {
        recorder?.release()
      } catch (_: Throwable) {}
      recorder = null
      return
    }

    job =
        GlobalScope.launch(Dispatchers.IO) {
          val buf = ShortArray(min)
          var frameCount = 0
          while (isActive && recorder?.recordingState == Recorder.RECORDSTATE_RECORDING) {
            val n = recorder?.read(buf, 0, buf.size) ?: 0
            if (n > 0) {
              var s = 0.0
              var maxVal = 0
              for (i in 0 until n) {
                val v = buf[i].toInt()
                s += v * v
                maxVal = maxOf(maxVal, abs(v))
              }
              val rms = sqrt(s / n)

              // Normalize RMS to [0..1] using a divisor tuned for voice sensitivity
              val norm =
                  (rms / RMS_NORMALIZATION_DIVISOR).coerceIn(MIN_LEVEL_D, MAX_LEVEL_D).toFloat()
              // Amplify for stronger visual response
              val amplified = (norm * AMPLIFICATION_FACTOR).coerceIn(MIN_LEVEL_F, MAX_LEVEL_F)

              _levels.emit(amplified)

              // Periodic debug log
              frameCount++
              if (frameCount % LOG_INTERVAL_FRAMES == 0) {
                Log.d(
                    TAG,
                    "RMS: ${String.format("%.2f", rms)}, Max: $maxVal, Normalized: ${String.format("%.3f", norm)}, Amplified: ${String.format("%.3f", amplified)}")
              }
            } else if (n < 0) {
              Log.w(TAG, "Audio read error: $n")
            }
            delay(FRAME_DELAY_MS)
          }
          Log.d(
              TAG,
              "Read loop finished (isActive=$isActive, recordingState=${recorder?.recordingState})")
        }
  }

  /** Stops audio capture and releases AudioRecord resources. */
  override fun stop() {
    job?.cancel()
    job = null
    recorder?.run {
      try {
        stop()
      } catch (_: Throwable) {}
      release()
    }
    recorder = null
  }

  internal companion object {
    const val TAG = "AndroidMicLevelSource"
    const val DEFAULT_SAMPLE_RATE = 16000
    const val MIN_AUDIO_BUFFER_SIZE = 4096
    const val FRAME_DELAY_MS = 16L
    const val LOG_INTERVAL_FRAMES = 60

    const val RMS_NORMALIZATION_DIVISOR = 1200.0

    const val AMPLIFICATION_FACTOR = 1.8f

    const val MIN_LEVEL_D = 0.0
    const val MAX_LEVEL_D = 1.0

    const val MIN_LEVEL_F = 0.0f
    const val MAX_LEVEL_F = 1.0f
  }
}
