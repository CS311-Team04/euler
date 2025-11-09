package com.android.sample.speech

import android.app.Application
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.text.Charsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ElevenLabsPlaybackTest {

  private val context: Application = RuntimeEnvironment.getApplication()
  private val cacheRoot
    get() = File(context.cacheDir, "elevenlabs_audio")

  @Before
  fun setUp() {
    cacheRoot.deleteRecursively()
  }

  @After
  fun tearDown() {
    cacheRoot.deleteRecursively()
  }

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

  @Test
  fun getOrDownloadAudio_reuses_cached_file() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val server = MockWebServer()
    server.enqueue(
        MockResponse().setResponseCode(200).setHeader("Content-Type", "audio/mpeg").setBody("mp3"))
    server.start()

    val baseUrl = server.url("/v1").toString().trimEnd('/')
    val playback =
        object :
            ElevenLabsPlayback(
                context = context,
                apiKeyProvider = { "key" },
                voiceIdProvider = { "voice" },
                baseUrl = baseUrl,
                httpClient = OkHttpClient(),
                coroutineScope = scope) {
          suspend fun download(text: String, id: String): File = getOrDownloadAudio(text, id)
        }

    try {
      val firstFile = kotlinx.coroutines.runBlocking { playback.download("Bonjour", "cache-id") }
      assertTrue(firstFile.exists())
      val firstRequest = server.takeRequest(1, TimeUnit.SECONDS)
      assertEquals("/v1/text-to-speech/voice", firstRequest?.path)
      val firstModified = firstFile.lastModified()

      val secondFile = kotlinx.coroutines.runBlocking { playback.download("Rebonjour", "cache-id") }
      assertEquals(firstFile.absolutePath, secondFile.absolutePath)
      assertEquals(firstModified, secondFile.lastModified())
      val secondRequest = server.takeRequest(200, TimeUnit.MILLISECONDS)
      assertNull("Second call should reuse cached audio without new network request", secondRequest)
    } finally {
      playback.shutdown()
      scope.cancel()
      server.shutdown()
    }
  }

  @Test
  fun getOrDownloadAudio_httpErrorThrows() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(500))
    server.start()

    val baseUrl = server.url("/v1").toString().trimEnd('/')
    val playback =
        object :
            ElevenLabsPlayback(
                context = context,
                apiKeyProvider = { "key" },
                voiceIdProvider = { "voice" },
                baseUrl = baseUrl,
                httpClient = OkHttpClient(),
                coroutineScope = scope) {
          suspend fun download(text: String, id: String): File = getOrDownloadAudio(text, id)
        }

    try {
      val exception =
          kotlin
              .runCatching {
                kotlinx.coroutines.runBlocking { playback.download("Bonjour", "err") }
              }
              .exceptionOrNull()
      assertTrue(exception is IOException)
      assertEquals(1, server.requestCount)
    } finally {
      playback.shutdown()
      scope.cancel()
      server.shutdown()
    }
  }

  @Test
  fun getOrDownloadAudio_emptyBodyThrows() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "audio/mpeg"))
    server.start()

    val baseUrl = server.url("/v1").toString().trimEnd('/')
    val playback =
        object :
            ElevenLabsPlayback(
                context = context,
                apiKeyProvider = { "key" },
                voiceIdProvider = { "voice" },
                baseUrl = baseUrl,
                httpClient = OkHttpClient(),
                coroutineScope = scope) {
          suspend fun download(text: String, id: String): File = getOrDownloadAudio(text, id)
        }

    try {
      val exception =
          kotlin
              .runCatching {
                kotlinx.coroutines.runBlocking { playback.download("Bonjour", "empty") }
              }
              .exceptionOrNull()
      assertTrue(exception is IOException)
      assertEquals(1, server.requestCount)
    } finally {
      playback.shutdown()
      scope.cancel()
      server.shutdown()
    }
  }

  @Test
  fun speak_missing_voiceId_reports_error() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val errorLatch = CountDownLatch(1)
    var received: Throwable? = null
    val playback =
        ElevenLabsPlayback(
            context = context,
            apiKeyProvider = { "key" },
            voiceIdProvider = { "" },
            httpClient = OkHttpClient(),
            coroutineScope = scope)

    try {
      playback.speak(
          text = "Bonjour",
          utteranceId = "no-voice",
          onStart = {},
          onDone = {},
          onError = {
            received = it
            errorLatch.countDown()
          })

      assertTrue(errorLatch.await(1, TimeUnit.SECONDS))
      assertTrue(received is IllegalStateException)
    } finally {
      playback.shutdown()
      scope.cancel()
    }
  }

  @Test
  fun speak_missing_apiKey_reports_error() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val errorLatch = CountDownLatch(1)
    var received: Throwable? = null
    val playback =
        ElevenLabsPlayback(
            context = context,
            apiKeyProvider = { "" },
            voiceIdProvider = { "voice" },
            httpClient = OkHttpClient(),
            coroutineScope = scope)

    try {
      playback.speak(
          text = "Bonjour",
          utteranceId = "no-key",
          onStart = {},
          onDone = {},
          onError = {
            received = it
            errorLatch.countDown()
          })

      assertTrue(errorLatch.await(1, TimeUnit.SECONDS))
      assertTrue(received is IllegalStateException)
    } finally {
      playback.shutdown()
      scope.cancel()
    }
  }

  private fun hashName(voice: String, utterance: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hex = digest.digest("$voice|$utterance".toByteArray(Charsets.UTF_8))
    return hex.joinToString("") { "%02x".format(it) } + ".mp3"
  }
}
