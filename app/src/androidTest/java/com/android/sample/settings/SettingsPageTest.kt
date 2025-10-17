package com.android.sample.settings

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
class SettingsPageTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_items_and_callbacks_work() {
    var backClicked = false
    var signOutClicked = false

    composeRule.setContent {
      MaterialTheme {
        SettingsPage(onBackClick = { backClicked = true }, onSignOut = { signOutClicked = true })
      }
    }

    // Header and a couple of options
    composeRule.onNodeWithText("Settings").assertIsDisplayed()
    composeRule.onNodeWithText("Profile & Account").assertIsDisplayed()
    composeRule.onNodeWithText("Notifications").assertIsDisplayed()

    // Interactions
    composeRule.onNodeWithText("Back").performClick()
    assertTrue(backClicked)

    composeRule.onNodeWithText("Sign Out").performClick()
    assertTrue(signOutClicked)
  }
}
