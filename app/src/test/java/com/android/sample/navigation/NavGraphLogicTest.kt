package com.android.sample.navigation

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NavGraphLogic functions These tests provide code coverage for extracted navigation
 * logic
 */
class NavGraphLogicTest {

  @Test
  fun shouldTriggerMicrosoftAuth_returns_true_for_microsoft_loading() {
    val authState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertTrue("Microsoft loading should trigger auth", shouldTriggerMicrosoftAuth(authState))
  }

  @Test
  fun shouldTriggerMicrosoftAuth_returns_false_for_switch_edu_loading() {
    val authState = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertFalse(
        "Switch Edu loading should not trigger Microsoft auth",
        shouldTriggerMicrosoftAuth(authState))
  }

  @Test
  fun shouldTriggerMicrosoftAuth_returns_false_for_other_states() {
    assertFalse(
        "Idle should not trigger Microsoft auth", shouldTriggerMicrosoftAuth(AuthUiState.Idle))
    assertFalse(
        "SignedIn should not trigger Microsoft auth",
        shouldTriggerMicrosoftAuth(AuthUiState.SignedIn))
    assertFalse(
        "Error should not trigger Microsoft auth",
        shouldTriggerMicrosoftAuth(AuthUiState.Error("Test error")))
  }

  @Test
  fun shouldNavigateToHomeFromSignIn_returns_true_for_signed_in_on_signin_screen() {
    val authState = AuthUiState.SignedIn
    val currentDestination = Routes.SignIn
    assertTrue(
        "Should navigate when signed in on SignIn screen",
        shouldNavigateToHomeFromSignIn(authState, currentDestination))
  }

  @Test
  fun shouldNavigateToHomeFromSignIn_returns_false_for_signed_in_on_other_screens() {
    val authState = AuthUiState.SignedIn
    assertFalse(
        "Should not navigate from Opening screen",
        shouldNavigateToHomeFromSignIn(authState, Routes.Opening))
    assertFalse(
        "Should not navigate from Home screen",
        shouldNavigateToHomeFromSignIn(authState, Routes.Home))
    assertFalse(
        "Should not navigate when destination is null",
        shouldNavigateToHomeFromSignIn(authState, null))
  }

  @Test
  fun shouldNavigateToHomeFromSignIn_returns_false_for_other_states() {
    val destination = Routes.SignIn
    assertFalse(
        "Idle should not navigate", shouldNavigateToHomeFromSignIn(AuthUiState.Idle, destination))
    assertFalse(
        "Loading should not navigate",
        shouldNavigateToHomeFromSignIn(AuthUiState.Loading(AuthProvider.MICROSOFT), destination))
    assertFalse(
        "Error should not navigate",
        shouldNavigateToHomeFromSignIn(AuthUiState.Error("Error"), destination))
  }

  @Test
  fun getErrorMessage_returns_error_message_when_present() {
    val errorState = AuthUiState.Error("Custom error message")
    val defaultMessage = "Default error"
    assertEquals(
        "Should return custom message",
        "Custom error message",
        getErrorMessage(errorState, defaultMessage))
  }

  @Test
  fun getErrorMessage_returns_default_when_message_empty() {
    val errorState = AuthUiState.Error("")
    val defaultMessage = "Default error message"
    assertEquals(
        "Should return default when empty",
        defaultMessage,
        getErrorMessage(errorState, defaultMessage))
  }

  @Test
  fun isErrorState_returns_true_for_error() {
    val errorState = AuthUiState.Error("Test error")
    assertTrue("Should identify error state", isErrorState(errorState))
  }

  @Test
  fun isErrorState_returns_false_for_other_states() {
    assertFalse("Idle is not error", isErrorState(AuthUiState.Idle))
    assertFalse("Loading is not error", isErrorState(AuthUiState.Loading(AuthProvider.MICROSOFT)))
    assertFalse("SignedIn is not error", isErrorState(AuthUiState.SignedIn))
  }

  @Test
  fun isLoadingState_returns_true_for_loading() {
    val loadingState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertTrue("Should identify loading state", isLoadingState(loadingState))
    val loadingSwitchState = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertTrue("Should identify loading state", isLoadingState(loadingSwitchState))
  }

  @Test
  fun isLoadingState_returns_false_for_other_states() {
    assertFalse("Idle is not loading", isLoadingState(AuthUiState.Idle))
    assertFalse("SignedIn is not loading", isLoadingState(AuthUiState.SignedIn))
    assertFalse("Error is not loading", isLoadingState(AuthUiState.Error("Test error")))
  }

  @Test
  fun isSignedInState_returns_true_for_signed_in() {
    val signedInState = AuthUiState.SignedIn
    assertTrue("Should identify signed in state", isSignedInState(signedInState))
  }

  @Test
  fun isSignedInState_returns_false_for_other_states() {
    assertFalse("Idle is not signed in", isSignedInState(AuthUiState.Idle))
    assertFalse(
        "Loading is not signed in", isSignedInState(AuthUiState.Loading(AuthProvider.MICROSOFT)))
    assertFalse("Error is not signed in", isSignedInState(AuthUiState.Error("Test error")))
  }

  @Test
  fun isIdleState_returns_true_for_idle() {
    val idleState = AuthUiState.Idle
    assertTrue("Should identify idle state", isIdleState(idleState))
  }

  @Test
  fun isIdleState_returns_false_for_other_states() {
    assertFalse("Loading is not idle", isIdleState(AuthUiState.Loading(AuthProvider.MICROSOFT)))
    assertFalse("SignedIn is not idle", isIdleState(AuthUiState.SignedIn))
    assertFalse("Error is not idle", isIdleState(AuthUiState.Error("Test error")))
  }

