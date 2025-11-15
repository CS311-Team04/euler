package com.android.sample.logic

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for AuthStateReducer. Tests auth state transitions without Android
 * dependencies.
 */
class AuthStateReducerTest {

  @Test
  fun `onMicrosoftLoginClick returns Loading state when not loading`() {
    val result = AuthStateReducer.onMicrosoftLoginClick(AuthUiState.Idle)

    assertEquals(AuthUiState.Loading(AuthProvider.MICROSOFT), result)
  }

  @Test
  fun `onMicrosoftLoginClick does not change state when already loading`() {
    val loading = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    val result = AuthStateReducer.onMicrosoftLoginClick(loading)

    assertEquals(loading, result)
  }

  @Test
  fun `onMicrosoftLoginClick works from Idle state`() {
    val result = AuthStateReducer.onMicrosoftLoginClick(AuthUiState.Idle)
    assertTrue(result is AuthUiState.Loading)
    assertEquals(AuthProvider.MICROSOFT, (result as AuthUiState.Loading).provider)
  }

  @Test
  fun `onMicrosoftLoginClick works from SignedIn state`() {
    val result = AuthStateReducer.onMicrosoftLoginClick(AuthUiState.SignedIn)
    assertTrue(result is AuthUiState.Loading)
    assertEquals(AuthProvider.MICROSOFT, (result as AuthUiState.Loading).provider)
  }

  @Test
  fun `onSwitchEduLoginClick returns Loading state when not loading`() {
    val result = AuthStateReducer.onSwitchEduLoginClick(AuthUiState.Idle)

    assertEquals(AuthUiState.Loading(AuthProvider.SWITCH_EDU), result)
  }

  @Test
  fun `onSwitchEduLoginClick does not change state when already loading`() {
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val result = AuthStateReducer.onSwitchEduLoginClick(loading)

    assertEquals(loading, result)
  }

  @Test
  fun `onAuthenticationSuccess returns SignedIn state`() {
    val result = AuthStateReducer.onAuthenticationSuccess()

    assertEquals(AuthUiState.SignedIn, result)
  }

  @Test
  fun `onAuthenticationError returns Error state with message`() {
    val errorMessage = "Authentication failed"
    val result = AuthStateReducer.onAuthenticationError(errorMessage)

    assertTrue(result is AuthUiState.Error)
    assertEquals(errorMessage, (result as AuthUiState.Error).message)
  }

  @Test
  fun `onAuthenticationError handles empty error message`() {
    val result = AuthStateReducer.onAuthenticationError("")

    assertTrue(result is AuthUiState.Error)
    assertEquals("", (result as AuthUiState.Error).message)
  }

  @Test
  fun `onSignOut returns Idle state`() {
    val result = AuthStateReducer.onSignOut()

    assertEquals(AuthUiState.Idle, result)
  }

  @Test
  fun `determineInitialState returns SignedIn when authenticated`() {
    val result = AuthStateReducer.determineInitialState(true)

    assertEquals(AuthUiState.SignedIn, result)
  }

  @Test
  fun `determineInitialState returns Idle when not authenticated`() {
    val result = AuthStateReducer.determineInitialState(false)

    assertEquals(AuthUiState.Idle, result)
  }

  @Test
  fun `onAuthStateChanged returns SignedIn when authenticated`() {
    val result = AuthStateReducer.onAuthStateChanged(true)

    assertEquals(AuthUiState.SignedIn, result)
  }

  @Test
  fun `onAuthStateChanged returns Idle when not authenticated`() {
    val result = AuthStateReducer.onAuthStateChanged(false)

    assertEquals(AuthUiState.Idle, result)
  }

  @Test
  fun `processSwitchEduFlow returns Guest for SwitchEdu provider`() {
    val result = AuthStateReducer.processSwitchEduFlow(AuthProvider.SWITCH_EDU)

    assertEquals(AuthUiState.Guest, result)
  }

  @Test
  fun `processSwitchEduFlow returns SignedIn for Microsoft provider`() {
    val result = AuthStateReducer.processSwitchEduFlow(AuthProvider.MICROSOFT)

    assertEquals(AuthUiState.SignedIn, result)
  }

  @Test
  fun `shouldTriggerAuth returns true for Loading state`() {
    val result = AuthStateReducer.shouldTriggerAuth(AuthUiState.Loading(AuthProvider.MICROSOFT))

    assertTrue(result)
  }

  @Test
  fun `shouldTriggerAuth returns false for Idle state`() {
    val result = AuthStateReducer.shouldTriggerAuth(AuthUiState.Idle)

    assertFalse(result)
  }

  @Test
  fun `shouldTriggerAuth returns false for SignedIn state`() {
    val result = AuthStateReducer.shouldTriggerAuth(AuthUiState.SignedIn)

    assertFalse(result)
  }

  @Test
  fun `shouldTriggerAuth returns false for Guest state`() {
    val result = AuthStateReducer.shouldTriggerAuth(AuthUiState.Guest)

    assertFalse(result)
  }

  @Test
  fun `shouldTriggerAuth returns false for Error state`() {
    val result = AuthStateReducer.shouldTriggerAuth(AuthUiState.Error("test"))

    assertFalse(result)
  }

  @Test
  fun `onAuthenticationError preserves error message exactly`() {
    val messages = listOf("Error 1", "Network timeout", "User cancelled", "Unknown error")

    messages.forEach { message ->
      val result = AuthStateReducer.onAuthenticationError(message)
      assertTrue(result is AuthUiState.Error)
      assertEquals(message, (result as AuthUiState.Error).message)
    }
  }

  @Test
  fun `onMicrosoftLoginClick from Error state transitions to Loading`() {
    val error = AuthUiState.Error("Previous error")
    val result = AuthStateReducer.onMicrosoftLoginClick(error)

    assertTrue(result is AuthUiState.Loading)
    assertEquals(AuthProvider.MICROSOFT, (result as AuthUiState.Loading).provider)
  }

  @Test
  fun `onSwitchEduLoginClick from Error state transitions to Loading`() {
    val error = AuthUiState.Error("Previous error")
    val result = AuthStateReducer.onSwitchEduLoginClick(error)

    assertTrue(result is AuthUiState.Loading)
    assertEquals(AuthProvider.SWITCH_EDU, (result as AuthUiState.Loading).provider)
  }
}
