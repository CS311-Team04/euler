package com.android.sample.Chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatMessageTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun chatMessage_shows_moodle_badge_and_clean_content_when_json() {
    val json =
        """
        {
          "type":"moodle_overview",
          "content":"- Topic: Details",
          "metadata":{
            "courseName":"Algebra",
            "weekLabel":"Week 2",
            "lastUpdated":"2025-12-11T04:15:00.000Z",
            "source":"Moodle"
          }
        }
        """
            .trimIndent()
    val message =
        ChatUIModel(id = "1", text = json, timestamp = 0L, type = ChatType.AI, isThinking = false)

    composeRule.setContent {
      MaterialTheme { ChatMessage(message = message, aiText = Color.Black) }
    }

    composeRule.onNodeWithTag("chat_ai_moodle_badge").assertIsDisplayed()
    composeRule.onNodeWithTag("chat_ai_text").assertTextEquals("- Topic")
  }

  @Test
  fun chatMessage_shows_plain_text_when_not_moodle_json() {
    val message =
        ChatUIModel(
            id = "2", text = "Hello", timestamp = 0L, type = ChatType.AI, isThinking = false)

    composeRule.setContent {
      MaterialTheme { ChatMessage(message = message, aiText = Color.Black) }
    }

    composeRule.onNodeWithText("Hello").assertIsDisplayed()
    composeRule.onAllNodesWithTag("chat_ai_moodle_badge").assertCountEquals(0)
  }
}
