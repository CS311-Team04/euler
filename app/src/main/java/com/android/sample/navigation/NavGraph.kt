package com.android.sample.navigation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.sample.VoiceChat.VoiceScreen
import com.android.sample.auth.MicrosoftAuth
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeViewModel
import com.android.sample.settings.SettingsPage
import com.android.sample.sign_in.AuthViewModel
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.splash.OpeningScreen

object Routes {
  const val Opening = "opening"
  const val SignIn = "signin"
  const val Home = "home"
  const val HomeWithDrawer = "home_with_drawer"
  const val Settings = "settings"
  const val VoiceChat = "voice_chat"
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNav(startOnSignedIn: Boolean = false, activity: Activity, speechHelper: SpeechToTextHelper) {
  val nav = rememberNavController()
  val authViewModel = remember { AuthViewModel() }
  val authState by authViewModel.state.collectAsState()

  // Get current back stack entry
  val navBackStackEntry by nav.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route

  // Handle Microsoft authentication when loading
  LaunchedEffect(authState) {
    val currentState = authState
    val command = resolveAuthCommand(currentState, currentDestination)
      executeAuthCommand(
          command,
          startMicrosoftSignIn = {
            MicrosoftAuth.signIn(
                activity = activity,
                onSuccess = { authViewModel.onAuthenticationSuccess() },
                onError = { exception ->
                  val errorMessage = buildAuthenticationErrorMessage(
                      currentState,
                      exception.message ?: "Authentication failed"
                  )
                  authViewModel.onAuthenticationError(errorMessage)
                }
            )
          },
          navigateHome = {
            nav.navigate(Routes.Home) {
              popUpTo(Routes.SignIn) { inclusive = true }
              launchSingleTop = true
              // important pour éviter de restaurer un ancien état de Home
              restoreState = false
            }
          }
      )
  }

  NavHost(
      navController = nav,
      startDestination = if (startOnSignedIn) Routes.Home else Routes.Opening) {
        // Opening Screen (new flow)
        composable(Routes.Opening) {
          OpeningScreen(
              authState = authState,
              onNavigateToSignIn = {
                nav.navigate(Routes.SignIn) {
                  popUpTo(Routes.Opening) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onNavigateToHome = {
                nav.navigate(Routes.Home) {
                  popUpTo(Routes.Opening) { inclusive = true }
                  launchSingleTop = true
                }
              })
        }

        // SignIn Screen
        composable(Routes.SignIn) {
          AuthUIScreen(
              state = authState,
              onMicrosoftLogin = { authViewModel.onMicrosoftLoginClick() },
              onSwitchEduLogin = { authViewModel.onSwitchEduLoginClick() })
        }
        navigation(startDestination = Routes.Home, route = "home_root") {
          // Home Screen
          composable(Routes.Home) {
            val parentEntry = remember { nav.getBackStackEntry("home_root") }
            val vm: HomeViewModel = viewModel(parentEntry)
            HomeScreen(
                viewModel = vm,
                onAction1Click = { /* ... */},
                onAction2Click = { /* ... */},
                onSendMessage = { /* ... */},
                speechHelper = speechHelper,
                onSignOut = {
                  android.util.Log.d("NavGraph", "Sign out button clicked")
                  authViewModel.signOut()
                  android.util.Log.d("NavGraph", "Navigating to SignIn")
                  nav.navigate(Routes.SignIn) {
                    popUpTo(Routes.Home) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                  }
                },
                onSettingsClick = { nav.navigate(Routes.Settings) },
                forceNewChatOnFirstOpen = false)
          }

          // Home With Drawer
          composable(Routes.HomeWithDrawer) {
            val parentEntry = remember { nav.getBackStackEntry("home_root") }
            val vm: HomeViewModel = viewModel(parentEntry)
            HomeScreen(
                viewModel = vm,
                onAction1Click = { /* ... */},
                onAction2Click = { /* ... */},
                onSendMessage = { /* ... */},
                speechHelper = speechHelper,
                onSignOut = {
                  android.util.Log.d("NavGraph", "Sign out button clicked (HomeWithDrawer)")
                  authViewModel.signOut()
                  android.util.Log.d("NavGraph", "Navigating to SignIn (HomeWithDrawer)")
                  nav.navigate(Routes.SignIn) {
                    popUpTo(Routes.Home) { inclusive = true }
                    launchSingleTop = true
                    // keep consistent with your Home to avoid restoring old state
                    restoreState = false
                  }
                },
                onSettingsClick = { nav.navigate(Routes.Settings) },
                openDrawerOnStart = true
            )
          }

          // Settings
          composable(Routes.Settings) {
            SettingsPage(
                onBackClick = { nav.popBackStack() },
                onSignOut = {
                  android.util.Log.d("NavGraph", "Sign out button clicked (Settings)")
                  authViewModel.signOut()
                  android.util.Log.d("NavGraph", "Navigating to SignIn (Settings)")
                  nav.navigate(Routes.SignIn) {
                    popUpTo(Routes.Home) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                  }
                })
          }
        }

        // Voice Chat Screen
        composable(Routes.VoiceChat) {
          VoiceScreen(onClose = { nav.popBackStack() }, modifier = Modifier.fillMaxSize())
        }
      }
}

internal sealed class AuthCommand {
  object StartMicrosoftSignIn : AuthCommand()

  object NavigateHome : AuthCommand()

  object None : AuthCommand()
}

@VisibleForTesting
internal fun resolveAuthCommand(authState: AuthUiState, currentDestination: String?): AuthCommand {
  return when {
    shouldTriggerMicrosoftAuth(authState) -> AuthCommand.StartMicrosoftSignIn
    shouldNavigateToHomeFromSignIn(authState, currentDestination) -> AuthCommand.NavigateHome
    else -> AuthCommand.None
  }
}

@VisibleForTesting
internal fun executeAuthCommand(
    command: AuthCommand,
    startMicrosoftSignIn: () -> Unit,
    navigateHome: () -> Unit
) {
  when (command) {
    AuthCommand.StartMicrosoftSignIn -> startMicrosoftSignIn()
    AuthCommand.NavigateHome -> navigateHome()
    AuthCommand.None -> {}
  }
}

@VisibleForTesting
internal fun buildAuthenticationErrorMessage(authState: AuthUiState, fallback: String): String {
  return if (authState is AuthUiState.Error) getErrorMessage(authState, fallback) else fallback
}
