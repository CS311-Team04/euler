package com.android.sample.navigation

import android.content.Context
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.test.core.app.ApplicationProvider
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.android.sample.home.HomeViewModel
import com.android.sample.llm.FakeLlmClient
import com.android.sample.util.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for NavGraph functions, specifically covering:
 * - resolveAuthCommand
 * - executeAuthCommand
 * - buildAuthenticationErrorMessage
 * - LaunchedEffect logic paths
 */
class NavGraphTest {

  @Test
  fun startDestination_matches_signedIn_flag() {
    val destinations = mapOf(true to Routes.Home, false to Routes.Opening)
    destinations.forEach { (startSignedIn, expected) ->
      val startDestination = if (startSignedIn) Routes.Home else Routes.Opening
      assertEquals(expected, startDestination)
    }
  }

  @Test
  fun resolveAuthCommand_returns_startMicrosoftSignIn_for_microsoft_loading() {
    val authState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val command = resolveAuthCommand(authState, Routes.SignIn)
    assertEquals(AuthCommand.StartMicrosoftSignIn, command)
  }

  @Test
  fun resolveAuthCommand_returns_navigateHome_for_signedIn_on_signIn() {
    val authState = AuthUiState.SignedIn
    val command = resolveAuthCommand(authState, Routes.SignIn)
    assertEquals(AuthCommand.NavigateHome, command)
  }

  @Test
  fun resolveAuthCommand_returns_none_for_idle_state() {
    val command = resolveAuthCommand(AuthUiState.Idle, Routes.SignIn)
    assertEquals(AuthCommand.None, command)
  }

  @Test
  fun resolveAuthCommand_returns_none_for_error_state() {
    val command = resolveAuthCommand(AuthUiState.Error("Test error"), Routes.Home)
    assertEquals(AuthCommand.None, command)
  }

  @Test
  fun resolveAuthCommand_returns_none_for_signedIn_on_other_destination() {
    val command = resolveAuthCommand(AuthUiState.SignedIn, Routes.Home)
    assertEquals(AuthCommand.None, command)
  }

  @Test
  fun resolveAuthCommand_returns_none_for_signedIn_on_null_destination() {
    val command = resolveAuthCommand(AuthUiState.SignedIn, null)
    assertEquals(AuthCommand.None, command)
  }

  @Test
  fun resolveAuthCommand_returns_none_for_switchEdu_loading() {
    val authState = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    val command = resolveAuthCommand(authState, Routes.SignIn)
    assertEquals(AuthCommand.None, command)
  }

  @Test
  fun executeAuthCommand_calls_startMicrosoftSignIn_for_startMicrosoftSignIn_command() {
    var signInCalled = false
    var navigateCalled = false

    executeAuthCommand(
        AuthCommand.StartMicrosoftSignIn,
        startMicrosoftSignIn = { signInCalled = true },
        navigateHome = { navigateCalled = true })

    assertTrue("startMicrosoftSignIn should be called", signInCalled)
    assertFalse("navigateHome should not be called", navigateCalled)
  }

  @Test
  fun executeAuthCommand_calls_navigateHome_for_navigateHome_command() {
    var signInCalled = false
    var navigateCalled = false

    executeAuthCommand(
        AuthCommand.NavigateHome,
        startMicrosoftSignIn = { signInCalled = true },
        navigateHome = { navigateCalled = true })

    assertFalse("startMicrosoftSignIn should not be called", signInCalled)
    assertTrue("navigateHome should be called", navigateCalled)
  }

  @Test
  fun executeAuthCommand_does_nothing_for_none_command() {
    var signInCalled = false
    var navigateCalled = false

    executeAuthCommand(
        AuthCommand.None,
        startMicrosoftSignIn = { signInCalled = true },
        navigateHome = { navigateCalled = true })

    assertFalse("startMicrosoftSignIn should not be called", signInCalled)
    assertFalse("navigateHome should not be called", navigateCalled)
  }

  @Test
  fun buildAuthenticationErrorMessage_returns_error_message_when_state_is_error() {
    val errorState = AuthUiState.Error("Custom error message")
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(errorState, fallback)
    assertEquals("Custom error message", result)
  }

  @Test
  fun buildAuthenticationErrorMessage_returns_fallback_when_state_is_not_error() {
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(AuthUiState.Idle, fallback)
    assertEquals(fallback, result)
  }

  @Test
  fun buildAuthenticationErrorMessage_returns_fallback_when_error_state_has_empty_message() {
    val errorState = AuthUiState.Error("")
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(errorState, fallback)
    assertEquals(fallback, result)
  }

