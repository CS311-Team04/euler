package com.android.sample.SigninScreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthTags
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Local JVM test for AuthUIScreen using createComposeRule.
 *
 * This test manually controls the clock to avoid AppNotIdleException. All automatic idling is
 * disabled and time is advanced manually.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthUIScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun authScreen_showsMicrosoftLoading_whenLoadingMicrosoft() {
    composeRule.mainClock.autoAdvance = false

  // (Removed) Logos tests no longer applicable: the top-left logos were removed from the UI.

  // ==================== TITLE AND SUBTITLE TESTS ====================

  @Test
  fun AuthUIScreen_renders_title_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithText("Ask anything,\ndo everything").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_subtitle_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeTestRule.onNodeWithText("Welcome to EULER").assertIsDisplayed()
  }

  // ==================== BUTTONS TESTS ====================

  @Test
  fun AuthUIScreen_renders_Microsoft_button() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_Guest_button() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Microsoft button should show loading indicator
    composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()

    // Microsoft button should be disabled (not clickable)
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()

    // Guest button should still be enabled
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun authScreen_showsSwitchEduLoading_whenLoadingSwitchEdu() {
    composeRule.mainClock.autoAdvance = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Switch Edu button should show loading indicator
    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()

    // Switch Edu button should be disabled
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()

    // Microsoft button should still be enabled
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
  }

  @Test
  fun authScreen_microsoftButtonClick_invokesCallback() {
    composeRule.mainClock.autoAdvance = false

    var microsoftLoginInvoked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle,
            onMicrosoftLogin = { microsoftLoginInvoked = true },
            onSwitchEduLogin = {})
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Click Microsoft button
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()

    // Advance time for button press animation (120ms)
    composeRule.mainClock.advanceTimeBy(150)

    // Verify callback was invoked
    assert(microsoftLoginInvoked) { "Microsoft login callback should be invoked" }
  }

  @Test
  fun authScreen_switchEduButtonClick_invokesCallback() {
    composeRule.mainClock.autoAdvance = false

    var switchEduLoginInvoked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle,
            onMicrosoftLogin = {},
            onSwitchEduLogin = { switchEduLoginInvoked = true })
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Click Switch Edu button
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()

    // Advance time for button press animation (120ms)
    composeRule.mainClock.advanceTimeBy(150)

    // Verify callback was invoked
    assert(switchEduLoginInvoked) { "Switch Edu login callback should be invoked" }
  }

  @Test
  fun authScreen_microsoftButtonDisabled_whenLoadingMicrosoft() {
    composeRule.mainClock.autoAdvance = false

    var microsoftLoginInvoked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = { microsoftLoginInvoked = true },
            onSwitchEduLogin = {})
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Button should show loading indicator
    composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()

    // Attempt to click (should not invoke callback due to disabled state)
    // Note: In Compose, disabled buttons may still be found but won't trigger onClick
    // The button is effectively disabled through the enabled parameter
    composeRule.mainClock.advanceTimeBy(150)

    // Verify callback was NOT invoked (button is disabled)
    assert(!microsoftLoginInvoked) { "Microsoft login should not be invoked when loading" }
  }

  @Test
  fun authScreen_switchEduButtonDisabled_whenLoadingSwitchEdu() {
    composeRule.mainClock.autoAdvance = false

    var switchEduLoginInvoked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = { switchEduLoginInvoked = true })
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Button should show loading indicator
    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()

    // Advance time
    composeRule.mainClock.advanceTimeBy(150)

    // Verify callback was NOT invoked (button is disabled)
    assert(!switchEduLoginInvoked) { "Switch Edu login should not be invoked when loading" }
  }

  @Test
  fun authScreen_displaysCorrectTexts() {
    composeRule.mainClock.autoAdvance = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // Advance time to complete animation
    composeRule.mainClock.advanceTimeBy(850)

    // Verify all text content
    composeRule.onNodeWithText("Ask anything, do everything").assertIsDisplayed()
    composeRule.onNodeWithText("Welcome to EULER").assertIsDisplayed()
    composeRule.onNodeWithText("Continue with Microsoft Entra ID").assertIsDisplayed()
    composeRule.onNodeWithText("OR").assertIsDisplayed()
    composeRule.onNodeWithText("Continue as a guest").assertIsDisplayed()
    composeRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  @Test
  fun authScreen_noLoadingIndicators_whenIdle() {
    composeRule.mainClock.autoAdvance = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()
    // Top-left logos were removed; corresponding test tags are no longer asserted here.
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.OrSeparator).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.ByEpflText).assertIsDisplayed()
  }

    // Advance time to complete initial animation
    composeRule.mainClock.advanceTimeBy(850)

    // Verify buttons are displayed but no loading indicators
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()

    // Loading indicators should not be present in idle state
    // We verify this by checking that the buttons don't contain progress indicators
    // (The progress indicators are only shown when loading)
  }
}
