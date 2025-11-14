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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.sample.VoiceChat.UI.VoiceScreen
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

@VisibleForTesting internal var appNavControllerObserver: ((NavHostController) -> Unit)? = null

@VisibleForTesting internal var authViewModelFactory: (() -> AuthViewModel)? = null

internal typealias NavigateAction = (String, NavOptionsBuilder.() -> Unit) -> Unit

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNav(startOnSignedIn: Boolean = false, activity: Activity, speechHelper: SpeechToTextHelper) {
  val nav =
      rememberNavController().also { controller -> appNavControllerObserver?.invoke(controller) }
  val authViewModel = remember { authViewModelFactory?.invoke() ?: AuthViewModel() }
  val authState by authViewModel.state.collectAsState()
  val homeViewModel: HomeViewModel = viewModel()
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
          navigateHomeFromSignIn { route, builder -> nav.navigate(route) { builder(this) } }
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
        composable(Routes.Opening) {
          OpeningScreen(
              authState = authState,
              onNavigateToSignIn = {
                navigateOpeningToSignIn { route, builder -> nav.navigate(route) { builder(this) } }
              },
              onNavigateToHome = {
                navigateOpeningToHome { route, builder -> nav.navigate(route) { builder(this) } }
              })
        }

        composable(Routes.SignIn) {
          AuthUIScreen(
              state = authState,
              onMicrosoftLogin = { authViewModel.onMicrosoftLoginClick() },
              onSwitchEduLogin = { authViewModel.onSwitchEduLoginClick() })
        }

        navigation(startDestination = Routes.Home, route = "home_root") {
          composable(Routes.Home) {
            HomeScreen(
                viewModel = homeViewModel,
                onAction1Click = { /* TODO hook */},
                onAction2Click = { /* TODO hook */},
                onSendMessage = { /* TODO hook */},
                speechHelper = speechHelper,
                onSignOut = {
                  homeViewModel.clearProfile()
                  authViewModel.signOut()
                  navigateHomeToSignIn { route, builder -> nav.navigate(route) { builder(this) } }
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

          composable(Routes.HomeWithDrawer) {
            HomeScreen(
                viewModel = homeViewModel,
                onAction1Click = { /* TODO hook */},
                onAction2Click = { /* TODO hook */},
                onSendMessage = { /* TODO hook */},
                speechHelper = speechHelper,
                onSignOut = {
                  homeViewModel.clearProfile()
                  authViewModel.signOut()
                  navigateHomeToSignIn { route, builder -> nav.navigate(route) { builder(this) } }
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

          composable(Routes.Settings) {
            SettingsPage(
                onBackClick = {
                  navigateSettingsBack { route, builder -> nav.navigate(route) { builder(this) } }
                },
                onSignOut = {
                  homeViewModel.clearProfile()
                  authViewModel.signOut()
                  navigateHomeToSignIn { route, builder -> nav.navigate(route) { builder(this) } }
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
        }

        composable(Routes.VoiceChat) {
          VoiceScreen(
              onClose = { nav.popBackStack() },
              modifier = Modifier.fillMaxSize(),
              speechHelper = speechHelper)
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

@VisibleForTesting
internal fun navigateOpeningToSignIn(navigate: NavigateAction) {
  navigate(Routes.SignIn) {
    popUpTo(Routes.Opening) { inclusive = true }
    launchSingleTop = true
  }
}

@VisibleForTesting
internal fun navigateOpeningToHome(navigate: NavigateAction) {
  navigate(Routes.Home) {
    popUpTo(Routes.Opening) { inclusive = true }
    launchSingleTop = true
  }
}

@VisibleForTesting
internal fun navigateHomeFromSignIn(navigate: NavigateAction) {
  navigate(Routes.Home) {
    popUpTo(Routes.SignIn) { inclusive = true }
    launchSingleTop = true
    restoreState = true
  }
}

@VisibleForTesting
internal fun navigateHomeToSignIn(navigate: NavigateAction) {
  navigate(Routes.SignIn) {
    popUpTo(Routes.Home) { inclusive = true }
    launchSingleTop = true
  }
}

@VisibleForTesting
internal fun navigateSettingsBack(navigate: NavigateAction) {
  navigate(Routes.HomeWithDrawer) { popUpTo(Routes.Home) { inclusive = false } }
}
