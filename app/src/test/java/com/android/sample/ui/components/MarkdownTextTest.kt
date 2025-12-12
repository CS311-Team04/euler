package com.android.sample.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
  fun simpleMarkdownText_rendersPlainText() {
    composeRule.setContent { SimpleMarkdownText(text = "Simple text here") }

    composeRule.onNodeWithText("Simple text here").assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersBoldText() {
    composeRule.setContent { SimpleMarkdownText(text = "This is **bold** text") }

    // The text should be visible without the asterisks
    composeRule.onNodeWithText("bold", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersItalicText() {
    composeRule.setContent { SimpleMarkdownText(text = "This is *italic* text") }

    composeRule.onNodeWithText("italic", substring = true).assertIsDisplayed()
  }

  @Test
  fun simpleMarkdownText_rendersInlineCode() {
    composeRule.setContent { SimpleMarkdownText(text = "Use `code` here") }

    composeRule.onNodeWithText("code", substring = true).assertIsDisplayed()
  }

  @Test
  fun codeBlock_rendersCodeContent() {
    composeRule.setContent { CodeBlock(code = "val x = 42", language = "kotlin") }

    composeRule.onNodeWithText("val x = 42").assertIsDisplayed()
    composeRule.onNodeWithText("KOTLIN").assertIsDisplayed()
  }
}
