package com.android.sample.navigation

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * Additional unit tests for navigation decision logic Provides comprehensive coverage for all
 * navigation scenarios
 */
class NavGraphNavigationDecisionTest {

  @Test
  fun shouldTriggerMicrosoftAuth_with_all_providers() {
    val microsoftLoading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val switchEduLoading = AuthUiState.Loading(AuthProvider.SWITCH_EDU)

    assertTrue(
        "Microsoft loading should trigger auth", shouldTriggerMicrosoftAuth(microsoftLoading))
    assertFalse(
        "Switch Edu loading should not trigger Microsoft auth",
        shouldTriggerMicrosoftAuth(switchEduLoading))
  }

  @Test
  fun shouldNavigateToHomeFromSignIn_all_destination_combinations() {
    val signedInState = AuthUiState.SignedIn
    val allDestinations =
        listOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat,
            null)

    for (destination in allDestinations) {
      val shouldNavigate = shouldNavigateToHomeFromSignIn(signedInState, destination)
      val expected = destination == Routes.SignIn
      assertEquals("Navigation decision for SignedIn on $destination", expected, shouldNavigate)
    }
  }

  @Test
  fun error_message_extraction_comprehensive() {
    val testCases =
        listOf(
            Pair("Network error", "Network error"),
            Pair("", "Default message"),
            Pair("   ", "   "),
            Pair("Authentication failed", "Authentication failed"))

    for ((errorMsg, expected) in testCases) {
      val errorState = AuthUiState.Error(errorMsg)
      val result = getErrorMessage(errorState, "Default message")
      assertEquals("Error message for '$errorMsg'", expected, result)
    }
  }

  @Test
  fun state_checking_functions_coverage() {
    // Comprehensive test for all state checking functions
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.SignedIn,
            AuthUiState.Error("Test"))

    for (state in states) {
      // Only one should be true at a time
      val isIdle = isIdleState(state)
      val isLoading = isLoadingState(state)
      val isSignedIn = isSignedInState(state)
      val isError = isErrorState(state)

      val trueCount = listOf(isIdle, isLoading, isSignedIn, isError).count { it }
      assertEquals("Exactly one state check should be true", 1, trueCount)
    }
  }

  @Test
  fun navigation_decision_edge_cases() {
    // Test null destination with different states
    assertFalse(
        "SignedIn with null destination should not navigate",
        shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, null))

    // Test empty string destination (shouldn't happen but test anyway)
    assertFalse(
        "SignedIn with empty destination should not navigate",
        shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, ""))

    // Test non-matching destination
    assertFalse(
        "SignedIn on Home should not navigate again",
        shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, Routes.Home))
  }

  @Test
  fun microsoft_auth_trigger_conditions() {
    // Only Microsoft provider should trigger
    val providers = listOf(AuthProvider.MICROSOFT, AuthProvider.SWITCH_EDU)

    for (provider in providers) {
      val loadingState = AuthUiState.Loading(provider)
      val shouldTrigger = shouldTriggerMicrosoftAuth(loadingState)
      val expected = provider == AuthProvider.MICROSOFT
      assertEquals("Microsoft auth for $provider", expected, shouldTrigger)
    }
  }

  @Test
  fun all_routes_values_used_in_navigation() {
    // Verify all route constants are used in navigation logic
    val routes =
        listOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat)

    // Test that Routes.SignIn is used correctly in navigation decision
    val signedInState = AuthUiState.SignedIn
    for (route in routes) {
      val result = shouldNavigateToHomeFromSignIn(signedInState, route)
      assertEquals("Navigation for route $route", route == Routes.SignIn, result)
    }
  }

  @Test
  fun state_type_discrimination() {
    // Ensure state checking functions correctly discriminate
    assertTrue("Idle is idle", isIdleState(AuthUiState.Idle))
    assertFalse("Idle is not loading", isLoadingState(AuthUiState.Idle))
    assertFalse("Idle is not signed in", isSignedInState(AuthUiState.Idle))
    assertFalse("Idle is not error", isErrorState(AuthUiState.Idle))

    val loadingState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertFalse("Loading is not idle", isIdleState(loadingState))
    assertTrue("Loading is loading", isLoadingState(loadingState))
    assertFalse("Loading is not signed in", isSignedInState(loadingState))
    assertFalse("Loading is not error", isErrorState(loadingState))

    assertFalse("SignedIn is not idle", isIdleState(AuthUiState.SignedIn))
    assertFalse("SignedIn is not loading", isLoadingState(AuthUiState.SignedIn))
    assertTrue("SignedIn is signed in", isSignedInState(AuthUiState.SignedIn))
    assertFalse("SignedIn is not error", isErrorState(AuthUiState.SignedIn))

    val errorState = AuthUiState.Error("Error")
    assertFalse("Error is not idle", isIdleState(errorState))
    assertFalse("Error is not loading", isLoadingState(errorState))
    assertFalse("Error is not signed in", isSignedInState(errorState))
    assertTrue("Error is error", isErrorState(errorState))
  }
}
