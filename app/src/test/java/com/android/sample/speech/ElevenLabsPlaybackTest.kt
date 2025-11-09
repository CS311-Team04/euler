package com.android.sample.speech

import android.app.Application
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ElevenLabsPlaybackTest {

  private val context: Application = RuntimeEnvironment.getApplication()

  @Test
  fun speak_success_invokes_callbacks() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val events = mutableListOf<String>()
    var stopCalls = 0
    val completionLatch = CountDownLatch(1)
    val playback =
        object :
            ElevenLabsPlayback(
                context = context,
                apiKeyProvider = { "key" },
                voiceIdProvider = { "voice" },
                httpClient = OkHttpClient(),
                coroutineScope = scope) {
          override suspend fun getOrDownloadAudio(text: String, utteranceId: String): File {
            events += "download:$text"
            return File(context.cacheDir, "$utteranceId.mp3").apply { writeText("dummy") }
          }

          override fun startPlayback(
              audioFile: File,
              onStart: () -> Unit,
              onDone: () -> Unit,
              onError: (Throwable?) -> Unit
          ) {
            events += "start:${audioFile.name}"
            onStart()
            onDone()
            completionLatch.countDown()
          }

          override fun stopInternal() {
            stopCalls++
            events += "stopInternal"
          }
        }

    try {
      playback.speak(
          text = "Bonjour",
          utteranceId = "abc",
          onStart = { events += "onStart" },
          onDone = { events += "onDone" },
          onError = { events += "onError" })

      assertTrue(completionLatch.await(1, TimeUnit.SECONDS))
      assertTrue(events.contains("download:Bonjour"))
      assertTrue(events.contains("start:abc.mp3"))
      assertTrue(events.contains("onStart"))
      assertTrue(events.contains("onDone"))
    } finally {
      playback.shutdown()
      scope.cancel()
    }
  }

  @Test
  fun speak_error_invokes_onError() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val errors = mutableListOf<String>()
    val errorLatch = CountDownLatch(1)
    val playback =
        object :
            ElevenLabsPlayback(
                context = context,
                apiKeyProvider = { "key" },
                voiceIdProvider = { "voice" },
                httpClient = OkHttpClient(),
                coroutineScope = scope) {
          override suspend fun getOrDownloadAudio(text: String, utteranceId: String): File {
            throw IOException("network down")
          }

          override fun stopInternal() {
            // ignore
          }
        }

    try {
      playback.speak(
          text = "Erreur",
          utteranceId = "err",
          onStart = {},
          onDone = {},
          onError = {
            errors += it?.message ?: "null"
            errorLatch.countDown()
          })

      assertTrue(errorLatch.await(1, TimeUnit.SECONDS))
      assertEquals(listOf("network down"), errors)
    } finally {
      playback.shutdown()
      scope.cancel()
    }
  }

  @Test
  fun stop_cancels_current_playback() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val events = mutableListOf<String>()
    val startLatch = CountDownLatch(1)
    val playback =
        object :
            ElevenLabsPlayback(
                context = context,
                apiKeyProvider = { "key" },
                voiceIdProvider = { "voice" },
                httpClient = OkHttpClient(),
                coroutineScope = scope) {
          var stopCalls = 0

          override suspend fun getOrDownloadAudio(text: String, utteranceId: String): File {
            return File(context.cacheDir, "$utteranceId.mp3").apply { writeText("dummy") }
          }

          override fun startPlayback(
              audioFile: File,
              onStart: () -> Unit,
              onDone: () -> Unit,
              onError: (Throwable?) -> Unit
          ) {
            events += "start"
            onStart()
            startLatch.countDown()
          }

          override fun stopInternal() {
            stopCalls++
            events += "stopInternal"
          }
        }

    try {
      playback.speak(
          text = "Bonjour", utteranceId = "stop", onStart = {}, onDone = {}, onError = {})
      assertTrue(startLatch.await(1, TimeUnit.SECONDS))

      val initialStops = playback.stopCalls
      playback.stop()

      assertTrue(playback.stopCalls >= initialStops + 1)
      assertTrue(events.contains("start"))
    } finally {
      playback.shutdown()
      scope.cancel()
    }
  }
}
