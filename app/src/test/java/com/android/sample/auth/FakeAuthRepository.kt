package com.android.sample.auth

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implémentation fake du AuthRepository pour les tests
 *
 * Cette classe permet de simuler le comportement de l'authentification sans avoir besoin de MSAL ou
 * d'une vraie connexion Microsoft.
 */
class FakeAuthRepository : AuthRepository {

  private val _authState = MutableStateFlow<AuthState>(AuthState.NotInitialized)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  private val _currentUser = MutableStateFlow<User?>(null)
  override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  // Variables pour contrôler le comportement dans les tests
  var shouldFailSignIn = false
  var shouldFailSignOut = false
  var signInDelay = 0L
  var mockUser: User? =
      User(
          username = "test@example.com",
          accessToken = "fake-token-123",
          tenantId = "fake-tenant-id")

  override fun initialize(context: Context, configResourceId: Int) {
    _authState.value = AuthState.Ready
  }

  override fun signIn(activity: Activity, scopes: Array<String>) {
    _authState.value = AuthState.SigningIn

    // Simuler un délai asynchrone si nécessaire
    if (signInDelay > 0) {
      Thread.sleep(signInDelay)
    }

    if (shouldFailSignIn) {
      _authState.value = AuthState.Error("Échec de connexion simulé")
    } else {
      _currentUser.value = mockUser
      _authState.value = AuthState.SignedIn
    }
  }

  override fun signOut() {
    if (shouldFailSignOut) {
      _authState.value = AuthState.Error("Échec de déconnexion simulé")
    } else {
      _currentUser.value = null
      _authState.value = AuthState.Ready
    }
  }

  override fun acquireTokenSilently(scopes: Array<String>) {
    if (_currentUser.value != null) {
      // Simuler un renouvellement de token
      _currentUser.value =
          _currentUser.value?.copy(accessToken = "refreshed-token-${System.currentTimeMillis()}")
    }
  }

  override fun checkSignedInUser() {
    // Pour les tests, on peut simuler qu'un utilisateur est déjà connecté
    if (_currentUser.value != null) {
      _authState.value = AuthState.SignedIn
    } else {
      _authState.value = AuthState.Ready
    }
  }

  // Méthodes helper pour les tests
  fun reset() {
    _authState.value = AuthState.NotInitialized
    _currentUser.value = null
    shouldFailSignIn = false
    shouldFailSignOut = false
    signInDelay = 0L
  }

  fun setSignedInUser(user: User) {
    _currentUser.value = user
    _authState.value = AuthState.SignedIn
  }
}
