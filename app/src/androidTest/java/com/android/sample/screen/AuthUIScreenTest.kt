package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
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

@RunWith(AndroidJUnit4::class)
class AuthUIScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_all_core_elements_in_idle_state() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()

    // Vérifie la présence du root
    composeRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()

    // Vérifie la card principale
    composeRule.onNodeWithTag(AuthTags.Card).assertIsDisplayed()

    // Vérifie la row des logos
    composeRule.onNodeWithTag(AuthTags.LogosRow).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.LogoEpfl).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.LogoPoint).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.LogoEuler).assertIsDisplayed()

    // Vérifie les boutons
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()

    // Vérifie le texte des conditions
    composeRule.onNodeWithTag(AuthTags.TermsText).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.TermsText.TERMS_AND_PRIVACY).assertIsDisplayed()
  }

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
  fun displays_logos_with_correct_content_descriptions() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()

    // Vérifie que les images ont les bonnes descriptions
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.EPFL_LOGO))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.SEPARATOR_DOT))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.EULER_LOGO))
        .assertIsDisplayed()
  }

  @Test
  fun displays_icons_in_buttons() {
    composeRule.setContent {
      MaterialTheme {
        AuthUIScreen(state = AuthUiState.Idle, onMicrosoftLogin = {}, onSwitchEduLogin = {})
      }
    }
    composeRule.waitForIdle()

    // Les icônes ont des contentDescription spécifiques
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.MICROSOFT_ICON))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.SWITCH_ICON))
        .assertIsDisplayed()
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
