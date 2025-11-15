package com.android.sample.logic

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState

/**
 * Pure Kotlin logic for AuthViewModel state transitions. Extracted from AuthViewModel for
 * testability.
 */
object AuthStateReducer {

  /**
   * Determines the next state when Microsoft login is clicked. Returns Loading state if not already
   * loading, otherwise returns current state.
   */
  fun onMicrosoftLoginClick(currentState: AuthUiState): AuthUiState {
    return if (currentState is AuthUiState.Loading) {
      currentState // Don't change state if already loading
    } else {
      AuthUiState.Loading(AuthProvider.MICROSOFT)
    }
  }

  /**
   * Determines the next state when SwitchEdu login is clicked. Returns Loading state if not already
   * loading, otherwise returns current state.
   */
  fun onSwitchEduLoginClick(currentState: AuthUiState): AuthUiState {
    return if (currentState is AuthUiState.Loading) {
      currentState // Don't change state if already loading
    } else {
      AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    }
  }

  /** Determines the final state after authentication succeeds. */
  fun onAuthenticationSuccess(): AuthUiState = AuthUiState.SignedIn

  /** Determines the state after authentication error occurs. */
  fun onAuthenticationError(error: String): AuthUiState = AuthUiState.Error(error)

  /** Determines the state after sign out. */
  fun onSignOut(): AuthUiState = AuthUiState.Idle

  /** Determines the initial state based on whether user is authenticated. */
  fun determineInitialState(isAuthenticated: Boolean): AuthUiState {
    return if (isAuthenticated) {
      AuthUiState.SignedIn
    } else {
      AuthUiState.Idle
    }
  }

  /** Handles auth state change from Firebase Auth listener. */
  fun onAuthStateChanged(isAuthenticated: Boolean): AuthUiState {
    return if (isAuthenticated) {
      AuthUiState.SignedIn
    } else {
      AuthUiState.Idle
    }
  }

  /**
   * Simulates the SwitchEdu guest flow after delay. This represents the async logic that happens in
   * startSignIn.
   */
  fun processSwitchEduFlow(provider: AuthProvider): AuthUiState {
    return if (provider == AuthProvider.SWITCH_EDU) {
      AuthUiState.Guest
    } else {
      AuthUiState.SignedIn
    }
  }

  /** Checks if authentication should trigger based on current state. */
  fun shouldTriggerAuth(currentState: AuthUiState): Boolean {
    return currentState is AuthUiState.Loading
  }
}
