package com.android.sample.authentification

/** UI state for the Auth screen. */
sealed interface AuthUiState {
  /** Initial state. */
  object Idle : AuthUiState

  /** Shows a loading indicator for the given provider. */
  data class Loading(val provider: AuthProvider) : AuthUiState

  /** Indicates the user has successfully signed in. */
  object SignedIn : AuthUiState

  /** Represents an error that occurred during sign-in. */
  data class Error(val message: String) : AuthUiState
}

/** Authentication providers supported by the screen. */
enum class AuthProvider {
  MICROSOFT,
  SWITCH_EDU
}