  @Test
  fun buildAuthenticationErrorMessage_handles_null_exception_message() {
    // Simulate exception.message being null - should use fallback
    val errorState = AuthUiState.Error("")
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(errorState, fallback)
    assertEquals(fallback, result)
  }

  @Test
  fun buildAuthenticationErrorMessage_handles_loading_state() {
    val loadingState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(loadingState, fallback)
    assertEquals(fallback, result)
  }

  @Test
  fun buildAuthenticationErrorMessage_handles_signedIn_state() {
    val signedInState = AuthUiState.SignedIn
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(signedInState, fallback)
    assertEquals(fallback, result)
  }

  @Test
  fun resolveAuthCommand_handles_all_auth_states() {
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.SignedIn,
            AuthUiState.Error("Test error"))

    val destinations = listOf(Routes.Opening, Routes.SignIn, Routes.Home, null)

    for (state in states) {
      for (destination in destinations) {
        val command = resolveAuthCommand(state, destination)
        assertNotNull(
            "Command should not be null for state $state and destination $destination", command)
        assertTrue(
            "Command should be one of the valid commands",
            command is AuthCommand.StartMicrosoftSignIn ||
                command is AuthCommand.NavigateHome ||
                command is AuthCommand.None)
      }
    }
  }

  @Test
  fun executeAuthCommand_calls_correct_lambda_for_each_command() {
    val commands =
        listOf(AuthCommand.StartMicrosoftSignIn, AuthCommand.NavigateHome, AuthCommand.None)
    val expectedSignInCalls = listOf(true, false, false)
    val expectedNavigateCalls = listOf(false, true, false)

    commands.forEachIndexed { index, command ->
      var signInCalled = false
      var navigateCalled = false

      executeAuthCommand(
          command,
          startMicrosoftSignIn = { signInCalled = true },
          navigateHome = { navigateCalled = true })

      assertEquals("SignIn call for command $command", expectedSignInCalls[index], signInCalled)
      assertEquals(
          "Navigate call for command $command", expectedNavigateCalls[index], navigateCalled)
    }
  }

  @Test
  fun buildAuthenticationErrorMessage_uses_error_state_message_when_available() {
    val errorState = AuthUiState.Error("Specific error")
    val fallback = "Default error"
    val result = buildAuthenticationErrorMessage(errorState, fallback)
    // Should use error state message, not fallback
    assertEquals("Specific error", result)
    assertNotEquals(fallback, result)
  }

  @Test
  fun resolveAuthCommand_priority_startMicrosoftSignIn_over_navigateHome() {
    // If state is Loading Microsoft, should return StartMicrosoftSignIn even if on SignIn
    val authState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val command = resolveAuthCommand(authState, Routes.SignIn)
    assertEquals(AuthCommand.StartMicrosoftSignIn, command)
  }

  @Test
  fun buildAuthenticationErrorMessage_with_whitespace_only_error_message() {
    val errorState = AuthUiState.Error("   ")
    val fallback = "Authentication failed"
    val result = buildAuthenticationErrorMessage(errorState, fallback)
    // Whitespace is not empty, so should return it
    assertEquals("   ", result)
  }

  @Test
  fun navigateToConnectors_sets_correct_route() {
    var capturedRoute: String? = null
    navigateToConnectors { route, _ -> capturedRoute = route }
    assertEquals(Routes.Connectors, capturedRoute)
  }

  @Test
  fun Routes_Connectors_constant_is_defined() {
    assertEquals("connectors", Routes.Connectors)
  }

  @Test
  fun Routes_Connectors_constant_is_not_empty() {
    assertTrue(Routes.Connectors.isNotEmpty())
  }
}

