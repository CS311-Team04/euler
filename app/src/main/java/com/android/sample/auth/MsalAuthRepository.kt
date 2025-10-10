package ch.epfl.euler.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implémentation du repository d'authentification utilisant Microsoft MSAL
 *
 * Cette classe gère toute la logique d'authentification Microsoft (Azure AD) en utilisant la
 * bibliothèque MSAL (Microsoft Authentication Library).
 */
class MsalAuthRepository private constructor() : AuthRepository {

  companion object {
    private const val TAG = "MsalAuthRepository"

    @Volatile private var INSTANCE: MsalAuthRepository? = null

    /** Récupère l'instance singleton du repository */
    fun getInstance(): MsalAuthRepository {
      return INSTANCE
          ?: synchronized(this) { INSTANCE ?: MsalAuthRepository().also { INSTANCE = it } }
    }
  }

  private var msalApp: ISingleAccountPublicClientApplication? = null

  private val _authState = MutableStateFlow<AuthState>(AuthState.NotInitialized)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  private val _currentUser = MutableStateFlow<User?>(null)
  override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  override fun initialize(context: Context, configResourceId: Int) {
    try {
      PublicClientApplication.createSingleAccountPublicClientApplication(
          context.applicationContext,
          configResourceId,
          object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
            override fun onCreated(application: ISingleAccountPublicClientApplication) {
              msalApp = application
              Log.d(TAG, "MSAL initialisé avec succès")

              // Vérifier si un utilisateur est déjà connecté
              checkSignedInUser()
            }

            override fun onError(exception: MsalException) {
              val errorMsg = "Erreur d'initialisation MSAL: ${exception.message}"
              _authState.value = AuthState.Error(errorMsg)
              Log.e(TAG, errorMsg, exception)
            }
          })
    } catch (e: Exception) {
      val errorMsg = "Exception lors de l'initialisation: ${e.message}"
      _authState.value = AuthState.Error(errorMsg)
      Log.e(TAG, errorMsg, e)
    }
  }

  override fun checkSignedInUser() {
    msalApp?.getCurrentAccountAsync(
        object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
          override fun onAccountLoaded(activeAccount: IAccount?) {
            if (activeAccount != null) {
              val user =
                  User(
                      username = activeAccount.username,
                      accessToken = "", // Le token sera récupéré silencieusement si nécessaire
                      tenantId = activeAccount.tenantId)
              _currentUser.value = user
              _authState.value = AuthState.SignedIn
              Log.d(TAG, "Utilisateur déjà connecté: ${user.username}")
            } else {
              _authState.value = AuthState.Ready
              Log.d(TAG, "Aucun utilisateur connecté")
            }
          }

          override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
            if (currentAccount != null) {
              val user =
                  User(
                      username = currentAccount.username,
                      accessToken = "",
                      tenantId = currentAccount.tenantId)
              _currentUser.value = user
              _authState.value = AuthState.SignedIn
            } else {
              _currentUser.value = null
              _authState.value = AuthState.Ready
            }
          }

          override fun onError(exception: MsalException) {
            Log.w(TAG, "Erreur lors de la vérification du compte", exception)
            _authState.value = AuthState.Ready
          }
        })
  }

  override fun signIn(activity: Activity, scopes: Array<String>) {
    val app = msalApp
    if (app == null) {
      val errorMsg = "MSAL non initialisé. Appelez initialize() d'abord."
      _authState.value = AuthState.Error(errorMsg)
      Log.e(TAG, errorMsg)
      return
    }

    _authState.value = AuthState.SigningIn
    Log.d(TAG, "Démarrage de la connexion avec scopes: ${scopes.joinToString()}")

    app.signIn(
        activity,
        null, // loginHint
        scopes,
        object : AuthenticationCallback {
          override fun onSuccess(result: IAuthenticationResult) {
            val user =
                User(
                    username = result.account.username,
                    accessToken = result.accessToken,
                    tenantId = result.account.tenantId)
            _currentUser.value = user
            _authState.value = AuthState.SignedIn
            Log.d(TAG, "Connexion réussie: ${user.username}")
          }

          override fun onError(exception: MsalException) {
            val errorMsg = "Erreur de connexion: ${exception.message}"
            _authState.value = AuthState.Error(errorMsg)
            Log.e(TAG, errorMsg, exception)
            Log.e(TAG, "Code d'erreur MSAL: ${exception.errorCode}")
          }

          override fun onCancel() {
            _authState.value = AuthState.Ready
            Log.d(TAG, "Connexion annulée par l'utilisateur")
          }
        })
  }

  override fun signOut() {
    val app = msalApp
    if (app == null) {
      Log.w(TAG, "Tentative de déconnexion alors que MSAL n'est pas initialisé")
      return
    }

    Log.d(TAG, "Déconnexion en cours...")

    app.signOut(
        object : ISingleAccountPublicClientApplication.SignOutCallback {
          override fun onSignOut() {
            _currentUser.value = null
            _authState.value = AuthState.Ready
            Log.d(TAG, "Déconnexion réussie")
          }

          override fun onError(exception: MsalException) {
            val errorMsg = "Erreur de déconnexion: ${exception.message}"
            _authState.value = AuthState.Error(errorMsg)
            Log.e(TAG, errorMsg, exception)
          }
        })
  }

  override fun acquireTokenSilently(scopes: Array<String>) {
    val app = msalApp
    val user = _currentUser.value

    if (app == null) {
      Log.e(TAG, "MSAL non initialisé")
      return
    }

    if (user == null) {
      val errorMsg = "Aucun utilisateur connecté pour renouveler le token"
      Log.w(TAG, errorMsg)
      _authState.value = AuthState.Error(errorMsg)
      return
    }

    Log.d(TAG, "Renouvellement silencieux du token...")

    app.acquireTokenSilentAsync(
        scopes,
        user.username,
        object : SilentAuthenticationCallback {
          override fun onSuccess(result: IAuthenticationResult) {
            val updatedUser =
                User(
                    username = result.account.username,
                    accessToken = result.accessToken,
                    tenantId = result.account.tenantId)
            _currentUser.value = updatedUser
            Log.d(TAG, "Token renouvelé avec succès")
          }

          override fun onError(exception: MsalException) {
            Log.w(TAG, "Impossible de renouveler le token silencieusement", exception)

            // Si le token est invalide, déconnecter l'utilisateur
            if (exception.errorCode == "invalid_grant") {
              Log.w(TAG, "Token expiré, déconnexion de l'utilisateur")
              signOut()
            }
          }
        })
  }
}