  @Test
  fun navigation_decision_comprehensive_coverage() {
    // Test all combinations of states and destinations
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.SignedIn,
            AuthUiState.Error("Test error"))

    val destinations = listOf(Routes.Opening, Routes.SignIn, Routes.Home, Routes.VoiceChat, null)

    for (state in states) {
      for (destination in destinations) {
        val shouldNavigate = shouldNavigateToHomeFromSignIn(state, destination)
        // Only SignedIn on SignIn should navigate
        val expectedNavigate = state is AuthUiState.SignedIn && destination == Routes.SignIn
        assertEquals(
            "Navigation decision for state $state and destination $destination",
            expectedNavigate,
            shouldNavigate)
      }
    }
  }

  @Test
  fun error_message_handling_edge_cases() {
    // Test various error message scenarios
    val errorWithMessage = AuthUiState.Error("Specific error")
    val errorEmpty = AuthUiState.Error("")
    val errorWhitespace = AuthUiState.Error("   ")

    assertEquals(
        "Should use specific message",
        "Specific error",
        getErrorMessage(errorWithMessage, "Default"))

    assertEquals("Should use default for empty", "Default", getErrorMessage(errorEmpty, "Default"))

    // Whitespace is not empty, so it should be returned
    assertEquals(
        "Should use whitespace message", "   ", getErrorMessage(errorWhitespace, "Default"))
  }

  @Test
  fun routes_object_contains_all_constants() {
    assertNotNull("Opening route should be defined", Routes.Opening)
    assertNotNull("SignIn route should be defined", Routes.SignIn)
    assertNotNull("Home route should be defined", Routes.Home)
    assertNotNull("HomeWithDrawer route should be defined", Routes.HomeWithDrawer)
    assertNotNull("Settings route should be defined", Routes.Settings)
    assertNotNull("VoiceChat route should be defined", Routes.VoiceChat)
  }

  @Test
  fun routes_have_correct_values_and_are_unique() {
    assertEquals("opening", Routes.Opening)
    assertEquals("signin", Routes.SignIn)
    assertEquals("home", Routes.Home)
    assertEquals("home_with_drawer", Routes.HomeWithDrawer)
    assertEquals("settings", Routes.Settings)
    assertEquals("voice_chat", Routes.VoiceChat)

    val allRoutes =
        setOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat)
    assertEquals("All routes should be unique", 6, allRoutes.size)
  }

  @Test
  fun routes_follow_naming_convention() {
    val routes =
        listOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat)
    routes.forEach { route ->
      assertTrue("Route should be lowercase: $route", route == route.lowercase())
      assertFalse("Route should not contain spaces: $route", route.contains(" "))
    }
  }

  @Test
  fun navigation_edge_cases_include_empty_destination() {
    assertFalse(
        "SignedIn with empty destination should not navigate",
        shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, ""))
  }

  @Test
  fun state_checking_functions_are_mutually_exclusive() {
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.SignedIn,
            AuthUiState.Error("Test"))
    for (state in states) {
      val flags =
          listOf(
              isIdleState(state),
              isLoadingState(state),
              isSignedInState(state),
              isErrorState(state))
      assertEquals("Exactly one state predicate should be true for $state", 1, flags.count { it })
    }
  }

  @Test
  fun state_immutability_after_operations() {
    val idleState = AuthUiState.Idle
    assertTrue("Idle is idle", isIdleState(idleState))
    assertFalse("Idle is not loading", isLoadingState(idleState))
    assertFalse("Idle is not signed in", isSignedInState(idleState))
    assertFalse("Idle is not error", isErrorState(idleState))
  }

  @Test
  fun resolveAuthCommand_returns_start_signin_when_loadingMicrosoft() {
    val command = resolveAuthCommand(AuthUiState.Loading(AuthProvider.MICROSOFT), Routes.SignIn)
    assertEquals(AuthCommand.StartMicrosoftSignIn, command)
  }

  @Test
  fun resolveAuthCommand_returns_navigate_home_when_signedInOnSignIn() {
    val command = resolveAuthCommand(AuthUiState.SignedIn, Routes.SignIn)
    assertEquals(AuthCommand.NavigateHome, command)
  }

  @Test
  fun resolveAuthCommand_returns_none_for_other_states() {
    val commandIdle = resolveAuthCommand(AuthUiState.Idle, Routes.SignIn)
    val commandError = resolveAuthCommand(AuthUiState.Error("oops"), Routes.Home)
    assertEquals(AuthCommand.None, commandIdle)
    assertEquals(AuthCommand.None, commandError)
  }

  @Test
  fun executeAuthCommand_dispatches_to_correct_action() {
    var signInCalled = 0
    var navigateCalled = 0

    executeAuthCommand(AuthCommand.StartMicrosoftSignIn, { signInCalled++ }, { navigateCalled++ })
    executeAuthCommand(AuthCommand.NavigateHome, { signInCalled++ }, { navigateCalled++ })
    executeAuthCommand(AuthCommand.None, { signInCalled++ }, { navigateCalled++ })

    assertEquals(1, signInCalled)
    assertEquals(1, navigateCalled)
  }

  @Test
  fun buildAuthenticationErrorMessage_prefers_state_message() {
    val errorState = AuthUiState.Error("")
    val fallback = "Authentication failed"
    assertEquals(fallback, buildAuthenticationErrorMessage(errorState, fallback))
  }

  @Test
  fun buildAuthenticationErrorMessage_uses_fallback_for_non_error_state() {
    val fallback = "Authentication failed"
    assertEquals(fallback, buildAuthenticationErrorMessage(AuthUiState.Idle, fallback))
  }
}
