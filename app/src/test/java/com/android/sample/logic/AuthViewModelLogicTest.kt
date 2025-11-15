package com.android.sample.logic

import com.android.sample.authentification.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM unit tests for AuthViewModelLogic. */
class AuthViewModelLogicTest {

  @Test
  fun `shouldUpdateLoggedStatusOnAuthStateChange returns true for null to non-null transition`() {
    val result =
        AuthViewModelLogic.shouldUpdateLoggedStatusOnAuthStateChange(wasNull = true, isNull = false)

    assertTrue(result)
  }

  @Test
  fun `shouldUpdateLoggedStatusOnAuthStateChange returns false for non-null to null transition`() {
    val result =
        AuthViewModelLogic.shouldUpdateLoggedStatusOnAuthStateChange(wasNull = false, isNull = true)

    assertFalse(result)
  }

  @Test
  fun `shouldUpdateLoggedStatusOnAuthStateChange returns false when already authenticated`() {
    val result =
        AuthViewModelLogic.shouldUpdateLoggedStatusOnAuthStateChange(
            wasNull = false, isNull = false)

    assertFalse(result)
  }

  @Test
  fun `shouldSetLoggedStatusOnSuccess always returns true`() {
    assertTrue(AuthViewModelLogic.shouldSetLoggedStatusOnSuccess())
  }

  @Test
  fun `shouldSetLoggedStatusOnInitialCheck returns true when authenticated`() {
    assertTrue(AuthViewModelLogic.shouldSetLoggedStatusOnInitialCheck(true))
  }

  @Test
  fun `shouldSetLoggedStatusOnInitialCheck returns false when not authenticated`() {
    assertFalse(AuthViewModelLogic.shouldSetLoggedStatusOnInitialCheck(false))
  }

  @Test
  fun `shouldSetLoggedStatusBeforeSignOut always returns true`() {
    assertTrue(AuthViewModelLogic.shouldSetLoggedStatusBeforeSignOut())
  }

  @Test
  fun `isSignOutSuccessful returns true when user is null`() {
    assertTrue(AuthViewModelLogic.isSignOutSuccessful(null))
  }

  @Test
  fun `isSignOutSuccessful returns true when user is false`() {
    assertTrue(AuthViewModelLogic.isSignOutSuccessful(false))
  }

  @Test
  fun `isSignOutSuccessful returns false when user exists`() {
    assertFalse(AuthViewModelLogic.isSignOutSuccessful(true))
  }

  @Test
  fun `determineStateAfterSignOut returns Idle regardless of success`() {
    val result1 = AuthViewModelLogic.determineStateAfterSignOut(true)
    val result2 = AuthViewModelLogic.determineStateAfterSignOut(false)

    assertEquals(AuthUiState.Idle, result1)
    assertEquals(AuthUiState.Idle, result2)
  }

  @Test
  fun `shouldProceedWithSwitchEduLogin returns true for Loading state`() {
    val result =
        AuthViewModelLogic.shouldProceedWithSwitchEduLogin(
            AuthUiState.Loading(com.android.sample.authentification.AuthProvider.SWITCH_EDU))

    assertTrue(result)
  }

  @Test
  fun `shouldProceedWithSwitchEduLogin returns false for non-Loading state`() {
    assertFalse(AuthViewModelLogic.shouldProceedWithSwitchEduLogin(AuthUiState.Idle))
    assertFalse(AuthViewModelLogic.shouldProceedWithSwitchEduLogin(AuthUiState.SignedIn))
    assertFalse(AuthViewModelLogic.shouldProceedWithSwitchEduLogin(AuthUiState.Guest))
  }

  @Test
  fun `shouldSetLoggedStatusAfterProviderFlow returns true for SignedIn`() {
    assertTrue(AuthViewModelLogic.shouldSetLoggedStatusAfterProviderFlow(AuthUiState.SignedIn))
  }

  @Test
  fun `shouldSetLoggedStatusAfterProviderFlow returns false for Guest`() {
    assertFalse(AuthViewModelLogic.shouldSetLoggedStatusAfterProviderFlow(AuthUiState.Guest))
  }

  @Test
  fun `shouldSetLoggedStatusAfterProviderFlow returns false for other states`() {
    assertFalse(AuthViewModelLogic.shouldSetLoggedStatusAfterProviderFlow(AuthUiState.Idle))
    assertFalse(
        AuthViewModelLogic.shouldSetLoggedStatusAfterProviderFlow(
            AuthUiState.Loading(com.android.sample.authentification.AuthProvider.MICROSOFT)))
  }

  @Test
  fun `handleAuthStateChange returns SignedIn when authenticated`() {
    val result =
        AuthViewModelLogic.handleAuthStateChange(wasAuthenticated = null, isAuthenticated = true)

    assertEquals(AuthUiState.SignedIn, result)
  }

  @Test
  fun `handleAuthStateChange returns Idle when not authenticated`() {
    val result =
        AuthViewModelLogic.handleAuthStateChange(wasAuthenticated = true, isAuthenticated = false)

    assertEquals(AuthUiState.Idle, result)
  }

  @Test
  fun `getErrorMessage returns throwable message when available`() {
    val error = RuntimeException("Test error")
    val result = AuthViewModelLogic.getErrorMessage(error)

    assertEquals("Test error", result)
  }

  @Test
  fun `getErrorMessage returns Unknown error when throwable is null`() {
    val result = AuthViewModelLogic.getErrorMessage(null)

    assertEquals("Unknown error", result)
  }

  @Test
  fun `getErrorMessage returns Unknown error when message is null`() {
    val error = RuntimeException(null as String?)
    val result = AuthViewModelLogic.getErrorMessage(error)

    assertEquals("Unknown error", result)
  }
}
