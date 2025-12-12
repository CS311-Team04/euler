package com.android.sample.Chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChatMessageRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun chatMessage_shows_cleaned_moodle_content_and_badge() {
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

    composeRule.onNodeWithTag("chat_ai_text").assertTextEquals("- Topic")
    composeRule.onNodeWithTag("chat_ai_moodle_badge").assertIsDisplayed()
  }
}

