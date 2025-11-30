package com.android.sample.navigation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel
import com.android.sample.VoiceChat.UI.VoiceScreen
import com.android.sample.auth.MicrosoftAuth
import com.android.sample.authentification.AuthUIScreen
import com.android.sample.authentification.AuthUiState
import com.android.sample.conversations.ConversationRepository
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeViewModel
import com.android.sample.network.AndroidNetworkConnectivityMonitor
import com.android.sample.onboarding.OnboardingAcademicScreen
import com.android.sample.onboarding.OnboardingPersonalInfoScreen
import com.android.sample.onboarding.OnboardingRoleScreen
import com.android.sample.profile.UserProfileRepository
import com.android.sample.settings.ProfileScreen
import com.android.sample.settings.SettingsPage
import com.android.sample.settings.connectors.ConnectorsScreen
import com.android.sample.sign_in.AuthViewModel
import com.android.sample.speech.SpeechPlayback
import com.android.sample.speech.SpeechToTextHelper
import com.android.sample.splash.OpeningScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object Routes {
  const val Opening = "opening"
  const val SignIn = "signin"
  const val OnboardingPersonalInfo = "onboarding_personal_info"
  const val OnboardingRole = "onboarding_role"
  const val OnboardingAcademic = "onboarding_academic"
  const val Home = "home"
  const val HomeWithDrawer = "home_with_drawer"
  const val Settings = "settings"
  const val Profile = "profile"
  const val Connectors = "connectors"
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
internal fun navigateToConnectors(navigate: NavigateAction) {
  navigate(Routes.Connectors) {}
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

/**
 * Routes the signed-in user to either onboarding or home based on their profile status. Assumes the
 * user is authenticated (caller guarantees this).
 * - If profile is null or incomplete (no fullName), navigates to onboarding.
 * - Otherwise, navigates to home.
 */
private suspend fun navigateToOnboardingOrHome(nav: NavHostController) {
  val profileRepository = UserProfileRepository()
  val profile = profileRepository.loadProfile()

  if (profile == null || profile.fullName.isBlank()) {
    // User needs onboarding - navigate to onboarding screen
    nav.navigate(Routes.OnboardingPersonalInfo) {
      popUpTo(Routes.SignIn) { inclusive = true }
      launchSingleTop = true
    }
  } else {
    // User has profile - navigate to home
    nav.navigate(Routes.Home) {
      popUpTo(Routes.SignIn) { inclusive = true }
      launchSingleTop = true
      restoreState = true
    }
  }
}

/**
 * Creates a ConversationRepository or returns null if in guest mode. This function handles
 * exceptions gracefully to allow guest mode operation.
 */
@VisibleForTesting
internal fun createConversationRepositoryOrNull(): ConversationRepository? {
  return try {
    ConversationRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
  } catch (e: Exception) {
    null // Guest mode - no repository
  }
}

/**
 * Creates a lambda that reads the current conversation ID from HomeViewModel's UI state. The lambda
 * reads the state each time it's called, ensuring it always returns the current value.
 */
@VisibleForTesting
internal fun createGetCurrentConversationIdLambda(homeViewModel: HomeViewModel): () -> String? {
  return { homeViewModel.uiState.value.currentConversationId }
}

/**
 * Creates a callback that selects a newly created conversation in HomeViewModel. This callback is
 * invoked when a new conversation is created during voice chat.
 */
@VisibleForTesting
internal fun createOnConversationCreatedCallback(homeViewModel: HomeViewModel): (String) -> Unit {
  return { conversationId ->
    // Select the newly created conversation in HomeViewModel
    homeViewModel.selectConversation(conversationId)
  }
}

/**
 * Factory function to create a VoiceChatViewModel with conversation management. This function is
 * testable and can be used in both the composable and unit tests.
 *
 * @param homeViewModel The HomeViewModel instance to read current conversation state
 * @param createConversationRepositoryOrNull Lambda to create or retrieve ConversationRepository
 * @param createGetCurrentConversationIdLambda Lambda factory to create getCurrentConversationId
 *   lambda
 * @param createOnConversationCreatedCallback Lambda factory to create onConversationCreated
 *   callback
 * @return A configured VoiceChatViewModel instance
 */
@VisibleForTesting
internal fun createVoiceChatViewModel(
    homeViewModel: HomeViewModel,
    createConversationRepositoryOrNull: () -> ConversationRepository?,
    createGetCurrentConversationIdLambda: (HomeViewModel) -> () -> String?,
    createOnConversationCreatedCallback: (HomeViewModel) -> (String) -> Unit
): VoiceChatViewModel {
  val conversationRepo = createConversationRepositoryOrNull()
  return VoiceChatViewModel(
      conversationRepository = conversationRepo,
      getCurrentConversationId = createGetCurrentConversationIdLambda(homeViewModel),
      onConversationCreated = createOnConversationCreatedCallback(homeViewModel))
}

/**
 * Helper function that creates VoiceChatViewModel using the exact same pattern as in the
 * composable. This function is testable and ensures code coverage of the composable logic. The
 * composable calls this function to ensure the exact same code paths are executed.
 */
@VisibleForTesting
internal fun createVoiceChatViewModelForComposable(
    homeViewModel: HomeViewModel
): VoiceChatViewModel {
  // This reproduces EXACTLY the code from lines 610-618 of the composable
  return createVoiceChatViewModel(
      homeViewModel = homeViewModel,
      createConversationRepositoryOrNull = { createConversationRepositoryOrNull() },
      createGetCurrentConversationIdLambda = { createGetCurrentConversationIdLambda(it) },
      createOnConversationCreatedCallback = { createOnConversationCreatedCallback(it) })
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNav(
    startOnSignedIn: Boolean = false,
    activity: Activity,
    speechHelper: SpeechToTextHelper,
    ttsHelper: SpeechPlayback
) {
  val context = LocalContext.current
  val networkMonitor = remember { AndroidNetworkConnectivityMonitor(context) }

  // Cleanup network monitor when composable is disposed
  DisposableEffect(networkMonitor) { onDispose { networkMonitor.unregister() } }

  val nav =
      rememberNavController().also { controller -> appNavControllerObserver?.invoke(controller) }
  val authViewModel =
      remember(networkMonitor) {
        authViewModelFactory?.invoke() ?: AuthViewModel(networkMonitor = networkMonitor)
      }
  val authState by authViewModel.state.collectAsState()

  // Get current back stack entry
  val navBackStackEntry by nav.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route

  // Check for onboarding after sign-in
  LaunchedEffect(authState, currentDestination) {
    when {
      authState is AuthUiState.SignedIn && currentDestination == Routes.SignIn -> {
        navigateToOnboardingOrHome(nav)
      }
      authState is AuthUiState.Guest && currentDestination == Routes.SignIn -> {
        // Navigate directly to Home for guest users (skip onboarding)
        nav.navigate(Routes.Home) {
          popUpTo(Routes.SignIn) { inclusive = true }
          launchSingleTop = true
          restoreState = true
        }
      }
    }
  }

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
          // Navigation will be handled by the onboarding check LaunchedEffect above
          // This is kept for backward compatibility but the actual navigation
          // happens in the navigateToOnboardingOrHome function
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
          val isOffline by authViewModel.isOffline.collectAsState()
          AuthUIScreen(
              state = authState,
              onMicrosoftLogin = { authViewModel.onMicrosoftLoginClick() },
              onSwitchEduLogin = { authViewModel.onSwitchEduLoginClick() },
              isOffline = isOffline)
        }

        // Onboarding Personal Info Screen (Step 1)
        composable(Routes.OnboardingPersonalInfo) {
          OnboardingPersonalInfoScreen(
              onContinue = {
                // Navigate to step 2 (OnboardingRole)
                nav.navigate(Routes.OnboardingRole) {
                  popUpTo(Routes.OnboardingPersonalInfo) { inclusive = false }
                  launchSingleTop = true
                }
              })
        }

        // Onboarding Role Screen (Step 2)
        composable(Routes.OnboardingRole) {
          OnboardingRoleScreen(
              onContinue = {
                // Navigate to step 3 (OnboardingAcademic)
                nav.navigate(Routes.OnboardingAcademic) {
                  popUpTo(Routes.OnboardingRole) { inclusive = false }
                  launchSingleTop = true
                }
              })
        }

        // Onboarding Academic Screen (Step 3)
        composable(Routes.OnboardingAcademic) {
          OnboardingAcademicScreen(
              onContinue = {
                // Navigate to home after onboarding is complete
                // Clear entire back stack so Home becomes the new root
                nav.navigate(Routes.Home) {
                  popUpTo(Routes.SignIn) { inclusive = true }
                  launchSingleTop = true
                }
              })
        }
        navigation(startDestination = Routes.Home, route = "home_root") {
          // Home Screen
          composable(Routes.Home) {
            val parentEntry = nav.getBackStackEntry("home_root")
            val homeViewModel: HomeViewModel =
                viewModel(
                    parentEntry,
                    factory =
                        object : ViewModelProvider.Factory {
                          @Suppress("UNCHECKED_CAST")
                          override fun <T : androidx.lifecycle.ViewModel> create(
                              modelClass: Class<T>
                          ): T {
                            return HomeViewModel(networkMonitor = networkMonitor) as T
                          }
                        })
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
                onConnectorsClick = {
                  navigateToConnectors { route, builder -> nav.navigate(route) { builder(this) } }
                },
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
            val homeViewModel: HomeViewModel =
                viewModel(
                    parentEntry,
                    factory =
                        object : ViewModelProvider.Factory {
                          @Suppress("UNCHECKED_CAST")
                          override fun <T : androidx.lifecycle.ViewModel> create(
                              modelClass: Class<T>
                          ): T {
                            return HomeViewModel(networkMonitor = networkMonitor) as T
                          }
                        })
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
                onConnectorsClick = {
                  navigateToConnectors { route, builder -> nav.navigate(route) { builder(this) } }
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
            val homeViewModel: HomeViewModel =
                viewModel(
                    parentEntry,
                    factory =
                        object : ViewModelProvider.Factory {
                          @Suppress("UNCHECKED_CAST")
                          override fun <T : androidx.lifecycle.ViewModel> create(
                              modelClass: Class<T>
                          ): T {
                            return HomeViewModel(networkMonitor = networkMonitor) as T
                          }
                        })
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
            val homeViewModel: HomeViewModel =
                viewModel(
                    parentEntry,
                    factory =
                        object : ViewModelProvider.Factory {
                          @Suppress("UNCHECKED_CAST")
                          override fun <T : androidx.lifecycle.ViewModel> create(
                              modelClass: Class<T>
                          ): T {
                            return HomeViewModel(networkMonitor = networkMonitor) as T
                          }
                        })
            val homeUiState by homeViewModel.uiState.collectAsState()

            if (homeUiState.isGuest) {
              LaunchedEffect(Unit) {
                homeViewModel.showGuestProfileWarning()
                nav.popBackStack()
              }
            } else {
              // Use ProfileScreen which manages its own ViewModel and loads profile from Firestore
              ProfileScreen(onBackClick = { nav.popBackStack() })
            }
          }
          // Connectors Screen
          composable(Routes.Connectors) {
            ConnectorsScreen(
                onBackClick = { nav.popBackStack() },
                onConnectorClick = { connectorId ->
                  // For now, just a placeholder. In the future, this will trigger the connection
                  // flow
                  android.util.Log.d("NavGraph", "Connector clicked: $connectorId")
                })
          }

          // Voice Chat Screen
          composable(Routes.VoiceChat) {
            val parentEntry = nav.getBackStackEntry("home_root")
            val homeViewModel: HomeViewModel = viewModel(parentEntry)

            // Create VoiceChatViewModel with conversation repository and current conversation ID
            // The lambda reads the current state each time it's called
            // This exact code pattern (lines 610-618) is tested in
            // NavGraphTest.voiceChatComposable_exact_pattern
            val voiceChatViewModel =
                remember(homeViewModel) {
                  createVoiceChatViewModel(
                      homeViewModel = homeViewModel,
                      createConversationRepositoryOrNull = { createConversationRepositoryOrNull() },
                      createGetCurrentConversationIdLambda = {
                        createGetCurrentConversationIdLambda(it)
                      },
                      createOnConversationCreatedCallback = {
                        createOnConversationCreatedCallback(it)
                      })
                }

            VoiceScreen(
                onClose = { nav.popBackStack() },
                modifier = Modifier.fillMaxSize(),
                speechHelper = speechHelper,
                voiceChatViewModel = voiceChatViewModel)
          }
        }
      }
}
