package com.android.sample.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class SettingsPageTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun renders_all_core_elements() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }

    // Title
    composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

    // Rows
    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    composeTestRule.onNodeWithText("Connectors").assertIsDisplayed()
    composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
    composeTestRule.onNodeWithText("Speech language").assertIsDisplayed()

    // Footer
    composeTestRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  @Test
  fun topBar_buttons_invoke_callbacks() {
    var backClicked = false
    var infoClicked = false

    composeTestRule.setContent {
      MaterialTheme {
        SettingsPage(onBackClick = { backClicked = true }, onInfoClick = { infoClicked = true })
      }
    }

    composeTestRule.onNodeWithContentDescription("Close").performClick()
    composeTestRule.onNodeWithContentDescription("Info").performClick()

    assertTrue(backClicked)
    assertTrue(infoClicked)
  }

  @Test
  fun appearance_dropdown_allows_selection() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }

    // Open dropdown by tapping the row
    composeTestRule.onNodeWithText("Appearance").performClick()

    // Select a value and verify trailing text updates
    composeTestRule.onNodeWithText("Light").performClick()
    composeTestRule.onNodeWithText("Light").assertIsDisplayed()
  }

  @Test
  fun language_dropdown_allows_selection() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }

    composeTestRule.onNodeWithText("Speech language").performClick()
    composeTestRule.onNodeWithText("FR").performClick()
    composeTestRule.onNodeWithText("FR").assertIsDisplayed()
  }

  @Test
  fun logout_invokes_callback() {
    var signOutClicked = false

    composeTestRule.setContent {
      MaterialTheme { SettingsPage(onSignOut = { signOutClicked = true }) }
    }

    composeTestRule.onNodeWithText("Log out").performClick()

    assertTrue(signOutClicked)
  }
}
