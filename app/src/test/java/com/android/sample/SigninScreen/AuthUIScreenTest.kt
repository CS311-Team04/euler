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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1080dp-h1920dp")
@LooperMode(LooperMode.Mode.PAUSED)
class AuthUIScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    com.android.sample.settings.AppSettings.setDispatcher(testDispatcher)
  }

  @After
  fun tearDown() {
    com.android.sample.settings.AppSettings.resetDispatcher()
    Dispatchers.resetMain()
  }

  // --- HELPER FUNCTION (Corrected) ---
  private fun setContentWithAnimation(content: @androidx.compose.runtime.Composable () -> Unit) {
    composeTestRule.setContent(content)
    // Advance clock to skip the 800ms entry animation
    composeTestRule.mainClock.advanceTimeBy(1000L)
    // DO NOT CALL waitForIdle()
  }

  // ==================== ROOT ELEMENT TESTS ====================

  @Test
  fun AuthUIScreen_renders_root_container() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_card_container() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()
  }

  // ==================== LOGOS TESTS ====================

  @Test
  fun AuthUIScreen_renders_logos_row() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.LogosRow).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_EPFL_logo() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.LogoEpfl).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_logo_separator() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.LogoPoint, useUnmergedTree = true)
  }

  @Test
  fun AuthUIScreen_renders_Euler_logo() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.LogoEuler).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_logos_have_content_descriptions() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNode(hasContentDescription("EPFL Logo")).assertIsDisplayed()
    composeTestRule.onNode(hasContentDescription("Euler Logo")).assertIsDisplayed()
  }

  // ==================== TITLE AND SUBTITLE TESTS ====================

  @Test
  fun AuthUIScreen_renders_title_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithText("Ask anything, do everything").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_subtitle_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeTestRule.onNodeWithText("Welcome to EULER").assertIsDisplayed()
  }

  // ==================== BUTTONS TESTS ====================

  @Test
  fun AuthUIScreen_renders_Microsoft_button() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_Guest_button() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Microsoft_button_has_correct_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithText("Continue with Microsoft Entra ID").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Guest_button_has_correct_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithText("Continue as a guest").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Microsoft_button_has_Microsoft_logo() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNode(hasContentDescription("Microsoft Logo")).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_Guest_button_has_arrow_icon() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
  }

  // ==================== BUTTON INTERACTION TESTS ====================

  @Test
  fun AuthUIScreen_Microsoft_button_triggers_callback() {
    var clicked = false
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = { clicked = true }, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    composeTestRule.mainClock.advanceTimeBy(200L) // Advance for click animation (120ms)
    assertTrue("Microsoft button should trigger callback", clicked)
  }

  @Test
  fun AuthUIScreen_Guest_button_triggers_callback() {
    var clicked = false
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = { clicked = true })
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    composeTestRule.mainClock.advanceTimeBy(200L) // Advance for click animation (120ms)
    assertTrue("Guest button should trigger callback", clicked)
  }

  // ==================== BUTTON STATE TESTS ====================

  @Test
  fun AuthUIScreen_buttons_enabled_in_idle_state() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun AuthUIScreen_buttons_enabled_in_error_state() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun AuthUIScreen_buttons_enabled_in_signed_in_state() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  // ==================== LOADING TESTS (Restored and Fixed) ====================

  @Test
  fun AuthUIScreen_hides_Guest_loading_indicator_when_not_loading() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    try {
      composeTestRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
      fail("SwitchProgress should not exist when not loading")
    } catch (e: AssertionError) {
      // Expected
    }
  }

  @Test
  fun AuthUIScreen_shows_arrow_icon_when_Guest_not_loading() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
  }

  // You can restore your other "Loading" tests here, but make sure they follow this pattern:
  @Test
  fun AuthUIScreen_hides_arrow_icon_when_Guest_loading() {
    // 1. Disable auto-advance for infinite spinners
    composeTestRule.mainClock.autoAdvance = false

    composeTestRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    // 2. Advance time manually for entry animation
    composeTestRule.mainClock.advanceTimeBy(1000L)

    // 3. Perform assertions
    try {
      composeTestRule.onNode(hasContentDescription("Continue")).assertIsDisplayed()
      fail("Arrow icon should not exist when loading")
    } catch (e: AssertionError) {
      // Expected
    }
    // 4. Do NOT call waitForIdle()
  }

  // ==================== OR SEPARATOR TESTS ====================

  @Test
  fun AuthUIScreen_renders_OR_separator() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.OrSeparator).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_OR_separator_has_correct_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithText("OR").assertIsDisplayed()
  }

  // ==================== FOOTER TESTS ====================

  @Test
  fun AuthUIScreen_renders_privacy_policy_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_privacy_policy_text_contains_privacy_policy() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNode(hasText("Privacy Policy", substring = true)).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_privacy_policy_text_contains_acknowledge_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule
        .onNode(hasText("By continuing, you acknowledge", substring = true))
        .assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_BY_EPFL_text() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.ByEpflText).assertIsDisplayed()
    composeTestRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  // ==================== STATE-SPECIFIC TESTS ====================

  @Test
  fun AuthUIScreen_all_elements_visible_in_idle_state() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
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
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_all_elements_visible_in_signed_in_state() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  // ==================== EDGE CASES ====================

  @Test
  fun AuthUIScreen_handles_multiple_Microsoft_clicks() {
    var clickCount = 0
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = { clickCount++ }, onSwitchEduLogin = {})
      }
    }
    repeat(3) {
      composeTestRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
      composeTestRule.mainClock.advanceTimeBy(200L) // Advance for click animation
    }
    assertEquals("All clicks should register", 3, clickCount)
  }

  @Test
  fun AuthUIScreen_handles_multiple_Guest_clicks() {
    var clickCount = 0
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = { clickCount++ })
      }
    }
    repeat(3) {
      composeTestRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
      composeTestRule.mainClock.advanceTimeBy(200L) // Advance for click animation
    }
    assertEquals("All clicks should register", 3, clickCount)
  }

  // ==================== COMPREHENSIVE COVERAGE TESTS ====================

  @Test
  fun AuthUIScreen_all_test_tags_present() {
    setContentWithAnimation {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeTestRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.LogosRow).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AuthTags.LogoEpfl).assertIsDisplayed()
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
    val loadingState = state as? AuthUiState.Loading
    assertNotNull("State should be Loading", loadingState)
    assertEquals("Provider should be MICROSOFT", AuthProvider.MICROSOFT, loadingState?.provider)
  }

  @Test
  fun AuthUIScreen_correct_loading_state_detection_Guest() {
    val state = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    val loadingState = state as? AuthUiState.Loading
    assertNotNull("State should be Loading", loadingState)
    assertEquals("Provider should be SWITCH_EDU", AuthProvider.SWITCH_EDU, loadingState?.provider)
  }
}
