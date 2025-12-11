package com.android.sample.Chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChatMessageTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun userMessage_rendersUserBubble() {
    val msg = ChatUIModel(id = "1", text = "Hello", timestamp = 0L, type = ChatType.USER)

    composeRule.setContent { ChatMessage(message = msg) }

    composeRule.onNodeWithTag("chat_user_text").assertIsDisplayed()
  }

  @Test
  fun aiMessageWithAttachment_showsPdfCard() {
    val msg =
        ChatUIModel(
            id = "2",
            text = "Voici le document demandé.",
            timestamp = 0L,
            type = ChatType.AI,
            attachment = ChatAttachment(url = "https://example.com/test.pdf", title = "test.pdf"))

    composeRule.setContent { ChatMessage(message = msg) }

    composeRule.onNodeWithText("PDF").assertIsDisplayed()
    composeRule.onNodeWithText("test.pdf").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Open PDF").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Download PDF").assertIsDisplayed()
  }
}
