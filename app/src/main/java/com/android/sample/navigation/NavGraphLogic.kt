package com.android.sample.navigation

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState

/** Navigation logic extracted from NavGraph for testability */

/**
 * Determines if Microsoft authentication should be triggered
 *
 * @param authState The current authentication state
 * @return true if Microsoft sign-in should be initiated
 */
fun shouldTriggerMicrosoftAuth(authState: AuthUiState): Boolean {
  return authState is AuthUiState.Loading && authState.provider == AuthProvider.MICROSOFT
}

/**
 * Determines if navigation to Home should occur from SignIn screen
 *
 * @param authState The current authentication state
 * @param currentDestination The current navigation destination
 * @return true if should navigate to Home
 */
fun shouldNavigateToHomeFromSignIn(authState: AuthUiState, currentDestination: String?): Boolean {
  return authState is AuthUiState.SignedIn && currentDestination == Routes.SignIn
}

/**
 * Gets the error message from an error state, or default message
 *
 * @param errorState The error state
 * @param defaultMessage Default message if error state has no message
 * @return The error message
 */
fun getErrorMessage(errorState: AuthUiState.Error, defaultMessage: String): String {
  return errorState.message.ifEmpty { defaultMessage }
}

/**
 * Checks if the authentication state is in an error condition
 *
 * @param authState The current authentication state
 * @return true if state is Error
 */
fun isErrorState(authState: AuthUiState): Boolean {
  return authState is AuthUiState.Error
}

/**
 * Checks if the authentication state is in loading condition
 *
 * @param authState The current authentication state
 * @return true if state is Loading
 */
fun isLoadingState(authState: AuthUiState): Boolean {
  return authState is AuthUiState.Loading
}

/**
 * Checks if the authentication state indicates signed in
 *
 * @param authState The current authentication state
 * @return true if state is SignedIn
 */
fun isSignedInState(authState: AuthUiState): Boolean {
  return authState is AuthUiState.SignedIn
}

/**
 * Checks if the authentication state is idle
 *
 * @param authState The current authentication state
 * @return true if state is Idle
 */
fun isIdleState(authState: AuthUiState): Boolean {
  return authState is AuthUiState.Idle
}
