package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.TestConstants
import com.android.sample.authentification.AuthTags
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AuthUIScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

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
  fun microsoft_button_triggers_callback() {
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
    assertTrue(msClicked)
  }

  @Test
  fun switch_button_triggers_callback() {
    var switchClicked = false

    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Idle,
            onMicrosoftLogin = {},
            onSwitchEduLogin = { switchClicked = true })
      }
    }

    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    assertTrue(switchClicked)
  }

  @Test
  fun displays_correct_button_texts() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    composeRule.onNodeWithText(TestConstants.ButtonTexts.MICROSOFT_LOGIN).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ButtonTexts.SWITCH_LOGIN).assertIsDisplayed()
  }

  @Test
  fun error_state_allows_buttons_to_be_enabled() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(
            state = AuthUiState.Error("Test error"), onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // En cas d'erreur, les boutons devraient être réactivés
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }

  @Test
  fun signed_in_state_allows_buttons_to_be_enabled() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.SignedIn, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }

    // Même après SignedIn, les boutons sont techniquement enabled
    // (mais en pratique, l'écran devrait avoir navigué ailleurs)
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
  }
}
