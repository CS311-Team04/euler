package com.android.sample.navigation

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.sample.VoiceChat.VoiceScreen
import com.android.sample.auth.MicrosoftAuth
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeViewModel
import com.android.sample.settings.ProfilePage
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
  const val Profile = "profile"
  const val VoiceChat = "voice_chat"
}

@Composable
fun AppNav(startOnSignedIn: Boolean = false, activity: Activity, speechHelper: SpeechToTextHelper) {
  val nav = rememberNavController()
  val authViewModel = remember { AuthViewModel() }
  val homeViewModel: HomeViewModel = viewModel()
  val authState by authViewModel.state.collectAsState()
  val homeUiState by homeViewModel.uiState.collectAsState()

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
                val errorMessage =
                    buildAuthenticationErrorMessage(
                        currentState, exception.message ?: "Authentication failed")
                authViewModel.onAuthenticationError(errorMessage)
              })
        },
        navigateHome = {
          nav.navigate(Routes.Home) {
            popUpTo(Routes.SignIn) { inclusive = true }
            launchSingleTop = true
            restoreState = true
          }
        })
  }

  LaunchedEffect(authState) {
    when (authState) {
      is AuthUiState.Guest -> homeViewModel.setGuestMode(true)
      is AuthUiState.SignedIn -> {
        homeViewModel.setGuestMode(false)
        homeViewModel.refreshProfile()
      }
      else -> {}
    }
  }

  NavHost(
      navController = nav,
      startDestination = if (startOnSignedIn) Routes.Home else Routes.Opening) {
        // Opening Screen (nouvelle logique)
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

        // Home Screen
        composable(Routes.Home) {
          HomeScreen(
              viewModel = homeViewModel,
              onAction1Click = { /* ... */},
              onAction2Click = { /* ... */},
              onSendMessage = { /* ... */},
              speechHelper = speechHelper,
              onSignOut = {
                android.util.Log.d("NavGraph", "Sign out button clicked")
                homeViewModel.clearProfile()
                authViewModel.signOut()
                android.util.Log.d("NavGraph", "Navigating to SignIn")
                nav.navigate(Routes.SignIn) {
                  popUpTo(Routes.Home) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onSettingsClick = { nav.navigate(Routes.Settings) },
              onProfileClick = {
                if (homeUiState.isGuest) {
                  homeViewModel.showGuestProfileWarning()
                } else {
                  nav.navigate(Routes.Profile)
                }
              },
              onVoiceChatClick = { nav.navigate(Routes.VoiceChat) })
        }

        // Home With Drawer
        composable(Routes.HomeWithDrawer) {
          HomeScreen(
              viewModel = homeViewModel,
              onAction1Click = { /* ... */},
              onAction2Click = { /* ... */},
              onSendMessage = { /* ... */},
              speechHelper = speechHelper,
              onSignOut = {
                android.util.Log.d("NavGraph", "Sign out button clicked (HomeWithDrawer)")
                homeViewModel.clearProfile()
                authViewModel.signOut()
                android.util.Log.d("NavGraph", "Navigating to SignIn (HomeWithDrawer)")
                nav.navigate(Routes.SignIn) {
                  popUpTo(Routes.Home) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onSettingsClick = { nav.navigate(Routes.Settings) },
              onProfileClick = {
                if (homeUiState.isGuest) {
                  homeViewModel.showGuestProfileWarning()
                } else {
                  nav.navigate(Routes.Profile)
                }
              },
              onVoiceChatClick = { nav.navigate(Routes.VoiceChat) },
              openDrawerOnStart = true)
        }

        // Settings
        composable(Routes.Settings) {
          SettingsPage(
              onBackClick = {
                nav.navigate(Routes.HomeWithDrawer) { popUpTo(Routes.Home) { inclusive = false } }
              },
              onSignOut = {
                android.util.Log.d("NavGraph", "Sign out button clicked (Settings)")
                homeViewModel.clearProfile()
                authViewModel.signOut()
                android.util.Log.d("NavGraph", "Navigating to SignIn (Settings)")
                nav.navigate(Routes.SignIn) {
                  popUpTo(Routes.Home) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onProfileClick = {
                if (homeUiState.isGuest) {
                  homeViewModel.showGuestProfileWarning()
                } else {
                  nav.navigate(Routes.Profile)
                }
              },
              onProfileDisabledClick = { homeViewModel.showGuestProfileWarning() },
              isProfileEnabled = !homeUiState.isGuest,
              showProfileWarning = homeUiState.showGuestProfileWarning,
              onDismissProfileWarning = { homeViewModel.hideGuestProfileWarning() },
              onConnectorsClick = { nav.navigate(Routes.Settings) })
        }

        composable(Routes.Profile) {
          if (homeUiState.isGuest) {
            LaunchedEffect(Unit) {
              homeViewModel.showGuestProfileWarning()
              nav.popBackStack()
            }
          } else {
            LaunchedEffect(Unit) { homeViewModel.refreshProfile() }
            ProfilePage(
                onBackClick = { nav.popBackStack() },
                onSaveProfile = { profile -> homeViewModel.saveProfile(profile) },
                initialProfile = homeUiState.profile)
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