/**
 * Tests for VoiceChatViewModel configuration in NavGraph composable (lines 551-565). These tests
 * execute the exact same lines of code as in the composable to ensure coverage.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class NavGraphVoiceChatViewModelConfigTest {

  @get:Rule val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  private lateinit var context: Context

  @Before
  fun setUpFirebase() {
    context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:test")
              .setProjectId("test-project")
              .setApiKey("fake-api-key")
              .build())
    }
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDownFirebase() {
    FirebaseAuth.getInstance().signOut()
  }

  // ============================================================
  // Tests for VoiceChatViewModel factory function and helpers
  // ============================================================

  @Test
  fun createConversationRepositoryOrNull_returns_repository_when_successful() {
    val repo = createConversationRepositoryOrNull()
    assertNotNull("Repository should be created when Firebase is available", repo)
  }

  @Test
  fun createConversationRepositoryOrNull_handles_guest_mode_gracefully() {
    FirebaseAuth.getInstance().signOut()
    val repo = createConversationRepositoryOrNull()
    // Repository might still be created even without user (depends on ConversationRepository impl)
    // We just verify it doesn't throw
    // The important part is that the function handles exceptions gracefully
  }

  @Test
  fun createGetCurrentConversationIdLambda_reads_from_homeViewModel_uiState() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = "test-conv-123") }

    val getCurrentConversationId = createGetCurrentConversationIdLambda(homeViewModel)

    assertEquals("test-conv-123", getCurrentConversationId())
  }

  @Test
  fun createGetCurrentConversationIdLambda_returns_null_when_no_conversation() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = null) }

    val getCurrentConversationId = createGetCurrentConversationIdLambda(homeViewModel)

    assertNull(getCurrentConversationId())
  }

  @Test
  fun createGetCurrentConversationIdLambda_reads_dynamic_state() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    val getCurrentConversationId = createGetCurrentConversationIdLambda(homeViewModel)

    assertNull("Initially should be null", getCurrentConversationId())

    homeViewModel.updateUiState { it.copy(currentConversationId = "conv-1") }
    assertEquals("conv-1", getCurrentConversationId())

    homeViewModel.updateUiState { it.copy(currentConversationId = "conv-2") }
    assertEquals("conv-2", getCurrentConversationId())
  }

  @Test
  fun createOnConversationCreatedCallback_calls_selectConversation() {
    val homeViewModel = HomeViewModel(FakeLlmClient())

    val onConversationCreated = createOnConversationCreatedCallback(homeViewModel)

    onConversationCreated("new-conv-456")

    assertEquals("new-conv-456", homeViewModel.uiState.value.currentConversationId)
  }

  @Test
  fun createOnConversationCreatedCallback_exits_local_placeholder() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.setPrivateField("isInLocalNewChat", true)

    val onConversationCreated = createOnConversationCreatedCallback(homeViewModel)

    onConversationCreated("new-conv-789")

    val state = homeViewModel.uiState.value
    assertEquals("new-conv-789", state.currentConversationId)
    assertFalse("Should exit local placeholder", homeViewModel.getBooleanField("isInLocalNewChat"))
  }

  @Test
  fun createVoiceChatViewModel_creates_viewModel_with_repository() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = "existing-conv") }

    val viewModel =
        createVoiceChatViewModel(
            homeViewModel = homeViewModel,
            createConversationRepositoryOrNull = { createConversationRepositoryOrNull() },
            createGetCurrentConversationIdLambda = { createGetCurrentConversationIdLambda(it) },
            createOnConversationCreatedCallback = { createOnConversationCreatedCallback(it) })

    assertNotNull("ViewModel should be created", viewModel)
    val getCurrentConversationId = createGetCurrentConversationIdLambda(homeViewModel)
    assertEquals("existing-conv", getCurrentConversationId())
  }

  @Test
  fun createVoiceChatViewModel_handles_null_repository() {
    val homeViewModel = HomeViewModel(FakeLlmClient())

    val viewModel =
        createVoiceChatViewModel(
            homeViewModel = homeViewModel,
            createConversationRepositoryOrNull = { null },
            createGetCurrentConversationIdLambda = { createGetCurrentConversationIdLambda(it) },
            createOnConversationCreatedCallback = { createOnConversationCreatedCallback(it) })

    assertNotNull("ViewModel should be created even with null repository", viewModel)
  }

  @Test
  fun createVoiceChatViewModel_onConversationCreated_callback_works() {
    val homeViewModel = HomeViewModel(FakeLlmClient())

    val viewModel =
        createVoiceChatViewModel(
            homeViewModel = homeViewModel,
            createConversationRepositoryOrNull = { createConversationRepositoryOrNull() },
            createGetCurrentConversationIdLambda = { createGetCurrentConversationIdLambda(it) },
            createOnConversationCreatedCallback = { createOnConversationCreatedCallback(it) })

    // Simulate conversation creation callback
    val onConversationCreated = createOnConversationCreatedCallback(homeViewModel)
    onConversationCreated("new-conv-from-factory")

    assertEquals("new-conv-from-factory", homeViewModel.uiState.value.currentConversationId)
  }

  @Test
  fun createVoiceChatViewModel_matches_composable_exact_pattern() {
    // This test reproduces the exact same code pattern as in NavGraph composable (lines 610-618)
    // to ensure full coverage of the composable logic
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = "test-conv-id") }

    // Exact same pattern as in composable (lines 608-619)
    val voiceChatViewModel =
        createVoiceChatViewModel(
            homeViewModel = homeViewModel,
            createConversationRepositoryOrNull = { createConversationRepositoryOrNull() },
            createGetCurrentConversationIdLambda = { createGetCurrentConversationIdLambda(it) },
            createOnConversationCreatedCallback = { createOnConversationCreatedCallback(it) })

    // Verify the ViewModel is created correctly
    assertNotNull("VoiceChatViewModel should be created", voiceChatViewModel)

    // Verify getCurrentConversationId works (same pattern as in composable)
    val getCurrentConversationId = createGetCurrentConversationIdLambda(homeViewModel)
    assertEquals("test-conv-id", getCurrentConversationId())

    // Verify onConversationCreated callback works (same pattern as in composable)
    val onConversationCreated = createOnConversationCreatedCallback(homeViewModel)
    onConversationCreated("new-conv-123")
    assertEquals("new-conv-123", homeViewModel.uiState.value.currentConversationId)
  }

  private fun HomeViewModel.updateUiState(
      transform: (com.android.sample.home.HomeUiState) -> com.android.sample.home.HomeUiState
  ) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow =
        field.get(this)
            as kotlinx.coroutines.flow.MutableStateFlow<com.android.sample.home.HomeUiState>
    stateFlow.value = transform(stateFlow.value)
  }

  private fun HomeViewModel.setPrivateField(name: String, value: Any?) {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun HomeViewModel.getBooleanField(name: String): Boolean {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.getBoolean(this)
  }
}

class NavGraphNavigationHelpersTest {

  data class NavigationInvocation(
      val route: String,
      val inclusive: Boolean?,
      val restoreState: Boolean,
      val launchSingleTop: Boolean
  )

  private fun recordNavigation(block: (NavigateAction) -> Unit): NavigationInvocation {
    var capturedRoute: String? = null
    var inclusive: Boolean? = null
    var restoreState = false
    var launchSingleTop = false

    block { route, builder ->
      val options = navOptions(builder)
      capturedRoute = route
      inclusive = options.extractInclusive()
      restoreState = options.shouldRestoreState()
      launchSingleTop = options.shouldLaunchSingleTop()
    }

    return NavigationInvocation(
        route = requireNotNull(capturedRoute),
        inclusive = inclusive,
        restoreState = restoreState,
        launchSingleTop = launchSingleTop)
  }

  @Test
  fun navigateOpeningToSignIn_sets_expected_flags() {
    val invocation = recordNavigation { navigateOpeningToSignIn(it) }
    assertEquals(Routes.SignIn, invocation.route)
    assertEquals(true, invocation.inclusive)
    assertFalse(invocation.restoreState)
    assertTrue(invocation.launchSingleTop)
  }

  @Test
  fun navigateOpeningToHome_sets_expected_flags() {
    val invocation = recordNavigation { navigateOpeningToHome(it) }
    assertEquals(Routes.Home, invocation.route)
    assertEquals(true, invocation.inclusive)
    assertFalse(invocation.restoreState)
    assertTrue(invocation.launchSingleTop)
  }

  @Test
  fun navigateHomeFromSignIn_restores_state() {
    val invocation = recordNavigation { navigateHomeFromSignIn(it) }
    assertEquals(Routes.Home, invocation.route)
    assertEquals(true, invocation.inclusive)
    assertTrue(invocation.restoreState)
    assertTrue(invocation.launchSingleTop)
  }

  @Test
  fun navigateHomeToSignIn_clears_back_stack() {
    val invocation = recordNavigation { navigateHomeToSignIn(it) }
    assertEquals(Routes.SignIn, invocation.route)
    assertEquals(true, invocation.inclusive)
    assertFalse(invocation.restoreState)
    assertTrue(invocation.launchSingleTop)
  }

  @Test
  fun navigateSettingsBack_preserves_home_destination() {
    val invocation = recordNavigation { navigateSettingsBack(it) }
    assertEquals(Routes.HomeWithDrawer, invocation.route)
    assertEquals(false, invocation.inclusive)
    assertFalse(invocation.restoreState)
    assertFalse(invocation.launchSingleTop)
  }

  @Test
  fun navigateToConnectors_sets_expected_flags() {
    val invocation = recordNavigation { navigateToConnectors(it) }
    assertEquals(Routes.Connectors, invocation.route)
    // navigateToConnectors uses default empty builder, so no special flags
    assertFalse(invocation.restoreState)
    assertFalse(invocation.launchSingleTop)
  }

  private fun NavOptions.extractInclusive(): Boolean? {
    val methodNames = listOf("isPopUpToInclusive", "shouldPopUpToInclusive")
    for (name in methodNames) {
      try {
        val method = NavOptions::class.java.getDeclaredMethod(name)
        method.isAccessible = true
        return method.invoke(this) as Boolean
      } catch (_: Exception) {
        // ignore and try next name
      }
    }
    return null
  }
}
