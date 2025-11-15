package com.android.sample.logic

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.android.sample.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for NavGraphCore. Tests navigation decision logic without Android
 * dependencies.
 */
class NavGraphCoreTest {

  @Test
  fun `shouldTriggerMicrosoftAuth returns true for Microsoft loading state`() {
    val result =
        NavGraphCore.shouldTriggerMicrosoftAuth(AuthUiState.Loading(AuthProvider.MICROSOFT))

    assertTrue(result)
  }

  @Test
  fun `shouldTriggerMicrosoftAuth returns false for SwitchEdu loading state`() {
    val result =
        NavGraphCore.shouldTriggerMicrosoftAuth(AuthUiState.Loading(AuthProvider.SWITCH_EDU))

    assertFalse(result)
  }

  @Test
  fun `shouldTriggerMicrosoftAuth returns false for non-loading states`() {
    assertFalse(NavGraphCore.shouldTriggerMicrosoftAuth(AuthUiState.Idle))
    assertFalse(NavGraphCore.shouldTriggerMicrosoftAuth(AuthUiState.SignedIn))
    assertFalse(NavGraphCore.shouldTriggerMicrosoftAuth(AuthUiState.Guest))
    assertFalse(NavGraphCore.shouldTriggerMicrosoftAuth(AuthUiState.Error("error")))
  }

  @Test
  fun `shouldNavigateToHomeFromSignIn returns true when on SignIn and SignedIn`() {
    val result = NavGraphCore.shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, Routes.SignIn)

    assertTrue(result)
  }

  @Test
  fun `shouldNavigateToHomeFromSignIn returns true when on SignIn and Guest`() {
    val result = NavGraphCore.shouldNavigateToHomeFromSignIn(AuthUiState.Guest, Routes.SignIn)

    assertTrue(result)
  }

  @Test
  fun `shouldNavigateToHomeFromSignIn returns false when not on SignIn route`() {
    val result = NavGraphCore.shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, Routes.Home)

    assertFalse(result)
  }

  @Test
  fun `shouldNavigateToHomeFromSignIn returns false for Idle state`() {
    val result = NavGraphCore.shouldNavigateToHomeFromSignIn(AuthUiState.Idle, Routes.SignIn)

    assertFalse(result)
  }

  @Test
  fun `shouldNavigateToHomeFromSignIn returns false for null destination`() {
    val result = NavGraphCore.shouldNavigateToHomeFromSignIn(AuthUiState.SignedIn, null)

    assertFalse(result)
  }

  @Test
  fun `buildAuthenticationErrorMessage uses error state message when available`() {
    val errorState = AuthUiState.Error("Custom error")
    val result = NavGraphCore.buildAuthenticationErrorMessage(errorState, "fallback")

    assertEquals("Custom error", result)
  }

  @Test
  fun `buildAuthenticationErrorMessage uses fallback when error message empty`() {
    val errorState = AuthUiState.Error("")
    val result = NavGraphCore.buildAuthenticationErrorMessage(errorState, "fallback message")

    assertEquals("fallback message", result)
  }

  @Test
  fun `buildAuthenticationErrorMessage uses fallback for non-error states`() {
    val result = NavGraphCore.buildAuthenticationErrorMessage(AuthUiState.Idle, "fallback message")

    assertEquals("fallback message", result)
  }

  @Test
  fun `determineStartDestination returns Home when startOnSignedIn is true`() {
    val result = NavGraphCore.determineStartDestination(true)

    assertEquals(Routes.Home, result)
  }

  @Test
  fun `determineStartDestination returns Opening when startOnSignedIn is false`() {
    val result = NavGraphCore.determineStartDestination(false)

    assertEquals(Routes.Opening, result)
  }

  @Test
  fun `shouldShowGuestProfileWarning returns true for guest`() {
    assertTrue(NavGraphCore.shouldShowGuestProfileWarning(true))
  }

  @Test
  fun `shouldShowGuestProfileWarning returns false for non-guest`() {
    assertFalse(NavGraphCore.shouldShowGuestProfileWarning(false))
  }

  @Test
  fun `shouldNavigateToProfile returns false for guest`() {
    assertFalse(NavGraphCore.shouldNavigateToProfile(true))
  }

  @Test
  fun `shouldNavigateToProfile returns true for non-guest`() {
    assertTrue(NavGraphCore.shouldNavigateToProfile(false))
  }

  @Test
  fun `resolveAuthCommand returns TriggerMicrosoftAuth for Microsoft loading`() {
    val command =
        NavGraphCore.resolveAuthCommand(AuthUiState.Loading(AuthProvider.MICROSOFT), Routes.SignIn)

    assertTrue(command is AuthCommand.TriggerMicrosoftAuth)
  }

  @Test
  fun `resolveAuthCommand returns NavigateToHome when conditions met`() {
    val command = NavGraphCore.resolveAuthCommand(AuthUiState.SignedIn, Routes.SignIn)

    assertTrue(command is AuthCommand.NavigateToHome)
  }

  @Test
  fun `resolveAuthCommand returns DoNothing otherwise`() {
    val command = NavGraphCore.resolveAuthCommand(AuthUiState.Idle, Routes.Home)

    assertTrue(command is AuthCommand.DoNothing)
  }
}
