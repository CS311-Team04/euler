package com.android.sample.VoiceChat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel.VoiceChatUiState
import com.android.sample.VoiceChat.UI.StatusCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceChatStatusTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun statusCard_showsErrorWhenPresent() {
    composeRule.setContent {
      MaterialTheme { StatusCard(uiState = VoiceChatUiState(lastError = "microphone unavailable")) }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("microphone unavailable").assertIsDisplayed()
  }
}
