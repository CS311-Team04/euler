package com.android.sample.auth

/** Possible states for Auth */
sealed class AuthState {
  /** Initial state */
  object NotInitialized : AuthState()

  /** MSAL is ready */
  object Ready : AuthState()

  /** Processus Signin */
  object SigningIn : AuthState()

  /** User connected */
  object SignedIn : AuthState()

  /** Error */
  data class Error(val message: String) : AuthState()
}
