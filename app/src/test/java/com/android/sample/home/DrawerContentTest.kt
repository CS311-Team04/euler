package com.android.sample.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    composeRule.setContent { MaterialTheme { DrawerContent() } }

    composeRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()

    // Primary rows
    composeRule.onNodeWithText("New chat").assertIsDisplayed()
    composeRule.onNodeWithText("Connectors").assertIsDisplayed()

    // Recents section and sample items
    composeRule.onNodeWithTag(DrawerTags.RecentsSection).assertIsDisplayed()
    composeRule.onNodeWithText("CS220 Final Exam retrieval").assertIsDisplayed()
    composeRule.onNodeWithText("Linear Algebra help").assertIsDisplayed()
    composeRule.onNodeWithText("Project deadline query").assertIsDisplayed()
    composeRule.onNodeWithText("Course registration info").assertIsDisplayed()

    // Footer link
    composeRule.onNodeWithText("View all chats").assertIsDisplayed()
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
}
