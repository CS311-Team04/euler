package com.android.sample.settings

// import android.os.Looper // <-- FIX: No longer needed
// import org.robolectric.Shadows.shadowOf // <-- FIX: No longer needed
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class AAASettingsPageTest {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    AppSettings.setDispatcher(testDispatcher)
    AppSettings.clearForTests()
    context = ApplicationProvider.getApplicationContext()
    AppSettings.initialize(context)
    // Reset to English for consistent tests
    AppSettings.setLanguage(Language.EN)
  }

  @After
  fun tearDown() {
    AppSettings.resetDispatcher()
    AppSettings.clearForTests()
    Dispatchers.resetMain()
  }

  @Test
  fun renders_all_core_elements() {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }
    composeTestRule.waitForIdle() // This is the only line we need to wait.

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
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithContentDescription(Localization.t("close")).performClick()
    composeTestRule.waitForIdle() // Let the click event finish

    composeTestRule.onNodeWithContentDescription(Localization.t("info")).performClick()
    composeTestRule.waitForIdle() // Let the click event finish

    assertTrue(backClicked)
    assertTrue(infoClicked)
  }

  @Test
  fun language_dropdown_allows_selection() = runTest {
    composeTestRule.setContent { MaterialTheme { SettingsPage() } }
    composeTestRule.waitForIdle()
    advanceUntilIdle()

    composeTestRule.onNodeWithText(Localization.t("speech_language")).performClick()
    composeTestRule.waitForIdle()
    advanceUntilIdle()

    composeTestRule.onNodeWithText("FR").performClick()
    composeTestRule.waitForIdle()
    advanceUntilIdle()

    composeTestRule.onNodeWithText("FR").assertIsDisplayed()
  }

  @Test
  fun logout_invokes_callback() {
    var signOutClicked = false

    composeTestRule.setContent {
      MaterialTheme { SettingsPage(onSignOut = { signOutClicked = true }) }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText(Localization.t("log_out")).performClick()
    composeTestRule.waitForIdle()

    assertTrue(signOutClicked)
  }
}
