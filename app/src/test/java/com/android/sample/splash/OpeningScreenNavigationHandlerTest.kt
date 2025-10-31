package com.android.sample.splash

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpeningScreenNavigationHandler These tests cover the navigation logic extracted
 * from OpeningScreen.kt
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpeningScreenNavigationHandlerTest {

  @Test
  fun handleOpeningScreenNavigation_calls_home_when_signed_in() = runTest {
    var homeCalled = false
    var signInCalled = false

    handleOpeningScreenNavigation(
        authState = AuthUiState.SignedIn,
        delayMillis = 0L, // Skip delay for testing
        onNavigateToHome = { homeCalled = true },
        onNavigateToSignIn = { signInCalled = true })

    assertTrue("Should navigate to home when signed in", homeCalled)
    assertFalse("Should not navigate to sign in when signed in", signInCalled)
  }

  @Test
  fun handleOpeningScreenNavigation_calls_signin_when_idle() = runTest {
    var homeCalled = false
    var signInCalled = false

    handleOpeningScreenNavigation(
        authState = AuthUiState.Idle,
        delayMillis = 0L,
        onNavigateToHome = { homeCalled = true },
        onNavigateToSignIn = { signInCalled = true })

    assertTrue("Should navigate to sign in when idle", signInCalled)
    assertFalse("Should not navigate to home when idle", homeCalled)
  }

  @Test
  fun handleOpeningScreenNavigation_calls_signin_when_loading() = runTest {
    var homeCalled = false
    var signInCalled = false

    handleOpeningScreenNavigation(
        authState = AuthUiState.Loading(AuthProvider.MICROSOFT),
        delayMillis = 0L,
        onNavigateToHome = { homeCalled = true },
        onNavigateToSignIn = { signInCalled = true })

    assertTrue("Should navigate to sign in when loading", signInCalled)
    assertFalse("Should not navigate to home when loading", homeCalled)
  }

  @Test
  fun handleOpeningScreenNavigation_calls_signin_when_error() = runTest {
    var homeCalled = false
    var signInCalled = false

    handleOpeningScreenNavigation(
        authState = AuthUiState.Error("Test error"),
        delayMillis = 0L,
        onNavigateToHome = { homeCalled = true },
        onNavigateToSignIn = { signInCalled = true })

    assertTrue("Should navigate to sign in when error", signInCalled)
    assertFalse("Should not navigate to home when error", homeCalled)
  }

  @Test
  fun handleOpeningScreenNavigation_respects_delay() = runTest {
    var homeCalled = false
    var signInCalled = false

    val job = launch {
      handleOpeningScreenNavigation(
          authState = AuthUiState.SignedIn,
          delayMillis = 1000L,
          onNavigateToHome = { homeCalled = true },
          onNavigateToSignIn = { signInCalled = true })
    }

    // Before delay
    assertFalse("Should not navigate before delay", homeCalled)
    assertFalse("Should not navigate before delay", signInCalled)

    // Advance time by 500ms (not enough)
    advanceTimeBy(500)
    assertFalse("Should not navigate after 500ms", homeCalled)

    // Advance time by 600ms more (total 1100ms, enough)
    advanceTimeBy(600)
    assertTrue("Should navigate after delay", homeCalled)
    assertFalse("Should not navigate to sign in", signInCalled)

    job.cancel()
  }

  @Test
  fun getOpeningScreenBackgroundColor_returns_correct_value() {
    val color = getOpeningScreenBackgroundColor()
    assertEquals("Background color should be 0xFF121212", 0xFF121212, color)
  }

  @Test
  fun getOpeningScreenDelay_returns_correct_value() {
    val delay = getOpeningScreenDelay()
    assertEquals("Delay should be 2500ms", 2500L, delay)
  }

  @Test
  fun handleOpeningScreenNavigation_all_states_coverage() = runTest {
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.Error("Error message"),
            AuthUiState.SignedIn)

    for (state in states) {
      var homeCalled = false
      var signInCalled = false

      handleOpeningScreenNavigation(
          authState = state,
          delayMillis = 0L,
          onNavigateToHome = { homeCalled = true },
          onNavigateToSignIn = { signInCalled = true })

      if (state is AuthUiState.SignedIn) {
        assertTrue("SignedIn should navigate to home", homeCalled)
        assertFalse("SignedIn should not navigate to sign in", signInCalled)
      } else {
        assertTrue("Non-SignedIn should navigate to sign in", signInCalled)
        assertFalse("Non-SignedIn should not navigate to home", homeCalled)
      }
    }
  }
}
