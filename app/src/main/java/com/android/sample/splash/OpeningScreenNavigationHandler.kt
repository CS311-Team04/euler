package com.android.sample.splash

import com.android.sample.authentification.AuthUiState
import kotlinx.coroutines.delay

/** Navigation handler for OpeningScreen Extracted for testability */

/**
 * Delays and then determines navigation action based on auth state This simulates the
 * LaunchedEffect behavior for testing
 *
 * @param authState The current authentication state
 * @param delayMillis Delay in milliseconds before navigation
 * @param onNavigateToHome Callback for home navigation
 * @param onNavigateToSignIn Callback for sign-in navigation
 */
suspend fun handleOpeningScreenNavigation(
    authState: AuthUiState,
    delayMillis: Long = 2500L,
    onNavigateToHome: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
  delay(delayMillis)
  val target = determineNavigationTarget(authState)
  when (target) {
    NavigationTarget.Home -> onNavigateToHome()
    NavigationTarget.SignIn -> onNavigateToSignIn()
  }
}

/**
 * Gets the background color used in OpeningScreen
 *
 * @return The background color as Long (ARGB)
 */
fun getOpeningScreenBackgroundColor(): Long {
  return 0xFF121212
}

/**
 * Gets the delay duration for opening screen
 *
 * @return Delay in milliseconds
 */
fun getOpeningScreenDelay(): Long {
  return 2500L
}
