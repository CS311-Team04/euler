package com.android.sample.splash

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test constants for OpeningScreen */
object OpeningScreenTestConstants {
  const val OPENING_SCREEN_CONTENT_DESCRIPTION = "Opening Screen"
}

@RunWith(AndroidJUnit4::class)
class OpeningScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displays_opening_screen_image() {
    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(authState = AuthUiState.Idle, onNavigateToSignIn = {}, onNavigateToHome = {})
      }
    }

    // Verify the image is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
  }

  @Test
  fun screen_renders_with_idle_state() {
    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(authState = AuthUiState.Idle, onNavigateToSignIn = {}, onNavigateToHome = {})
      }
    }

    // Verify screen is visible
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
  }

  @Test
  fun screen_renders_with_signedin_state() {
    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.SignedIn, onNavigateToSignIn = {}, onNavigateToHome = {})
      }
    }

    // Verify screen is visible
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
  }

  @Test
  fun screen_renders_with_loading_state() {
    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onNavigateToSignIn = {},
            onNavigateToHome = {})
      }
    }

    // Verify screen is visible
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
  }

  @Test
  fun screen_renders_with_error_state() {
    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Error("Test error"),
            onNavigateToSignIn = {},
            onNavigateToHome = {})
      }
    }

    // Verify screen is visible
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()
  }

  @Test
  fun opening_screen_navigates_to_home_after_delay_when_signed_in() = runTest {
    var homeCalled = false
    var signInCalled = false

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.SignedIn,
            onNavigateToSignIn = { signInCalled = true },
            onNavigateToHome = { homeCalled = true })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for navigation delay (2.5 seconds)
    delay(2600)

    // Verify navigation occurred
    assertTrue("Should navigate to home when signed in", homeCalled)
    assertFalse("Should not navigate to sign in when signed in", signInCalled)
  }

  @Test
  fun opening_screen_navigates_to_signin_after_delay_when_idle() = runTest {
    var homeCalled = false
    var signInCalled = false

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Idle,
            onNavigateToSignIn = { signInCalled = true },
            onNavigateToHome = { homeCalled = true })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for navigation delay (2.5 seconds)
    delay(2600)

    // Verify navigation occurred
    assertTrue("Should navigate to sign in when idle", signInCalled)
    assertFalse("Should not navigate to home when idle", homeCalled)
  }

  @Test
  fun opening_screen_navigates_to_signin_after_delay_when_loading() = runTest {
    var homeCalled = false
    var signInCalled = false

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onNavigateToSignIn = { signInCalled = true },
            onNavigateToHome = { homeCalled = true })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for navigation delay (2.5 seconds)
    delay(2600)

    // Verify navigation occurred
    assertTrue("Should navigate to sign in when loading", signInCalled)
    assertFalse("Should not navigate to home when loading", homeCalled)
  }

  @Test
  fun opening_screen_navigates_to_signin_after_delay_when_error() = runTest {
    var homeCalled = false
    var signInCalled = false

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Error("Test error"),
            onNavigateToSignIn = { signInCalled = true },
            onNavigateToHome = { homeCalled = true })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for navigation delay (2.5 seconds)
    delay(2600)

    // Verify navigation occurred
    assertTrue("Should navigate to sign in when error", signInCalled)
    assertFalse("Should not navigate to home when error", homeCalled)
  }
}
