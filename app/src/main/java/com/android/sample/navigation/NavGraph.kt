package com.android.sample.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.auth.MicrosoftAuth
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import com.android.sample.home.HomeScreen
import com.android.sample.settings.SettingsPage
import com.android.sample.sign_in.AuthViewModel

object Routes {
  const val SignIn = "signin"
  const val Home = "home"
  const val HomeWithDrawer = "home_with_drawer"
  const val Settings = "settings"
}

@Composable
fun AppNav(startOnSignedIn: Boolean = false, activity: android.app.Activity) {
  val nav = rememberNavController()
  val authViewModel = remember { AuthViewModel() }
  val authState by authViewModel.state.collectAsState()

  // Handle Microsoft authentication when loading
  LaunchedEffect(authState) {
    val currentState = authState
    when (currentState) {
      is AuthUiState.Loading -> {
        if (currentState.provider == com.android.sample.authentification.AuthProvider.MICROSOFT) {
          // Handle Microsoft authentication using Firebase Auth
          MicrosoftAuth.signIn(
              activity = activity,
              onSuccess = { authViewModel.onAuthenticationSuccess() },
              onError = { exception ->
                authViewModel.onAuthenticationError(exception.message ?: "Authentication failed")
              })
        }
      }
      is AuthUiState.SignedIn -> {
        nav.navigate(Routes.Home) {
          popUpTo(Routes.SignIn) { inclusive = true }
          launchSingleTop = true
          restoreState = true
        }
      }
      is AuthUiState.Error -> {
        // Handle error state - could show snackbar or toast
      }
      else -> {
        /* Idle state - no navigation */
      }
    }
  }

  NavHost(
      navController = nav, startDestination = if (startOnSignedIn) Routes.Home else Routes.SignIn) {
        composable(Routes.SignIn) {
          AuthUIScreen(
              state = authState,
              onMicrosoftLogin = { authViewModel.onMicrosoftLoginClick() },
              onSwitchEduLogin = { authViewModel.onSwitchEduLoginClick() })
        }
        composable(Routes.Home) {
          HomeScreen(
              onAction1Click = { /* ... */},
              onAction2Click = { /* ... */},
              onSendMessage = { /* ... */},
              onSignOut = {
                nav.navigate(Routes.SignIn) {
                  // retire Home pour ne pas pouvoir revenir en arrière
                  popUpTo(Routes.Home) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onSettingsClick = { nav.navigate(Routes.Settings) })
        }
        composable(Routes.HomeWithDrawer) {
          HomeScreen(
              onAction1Click = { /* ... */},
              onAction2Click = { /* ... */},
              onSendMessage = { /* ... */},
              onSignOut = {
                nav.navigate(Routes.SignIn) {
                  popUpTo(Routes.Home) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onSettingsClick = { nav.navigate(Routes.Settings) },
              openDrawerOnStart = true)
        }
        composable(Routes.Settings) {
          SettingsPage(
              onBackClick = {
                nav.navigate(Routes.HomeWithDrawer) { popUpTo(Routes.Home) { inclusive = false } }
              },
              onSignOut = {
                nav.navigate(Routes.SignIn) {
                  popUpTo(Routes.Home) { inclusive = true }
                  launchSingleTop = true
                }
              })
        }
      }
}
