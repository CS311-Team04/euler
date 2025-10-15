package com.android.sample.auth

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface pour le repository d'authentification
 *
 * Cette interface permet de découpler la logique d'authentification de l'implémentation spécifique
 * (MSAL, Firebase, etc.) et facilite les tests unitaires.
 *
 * Supporte maintenant les utilisateurs multiples (Multi-Account).
 */
interface AuthRepository {

  /** État actuel de l'authentification */
  val authState: StateFlow<AuthState>

  /** Utilisateur actuellement sélectionné (null si aucun utilisateur) */
  val currentUser: StateFlow<User?>

  /** Liste de tous les utilisateurs connectés */
  val connectedUsers: StateFlow<List<User>>

  /**
   * Initialise le système d'authentification
   *
   * @param context Le contexte de l'application
   * @param configResourceId L'ID de la ressource de configuration
   */
  fun initialize(context: Context, configResourceId: Int)

  /**
   * Lance le processus de connexion interactive
   *
   * @param activity L'activité depuis laquelle lancer la connexion
   * @param scopes Les scopes/permissions demandés
   */
  fun signIn(activity: Activity, scopes: Array<String> = arrayOf("User.Read"))

  /** Déconnecte tous les utilisateurs */
  fun signOut()

  /**
   * Récupère un token d'accès silencieusement (sans interaction utilisateur) pour l'utilisateur
   * actuellement sélectionné
   *
   * @param scopes Les scopes/permissions demandés
   */
  fun acquireTokenSilently(scopes: Array<String> = arrayOf("User.Read"))

  /** Vérifie si des utilisateurs sont déjà connectés Utile au démarrage de l'application */
  fun checkSignedInUser()

  /** Force une nouvelle tentative de connexion après une erreur réseau */
  fun retryConnection()
}
