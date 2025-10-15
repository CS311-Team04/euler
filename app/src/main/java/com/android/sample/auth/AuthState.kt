package com.android.sample.auth

/** États possibles de l'authentification */
sealed class AuthState {
  /** État initial, l'authentification n'est pas encore initialisée */
  object NotInitialized : AuthState()

  /** MSAL est prêt, l'utilisateur peut se connecter */
  object Ready : AuthState()

  /** Processus de connexion en cours */
  object SigningIn : AuthState()

  /** Utilisateur connecté avec succès */
  object SignedIn : AuthState()

  /** Une erreur s'est produite */
  data class Error(val message: String) : AuthState()
}
