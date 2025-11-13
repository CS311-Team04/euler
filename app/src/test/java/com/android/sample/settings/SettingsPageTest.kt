package com.android.sample.settings

import android.content.Context
import android.os.Looper
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsPageTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    AppSettings.setDispatcher(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    AppSettings.initialize(context)
    // Reset to English for consistent tests
    AppSettings.setLanguage(Language.EN)
    // Flush any pending looper tasks from initialization
    shadowOf(Looper.getMainLooper()).idle()
  }

  @After
  fun tearDown() {
    AppSettings.resetDispatcher()
    Dispatchers.resetMain()
  }

  // Helper to idle the main looper in PAUSED mode
  private fun idleLooper() {
    shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun renders_all_core_elements() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }
    idleLooper()
    composeTestRule.waitForIdle()

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
    idleLooper()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription(Localization.t("close")).performClick()
    idleLooper()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithContentDescription(Localization.t("info")).performClick()
    idleLooper()
    composeTestRule.waitForIdle()

    assertTrue(backClicked)
    assertTrue(infoClicked)
  }

  @Test
  fun language_dropdown_allows_selection() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(Localization.t("speech_language")).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FR").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FR").assertIsDisplayed()
  }

  @Test
  fun logout_invokes_callback() {
    var signOutClicked = false

    composeTestRule.setContent {
      MaterialTheme { SettingsPage(onSignOut = { signOutClicked = true }) }
    }
    idleLooper()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(Localization.t("log_out")).performClick()
    idleLooper()
    composeTestRule.waitForIdle()

    assertTrue(signOutClicked)
  }
}
