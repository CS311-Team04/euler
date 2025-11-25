package com.android.sample.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.conversations.Conversation
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DrawerContentTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun renders_core_elements() {
    val uiState =
        HomeUiState(
            userName = "Student",
            conversations =
                listOf(
                    Conversation(id = "1", title = "CS220 Final Exam retrieval"),
                    Conversation(id = "2", title = "Linear Algebra help"),
                    Conversation(id = "3", title = "Project deadline query"),
                    Conversation(id = "4", title = "Course registration info"),
                    Conversation(id = "5", title = "Extra conversation"),
                ))
    composeRule.setContent { MaterialTheme { DrawerContent(ui = uiState) } }

    composeRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()

    // Primary rows
    composeRule.onNodeWithText("New chat").assertIsDisplayed()
    composeRule.onNodeWithText("Connectors").assertIsDisplayed()

    // Recents / All chats section et items visibles
    composeRule.onNodeWithTag(DrawerTags.RecentsSection).assertIsDisplayed()
    composeRule.onNodeWithText("CS220 Final Exam retrieval").assertIsDisplayed()
    composeRule.onNodeWithText("Linear Algebra help").assertIsDisplayed()
    composeRule.onNodeWithText("Project deadline query").assertIsDisplayed()
    composeRule.onNodeWithText("Course registration info").assertIsDisplayed()

    composeRule.onNodeWithTag(DrawerTags.ViewAllRow).assertIsDisplayed()
  }

  @Test
  fun connectors_click_invokes_settings_callback() {
    var settingsClicked = false
    composeRule.setContent {
      MaterialTheme { DrawerContent(onSettingsClick = { settingsClicked = true }) }
    }
    composeRule.onNodeWithTag(DrawerTags.ConnectorsRow).performClick()
    assertTrue(settingsClicked)
  }

  @Test
  fun user_settings_invokes_settings_callback() {
    var settingsClicked = false
    composeRule.setContent {
      MaterialTheme { DrawerContent(onSettingsClick = { settingsClicked = true }) }
    }
    composeRule.onNodeWithTag(DrawerTags.UserSettings).performClick()
    assertTrue(settingsClicked)
  }

  @Test
  fun long_press_triggers_selection_mode() {
    val uiState =
        HomeUiState(
            userName = "Student",
            isDrawerOpen = true, // Important: drawer must be open for selection mode to work
            conversations =
                listOf(
                    Conversation(id = "1", title = "CS220 Final Exam retrieval"),
                    Conversation(id = "2", title = "Linear Algebra help"),
                ))
    // Use test helper to directly trigger long press
    composeRule.setContent {
      MaterialTheme { DrawerContent(ui = uiState, testLongPressConversationId = "1") }
    }
    composeRule.waitForIdle()

    // Selection mode should be active: delete and cancel buttons should appear
    composeRule.onNodeWithTag(DrawerTags.DeleteButton).assertIsDisplayed()
    composeRule.onNodeWithTag(DrawerTags.CancelButton).assertIsDisplayed()
  }

  @Test
  fun clicking_items_toggles_selection_in_selection_mode() {
    val uiState =
        HomeUiState(
            userName = "Student",
            isDrawerOpen = true,
            conversations =
                listOf(
                    Conversation(id = "1", title = "CS220 Final Exam retrieval"),
                    Conversation(id = "2", title = "Linear Algebra help"),
                    Conversation(id = "3", title = "Project deadline query"),
                ))
    // Use test helper to directly trigger long press on first item
    composeRule.setContent {
      MaterialTheme { DrawerContent(ui = uiState, testLongPressConversationId = "1") }
    }
    composeRule.waitForIdle()

    // Delete button should show count
    composeRule.onNodeWithText("Delete (1)").assertIsDisplayed()

    // Click another item to select it
    val conversationRows = composeRule.onAllNodesWithTag(DrawerTags.ConversationRow)
    conversationRows[1].performClick()
    composeRule.waitForIdle()

    // Delete button should show updated count
    composeRule.onNodeWithText("Delete (2)").assertIsDisplayed()

    // Click first item again to deselect it
    conversationRows[0].performClick()
    composeRule.waitForIdle()

    // Delete button should show updated count
    composeRule.onNodeWithText("Delete (1)").assertIsDisplayed()
  }

  @Test
  fun delete_button_removes_selected_conversations() {
    val uiState =
        HomeUiState(
            userName = "Student",
            isDrawerOpen = true,
            conversations =
                listOf(
                    Conversation(id = "1", title = "CS220 Final Exam retrieval"),
                    Conversation(id = "2", title = "Linear Algebra help"),
                    Conversation(id = "3", title = "Project deadline query"),
                ))
    var deletedIds: List<String>? = null
    // Use test helper to directly trigger long press on first item
    composeRule.setContent {
      MaterialTheme {
        DrawerContent(
            ui = uiState,
            onDeleteConversations = { ids -> deletedIds = ids },
            testLongPressConversationId = "1")
      }
    }
    composeRule.waitForIdle()

    // Select another item
    val conversationRows = composeRule.onAllNodesWithTag(DrawerTags.ConversationRow)
    conversationRows[1].performClick()
    composeRule.waitForIdle()

    // Click delete button
    composeRule.onNodeWithTag(DrawerTags.DeleteButton).performClick()
    composeRule.waitForIdle()

    // Verify callback was called with correct IDs
    assertTrue("Delete callback should be called", deletedIds != null)
    assertTrue("Should delete 2 conversations", deletedIds!!.size == 2)
    assertTrue("Should include first conversation", deletedIds!!.contains("1"))
    assertTrue("Should include second conversation", deletedIds!!.contains("2"))
  }

  @Test
  fun cancel_button_exits_selection_mode() {
    val uiState =
        HomeUiState(
            userName = "Student",
            isDrawerOpen = true,
            conversations =
                listOf(
                    Conversation(id = "1", title = "CS220 Final Exam retrieval"),
                    Conversation(id = "2", title = "Linear Algebra help"),
                ))
    // Use test helper to directly trigger long press
    composeRule.setContent {
      MaterialTheme { DrawerContent(ui = uiState, testLongPressConversationId = "1") }
    }
    composeRule.waitForIdle()

    // Verify selection mode is active
    composeRule.onNodeWithTag(DrawerTags.DeleteButton).assertIsDisplayed()
    composeRule.onNodeWithTag(DrawerTags.CancelButton).assertIsDisplayed()

    // Click cancel
    composeRule.onNodeWithTag(DrawerTags.CancelButton).performClick()
    composeRule.waitForIdle()

    // Selection mode should be exited
    composeRule.onNodeWithTag(DrawerTags.DeleteButton).assertIsNotDisplayed()
    composeRule.onNodeWithTag(DrawerTags.CancelButton).assertIsNotDisplayed()
  }

  @Test
  fun selection_mode_exits_when_all_items_deselected() {
    val uiState =
        HomeUiState(
            userName = "Student",
            isDrawerOpen = true,
            conversations =
                listOf(
                    Conversation(id = "1", title = "CS220 Final Exam retrieval"),
                ))
    // Use test helper to directly trigger long press
    composeRule.setContent {
      MaterialTheme { DrawerContent(ui = uiState, testLongPressConversationId = "1") }
    }
    composeRule.waitForIdle()

    // Verify selection mode is active
    composeRule.onNodeWithTag(DrawerTags.DeleteButton).assertIsDisplayed()

    // Deselect the item
    val conversationRow = composeRule.onAllNodesWithTag(DrawerTags.ConversationRow)[0]
    conversationRow.performClick()
    composeRule.waitForIdle()

    // Selection mode should be exited automatically
    composeRule.onNodeWithTag(DrawerTags.DeleteButton).assertIsNotDisplayed()
    composeRule.onNodeWithTag(DrawerTags.CancelButton).assertIsNotDisplayed()
  }
}
