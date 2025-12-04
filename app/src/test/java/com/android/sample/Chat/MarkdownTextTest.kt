package com.android.sample.Chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MarkdownTextTest {

  @get:Rule val composeRule = createComposeRule()

  // ========== escapeMarkdown Tests ==========

  @Test
  fun escapeMarkdown_escapes_backslash() {
    val input = "test\\path"
    val result = escapeMarkdown(input)
    assertEquals("test\\\\path", result)
  }

  @Test
  fun escapeMarkdown_escapes_backticks() {
    val input = "use `code` here"
    val result = escapeMarkdown(input)
    assertEquals("use \\`code\\` here", result)
  }

  @Test
  fun escapeMarkdown_escapes_asterisks() {
    val input = "make it **bold**"
    val result = escapeMarkdown(input)
    assertEquals("make it \\*\\*bold\\*\\*", result)
  }

  @Test
  fun escapeMarkdown_escapes_underscores() {
    val input = "_italic_ text"
    val result = escapeMarkdown(input)
    assertEquals("\\_italic\\_ text", result)
  }

  @Test
  fun escapeMarkdown_escapes_curly_braces() {
    val input = "{content}"
    val result = escapeMarkdown(input)
    assertEquals("\\{content\\}", result)
  }

  @Test
  fun escapeMarkdown_escapes_square_brackets() {
    val input = "[link]"
    val result = escapeMarkdown(input)
    assertEquals("\\[link\\]", result)
  }

  @Test
  fun escapeMarkdown_escapes_parentheses() {
    val input = "(url)"
    val result = escapeMarkdown(input)
    assertEquals("\\(url\\)", result)
  }

  @Test
  fun escapeMarkdown_escapes_hash() {
    val input = "# heading"
    val result = escapeMarkdown(input)
    assertEquals("\\# heading", result)
  }

  @Test
  fun escapeMarkdown_escapes_plus() {
    val input = "+ item"
    val result = escapeMarkdown(input)
    assertEquals("\\+ item", result)
  }

  @Test
  fun escapeMarkdown_escapes_dash() {
    val input = "- list item"
    val result = escapeMarkdown(input)
    assertEquals("\\- list item", result)
  }

  @Test
  fun escapeMarkdown_escapes_dot() {
    val input = "1. numbered"
    val result = escapeMarkdown(input)
    assertEquals("1\\. numbered", result)
  }

  @Test
  fun escapeMarkdown_escapes_exclamation() {
    val input = "![image](url)"
    val result = escapeMarkdown(input)
    assertEquals("\\!\\[image\\]\\(url\\)", result)
  }

  @Test
  fun escapeMarkdown_handles_empty_string() {
    val input = ""
    val result = escapeMarkdown(input)
    assertEquals("", result)
  }

  @Test
  fun escapeMarkdown_handles_plain_text() {
    val input = "Hello World"
    val result = escapeMarkdown(input)
    assertEquals("Hello World", result)
  }

  @Test
  fun escapeMarkdown_escapes_all_special_chars_together() {
    val input = "**bold** and _italic_ with [link](url)"
    val result = escapeMarkdown(input)
    assertEquals("\\*\\*bold\\*\\* and \\_italic\\_ with \\[link\\]\\(url\\)", result)
  }

  @Test
  fun escapeMarkdown_handles_multiline() {
    val input = "line1\n# heading\nline3"
    val result = escapeMarkdown(input)
    assertEquals("line1\n\\# heading\nline3", result)
  }

  @Test
  fun escapeMarkdown_handles_code_block_chars() {
    val input = "```kotlin\nval x = 1\n```"
    val result = escapeMarkdown(input)
    assertEquals("\\`\\`\\`kotlin\nval x = 1\n\\`\\`\\`", result)
  }

  @Test
  fun escapeMarkdown_handles_horizontal_rule() {
    val input = "text\n---\nmore text"
    val result = escapeMarkdown(input)
    assertEquals("text\n\\-\\-\\-\nmore text", result)
  }

  // ========== MarkdownText Composable Tests ==========

  @Test
  fun markdownText_renders_plain_text() {
    composeRule.setContent { SampleAppTheme { MarkdownText(markdown = "Hello World") } }

    composeRule.onNode(hasText("Hello World", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_bold_text() {
    composeRule.setContent { SampleAppTheme { MarkdownText(markdown = "This is **bold** text") } }

    composeRule.onNode(hasText("bold", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_italic_text() {
    composeRule.setContent { SampleAppTheme { MarkdownText(markdown = "This is *italic* text") } }

    composeRule.onNode(hasText("italic", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_bullet_list() {
    composeRule.setContent {
      SampleAppTheme {
        MarkdownText(
            markdown =
                """
                    - First item
                    - Second item
                    - Third item
                """
                    .trimIndent())
      }
    }

    composeRule.onNode(hasText("First item", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Second item", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Third item", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_numbered_list() {
    composeRule.setContent {
      SampleAppTheme {
        MarkdownText(
            markdown =
                """
                    1. Step one
                    2. Step two
                    3. Step three
                """
                    .trimIndent())
      }
    }

    composeRule.onNode(hasText("Step one", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Step two", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_heading() {
    composeRule.setContent {
      SampleAppTheme { MarkdownText(markdown = "## Section Title\nSome content here") }
    }

    composeRule.onNode(hasText("Section Title", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Some content here", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_inline_code() {
    composeRule.setContent {
      SampleAppTheme { MarkdownText(markdown = "Use `println()` to print") }
    }

    composeRule.onNode(hasText("println()", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_link_text() {
    composeRule.setContent {
      SampleAppTheme { MarkdownText(markdown = "Visit [EPFL](https://epfl.ch) for info") }
    }

    composeRule.onNode(hasText("EPFL", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("for info", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_complex_markdown() {
    val complexMarkdown =
        """
            ## Today's Schedule
            
            **Morning:**
            - 📚 *Algorithms* at **CM 1 1**
            - 📖 Analysis lecture
            
            **Afternoon:**
            1. Lab session
            2. Project meeting
            
            Check the [portal](https://is-academia.epfl.ch) for updates.
        """
            .trimIndent()

    composeRule.setContent { SampleAppTheme { MarkdownText(markdown = complexMarkdown) } }

    composeRule.onNode(hasText("Today's Schedule", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Morning", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Algorithms", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_handles_empty_string() {
    composeRule.setContent { SampleAppTheme { MarkdownText(markdown = "") } }

    // Should not crash, nothing to assert except no exception
    composeRule.waitForIdle()
  }

  @Test
  fun markdownText_handles_whitespace_only() {
    composeRule.setContent { SampleAppTheme { MarkdownText(markdown = "   \n\n   ") } }

    composeRule.waitForIdle()
  }

  @Test
  fun markdownText_renders_with_custom_color() {
    composeRule.setContent {
      SampleAppTheme { MarkdownText(markdown = "Colored text", color = Color.Red) }
    }

    composeRule.onNode(hasText("Colored text", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_emoji() {
    composeRule.setContent {
      SampleAppTheme { MarkdownText(markdown = "📅 Schedule for today 🎓") }
    }

    composeRule.onNode(hasText("Schedule for today", substring = true)).assertIsDisplayed()
  }

  @Test
  fun markdownText_renders_special_characters() {
    composeRule.setContent {
      SampleAppTheme { MarkdownText(markdown = "Price: 8.50 CHF (student rate)") }
    }

    composeRule.onNode(hasText("8.50 CHF", substring = true)).assertIsDisplayed()
  }

  // ========== ChatMessage with Markdown Tests ==========

  @Test
  fun chatMessage_aiMessage_usesMarkdownRendering() {
    val aiMessage =
        ChatUIModel(
            id = "ai-1",
            text = "**Important:** Check your schedule",
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = aiMessage) } }

    composeRule.onNodeWithTag("chat_ai_text").assertIsDisplayed()
    composeRule.onNode(hasText("Important", substring = true)).assertIsDisplayed()
  }

  @Test
  fun chatMessage_aiMessage_rendersBulletList() {
    val aiMessage =
        ChatUIModel(
            id = "ai-2",
            text =
                """
                Today's menu:
                - Pasta 8.50 CHF
                - Salad 6.00 CHF
                - Coffee 2.50 CHF
            """
                    .trimIndent(),
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = aiMessage) } }

    composeRule.onNode(hasText("Pasta", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Salad", substring = true)).assertIsDisplayed()
  }

  @Test
  fun chatMessage_aiMessage_rendersNumberedSteps() {
    val aiMessage =
        ChatUIModel(
            id = "ai-3",
            text =
                """
                To register for exchange:
                1. Check eligibility
                2. Submit application
                3. Wait for confirmation
            """
                    .trimIndent(),
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = aiMessage) } }

    composeRule.onNode(hasText("Check eligibility", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Submit application", substring = true)).assertIsDisplayed()
  }

  @Test
  fun chatMessage_userMessage_doesNotUseMarkdown() {
    val userMessage =
        ChatUIModel(
            id = "user-1",
            text = "**This should show as-is**",
            timestamp = System.currentTimeMillis(),
            type = ChatType.USER)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = userMessage) } }

    // User messages show raw text, not rendered markdown
    composeRule.onNodeWithTag("chat_user_text").assertIsDisplayed()
    composeRule.onNodeWithText("**This should show as-is**").assertIsDisplayed()
  }

  @Test
  fun chatMessage_aiMessage_streaming_showsCursor() {
    val aiMessage =
        ChatUIModel(
            id = "ai-4", text = "", timestamp = System.currentTimeMillis(), type = ChatType.AI)

    composeRule.setContent {
      SampleAppTheme { ChatMessage(message = aiMessage, isStreaming = true) }
    }

    composeRule.onNodeWithTag("chat_ai_cursor").assertIsDisplayed()
  }

  @Test
  fun chatMessage_aiMessage_streaming_withText_showsText() {
    val aiMessage =
        ChatUIModel(
            id = "ai-5",
            text = "Streaming content...",
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI)

    composeRule.setContent {
      SampleAppTheme { ChatMessage(message = aiMessage, isStreaming = true) }
    }

    composeRule.onNode(hasText("Streaming content", substring = true)).assertIsDisplayed()
    // Cursor should not show when there's text
    assertTrue(composeRule.onAllNodesWithTag("chat_ai_cursor").fetchSemanticsNodes().isEmpty())
  }

  @Test
  fun chatMessage_aiMessage_rendersEulerStyleResponse() {
    val aiMessage =
        ChatUIModel(
            id = "ai-6",
            text =
                """
                📅 **Votre emploi du temps aujourd'hui:**
                
                - **08:15** — *Algorithms* à CM 1 1
                - **10:15** — *Analysis II* à INR 113
                
                Consultez [IS-Academia](https://is-academia.epfl.ch) pour les détails.
            """
                    .trimIndent(),
            timestamp = System.currentTimeMillis(),
            type = ChatType.AI)

    composeRule.setContent { SampleAppTheme { ChatMessage(message = aiMessage) } }

    composeRule.onNode(hasText("Votre emploi du temps", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("Algorithms", substring = true)).assertIsDisplayed()
    composeRule.onNode(hasText("IS-Academia", substring = true)).assertIsDisplayed()
  }

  // ========== ChatUIModel Tests ==========

  @Test
  fun chatUIModel_hasCorrectFields() {
    val model =
        ChatUIModel(
            id = "test-id",
            text = "Test message",
            timestamp = 12345L,
            type = ChatType.AI,
            source = null)

    assertEquals("test-id", model.id)
    assertEquals("Test message", model.text)
    assertEquals(12345L, model.timestamp)
    assertEquals(ChatType.AI, model.type)
    assertEquals(null, model.source)
  }

  @Test
  fun chatType_hasCorrectValues() {
    assertEquals(2, ChatType.values().size)
    assertTrue(ChatType.values().contains(ChatType.USER))
    assertTrue(ChatType.values().contains(ChatType.AI))
  }

  @Test
  fun chatType_ordinals_areCorrect() {
    assertEquals(0, ChatType.USER.ordinal)
    assertEquals(1, ChatType.AI.ordinal)
  }

  @Test
  fun messageAudioState_hasCorrectFields() {
    var playCalled = false
    var stopCalled = false
    val state =
        MessageAudioState(
            isLoading = true,
            isPlaying = false,
            onPlay = { playCalled = true },
            onStop = { stopCalled = true })

    assertEquals(true, state.isLoading)
    assertEquals(false, state.isPlaying)

    state.onPlay()
    assertTrue(playCalled)

    state.onStop()
    assertTrue(stopCalled)
  }

  // Helper to get all nodes with a tag
  private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTag(tag: String) =
      onAllNodes(androidx.compose.ui.test.hasTestTag(tag))
}
