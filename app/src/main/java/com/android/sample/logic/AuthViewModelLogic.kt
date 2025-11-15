package com.android.sample.logic

import com.android.sample.authentification.AuthUiState

/**
 * Pure Kotlin logic for AuthViewModel decision-making. Extracted from AuthViewModel for
 * testability.
 */
object AuthViewModelLogic {

  /**
   * Determines if logged status should be updated after auth state change. Only update when user
   * actually signs in, not when detecting existing auth state.
   */
  fun shouldUpdateLoggedStatusOnAuthStateChange(wasNull: Boolean, isNull: Boolean): Boolean {
    // Only update if transitioning from null to non-null (actual sign-in)
    // Not when detecting existing auth state
    return wasNull && !isNull
  }

  /** Determines if setLoggedStatus should be called after authentication success. */
  fun shouldSetLoggedStatusOnSuccess(): Boolean = true

  /** Determines if setLoggedStatus should be called after initial check. */
  fun shouldSetLoggedStatusOnInitialCheck(isAuthenticated: Boolean): Boolean {
    return isAuthenticated
  }

  /** Determines if setLoggedStatus should be called before sign out. */
  fun shouldSetLoggedStatusBeforeSignOut(): Boolean = true

  /** Validates if sign out was successful. */
  fun isSignOutSuccessful(userAfterSignOut: Boolean?): Boolean {
    return userAfterSignOut == null || userAfterSignOut == false
  }

  /** Determines next state after sign out validation. */
  fun determineStateAfterSignOut(isSuccessful: Boolean): AuthUiState {
    return AuthStateReducer.onSignOut()
  }

  /** Determines if should proceed with SwitchEdu login after state update. */
  fun shouldProceedWithSwitchEduLogin(currentState: AuthUiState): Boolean {
    return currentState is AuthUiState.Loading
  }

  /** Determines if should call setLoggedStatus after provider flow. */
  fun shouldSetLoggedStatusAfterProviderFlow(nextState: AuthUiState): Boolean {
    return nextState is AuthUiState.SignedIn
  }

  /** Handles auth state listener callback logic. */
  fun handleAuthStateChange(wasAuthenticated: Boolean?, isAuthenticated: Boolean): AuthUiState {
    return if (isAuthenticated) {
      AuthStateReducer.onAuthStateChanged(true)
    } else {
      AuthStateReducer.onAuthStateChanged(false)
    }
  }

  /** Determines error message for authentication errors. */
  fun getErrorMessage(throwable: Throwable?): String {
    return throwable?.message ?: "Unknown error"
  }
}
