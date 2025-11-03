package com.android.sample.VoiceChat

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidMicLevelSource(
    private val sampleRate: Int = 16000,
) : LevelSource {

  private var recorder: AudioRecord? = null
  private var job: kotlinx.coroutines.Job? = null
  private val _levels = MutableSharedFlow<Float>(replay = 1)
  override val levels: Flow<Float> = _levels

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  override fun start() {
    if (recorder != null) return

    val min =
        AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(4096)

    try {
      recorder =
          AudioRecord(
              MediaRecorder.AudioSource.VOICE_RECOGNITION,
              sampleRate,
              AudioFormat.CHANNEL_IN_MONO,
              AudioFormat.ENCODING_PCM_16BIT,
              min)

      if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
        android.util.Log.e(
            "AndroidMicLevelSource",
            "Erreur: AudioRecord n'est pas initialisé correctement, état: ${recorder?.state}")
        recorder?.release()
        recorder = null
        return
      }

      android.util.Log.d("AndroidMicLevelSource", "AudioRecord initialisé avec succès")

      recorder?.startRecording()

      val recordingState = recorder?.recordingState
      android.util.Log.d(
          "AndroidMicLevelSource",
          "AudioRecord démarré, état: $recordingState (RECORDSTATE_RECORDING=${AudioRecord.RECORDSTATE_RECORDING})")

      if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
        android.util.Log.e(
            "AndroidMicLevelSource",
            "Erreur: AudioRecord n'est pas en état RECORDING après startRecording(), état: $recordingState")
        recorder?.stop()
        recorder?.release()
        recorder = null
        return
      }
    } catch (e: Exception) {
      android.util.Log.e(
          "AndroidMicLevelSource", "Exception lors de l'initialisation d'AudioRecord", e)
      recorder?.release()
      recorder = null
      return
    }

    job =
        GlobalScope.launch(Dispatchers.IO) {
          val buf = ShortArray(min)
          var frameCount = 0
          while (isActive && recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val n = recorder?.read(buf, 0, buf.size) ?: 0
            if (n > 0) {
              var s = 0.0
              var maxVal = 0
              for (i in 0 until n) {
                val v = buf[i].toInt()
                s += v * v
                maxVal = maxOf(maxVal, kotlin.math.abs(v))
              }
              val rms = sqrt(s / n)

              // Seuil encore plus bas pour être plus sensible (de 1800 à 1200)
              val norm = (rms / 1200.0).coerceIn(0.0, 1.0).toFloat()
              // Amplification plus forte
              val amplified = (norm * 1.8f).coerceIn(0.0f, 1.0f)

              _levels.emit(amplified)

              // Log toutes les 60 frames (~1 seconde) pour débugger
              frameCount++
              if (frameCount % 60 == 0) {
                android.util.Log.d(
                    "AndroidMicLevelSource",
                    "RMS: ${String.format("%.2f", rms)}, Max: $maxVal, Normalized: ${String.format("%.3f", norm)}, Amplified: ${String.format("%.3f", amplified)}")
              }
            } else if (n < 0) {
              android.util.Log.w("AndroidMicLevelSource", "Erreur lecture audio: $n")
            }
            delay(16)
          }
          android.util.Log.d(
              "AndroidMicLevelSource",
              "Boucle de lecture terminée (isActive=$isActive, recordingState=${recorder?.recordingState})")
        }
  }

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
}
