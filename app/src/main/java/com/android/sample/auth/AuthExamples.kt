package ch.epfl.euler.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.epfl.euler.viewmodel.AuthViewModel

/**
 * Exemples d'utilisation de l'authentification
 *
 * Ce fichier contient des exemples de composants Composables qui montrent comment utiliser le
 * système d'authentification dans différents scénarios.
 */

/** Exemple 1: Écran de connexion simple */
@Composable
fun SimpleLoginScreen(authViewModel: AuthViewModel, activity: Activity) {
  val authState by authViewModel.authState.collectAsStateWithLifecycle()
  val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

  when (authState) {
    is AuthState.NotInitialized -> {
      LoadingIndicator("Initialisation...")
    }
    is AuthState.Ready -> {
      LoginButton { authViewModel.signIn(activity) }
    }
    is AuthState.SigningIn -> {
      LoadingIndicator("Connexion en cours...")
    }
    is AuthState.SignedIn -> {
      currentUser?.let { user -> UserProfile(user) { authViewModel.signOut() } }
    }
    is AuthState.Error -> {
      val errorState = authState as AuthState.Error
      ErrorDisplay(errorState.message)
    }
  }
}

/** Exemple 2: Protection d'un écran (nécessite authentification) */
@Composable
fun ProtectedScreen(
    authViewModel: AuthViewModel,
    activity: Activity,
    content: @Composable (User) -> Unit
) {
  val isSignedIn by authViewModel.isSignedIn.collectAsStateWithLifecycle()
  val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

  if (isSignedIn && currentUser != null) {
    content(currentUser!!)
  } else {
    RequireAuthScreen { authViewModel.signIn(activity) }
  }
}

/** Exemple 3: Bouton de connexion/déconnexion conditionnel */
@Composable
fun AuthToggleButton(authViewModel: AuthViewModel, activity: Activity) {
  val isSignedIn by authViewModel.isSignedIn.collectAsStateWithLifecycle()
  val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()

  Button(
      onClick = {
        if (isSignedIn) {
          authViewModel.signOut()
        } else {
          authViewModel.signIn(activity)
        }
      },
      enabled = !isLoading) {
        if (isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text(if (isSignedIn) "Se déconnecter" else "Se connecter")
        }
      }
}

/** Exemple 4: Affichage de l'utilisateur dans un header */
@Composable
fun AppHeader(authViewModel: AuthViewModel) {
  val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

  Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(text = "Mon Application", style = MaterialTheme.typography.headlineSmall)

          currentUser?.let { user ->
            Column(horizontalAlignment = Alignment.End) {
              Text(text = user.username, style = MaterialTheme.typography.bodyMedium)
              Text(
                  text = "Connecté",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
          }
        }
  }
}

/** Exemple 5: Gestion d'erreur avec retry */
@Composable
fun AuthWithRetry(authViewModel: AuthViewModel, activity: Activity) {
  val authState by authViewModel.authState.collectAsStateWithLifecycle()
  val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        when (authState) {
          is AuthState.Error -> {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.size(64.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Erreur d'authentification",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error)

            errorMessage?.let {
              Text(
                  text = it,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { authViewModel.signIn(activity) }) { Text("Réessayer") }
          }
          else -> {
            SimpleLoginScreen(authViewModel, activity)
          }
        }
      }
}

/** Exemple 6: Utiliser le token pour un appel API */
@Composable
fun ApiCallExample(authViewModel: AuthViewModel) {
  val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
  var apiResponse by remember { mutableStateOf("") }

  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = "Appel API avec token", style = MaterialTheme.typography.titleMedium)

    Button(
        onClick = {
          currentUser?.accessToken?.let { token ->
            // Exemple d'utilisation du token
            apiResponse = "Appel API avec token: ${token.take(20)}..."
            // makeApiCall(token)
          }
        },
        enabled = currentUser != null) {
          Text("Appeler API")
        }

    if (apiResponse.isNotEmpty()) {
      Card(
          modifier = Modifier.fillMaxWidth(),
          colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(
                text = apiResponse,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall)
          }
    }
  }
}

// ========== Composables Helper ==========

@Composable
private fun LoadingIndicator(message: String) {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message)
      }
}

@Composable
private fun LoginButton(onClick: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = onClick) { Text("Se connecter avec Microsoft") }
      }
}

@Composable
private fun UserProfile(user: User, onSignOut: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Bienvenue !", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
          Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Email:", style = MaterialTheme.typography.labelMedium)
                Text(text = user.username, style = MaterialTheme.typography.bodyLarge)

                user.tenantId?.let { tenantId ->
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(text = "Tenant ID:", style = MaterialTheme.typography.labelMedium)
                  Text(text = tenantId, style = MaterialTheme.typography.bodySmall)
                }
              }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSignOut,
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
              Text("Se déconnecter")
            }
      }
}

@Composable
private fun ErrorDisplay(message: String) {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Erreur",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message)
      }
}

@Composable
private fun RequireAuthScreen(onSignIn: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Authentification requise", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vous devez être connecté pour accéder à cette page.",
            style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onSignIn) { Text("Se connecter") }
      }
}
