package com.android.sample.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for the MarkdownText composable. Tests basic markdown rendering functionality. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MarkdownTextTest {

  @get:Rule val composeRule = createComposeRule()

  // ==================== MarkdownText Tests ====================

  @Test
  fun markdownText_rendersPlainText() {
    composeRule.setContent { MarkdownText(markdown = "Hello World") }

    composeRule.onNodeWithText("Hello World", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersBoldText() {
    composeRule.setContent { MarkdownText(markdown = "This is **bold** text") }

    // Bold text should be visible (the text will render without the asterisks)
    composeRule.onNodeWithText("bold", substring = true, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersBulletList() {
    composeRule.setContent { MarkdownText(markdown = "- Item 1\n- Item 2\n- Item 3") }

    composeRule
        .onNodeWithText("Item 1", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithText("Item 2", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersItalicText() {
    composeRule.setContent { MarkdownText(markdown = "This is *italic* text") }

    composeRule
        .onNodeWithText("italic", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersCodeBlock() {
    composeRule.setContent { MarkdownText(markdown = "```kotlin\nval x = 42\n```") }

    composeRule
        .onNodeWithText("val x = 42", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersHeaders() {
    composeRule.setContent { MarkdownText(markdown = "# Header 1\n## Header 2") }

    composeRule
        .onNodeWithText("Header 1", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithText("Header 2", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersNumberedList() {
    composeRule.setContent { MarkdownText(markdown = "1. First\n2. Second\n3. Third") }

    composeRule
        .onNodeWithText("First", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
    composeRule
        .onNodeWithText("Second", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersBlockquote() {
    composeRule.setContent { MarkdownText(markdown = "> This is a quote") }

    composeRule
        .onNodeWithText("This is a quote", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersLink() {
    composeRule.setContent { MarkdownText(markdown = "Click [here](https://example.com)") }

    composeRule.onNodeWithText("here", substring = true, useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersInlineCode() {
    composeRule.setContent { MarkdownText(markdown = "Use `inline code` here") }

    composeRule
        .onNodeWithText("inline code", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun markdownText_rendersEmptyString() {
    composeRule.setContent { MarkdownText(markdown = "") }

    // Should not crash with empty string
  }

  @Test
  fun markdownText_rendersMixedContent() {
    composeRule.setContent {
      MarkdownText(markdown = "**Bold** and *italic* with `code` and a [link](url)")
    }

    composeRule.onNodeWithText("Bold", substring = true, useUnmergedTree = true).assertIsDisplayed()
    composeRule
        .onNodeWithText("italic", substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  // ==================== SimpleMarkdownText Tests ====================

  @Test
  fun simpleMarkdownText_rendersPlainText() {
    composeRule.setContent { SimpleMarkdownText(text = "Simple text here") }

    composeRule.onNodeWithText("Simple text here").assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersBoldWithAsterisks() {
    composeRule.setContent { SimpleMarkdownText(text = "This is **bold** text") }

    composeRule.onNodeWithText("bold", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersBoldWithUnderscores() {
    composeRule.setContent { SimpleMarkdownText(text = "This is __bold__ text") }

    composeRule.onNodeWithText("bold", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersItalicWithAsterisk() {
    composeRule.setContent { SimpleMarkdownText(text = "This is *italic* text") }

    composeRule.onNodeWithText("italic", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersItalicWithUnderscore() {
    composeRule.setContent { SimpleMarkdownText(text = "This is _italic_ text") }

    composeRule.onNodeWithText("italic", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersInlineCode() {
    composeRule.setContent { SimpleMarkdownText(text = "Use `code` here") }

    composeRule.onNodeWithText("code", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersLink() {
    composeRule.setContent { SimpleMarkdownText(text = "Click [here](https://example.com)") }

    composeRule.onNodeWithText("here", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_unclosedBoldAsterisks() {
    // Unclosed **bold should render the asterisks
    composeRule.setContent { SimpleMarkdownText(text = "This is **unclosed bold") }

    composeRule.onNodeWithText("*", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_unclosedBoldUnderscores() {
    // Unclosed __bold should render the underscores
    composeRule.setContent { SimpleMarkdownText(text = "This is __unclosed bold") }

    composeRule.onNodeWithText("_", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_unclosedItalicAsterisk() {
    composeRule.setContent { SimpleMarkdownText(text = "This is *unclosed italic") }

    composeRule.onNodeWithText("*", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_unclosedItalicUnderscore() {
    composeRule.setContent { SimpleMarkdownText(text = "This is _unclosed italic") }

    composeRule.onNodeWithText("_", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_unclosedInlineCode() {
    composeRule.setContent { SimpleMarkdownText(text = "This is `unclosed code") }

    composeRule.onNodeWithText("`", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_malformedLink() {
    // Malformed link should render as plain text
    composeRule.setContent { SimpleMarkdownText(text = "Click [here](broken link") }

    composeRule.onNodeWithText("[here]", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_linkWithoutUrl() {
    composeRule.setContent { SimpleMarkdownText(text = "Click [here] without url") }

    composeRule.onNodeWithText("[here]", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_emptyString() {
    composeRule.setContent { SimpleMarkdownText(text = "") }

    // Should not crash with empty string
  }

  @Test
  fun simpleMarkdownText_mixedFormatting() {
    composeRule.setContent {
      SimpleMarkdownText(text = "**Bold** and *italic* and `code` and [link](url)")
    }

    composeRule.onNodeWithText("Bold", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("italic", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("code", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("link", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_tripleBackticksNotProcessedAsCode() {
    // Triple backticks should not be processed by SimpleMarkdownText
    composeRule.setContent { SimpleMarkdownText(text = "```code block```") }

    // Should still display something (triple backticks aren't parsed)
    composeRule.onNodeWithText("code block", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_customTextColor() {
    composeRule.setContent { SimpleMarkdownText(text = "Colored text", textColor = Color.Red) }

    composeRule.onNodeWithText("Colored text").assertIsDisplayed()
  }

  // ==================== CodeBlock Tests ====================

  @Test
  fun codeBlock_rendersCodeContent() {
    composeRule.setContent { CodeBlock(code = "val x = 42", language = "kotlin") }

    composeRule.onNodeWithText("val x = 42").assertIsDisplayed()
    composeRule.onNodeWithText("KOTLIN").assertIsDisplayed()
  }

  @Test
  fun codeBlock_rendersWithoutLanguage() {
    composeRule.setContent { CodeBlock(code = "some code", language = null) }

    composeRule.onNodeWithText("some code").assertIsDisplayed()
  }

  @Test
  fun codeBlock_rendersWithBlankLanguage() {
    composeRule.setContent { CodeBlock(code = "some code", language = "") }

    composeRule.onNodeWithText("some code").assertIsDisplayed()
  }

  @Test
  fun codeBlock_multilineCode() {
    composeRule.setContent { CodeBlock(code = "line1\nline2\nline3", language = "text") }

    composeRule.onNodeWithText("line1", substring = true).assertIsDisplayed()
  }

  @Test
  fun codeBlock_emptyCode() {
    composeRule.setContent { CodeBlock(code = "", language = "kotlin") }

    composeRule.onNodeWithText("KOTLIN").assertIsDisplayed()
  }

  // ==================== BlockQuote Tests ====================

  @Test
  fun blockQuote_rendersContent() {
    composeRule.setContent { BlockQuote(content = { Text("Quote content") }) }

    composeRule.onNodeWithText("Quote content").assertIsDisplayed()
  }

  @Test
  fun blockQuote_rendersNestedContent() {
    composeRule.setContent {
      BlockQuote(
          content = {
            Text("First line")
            Text("Second line")
          })
    }

    composeRule.onNodeWithText("First line").assertIsDisplayed()
  }

  // ==================== BulletPoint Tests ====================

  @Test
  fun bulletPoint_rendersContent() {
    composeRule.setContent { BulletPoint(content = { Text("Bullet item") }) }

    composeRule.onNodeWithText("Bullet item").assertIsDisplayed()
  }

  @Test
  fun bulletPoint_customBulletColor() {
    composeRule.setContent {
      BulletPoint(content = { Text("Blue bullet") }, bulletColor = Color.Blue)
    }

    composeRule.onNodeWithText("Blue bullet").assertIsDisplayed()
  }

  // ==================== NumberedListItem Tests ====================

  @Test
  fun numberedListItem_rendersNumberAndContent() {
    composeRule.setContent { NumberedListItem(number = 1, content = { Text("First item") }) }

    composeRule.onNodeWithText("1.").assertIsDisplayed()
    composeRule.onNodeWithText("First item").assertIsDisplayed()
  }

  @Test
  fun numberedListItem_differentNumbers() {
    composeRule.setContent { NumberedListItem(number = 42, content = { Text("Item 42") }) }

    composeRule.onNodeWithText("42.").assertIsDisplayed()
    composeRule.onNodeWithText("Item 42").assertIsDisplayed()
  }

  @Test
  fun numberedListItem_customNumberColor() {
    composeRule.setContent {
      NumberedListItem(number = 1, content = { Text("Green number") }, numberColor = Color.Green)
    }

    composeRule.onNodeWithText("1.").assertIsDisplayed()
  }

  // ==================== MarkdownDivider Tests ====================

  @Test
  fun markdownDivider_renders() {
    composeRule.setContent { MarkdownDivider() }

    // Divider should render without crashing
  }

  @Test
  fun markdownDivider_customColor() {
    composeRule.setContent { MarkdownDivider(color = Color.Red) }

    // Should render without crashing
  }

  // ==================== MarkdownStyles Tests ====================

  @Test
  fun markdownStyles_hasCorrectCodeBlockBackground() {
    assertEquals(Color(0xFF1E1E1E), MarkdownStyles.codeBlockBackground)
  }

  @Test
  fun markdownStyles_hasCorrectCodeBlockBorder() {
    assertEquals(Color(0xFF3C3C3C), MarkdownStyles.codeBlockBorder)
  }

  @Test
  fun markdownStyles_hasCorrectCodeTextColor() {
    assertEquals(Color(0xFFE6E6E6), MarkdownStyles.codeTextColor)
  }

  @Test
  fun markdownStyles_hasCorrectInlineCodeBackground() {
    assertEquals(Color(0xFF2D2D2D), MarkdownStyles.inlineCodeBackground)
  }

  @Test
  fun markdownStyles_hasCorrectLinkColor() {
    assertEquals(Color(0xFF6B9FFF), MarkdownStyles.linkColor)
  }

  @Test
  fun markdownStyles_hasCorrectHeadingColor() {
    assertEquals(Color(0xFFFFFFFF), MarkdownStyles.headingColor)
  }

  @Test
  fun markdownStyles_hasCorrectBlockquoteBackground() {
    assertEquals(Color(0xFF1A1A1A), MarkdownStyles.blockquoteBackground)
  }

  @Test
  fun markdownStyles_hasCorrectTableBorderColor() {
    assertEquals(Color(0xFF3C3C3C), MarkdownStyles.tableBorderColor)
  }

  @Test
  fun markdownStyles_hasCorrectTableHeaderBackground() {
    assertEquals(Color(0xFF2A2A2A), MarkdownStyles.tableHeaderBackground)
  }

  @Test
  fun markdownStyles_bulletColorIsNotNull() {
    assertNotNull(MarkdownStyles.bulletColor)
  }

  @Test
  fun markdownStyles_blockquoteBorderColorIsNotNull() {
    assertNotNull(MarkdownStyles.blockquoteBorderColor)
  }
}
