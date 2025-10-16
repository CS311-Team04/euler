package com.android.sample.navigation

// import com.android.sample.authentification.AuthUIScreen
// import com.android.sample.authentification.AuthUiState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import com.android.sample.home.HomeScreen
import com.android.sample.settings.SettingsPage

object Routes {
  const val SignIn = "signin"
  const val Home = "home"
  const val HomeWithDrawer = "home_with_drawer"
  const val Settings = "settings"
}

@Composable
fun AppNav(startOnSignedIn: Boolean = false) {
  val nav = rememberNavController()

  NavHost(
      navController = nav, startDestination = if (startOnSignedIn) Routes.Home else Routes.SignIn) {
        composable(Routes.SignIn) {
          // TODO: Add AuthUIScreen when authentication is implemented
          AuthUIScreen(
              state = AuthUiState.Idle,
              onMicrosoftLogin = {
                // TODO: vrai login, puis si OK :
                nav.navigate(Routes.Home) {
                  popUpTo(Routes.SignIn) { inclusive = true }
                  launchSingleTop = true
                }
              },
              onSwitchEduLogin = {
                nav.navigate(Routes.Home) {
                  popUpTo(Routes.SignIn) { inclusive = true }
                  launchSingleTop = true
                }
              })
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
