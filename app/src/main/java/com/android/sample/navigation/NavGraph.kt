package com.android.sample.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
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
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNav(
    startOnSignedIn: Boolean = false,
    activity: android.app.Activity,
    speechHelper: SpeechToTextHelper
) {
  val nav = rememberNavController()
  val authViewModel = remember { AuthViewModel() }
  val authState by authViewModel.state.collectAsState()

  // Get current back stack entry
  val navBackStackEntry by nav.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route

  // Handle Microsoft authentication when loading
  LaunchedEffect(authState) {
    val currentState = authState
    when {
      shouldTriggerMicrosoftAuth(currentState) -> {
        // Handle Microsoft authentication using Firebase Auth
        MicrosoftAuth.signIn(
            activity = activity,
            onSuccess = { authViewModel.onAuthenticationSuccess() },
            onError = { exception ->
              val errorMessage =
                  if (currentState is AuthUiState.Error) {
                    getErrorMessage(currentState, exception.message ?: "Authentication failed")
                  } else {
                    exception.message ?: "Authentication failed"
                  }
              authViewModel.onAuthenticationError(errorMessage)
            })
      }
      shouldNavigateToHomeFromSignIn(currentState, currentDestination) -> {
        // Only navigate to Home from SignIn screen, not from Opening screen
        nav.navigate(Routes.Home) {
          popUpTo(Routes.SignIn) { inclusive = true }
          launchSingleTop = true
          restoreState = false
        }
      }
      isErrorState(currentState) -> {
        // Handle error state
      }
      else -> {
        /* Idle state - no navigation */
      }
    }
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
                    restoreState = false
                  }
                },
                onSettingsClick = { nav.navigate(Routes.Settings) },
                openDrawerOnStart = true)
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
      }
}
