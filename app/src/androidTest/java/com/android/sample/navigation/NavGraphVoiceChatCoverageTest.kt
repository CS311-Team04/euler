package com.android.sample.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation.NavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.speech.SpeechPlayback
import com.android.sample.speech.SpeechToTextHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test that exercises the VoiceChat route in NavGraph to ensure production code is
 * covered by JaCoCo/Sonar. This renders the real AppNav composable, navigates to VoiceChat, and
 * verifies that the VoiceScreen content is displayed.
 */
@RunWith(AndroidJUnit4::class)
class NavGraphVoiceChatCoverageTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var speechHelper: SpeechToTextHelper
  private var capturedController: NavHostController? = null

  @Before
  fun setUp() {
    val activity = composeRule.activity
    speechHelper = SpeechToTextHelper(activity, activity)
    appNavControllerObserver = { controller -> capturedController = controller }
  }

  @After
  fun tearDown() {
    appNavControllerObserver = null
    if (::speechHelper.isInitialized) {
      speechHelper.destroy()
    }
  }

  @Test
  fun voiceChatRoute_isDisplayed() {
    composeRule.setContent {
      AppNav(
          startOnSignedIn = true,
          activity = composeRule.activity,
          speechHelper = speechHelper,
          ttsHelper = FakeSpeechPlayback())
    }

    composeRule.waitForIdle()
    composeRule.runOnIdle {
      val navController =
          requireNotNull(capturedController) { "NavController not captured from AppNav" }
      navController.navigate(Routes.VoiceChat)
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithContentDescription("Close voice screen").assertIsDisplayed()
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
