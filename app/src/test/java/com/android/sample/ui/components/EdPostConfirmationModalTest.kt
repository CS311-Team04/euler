package com.android.sample.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EdPostConfirmationModalTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun EdPostConfirmationModal_displays_and_allows_editing() {
    var publishTitle = ""
    var publishBody = ""

    composeRule.setContent {
      EdPostConfirmationModal(
          title = "Initial Title",
          body = "Initial Body",
          onPublish = { title, body ->
            publishTitle = title
            publishBody = body
          },
          onCancel = {})
    }

    composeRule.onNodeWithText("Initial Title").assertIsDisplayed()
    composeRule.onNode(hasText("Initial Title")).performTextReplacement("Edited Title")
    composeRule.onNode(hasText("Initial Body")).performTextReplacement("Edited Body")
    composeRule.onNodeWithText("Post").performClick()

    composeRule.runOnIdle {
      assertEquals("Edited Title", publishTitle)
      assertEquals("Edited Body", publishBody)
    }
  }

  @Test
  fun EdPostConfirmationModal_calls_onCancel_when_cancel_clicked() {
    var cancelCalled = false

    composeRule.setContent {
      EdPostConfirmationModal(
          title = "Title",
          body = "Body",
          onPublish = { _, _ -> },
          onCancel = { cancelCalled = true })
    }

    composeRule.onNodeWithText("Cancel").performClick()

    composeRule.runOnIdle { assertTrue(cancelCalled) }
  }

  @Test
  fun EdPostConfirmationModal_handles_backslash_n_in_title() {
    composeRule.setContent {
      EdPostConfirmationModal(
          title = "Title\\nWith\\nNewlines", body = "Body", onPublish = { _, _ -> }, onCancel = {})
    }

    // The text should be displayed (the component replaces \n internally)
    // After replacement, it becomes "Title\nWith\nNewlines" (actual newlines)
    composeRule.onNode(hasText("Title", substring = true)).assertIsDisplayed()
  }

  @Test
  fun EdPostConfirmationModal_displays_initial_title() {
    composeRule.setContent {
      EdPostConfirmationModal(
          title = "Title 1", body = "Body", onPublish = { _, _ -> }, onCancel = {})
    }

    composeRule.onNode(hasText("Title 1")).assertIsDisplayed()
  }

  @Test
  fun EdPostConfirmationModal_displays_initial_body() {
    composeRule.setContent {
      EdPostConfirmationModal(
          title = "Title", body = "Body 1", onPublish = { _, _ -> }, onCancel = {})
    }

    composeRule.onNode(hasText("Body 1")).assertIsDisplayed()
  }
}
