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
  fun AuthUIScreen_renders_title_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeRule.onNodeWithText("Ask anything,\ndo everything").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_subtitle_text() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(AuthTags.Subtitle).assertIsDisplayed()
    composeRule.onNodeWithText("Welcome to EULER").assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_Microsoft_button() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
  }

  @Test
  fun AuthUIScreen_renders_Guest_button() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
  }

  @Test
  fun authScreen_showsMicrosoftLoading_whenLoadingMicrosoft() {
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onMicrosoftLogin = {},
            onSwitchEduLogin = {})
      }
    }
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
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
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()
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
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()
    composeRule.mainClock.advanceTimeBy(150)
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
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    composeRule.mainClock.advanceTimeBy(150)
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
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
    composeRule.mainClock.advanceTimeBy(150)
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
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithTag(AuthTags.SwitchProgress).assertIsDisplayed()
    composeRule.mainClock.advanceTimeBy(150)
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
    composeRule.mainClock.advanceTimeBy(850)
    composeRule.onNodeWithText("Ask anything,\ndo everything").assertIsDisplayed()
    composeRule.onNodeWithText("Welcome to EULER").assertIsDisplayed()
    composeRule.onNodeWithText("Continue with Microsoft Entra ID").assertIsDisplayed()
    composeRule.onNodeWithText("OR").assertIsDisplayed()
    composeRule.onNodeWithText("Continue as a guest").assertIsDisplayed()
    composeRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }
}
