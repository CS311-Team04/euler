package com.android.sample.VoiceChat

import android.Manifest
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel
import com.android.sample.VoiceChat.UI.VoiceScreen
import com.android.sample.llm.FakeLlmClient
import com.android.sample.speech.SpeechPlayback
import com.android.sample.util.MainDispatcherRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceScreenInteractionTest {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule
  val dispatcherRule = MainDispatcherRule(kotlinx.coroutines.test.UnconfinedTestDispatcher())

  private fun createVoiceViewModel(): VoiceChatViewModel =
      VoiceChatViewModel(FakeLlmClient(), dispatcherRule.dispatcher)

  @Test
  fun voiceScreen_withGrantedPermission_renders() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent {
      val playback = remember { FakeSpeechPlayback() }
      VoiceScreen(
          onClose = {}, voiceChatViewModel = createVoiceViewModel(), speechPlayback = playback)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_withoutPermission_renders() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).denyPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent {
      val playback = remember { FakeSpeechPlayback() }
      VoiceScreen(
          onClose = {}, voiceChatViewModel = createVoiceViewModel(), speechPlayback = playback)
    }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun closeButton_invokesCallback() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    var closed = false
    composeTestRule.setContent {
      val playback = remember { FakeSpeechPlayback() }
      VoiceScreen(
          onClose = { closed = true },
          voiceChatViewModel = createVoiceViewModel(),
          speechPlayback = playback)
    }

    composeTestRule.onNodeWithContentDescription("Close voice screen").performClick()

    org.junit.Assert.assertTrue(closed)
  }

  private class FakeSpeechPlayback : SpeechPlayback {
    override fun speak(
        text: String,
        utteranceId: String,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable?) -> Unit
    ) {
      onStart()
      onDone()
    }

    override fun stop() {}

    override fun shutdown() {}
  }
}
