package com.android.sample.splash

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpeningScreenLogic - covers navigation logic functions These tests provide code
 * coverage for the extracted logic functions
 */
class OpeningScreenLogicTest {

  @Test
  fun shouldNavigateToHome_returns_true_for_signed_in() {
    val authState = AuthUiState.SignedIn
    assertTrue("SignedIn should navigate to home", shouldNavigateToHome(authState))
  }

  @Test
  fun shouldNavigateToHome_returns_false_for_idle() {
    val authState = AuthUiState.Idle
    assertFalse("Idle should not navigate to home", shouldNavigateToHome(authState))
  }

  @Test
  fun shouldNavigateToHome_returns_false_for_loading() {
    val authState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertFalse("Loading should not navigate to home", shouldNavigateToHome(authState))
  }

  @Test
  fun shouldNavigateToHome_returns_false_for_error() {
    val authState = AuthUiState.Error("Test error")
    assertFalse("Error should not navigate to home", shouldNavigateToHome(authState))
  }

  @Test
  fun determineNavigationTarget_returns_home_for_signed_in() {
    val authState = AuthUiState.SignedIn
    val target = determineNavigationTarget(authState)
    assertEquals("Should return Home target", NavigationTarget.Home, target)
  }

  @Test
  fun determineNavigationTarget_returns_signin_for_idle() {
    val authState = AuthUiState.Idle
    val target = determineNavigationTarget(authState)
    assertEquals("Should return SignIn target", NavigationTarget.SignIn, target)
  }

  @Test
  fun determineNavigationTarget_returns_signin_for_loading() {
    val authState = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    val target = determineNavigationTarget(authState)
    assertEquals("Should return SignIn target", NavigationTarget.SignIn, target)
  }

  @Test
  fun determineNavigationTarget_returns_signin_for_error() {
    val authState = AuthUiState.Error("Network error")
    val target = determineNavigationTarget(authState)
    assertEquals("Should return SignIn target", NavigationTarget.SignIn, target)
  }

  @Test
  fun validateNavigationLogic_returns_true() {
    assertTrue("Navigation logic should be valid", validateNavigationLogic())
  }

  @Test
  fun navigationTarget_enum_values() {
    // Test that enum values are accessible
    assertNotNull("Home enum should exist", NavigationTarget.Home)
    assertNotNull("SignIn enum should exist", NavigationTarget.SignIn)
    assertEquals("Should have 2 enum values", 2, NavigationTarget.values().size)
  }

  @Test
  fun all_auth_states_navigation_coverage() {
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.Error("Error message"),
            AuthUiState.SignedIn)

    val homeTargets = states.count { shouldNavigateToHome(it) }
    assertEquals("Only 1 state should navigate to home", 1, homeTargets)

    val signInTargets = states.count { !shouldNavigateToHome(it) }
    assertEquals("4 states should navigate to sign in", 4, signInTargets)
  }

  @Test
  fun navigationTarget_consistency() {
    // Test that determineNavigationTarget and shouldNavigateToHome are consistent
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.SignedIn,
            AuthUiState.Error("Test"))

    for (state in states) {
      val shouldGoHome = shouldNavigateToHome(state)
      val target = determineNavigationTarget(state)

      if (shouldGoHome) {
        assertEquals(
            "Target should be Home if shouldNavigateToHome is true", NavigationTarget.Home, target)
      } else {
        assertEquals(
            "Target should be SignIn if shouldNavigateToHome is false",
            NavigationTarget.SignIn,
            target)
      }
    }
  }
}
