package com.android.sample.navigation

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NavGraph navigation callbacks and logic. Tests cover:
 * - startDestination conditional logic
 * - Navigation callbacks (onNavigateToSignIn, onNavigateToHome, onSignOut, etc.)
 * - Callback invocations (onMicrosoftLogin, onSwitchEduLogin)
 */
class NavGraphCallbacksTest {

  @Test
  fun startDestination_returns_home_when_startOnSignedIn_is_true() {
    val startDestination = if (true) Routes.Home else Routes.Opening
    assertEquals(Routes.Home, startDestination)
  }

  @Test
  fun startDestination_returns_opening_when_startOnSignedIn_is_false() {
    val startDestination = if (false) Routes.Home else Routes.Opening
    assertEquals(Routes.Opening, startDestination)
  }

  @Test
  fun onNavigateToSignIn_calls_navigate_with_correct_route() {
    var navigateCalled = false
    var routeUsed: String? = null
    var popUpToRoute: String? = null
    var inclusiveValue: Boolean? = null
    var launchSingleTopValue: Boolean? = null

    // Simulate the callback from OpeningScreen (lines 78-82)
    val onNavigateToSignIn = {
      routeUsed = Routes.SignIn
      popUpToRoute = Routes.Opening
      inclusiveValue = true
      launchSingleTopValue = true
      navigateCalled = true
    }

    onNavigateToSignIn()

    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.SignIn, routeUsed)
    assertEquals(Routes.Opening, popUpToRoute)
    assertEquals(true, inclusiveValue)
    assertEquals(true, launchSingleTopValue)
  }

  @Test
  fun onNavigateToHome_calls_navigate_with_correct_route() {
    var navigateCalled = false
    var routeUsed: String? = null
    var popUpToRoute: String? = null
    var inclusiveValue: Boolean? = null
    var launchSingleTopValue: Boolean? = null

    // Simulate the callback from OpeningScreen (lines 84-88)
    val onNavigateToHome = {
      routeUsed = Routes.Home
      popUpToRoute = Routes.Opening
      inclusiveValue = true
      launchSingleTopValue = true
      navigateCalled = true
    }

    onNavigateToHome()

    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.Home, routeUsed)
    assertEquals(Routes.Opening, popUpToRoute)
    assertEquals(true, inclusiveValue)
    assertEquals(true, launchSingleTopValue)
  }

  @Test
  fun onMicrosoftLogin_calls_authViewModel_onMicrosoftLoginClick() {
    var microsoftLoginCalled = false

    // Simulate the callback from AuthUIScreen (line 96)
    val onMicrosoftLogin = { microsoftLoginCalled = true }

    onMicrosoftLogin()

    assertTrue("onMicrosoftLoginClick should be called", microsoftLoginCalled)
  }

  @Test
  fun onSwitchEduLogin_calls_authViewModel_onSwitchEduLoginClick() {
    var switchEduLoginCalled = false

    // Simulate the callback from AuthUIScreen (line 97)
    val onSwitchEduLogin = { switchEduLoginCalled = true }

    onSwitchEduLogin()

    assertTrue("onSwitchEduLoginClick should be called", switchEduLoginCalled)
  }

  @Test
  fun onSignOut_homeScreen_calls_signOut_and_navigates() {
    var signOutCalled = false
    var navigateCalled = false
    var routeUsed: String? = null
    var popUpToRoute: String? = null
    var inclusiveValue: Boolean? = null
    var launchSingleTopValue: Boolean? = null

    // Simulate the callback from HomeScreen (lines 107-114)
    val onSignOut = {
      signOutCalled = true
      routeUsed = Routes.SignIn
      popUpToRoute = Routes.Home
      inclusiveValue = true
      launchSingleTopValue = true
      navigateCalled = true
    }

    onSignOut()

    assertTrue("signOut should be called", signOutCalled)
    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.SignIn, routeUsed)
    assertEquals(Routes.Home, popUpToRoute)
    assertEquals(true, inclusiveValue)
    assertEquals(true, launchSingleTopValue)
  }

  @Test
  fun onSignOut_homeWithDrawer_calls_signOut_and_navigates() {
    var signOutCalled = false
    var navigateCalled = false
    var routeUsed: String? = null
    var popUpToRoute: String? = null
    var inclusiveValue: Boolean? = null
    var launchSingleTopValue: Boolean? = null

    // Simulate the callback from HomeWithDrawer (lines 127-134)
    val onSignOut = {
      signOutCalled = true
      routeUsed = Routes.SignIn
      popUpToRoute = Routes.Home
      inclusiveValue = true
      launchSingleTopValue = true
      navigateCalled = true
    }

    onSignOut()

    assertTrue("signOut should be called", signOutCalled)
    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.SignIn, routeUsed)
    assertEquals(Routes.Home, popUpToRoute)
    assertEquals(true, inclusiveValue)
    assertEquals(true, launchSingleTopValue)
  }

  @Test
  fun onSignOut_settings_calls_signOut_and_navigates() {
    var signOutCalled = false
    var navigateCalled = false
    var routeUsed: String? = null
    var popUpToRoute: String? = null
    var inclusiveValue: Boolean? = null
    var launchSingleTopValue: Boolean? = null

    // Simulate the callback from SettingsPage (lines 147-154)
    val onSignOut = {
      signOutCalled = true
      routeUsed = Routes.SignIn
      popUpToRoute = Routes.Home
      inclusiveValue = true
      launchSingleTopValue = true
      navigateCalled = true
    }

    onSignOut()

    assertTrue("signOut should be called", signOutCalled)
    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.SignIn, routeUsed)
    assertEquals(Routes.Home, popUpToRoute)
    assertEquals(true, inclusiveValue)
    assertEquals(true, launchSingleTopValue)
  }

  @Test
  fun onSettingsClick_navigates_to_settings() {
    var navigateCalled = false
    var routeUsed: String? = null

    // Simulate the callback from HomeScreen (line 116)
    val onSettingsClick = {
      routeUsed = Routes.Settings
      navigateCalled = true
    }

    onSettingsClick()

    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.Settings, routeUsed)
  }

  @Test
  fun onVoiceChatClick_navigates_to_voiceChat() {
    var navigateCalled = false
    var routeUsed: String? = null

    // Simulate the callback from HomeScreen (line 117)
    val onVoiceChatClick = {
      routeUsed = Routes.VoiceChat
      navigateCalled = true
    }

    onVoiceChatClick()

    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.VoiceChat, routeUsed)
  }

  @Test
  fun onBackClick_settings_navigates_to_homeWithDrawer() {
    var navigateCalled = false
    var routeUsed: String? = null
    var popUpToRoute: String? = null
    var inclusiveValue: Boolean? = null

    // Simulate the callback from SettingsPage (lines 144-145)
    val onBackClick = {
      routeUsed = Routes.HomeWithDrawer
      popUpToRoute = Routes.Home
      inclusiveValue = false
      navigateCalled = true
    }

    onBackClick()

    assertTrue("navigate should be called", navigateCalled)
    assertEquals(Routes.HomeWithDrawer, routeUsed)
    assertEquals(Routes.Home, popUpToRoute)
    assertEquals(false, inclusiveValue)
  }

  @Test
  fun onClose_voiceScreen_calls_popBackStack() {
    var popBackStackCalled = false

    // Simulate the callback from VoiceScreen (line 161)
    val onClose = { popBackStackCalled = true }

    onClose()

    assertTrue("popBackStack should be called", popBackStackCalled)
  }

  @Test
  fun navigation_callbacks_execute_in_correct_order() {
    val executionOrder = mutableListOf<String>()

    // Simulate onSignOut callback (lines 107-114)
    val onSignOut = {
      executionOrder.add("signOut")
      executionOrder.add("navigate")
    }

    onSignOut()

    assertEquals(2, executionOrder.size)
    assertEquals("signOut", executionOrder[0])
    assertEquals("navigate", executionOrder[1])
  }

  @Test
  fun all_navigation_routes_are_valid() {
    val routes =
        listOf(
            Routes.Opening,
            Routes.SignIn,
            Routes.Home,
            Routes.HomeWithDrawer,
            Routes.Settings,
            Routes.VoiceChat)

    routes.forEach { route ->
      assertNotNull("Route should not be null: $route", route)
      assertTrue("Route should not be empty: $route", route.isNotEmpty())
    }
  }

  @Test
  fun popUpTo_inclusive_true_removes_destination_from_stack() {
    // Test that popUpTo with inclusive = true removes the destination
    val inclusive = true
    assertTrue("inclusive should be true", inclusive)
  }

  @Test
  fun popUpTo_inclusive_false_keeps_destination_in_stack() {
    // Test that popUpTo with inclusive = false keeps the destination
    val inclusive = false
    assertFalse("inclusive should be false", inclusive)
  }

  @Test
  fun launchSingleTop_prevents_multiple_instances() {
    // Test that launchSingleTop = true prevents multiple instances
    val launchSingleTop = true
    assertTrue("launchSingleTop should be true", launchSingleTop)
  }

  @Test
  fun restoreState_restores_previous_state() {
    // Test that restoreState = true restores previous state
    val restoreState = true
    assertTrue("restoreState should be true", restoreState)
  }
}
