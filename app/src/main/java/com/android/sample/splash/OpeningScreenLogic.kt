package com.android.sample.splash

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState

/** Navigation logic for OpeningScreen This function is extracted for unit testing purposes */
fun shouldNavigateToHome(authState: AuthUiState): Boolean {
  return authState is AuthUiState.SignedIn
}

/**
 * Determines which screen to navigate to based on auth state
 *
 * @return true if should navigate to home, false for sign in
 */
fun determineNavigationTarget(authState: AuthUiState): NavigationTarget {
  return when {
    authState is AuthUiState.SignedIn -> NavigationTarget.Home
    else -> NavigationTarget.SignIn
  }
}

/** Represents navigation target */
enum class NavigationTarget {
  Home,
  SignIn
}

/** Validates if the navigation logic is correct for all states */
fun validateNavigationLogic(): Boolean {
  val signedIn = AuthUiState.SignedIn
  val idle = AuthUiState.Idle
  val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
  val error = AuthUiState.Error("Test")

  // Only SignedIn should navigate to home
  val signedInGoesToHome = shouldNavigateToHome(signedIn)
  val idleGoesToHome = shouldNavigateToHome(idle)
  val loadingGoesToHome = shouldNavigateToHome(loading)
  val errorGoesToHome = shouldNavigateToHome(error)

  return signedInGoesToHome && !idleGoesToHome && !loadingGoesToHome && !errorGoesToHome
}
