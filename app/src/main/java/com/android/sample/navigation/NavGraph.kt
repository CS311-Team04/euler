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
import com.android.sample.speech.SpeechPlayback
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

@VisibleForTesting
internal fun navigateSignOut(navigate: NavigateAction, startDestinationRoute: String?) {
  val startRoute = startDestinationRoute ?: Routes.Opening
  navigate(Routes.SignIn) {
    popUpTo(startRoute) { inclusive = true }
    launchSingleTop = true
    restoreState = false
  }
}

@VisibleForTesting
internal fun navigateToSettings(navigate: NavigateAction) {
  navigate(Routes.Settings) {}
}

@VisibleForTesting
internal fun navigateToProfile(navigate: NavigateAction) {
  navigate(Routes.Profile) {}
}

@VisibleForTesting
internal fun navigateToVoiceChat(navigate: NavigateAction) {
  navigate(Routes.VoiceChat) {}
}

@VisibleForTesting
internal fun handleProfileClick(
    isGuest: Boolean,
    showGuestWarning: () -> Unit,
    navigateToProfile: () -> Unit
) {
  if (isGuest) {
    showGuestWarning()
  } else {
    navigateToProfile()
  }
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNav(
    startOnSignedIn: Boolean = false,
    activity: Activity,
    speechHelper: SpeechToTextHelper,
    ttsHelper: SpeechPlayback
) {
  val nav =
      rememberNavController().also { controller -> appNavControllerObserver?.invoke(controller) }
  val authViewModel = remember { authViewModelFactory?.invoke() ?: AuthViewModel() }
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

  NavHost(
      navController = nav,
      startDestination = if (startOnSignedIn) Routes.Home else Routes.Opening) {
        // Opening Screen (new flow)
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
            val parentEntry = nav.getBackStackEntry("home_root")
            val homeViewModel: HomeViewModel = viewModel(parentEntry)
            val homeUiState by homeViewModel.uiState.collectAsState()

            // LaunchedEffect for guest mode synchronization
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
            HomeScreen(
                viewModel = homeViewModel,
                onAction1Click = { /* ... */},
                onAction2Click = { /* ... */},
                onSendMessage = { /* ... */},
                speechHelper = speechHelper,
                ttsHelper = ttsHelper,
                onSignOut = {
                  android.util.Log.d("NavGraph", "Sign out button clicked")
                  homeViewModel.clearProfile()
                  authViewModel.signOut()
                  android.util.Log.d("NavGraph", "Navigating to SignIn")
                  // Navigate to SignIn and clear entire back stack
                  val startRoute = nav.graph.startDestinationRoute ?: Routes.Opening
                  nav.navigate(Routes.SignIn) {
                    popUpTo(startRoute) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
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
            val parentEntry = nav.getBackStackEntry("home_root")
            val homeViewModel: HomeViewModel = viewModel(parentEntry)
            val homeUiState by homeViewModel.uiState.collectAsState()

            HomeScreen(
                viewModel = homeViewModel,
                onAction1Click = { /* ... */},
                onAction2Click = { /* ... */},
                onSendMessage = { /* ... */},
                speechHelper = speechHelper,
                ttsHelper = ttsHelper,
                onSignOut = {
                  android.util.Log.d("NavGraph", "Sign out button clicked (HomeWithDrawer)")
                  homeViewModel.clearProfile()
                  authViewModel.signOut()
                  android.util.Log.d("NavGraph", "Navigating to SignIn (HomeWithDrawer)")
                  // Navigate to SignIn and clear entire back stack
                  navigateSignOut(
                      navigate = { route, builder -> nav.navigate(route) { builder(this) } },
                      startDestinationRoute = nav.graph.startDestinationRoute)
                },
                onSettingsClick = {
                  navigateToSettings { route, builder -> nav.navigate(route) { builder(this) } }
                },
                onProfileClick = {
                  handleProfileClick(
                      isGuest = homeUiState.isGuest,
                      showGuestWarning = { homeViewModel.showGuestProfileWarning() },
                      navigateToProfile = {
                        navigateToProfile { route, builder ->
                          nav.navigate(route) { builder(this) }
                        }
                      })
                },
                onVoiceChatClick = {
                  navigateToVoiceChat { route, builder -> nav.navigate(route) { builder(this) } }
                },
                openDrawerOnStart = true)
          }

          // Settings
          composable(Routes.Settings) {
            val parentEntry = nav.getBackStackEntry("home_root")
            val homeViewModel: HomeViewModel = viewModel(parentEntry)
            val homeUiState by homeViewModel.uiState.collectAsState()

            SettingsPage(
                onBackClick = {
                  nav.navigate(Routes.HomeWithDrawer) { popUpTo(Routes.Home) { inclusive = false } }
                },
                onSignOut = {
                  android.util.Log.d("NavGraph", "Sign out button clicked (Settings)")
                  homeViewModel.clearProfile()
                  authViewModel.signOut()
                  android.util.Log.d("NavGraph", "Navigating to SignIn (Settings)")
                  // Navigate to SignIn and clear entire back stack
                  val startRoute = nav.graph.startDestinationRoute ?: Routes.Opening
                  nav.navigate(Routes.SignIn) {
                    popUpTo(startRoute) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
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
            val parentEntry = nav.getBackStackEntry("home_root")
            val homeViewModel: HomeViewModel = viewModel(parentEntry)
            val homeUiState by homeViewModel.uiState.collectAsState()

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
            VoiceScreen(
                onClose = { nav.popBackStack() },
                modifier = Modifier.fillMaxSize(),
                speechHelper = speechHelper)
          }
        }
      }
}
