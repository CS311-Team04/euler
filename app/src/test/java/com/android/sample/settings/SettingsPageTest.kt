package com.android.sample.settings

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsPageTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    AppSettings.initialize(context)
    Thread.sleep(100)
    // Reset to English for consistent tests
    AppSettings.setLanguage(Language.EN)
    Thread.sleep(100)
  }

  @Test
  fun renders_all_core_elements() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }

    // Title
    composeTestRule.onNodeWithText(Localization.t("settings_title")).assertIsDisplayed()

    // Rows
    composeTestRule.onNodeWithText(Localization.t("profile")).assertIsDisplayed()
    composeTestRule.onNodeWithText(Localization.t("connectors")).assertIsDisplayed()
    composeTestRule.onNodeWithText(Localization.t("speech_language")).assertIsDisplayed()

    // Footer
    composeTestRule.onNodeWithText(Localization.t("by_epfl")).assertIsDisplayed()
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

    composeTestRule.onNodeWithContentDescription(Localization.t("close")).performClick()
    composeTestRule.onNodeWithContentDescription(Localization.t("info")).performClick()

    assertTrue(backClicked)
    assertTrue(infoClicked)
  }

  @Test
  fun language_dropdown_allows_selection() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }

    composeTestRule.onNodeWithText(Localization.t("speech_language")).performClick()
    composeTestRule.onNodeWithText("FR").performClick()
    composeTestRule.onNodeWithText("FR").assertIsDisplayed()
  }

  @Test
  fun logout_invokes_callback() {
    var signOutClicked = false

    composeTestRule.setContent {
      MaterialTheme { SettingsPage(onSignOut = { signOutClicked = true }) }
    }

    composeTestRule.onNodeWithText(Localization.t("log_out")).performClick()

    assertTrue(signOutClicked)
  }
}
