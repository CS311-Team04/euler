package com.android.sample.speech

import android.content.Context
import android.media.MediaPlayer
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ElevenLabsPlaybackMediaTest {

  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun startPlayback_missingFile_invokesErrorAndReleasesPlayer() {
    val playback = TestPlayback(context)
    val missing = File(context.cacheDir, "missing_audio.mp3").apply { if (exists()) delete() }
    var error: Throwable? = null

    playback.callStartPlayback(missing, onStart = {}, onDone = {}, onError = { error = it })
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    assertTrue(error != null)
    assertNull(playback.peekMediaPlayer())
  }

  @Test
  fun stopInternal_releasesActivePlayer() {
    val playback = TestPlayback(context)
    val player = MediaPlayer()
    playback.setMediaPlayer(player)

    playback.callStopInternal()

    assertNull(playback.peekMediaPlayer())
  }

  private class TestPlayback(context: Context) :
      ElevenLabsPlayback(
          context = context,
          apiKeyProvider = { "key" },
          voiceIdProvider = { "voice" },
          httpClient = OkHttpClient(),
          coroutineScope = CoroutineScope(Dispatchers.Main)) {

    fun callStartPlayback(
        file: File,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable?) -> Unit
    ) {
      startPlayback(file, onStart, onDone, onError)
    }

    fun callStopInternal() {
      stopInternal()
    }

    fun peekMediaPlayer(): MediaPlayer? = getMediaPlayer()

    fun setMediaPlayer(player: MediaPlayer?) {
      setMediaPlayerField(player)
    }
  }
}

private fun ElevenLabsPlayback.getMediaPlayer(): MediaPlayer? {
  val field = ElevenLabsPlayback::class.java.getDeclaredField("mediaPlayer")
  field.isAccessible = true
  return field.get(this) as MediaPlayer?
}

private fun ElevenLabsPlayback.setMediaPlayerField(player: MediaPlayer?) {
  val field = ElevenLabsPlayback::class.java.getDeclaredField("mediaPlayer")
  field.isAccessible = true
  field.set(this, player)
}
