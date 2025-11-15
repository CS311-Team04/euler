package com.android.sample.logic

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.android.sample.navigation.Routes

/**
 * Core navigation decision logic extracted from NavGraph. Pure Kotlin functions for testability.
 */
sealed class AuthCommand {
  object TriggerMicrosoftAuth : AuthCommand()

  object NavigateToHome : AuthCommand()

  object DoNothing : AuthCommand()
}

object NavGraphCore {

  /** Resolves what action to take based on auth state and current destination. */
  fun resolveAuthCommand(authState: AuthUiState, currentDestination: String?): AuthCommand {
    return when {
      shouldTriggerMicrosoftAuth(authState) -> AuthCommand.TriggerMicrosoftAuth
      shouldNavigateToHomeFromSignIn(authState, currentDestination) -> AuthCommand.NavigateToHome
      else -> AuthCommand.DoNothing
    }
  }

  /** Determines if Microsoft authentication should be triggered. */
  fun shouldTriggerMicrosoftAuth(authState: AuthUiState): Boolean {
    return authState is AuthUiState.Loading && authState.provider == AuthProvider.MICROSOFT
  }

  /** Determines if navigation to Home should occur from SignIn screen. */
  fun shouldNavigateToHomeFromSignIn(authState: AuthUiState, currentDestination: String?): Boolean {
    return currentDestination == Routes.SignIn &&
        (authState is AuthUiState.SignedIn || authState is AuthUiState.Guest)
  }

  /** Builds authentication error message from state and exception. */
  fun buildAuthenticationErrorMessage(authState: AuthUiState, exceptionMessage: String): String {
    return if (authState is AuthUiState.Error) {
      authState.message.ifEmpty { exceptionMessage }
    } else {
      exceptionMessage
    }
  }

  /** Determines start destination based on authentication state. */
  fun determineStartDestination(startOnSignedIn: Boolean): String {
    return if (startOnSignedIn) Routes.Home else Routes.Opening
  }

  /** Determines if profile navigation should show warning for guest users. */
  fun shouldShowGuestProfileWarning(isGuest: Boolean): Boolean {
    return isGuest
  }

  /**
   * Determines what action to take when profile is clicked. Returns true if should navigate, false
   * if should show warning.
   */
  fun shouldNavigateToProfile(isGuest: Boolean): Boolean {
    return !isGuest
  }
}
