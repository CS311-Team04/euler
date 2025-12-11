package com.android.sample.Chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ChatMessageMarkdownTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun aiMessage_rendersMarkdownHeadingAndList() {
    val message =
        ChatUIModel(
            id = "ai-md",
            text = "# Titre\n- point **important**",
            timestamp = 0L,
            type = ChatType.AI)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = message) } }

    composeRule.onNodeWithTag("chat_ai_text").assertIsDisplayed()
  }

  @Test
  fun userMessage_rendersMarkdownInsideBubble() {
    val message =
        ChatUIModel(
            id = "user-md",
            text = "Bonjour **Euler**\n- premier item",
            timestamp = 0L,
            type = ChatType.USER)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = message) } }

    composeRule.onNodeWithTag("chat_user_text").assertIsDisplayed()
  }
}
