package com.android.sample.Chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ChatMessageAudioTest {

  @get:Rule val composeRule = createComposeRule()

  private val aiMessage =
      ChatUIModel(id = "ai-1", text = "Bonjour, je suis Euler.", timestamp = 0L, type = ChatType.AI)

  @Test
  fun audioButton_hidden_whenStateNull() {
    composeRule.setContent {
      SampleAppTheme { ChatMessage(message = aiMessage, audioState = null) }
    }

    assertTrue(composeRule.onAllNodesWithTag("chat_audio_btn_play").fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithTag("chat_audio_btn_stop").fetchSemanticsNodes().isEmpty())
    assertTrue(
        composeRule.onAllNodesWithTag("chat_audio_btn_loading").fetchSemanticsNodes().isEmpty())
  }

  @Test
  fun audioButton_showsPlayAndInvokesCallback() {
    var played = false
    val state =
        MessageAudioState(
            isLoading = false, isPlaying = false, onPlay = { played = true }, onStop = {})

    composeRule.setContent {
      SampleAppTheme { ChatMessage(message = aiMessage, audioState = state) }
    }

    assertFalse(
        composeRule.onAllNodesWithTag("chat_audio_btn_play").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("chat_audio_btn_play").assertIsDisplayed().performClick()
    assertTrue("Play callback should be triggered", played)
  }

  @Test
  fun audioButton_showsLoadingIndicator() {
    val state = MessageAudioState(isLoading = true, isPlaying = false, onPlay = {}, onStop = {})

    composeRule.setContent {
      SampleAppTheme { ChatMessage(message = aiMessage, audioState = state) }
    }

    assertFalse(
        composeRule.onAllNodesWithTag("chat_audio_btn_loading").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("chat_audio_btn_loading").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("chat_audio_btn_play").fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithTag("chat_audio_btn_stop").fetchSemanticsNodes().isEmpty())
  }

  @Test
  fun audioButton_showsStopWhenPlaying() {
    val state = MessageAudioState(isLoading = false, isPlaying = true, onPlay = {}, onStop = {})

    composeRule.setContent {
      SampleAppTheme { ChatMessage(message = aiMessage, audioState = state) }
    }

    assertFalse(
        composeRule.onAllNodesWithTag("chat_audio_btn_stop").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("chat_audio_btn_stop").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("chat_audio_btn_play").fetchSemanticsNodes().isEmpty())
  }
}
