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

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun renders_core_elements() {
    composeTestRule.setContent { MaterialTheme { DrawerContent() } }
    composeTestRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()
    composeTestRule.onNodeWithText("New chat").assertIsDisplayed()
    composeTestRule.onNodeWithText("Connectors").assertIsDisplayed()
  }

  @Test
  fun connectors_click_invokes_settings_callback() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { DrawerContent(onSettingsClick = { clicked = true }) }
    }
    composeTestRule.onNodeWithTag(DrawerTags.ConnectorsRow).performClick()
    assertTrue(clicked)
  }

  @Test
  fun user_settings_invokes_callback() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { DrawerContent(onSettingsClick = { clicked = true }) }
    }
    composeTestRule.onNodeWithTag(DrawerTags.UserSettings).performClick()
    assertTrue(clicked)
  }
}
