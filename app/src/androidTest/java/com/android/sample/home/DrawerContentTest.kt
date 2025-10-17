package com.android.sample.home

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrawerContentTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun shows_sections_and_triggers_callbacks() {
    var toggled = false
    var settingsClicked = false
    var signOutClicked = false

    composeRule.setContent {
      MaterialTheme {
        DrawerContent(
            onToggleSystem = { toggled = true },
            onSettingsClick = { settingsClicked = true },
            onSignOut = { signOutClicked = true })
      }
    }

    // Sections
    composeRule.onNodeWithText("CONNECTED SYSTEMS").assertIsDisplayed()
    composeRule.onNodeWithText("RECENT ACTIONS").assertIsDisplayed()
    composeRule.onNodeWithText("Settings").assertIsDisplayed()

    // Click any system label to trigger toggle
    composeRule.onNodeWithText("Moodle").performClick()
    assertTrue(toggled)

    // Click Settings
    composeRule.onNodeWithText("Settings").performClick()
    assertTrue(settingsClicked)

    // Click Sign Out
    composeRule.onNodeWithText("Sign Out").performClick()
    assertTrue(signOutClicked)
  }
}
