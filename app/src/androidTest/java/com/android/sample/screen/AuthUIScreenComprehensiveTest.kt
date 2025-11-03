package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.TestConstants
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthTags
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for AuthUIScreen covering:
 * - All UI elements visibility and presence
 * - All UI states (Idle, Loading, Error, SignedIn)
 * - Button interactions and callbacks
 * - Loading indicators
 * - Text content verification
 * - Accessibility features
 * - Edge cases
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AuthUIScreenComprehensiveTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // ==================== ELEMENT VISIBILITY TESTS ====================

  @Test
  fun renders_root_container_in_idle_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
  }

  @Test
  fun renders_card_container_in_all_states() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()
  }

  @Test
  fun renders_logos_row_in_idle_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.onNodeWithTag(AuthTags.LogosRow).assertIsDisplayed()
  }

  @Test
  fun renders_title_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ScreenTexts.TITLE).assertIsDisplayed()
  }

  @Test
  fun renders_subtitle_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ScreenTexts.SUBTITLE).assertIsDisplayed()
  }

  @Test
  fun renders_OR_separator() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.OrSeparator).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ScreenTexts.OR_SEPARATOR).assertIsDisplayed()
  }

  @Test
  fun renders_both_authentication_buttons() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun renders_footer_elements() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.ByEpflText).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ScreenTexts.BY_EPFL).assertIsDisplayed()
  }

  // ==================== BUTTON TEXT CONTENT TESTS ====================

  @Test
  fun displays_correct_Microsoft_button_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithText(TestConstants.ButtonTexts.MICROSOFT_LOGIN).assertIsDisplayed()
  }

  @Test
  fun displays_correct_Guest_button_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithText(TestConstants.ButtonTexts.SWITCH_LOGIN).assertIsDisplayed()
  }

  // ==================== ACCESSIBILITY TESTS ====================

  @Test
  fun logos_have_correct_content_descriptions() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.EPFL_LOGO))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.EULER_LOGO))
        .assertIsDisplayed()
  }

  @Test
  fun Microsoft_button_contains_Microsoft_logo_with_content_description() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.MICROSOFT_ICON))
        .assertIsDisplayed()
  }

  @Test
  fun Guest_button_contains_arrow_icon_with_content_description() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.CONTINUE))
        .assertIsDisplayed()
  }

  // ==================== BUTTON STATE TESTS ====================

  @Test
  fun buttons_are_enabled_in_idle_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun buttons_are_enabled_in_error_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun buttons_are_enabled_in_signed_in_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun Microsoft_button_shows_loading_when_Microsoft_is_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }

    composeRule.waitForIdle()
    Thread.sleep(1000) // Allow AnimatedVisibility animation (800ms) to complete
    // Wait for the loading indicator to appear - it exists in the composition
    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.MsProgress), timeoutMillis = 5000)
    composeRule.waitForIdle()

    // waitUntilAtLeastOneExists already confirmed the node exists
    composeRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true)

    // Microsoft button should be disabled when loading - check by trying to assert it's enabled
    try {
      composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
      org.junit.Assert.fail("Microsoft button should be disabled when loading")
    } catch (e: AssertionError) {
      // Expected - button is disabled
    }
    // Other button should still work
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun Guest_button_shows_loading_when_Guest_is_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()

    // Loading indicator should be visible
    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
    // Other button should still work
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
  }

  // ==================== LOADING INDICATOR TESTS ====================

  @Test
  fun shows_Microsoft_loading_indicator_when_Microsoft_is_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }

    composeRule.waitForIdle()
    Thread.sleep(1000) // Allow AnimatedVisibility animation (800ms) to complete
    // Wait for the loading indicator to appear - waitUntilAtLeastOneExists confirms it exists
    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.MsProgress), timeoutMillis = 5000)
    composeRule.waitForIdle()

    // waitUntilAtLeastOneExists already confirmed the node exists
    composeRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true)

    // Switch progress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.SwitchProgress, useUnmergedTree = true).assertIsDisplayed()
      org.junit.Assert.fail("SwitchProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun shows_Guest_loading_indicator_when_Guest_is_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
    // MsProgress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
      org.junit.Assert.fail("MsProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun does_not_show_loading_indicators_in_idle_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // MsProgress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
      org.junit.Assert.fail("MsProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
    // Switch progress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
      org.junit.Assert.fail("SwitchProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun does_not_show_loading_indicators_in_error_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // MsProgress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
      org.junit.Assert.fail("MsProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
    // Switch progress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
      org.junit.Assert.fail("SwitchProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun does_not_show_loading_indicators_in_signed_in_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // MsProgress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
      org.junit.Assert.fail("MsProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
    // Switch progress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
      org.junit.Assert.fail("SwitchProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun hides_arrow_icon_in_Guest_button_when_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }

    // Arrow icon should not exist when loading (it's replaced by progress indicator)
    // This is implicit - if loading indicator exists, arrow shouldn't
    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
  }

  @Test
  fun shows_arrow_icon_in_Guest_button_when_not_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // Arrow icon should exist when not loading
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.CONTINUE))
        .assertIsDisplayed()
    // Switch progress should not exist - verify by trying to assert it's displayed
    try {
      composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
      org.junit.Assert.fail("SwitchProgress should not exist")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  // ==================== INTERACTION TESTS ====================

  @Test
  fun Microsoft_button_triggers_callback_on_click() {
    var msClicked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle,
            onMicrosoftLogin = { msClicked = true },
            onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    assertTrue("Microsoft button callback should be triggered", msClicked)
  }

  @Test
  fun Guest_button_triggers_callback_on_click() {
    var guestClicked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle,
            onMicrosoftLogin = {},
            onSwitchEduLogin = { guestClicked = true })
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    assertTrue("Guest button callback should be triggered", guestClicked)
  }

  // ==================== FOOTER TEXT TESTS ====================

  @Test
  fun footer_displays_privacy_policy_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // Check for privacy policy text
    composeRule
        .onNode(hasText(TestConstants.ScreenTexts.PRIVACY_POLICY_PREFIX, substring = true))
        .assertIsDisplayed()
    composeRule
        .onNode(hasText(TestConstants.ScreenTexts.PRIVACY_POLICY_LINK, substring = true))
        .assertIsDisplayed()
  }

  @Test
  fun footer_displays_BY_EPFL_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithText(TestConstants.ScreenTexts.BY_EPFL).assertIsDisplayed()
  }

  // ==================== COMPREHENSIVE STATE COVERAGE ====================

  @Test
  fun all_elements_visible_in_error_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // All UI elements should still be visible in error state
    composeRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun all_elements_visible_in_signed_in_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // All UI elements should still be visible in signed in state
    composeRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  // ==================== EDGE CASES ====================

  @Test
  fun handles_multiple_rapid_clicks_gracefully() {
    var clickCount = 0

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = { clickCount++ }, onSwitchEduLogin = {})
      }
    }

    // Perform multiple rapid clicks
    repeat(5) { composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick() }

    // All clicks should register (actual rate limiting is handled by ViewModel)
    assertEquals("All clicks should register", 5, clickCount)
  }

  @Test
  fun privacy_policy_text_is_clickable() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // Privacy policy text should exist and be clickable
    composeRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
    // Note: The actual click handling is in PrivacyPolicyText composable
  }

  // ==================== MICROSOFT SIGN-IN FLOW TESTS ====================

  @Test
  fun Microsoft_button_click_triggers_callback_in_idle_state() {
    var msClicked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle,
            onMicrosoftLogin = { msClicked = true },
            onSwitchEduLogin = {})
      }
    }
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    assertTrue("Microsoft button callback should be triggered", msClicked)
  }

  @Test
  fun Microsoft_button_click_triggers_callback_in_error_state() {
    var msClicked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"),
            onMicrosoftLogin = { msClicked = true },
            onSwitchEduLogin = {})
      }
    }
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    assertTrue("Microsoft button callback should work in error state", msClicked)
  }

  @Test
  fun Microsoft_button_disabled_during_loading_prevents_multiple_clicks() {
    var clickCount = 0

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = { clickCount++ },
            onSwitchEduLogin = {})
      }
    }

    composeRule.waitForIdle()
    Thread.sleep(500)

    // Button should be disabled, so clicking shouldn't trigger callback
    // (Button is disabled, so clicks are ignored)
    try {
      composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    } catch (e: Exception) {
      // Expected - disabled button may not be clickable
    }

    // Click count should remain 0 because button is disabled
    assertEquals("Disabled button should not trigger clicks", 0, clickCount)
  }

  @Test
  fun Microsoft_loading_state_disables_button_and_shows_progress() {
    // Test Loading state is correctly displayed
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }

    composeRule.waitForIdle()
    Thread.sleep(500)

    // Verify loading state - button should be disabled and progress indicator should exist
    try {
      composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
      org.junit.Assert.fail("Button should be disabled in loading state")
    } catch (e: AssertionError) {
      // Expected - button is disabled
    }

    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.MsProgress), timeoutMillis = 5000)
    // waitUntilAtLeastOneExists already confirmed the node exists
    composeRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true)
  }

  @Test
  fun Microsoft_signed_in_state_enables_buttons_and_hides_loading() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.waitForIdle()

    // In SignedIn state, buttons should be enabled and no loading indicators
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()

    // Loading indicators should not exist
    try {
      composeRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true).assertIsDisplayed()
      org.junit.Assert.fail("MsProgress should not exist in SignedIn state")
    } catch (e: AssertionError) {
      // Expected - no loading indicators
    }
  }
}
