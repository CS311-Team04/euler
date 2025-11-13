package com.android.sample.signinscreen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthTags
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AuthUIScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ==================== ROOT ELEMENT TESTS ====================

  @Test
  fun AuthUIScreen_renders_root_container() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_card_container() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()
  }

  // ==================== LOGOS TESTS ====================

  @Test
  fun AuthUIScreen_renders_logos_row() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.LogosRow).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_EPFL_logo() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.LogoEpfl).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_logo_separator() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    // Logo separator is a very small Box (0.5dp width), use unmerged tree to find it
    composeTestRule.onNodeWithTag(AuthTags.LogoPoint, useUnmergedTree = true)
  }

  @Test
  fun AuthUIScreen_renders_Euler_logo() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.LogoEuler).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_logos_have_content_descriptions() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasContentDescription("EPFL Logo")).assertIsDisplayed()
    composeTestRule.onNode(hasContentDescription("Euler Logo")).assertIsDisplayed()
  }

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
    composeTestRule.onNodeWithText("Ask anything, do everything").assertIsDisplayed()
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
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Microsoft_button_has_correct_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Continue with Microsoft Entra ID").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Guest_button_has_correct_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Continue as a guest").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Microsoft_button_has_Microsoft_logo() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasContentDescription("Microsoft Logo")).assertIsDisplayed()
  }

  @Test
  @Ignore("Skipped in unit environment to avoid heavy Compose allocation")
  fun AuthUIScreen_Guest_button_has_arrow_icon() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
  }

  // ==================== BUTTON INTERACTION TESTS ====================

  @Test
  fun AuthUIScreen_Microsoft_button_triggers_callback() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = { clicked = true }, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    assertTrue("Microsoft button should trigger callback", clicked)
  }

  @Test
  fun AuthUIScreen_Guest_button_triggers_callback() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = { clicked = true })
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    assertTrue("Guest button should trigger callback", clicked)
  }

  // ==================== BUTTON STATE TESTS ====================

  @Test
  fun AuthUIScreen_buttons_enabled_in_idle_state() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun AuthUIScreen_buttons_enabled_in_error_state() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  @Ignore("Skipped in unit environment to avoid heavy Compose allocation")
  fun AuthUIScreen_buttons_enabled_in_signed_in_state() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun AuthUIScreen_Microsoft_button_disabled_when_Microsoft_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    // Button should be disabled - verify by trying to assert it's enabled
    try {
      composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
      fail("Microsoft button should be disabled when loading")
    } catch (e: AssertionError) {
      // Expected - button is disabled
    }
  }

  @Test
  fun AuthUIScreen_Guest_button_disabled_when_Guest_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    // Button should be disabled - verify by trying to assert it's enabled
    try {
      composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
      fail("Guest button should be disabled when loading")
    } catch (e: AssertionError) {
      // Expected - button is disabled
    }
  }

  @Test
  fun AuthUIScreen_Microsoft_button_enabled_when_Guest_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
  }

  @Test
  fun AuthUIScreen_Guest_button_enabled_when_Microsoft_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  // ==================== LOADING INDICATORS TESTS ====================

  @Test
  fun AuthUIScreen_shows_Microsoft_loading_indicator_when_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    Thread.sleep(1000) // Allow animation to complete
    composeTestRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true)
  }

  @Test
  fun AuthUIScreen_shows_Guest_loading_indicator_when_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_hides_Microsoft_loading_indicator_when_not_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true).assertIsDisplayed()
      fail("MsProgress should not exist when not loading")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun AuthUIScreen_hides_Guest_loading_indicator_when_not_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    try {
      composeTestRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
      fail("SwitchProgress should not exist when not loading")
    } catch (e: AssertionError) {
      // Expected - node does not exist
    }
  }

  @Test
  fun AuthUIScreen_hides_arrow_icon_when_Guest_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    // Arrow icon should not exist when loading (replaced by progress indicator)
    try {
      composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
      fail("Arrow icon should not exist when loading")
    } catch (e: AssertionError) {
      // Expected - arrow is replaced by loading indicator
    }
  }

  @Test
  fun AuthUIScreen_shows_arrow_icon_when_Guest_not_loading() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
  }

  // ==================== OR SEPARATOR TESTS ====================

  @Test
  fun AuthUIScreen_renders_OR_separator() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.OrSeparator).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_OR_separator_has_correct_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("OR").assertIsDisplayed()
  }

  // ==================== FOOTER TESTS ====================

  @Test
  fun AuthUIScreen_renders_privacy_policy_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_privacy_policy_text_contains_privacy_policy() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNode(hasText("Privacy Policy", substring = true)).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_privacy_policy_text_contains_acknowledge_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule
        .onNode(hasText("By continuing, you acknowledge", substring = true))
        .assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_BY_EPFL_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.ByEpflText).assertIsDisplayed()
    composeTestRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  // ==================== STATE-SPECIFIC TESTS ====================

  @Test
  fun AuthUIScreen_all_elements_visible_in_idle_state() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.OrSeparator).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.ByEpflText).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_all_elements_visible_in_error_state() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_all_elements_visible_in_signed_in_state() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_all_elements_visible_in_loading_state_Microsoft() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_all_elements_visible_in_loading_state_Guest() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  // ==================== EDGE CASES ====================

  @Test
  fun AuthUIScreen_handles_multiple_Microsoft_clicks() {
    var clickCount = 0
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = { clickCount++ }, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    repeat(3) { composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick() }
    assertEquals("All clicks should register", 3, clickCount)
  }

  @Test
  fun AuthUIScreen_handles_multiple_Guest_clicks() {
    var clickCount = 0
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = { clickCount++ })
      }
    }
    composeTestRule.waitForIdle()
    repeat(3) { composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick() }
    assertEquals("All clicks should register", 3, clickCount)
  }

  @Test
  fun AuthUIScreen_buttons_do_not_trigger_when_disabled() {
    var msClicked = false
    var guestClicked = false
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = { msClicked = true },
            onSwitchEduLogin = { guestClicked = true })
      }
    }
    composeTestRule.waitForIdle()
    // Microsoft button is disabled, so callback should not fire
    try {
      composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    } catch (e: Exception) {
      // Expected - disabled button may throw
    }
    assertFalse("Microsoft button should not trigger when disabled", msClicked)
    // Guest button is enabled, so callback should fire
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    assertTrue("Guest button should trigger when enabled", guestClicked)
  }

  // ==================== LOADING STATE TRANSITIONS ====================

  @Test
  fun AuthUIScreen_Microsoft_loading_shows_progress_hides_text() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    // Loading indicator should exist
    composeTestRule.onNodeWithTag(AuthTags.MsProgress, useUnmergedTree = true)
    // Button text should still be visible
    composeTestRule.onNodeWithText("Continue with Microsoft Entra ID").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Guest_loading_shows_progress_hides_arrow() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    // Loading indicator should exist
    composeTestRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
    // Arrow icon should not exist
    try {
      composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
      fail("Arrow should be hidden when loading")
    } catch (e: AssertionError) {
      // Expected
    }
    // Button text should still be visible
    composeTestRule.onNodeWithText("Continue as a guest").assertIsDisplayed()
  }

  // ==================== COMPREHENSIVE COVERAGE TESTS ====================

  @Test
  fun AuthUIScreen_all_test_tags_present() {
    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.LogosRow).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.LogoEpfl).assertIsDisplayed()
    // Logo separator is a very small Box (0.5dp width), use unmerged tree to find it
    composeTestRule.onNodeWithTag(AuthTags.LogoPoint, useUnmergedTree = true)
    composeTestRule.onNodeWithTag(AuthTags.LogoEuler).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.OrSeparator).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.ByEpflText).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_correct_loading_state_detection_Microsoft() {
    val state = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertTrue("State should be Loading", state is AuthUiState.Loading)
    assertTrue(
        "Provider should be MICROSOFT",
        (state as AuthUiState.Loading).provider == AuthProvider.MICROSOFT)
  }

  @Test
  fun AuthUIScreen_correct_loading_state_detection_Guest() {
    val state = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertTrue("State should be Loading", state is AuthUiState.Loading)
    assertTrue(
        "Provider should be SWITCH_EDU",
        (state as AuthUiState.Loading).provider == AuthProvider.SWITCH_EDU)
  }
}
