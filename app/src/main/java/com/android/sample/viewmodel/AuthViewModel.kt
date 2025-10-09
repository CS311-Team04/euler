package ch.epfl.euler.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.epfl.euler.auth.AuthRepository
import ch.epfl.euler.auth.AuthState
import ch.epfl.euler.auth.MsalAuthRepository
import ch.epfl.euler.auth.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel pour gérer l'état de l'authentification dans l'interface utilisateur
 *
 * Ce ViewModel agit comme un pont entre l'interface utilisateur (UI) et le repository
 * d'authentification. Il expose les états sous forme de StateFlow et fournit des méthodes pour
 * déclencher les actions d'authentification.
 */
class AuthViewModel(private val authRepository: AuthRepository = MsalAuthRepository.getInstance()) :
    ViewModel() {

  // ========== États exposés à l'UI ==========

  /** État actuel de l'authentification */
  val authState: StateFlow<AuthState> = authRepository.authState

  /** Utilisateur actuellement connecté (null si non connecté) */
  val currentUser: StateFlow<User?> = authRepository.currentUser

  // ========== États calculés ==========

  /** Indique si l'utilisateur est connecté */
  val isSignedIn: StateFlow<Boolean> =
      authState
          .map { it is AuthState.SignedIn }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = false)

  /** Indique si un processus de connexion est en cours */
  val isLoading: StateFlow<Boolean> =
      authState
          .map { it is AuthState.SigningIn }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = false)

  /** Message d'erreur actuel (null si pas d'erreur) */
  val errorMessage: StateFlow<String?> =
      authState
          .map { if (it is AuthState.Error) it.message else null }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = null)

  // ========== Méthodes publiques ==========

  /**
   * Initialise le système d'authentification
   *
   * @param context Le contexte de l'application
   * @param configResourceId L'ID de la ressource de configuration MSAL
   */
  fun initializeAuth(context: Context, configResourceId: Int) {
    authRepository.initialize(context, configResourceId)
  }

  /**
   * Lance le processus de connexion interactive
   *
   * @param activity L'activité depuis laquelle lancer la connexion
   * @param scopes Les permissions Microsoft Graph demandées
   */
  fun signIn(activity: Activity, scopes: Array<String> = arrayOf("User.Read")) {
    viewModelScope.launch { authRepository.signIn(activity, scopes) }
  }

  /** Déconnecte l'utilisateur actuel */
  fun signOut() {
    viewModelScope.launch { authRepository.signOut() }
  }

  /**
   * Récupère un nouveau token d'accès silencieusement Utile pour rafraîchir un token expiré
   *
   * @param scopes Les permissions demandées
   */
  fun refreshToken(scopes: Array<String> = arrayOf("User.Read")) {
    viewModelScope.launch { authRepository.acquireTokenSilently(scopes) }
  }

  /** Vérifie si un utilisateur est déjà connecté Utile au démarrage de l'application */
  fun checkSignedInUser() {
    viewModelScope.launch { authRepository.checkSignedInUser() }
  }
}
