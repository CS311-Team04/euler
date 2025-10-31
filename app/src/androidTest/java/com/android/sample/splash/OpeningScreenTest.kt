package com.android.sample.splash

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
  fun opening_screen_navigates_to_home_after_delay_when_signed_in() {
    val homeCalled = AtomicBoolean(false)
    val signInCalled = AtomicBoolean(false)

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.SignedIn,
            onNavigateToSignIn = { signInCalled.set(true) },
            onNavigateToHome = { homeCalled.set(true) })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for Compose to be idle to ensure LaunchedEffect starts
    composeRule.waitForIdle()

    // Wait for navigation callback with timeout (up to 4 seconds to account for delay)
    val startTime = System.currentTimeMillis()
    while (!homeCalled.get() &&
        !signInCalled.get() &&
        (System.currentTimeMillis() - startTime) < 4000) {
      Thread.sleep(100)
    }

    // Verify navigation occurred
    assertTrue("Should navigate to home when signed in", homeCalled.get())
    assertFalse("Should not navigate to sign in when signed in", signInCalled.get())
  }

  @Test
  fun opening_screen_navigates_to_signin_after_delay_when_idle() {
    val homeCalled = AtomicBoolean(false)
    val signInCalled = AtomicBoolean(false)

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Idle,
            onNavigateToSignIn = { signInCalled.set(true) },
            onNavigateToHome = { homeCalled.set(true) })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for Compose to be idle to ensure LaunchedEffect starts
    composeRule.waitForIdle()

    // Wait for navigation callback with timeout (up to 4 seconds to account for delay)
    val startTime = System.currentTimeMillis()
    while (!homeCalled.get() &&
        !signInCalled.get() &&
        (System.currentTimeMillis() - startTime) < 4000) {
      Thread.sleep(100)
    }

    // Verify navigation occurred
    assertTrue("Should navigate to sign in when idle", signInCalled.get())
    assertFalse("Should not navigate to home when idle", homeCalled.get())
  }

  @Test
  fun opening_screen_navigates_to_signin_after_delay_when_loading() {
    val homeCalled = AtomicBoolean(false)
    val signInCalled = AtomicBoolean(false)

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Loading(AuthProvider.MICROSOFT),
            onNavigateToSignIn = { signInCalled.set(true) },
            onNavigateToHome = { homeCalled.set(true) })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for Compose to be idle to ensure LaunchedEffect starts
    composeRule.waitForIdle()

    // Wait for navigation callback with timeout (up to 4 seconds to account for delay)
    val startTime = System.currentTimeMillis()
    while (!homeCalled.get() &&
        !signInCalled.get() &&
        (System.currentTimeMillis() - startTime) < 4000) {
      Thread.sleep(100)
    }

    // Verify navigation occurred
    assertTrue("Should navigate to sign in when loading", signInCalled.get())
    assertFalse("Should not navigate to home when loading", homeCalled.get())
  }

  @Test
  fun opening_screen_navigates_to_signin_after_delay_when_error() {
    val homeCalled = AtomicBoolean(false)
    val signInCalled = AtomicBoolean(false)

    composeRule.setContent {
      MaterialTheme {
        OpeningScreen(
            authState = AuthUiState.Error("Test error"),
            onNavigateToSignIn = { signInCalled.set(true) },
            onNavigateToHome = { homeCalled.set(true) })
      }
    }

    // Verify screen is displayed
    composeRule
        .onNodeWithContentDescription(OpeningScreenTestConstants.OPENING_SCREEN_CONTENT_DESCRIPTION)
        .assertIsDisplayed()

    // Wait for Compose to be idle to ensure LaunchedEffect starts
    composeRule.waitForIdle()

    // Wait for navigation callback with timeout (up to 4 seconds to account for delay)
    val startTime = System.currentTimeMillis()
    while (!homeCalled.get() &&
        !signInCalled.get() &&
        (System.currentTimeMillis() - startTime) < 4000) {
      Thread.sleep(100)
    }

    // Verify navigation occurred
    assertTrue("Should navigate to sign in when error", signInCalled.get())
    assertFalse("Should not navigate to home when error", homeCalled.get())
  }
}
