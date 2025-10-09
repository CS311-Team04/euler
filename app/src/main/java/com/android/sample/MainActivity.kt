package ch.epfl.euler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.epfl.euler.R
import ch.epfl.euler.auth.AuthState
import ch.epfl.euler.ui.theme.SampleAppTheme
import ch.epfl.euler.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

  private val authViewModel: AuthViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialiser l'authentification
    authViewModel.initializeAuth(this, R.raw.msal_config)

    setContent {
      SampleAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AuthScreen(authViewModel = authViewModel, activity = this@MainActivity)
        }
      }
    }
  }
}

@Composable
fun AuthScreen(authViewModel: AuthViewModel, activity: MainActivity) {
  val authState by authViewModel.authState.collectAsStateWithLifecycle()
  val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
  val errorMessage by authViewModel.errorMessage.collectAsStateWithLifecycle()

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        when (authState) {
          is AuthState.NotInitialized -> {
            Text(text = "Initialisation...", style = MaterialTheme.typography.bodyLarge)
          }
          is AuthState.Ready -> {
            LoginContent(onSignInClick = { authViewModel.signIn(activity) })
          }
          is AuthState.SigningIn -> {
            SigningInContent()
          }
          is AuthState.SignedIn -> {
            currentUser?.let { user ->
              SignedInContent(user = user, onSignOutClick = { authViewModel.signOut() })
            }
          }
          is AuthState.Error -> {
            val errorState = authState as AuthState.Error
            ErrorContent(
                errorMessage = errorState.message, onRetryClick = { /* Optionnel: retry logic */})
          }
        }

        // Affichage des erreurs
        errorMessage?.let { error ->
          Card(
              modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = "Erreur: $error",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer)
              }
        }
      }
}

@Composable
fun LoginContent(onSignInClick: () -> Unit) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Connexion Microsoft", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "Connectez-vous avec votre compte Microsoft pour continuer",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp))

        Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth()) {
          Text("Se connecter avec Microsoft")
        }
      }
}

@Composable
fun SigningInContent() {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator()
        Text(text = "Connexion en cours...", style = MaterialTheme.typography.bodyLarge)
      }
}

@Composable
fun SignedInContent(user: ch.epfl.euler.auth.User, onSignOutClick: () -> Unit) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Connexion réussie !",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
              Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Utilisateur connecté:",
                        style = MaterialTheme.typography.labelMedium)
                    Text(text = user.username, style = MaterialTheme.typography.bodyLarge)
                    user.tenantId?.let { tenantId ->
                      Text(
                          text = "Tenant ID: $tenantId", style = MaterialTheme.typography.bodySmall)
                    }
                  }
            }

        Button(
            onClick = onSignOutClick,
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
              Text("Se déconnecter")
            }
      }
}

@Composable
fun ErrorContent(errorMessage: String, onRetryClick: () -> Unit) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Erreur de connexion",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error)

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp))

        Button(onClick = onRetryClick) { Text("Réessayer") }
      }
}
