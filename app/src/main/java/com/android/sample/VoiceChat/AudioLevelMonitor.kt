package com.android.sample.VoiceChat

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioLevelMonitor {
  private var recorder: AudioRecord? = null
  private var job: kotlinx.coroutines.Job? = null
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val _level = MutableStateFlow(0f)
  val levelFlow: StateFlow<Float> = _level

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  fun start() {
    if (recorder != null) {
      android.util.Log.w("AudioLevelMonitor", "Recorder already started")
      return
    }

    android.util.Log.d("AudioLevelMonitor", "Initializing AudioRecord...")
    val sampleRate = 16000
    val bufSize =
        AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

    if (bufSize == AudioRecord.ERROR_BAD_VALUE || bufSize == AudioRecord.ERROR) {
      android.util.Log.e("AudioLevelMonitor", "Invalid buffer size: $bufSize")
      _level.value = 0f
      return
    }

    val actualBufSize = bufSize.coerceAtLeast(2048)
    android.util.Log.d("AudioLevelMonitor", "Buffer size: $actualBufSize")

    try {
      recorder =
          AudioRecord(
              MediaRecorder.AudioSource.VOICE_RECOGNITION,
              sampleRate,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
              actualBufSize)

      if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
        android.util.Log.e(
            "AudioLevelMonitor", "AudioRecord not initialized. State: ${recorder?.state}")
        recorder?.release()
        recorder = null
        _level.value = 0f
        return
      }

      android.util.Log.d("AudioLevelMonitor", "AudioRecord initialized successfully")
    } catch (e: Exception) {
      android.util.Log.e("AudioLevelMonitor", "Error creating AudioRecord", e)
      _level.value = 0f
      return
    }

    try {
      recorder?.startRecording()
      val state = recorder?.recordingState
      android.util.Log.d("AudioLevelMonitor", "Recording state after start: $state")

      if (state != AudioRecord.RECORDSTATE_RECORDING) {
        android.util.Log.e("AudioLevelMonitor", "Failed to start recording. State: $state")
        recorder?.release()
        recorder = null
        _level.value = 0f
        return
      }
    } catch (e: Exception) {
      android.util.Log.e("AudioLevelMonitor", "Error starting recording", e)
      recorder?.release()
      recorder = null
      _level.value = 0f
      return
    }

    android.util.Log.d("AudioLevelMonitor", "Starting audio level collection...")
    job =
        scope.launch {
          val buf = ShortArray(actualBufSize)
          var readErrorCount = 0
          while (isActive && recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            try {
              val read = recorder?.read(buf, 0, buf.size) ?: 0
              if (read > 0) {
                readErrorCount = 0 // Reset error count on success
                // RMS → normalisé 0..1 (plus sensible)
                var sum = 0.0
                for (i in 0 until read) {
                  val v = buf[i].toInt()
                  sum += (v * v)
                }
                val rms = sqrt(sum / read)
                // Facteur encore plus sensible - les voix normales ont un RMS entre 50-500
                // Utilisons une échelle logarithmique pour plus de sensibilité
                val logScale =
                    if (rms > 10.0) {
                      kotlin.math.ln(rms / 10.0) /
                          kotlin.math.ln(50.0) // Normalise entre 10 et ~500
                    } else {
                      0.0
                    }
                val norm = logScale.coerceIn(0.0, 1.0)
                _level.value = norm.toFloat()

                // Log pour déboguer - afficher plus souvent
                if (rms > 20.0) {
                  android.util.Log.d(
                      "AudioLevelMonitor",
                      "RMS: ${rms.toInt()}, Normalized: ${String.format("%.3f", norm)}")
                }
              } else if (read < 0) {
                readErrorCount++
                android.util.Log.w(
                    "AudioLevelMonitor", "Read error: $read (error count: $readErrorCount)")
                if (readErrorCount > 10) {
                  android.util.Log.e("AudioLevelMonitor", "Too many read errors, stopping")
                  break
                }
              } else {
                // Si pas de données, décroissance douce
                _level.value = _level.value * 0.95f
              }
              delay(30) // ~33 fps de mise à jour pour plus de réactivité
            } catch (e: Exception) {
              android.util.Log.e("AudioLevelMonitor", "Error reading audio", e)
              break
            }
          }
          android.util.Log.d("AudioLevelMonitor", "Audio level collection stopped")
        }
  }

  fun stop() {
    job?.cancel()
    job = null
    recorder?.apply {
      try {
        stop()
      } catch (_: Throwable) {}
      release()
    }
    recorder = null
  }
}
