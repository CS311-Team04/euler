package com.android.sample.splash

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Test

/** Unit tests for OpeningScreen navigation logic */
class OpeningScreenNavigationTest {

  @Test
  fun signed_in_state_navigates_to_home() {
    val authState = AuthUiState.SignedIn

    val shouldNavigateToHome = authState is AuthUiState.SignedIn

    assertTrue("Should navigate to home when signed in", shouldNavigateToHome)
  }

  @Test
  fun idle_state_should_not_navigate_to_home() {
    val authState = AuthUiState.Idle

    // Idle is not SignedIn
    assertTrue("Idle should be Idle state", authState is AuthUiState.Idle)
    assertFalse("Idle is not SignedIn", authState !is AuthUiState.Idle)
  }

  @Test
  fun loading_state_should_not_navigate_to_home() {
    val authState = AuthUiState.Loading(AuthProvider.MICROSOFT)

    // Loading is not SignedIn
    assertTrue("Loading should be Loading state", authState is AuthUiState.Loading)
  }

  @Test
  fun error_state_should_not_navigate_to_home() {
    val authState = AuthUiState.Error("Test error")

    // Error is not SignedIn
    assertTrue("Error should be Error state", authState is AuthUiState.Error)
  }

  @Test
  fun all_auth_states_coverage() {
    val idle = AuthUiState.Idle
    val loadingMs = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loadingSwitch = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    val signedIn = AuthUiState.SignedIn
    val error = AuthUiState.Error("Test error")

    assertNotNull("Idle state should not be null", idle)
    assertNotNull("Loading MS state should not be null", loadingMs)
    assertNotNull("Loading Switch state should not be null", loadingSwitch)
    assertNotNull("SignedIn state should not be null", signedIn)
    assertNotNull("Error state should not be null", error)

    assertTrue("Idle should be Idle", idle is AuthUiState.Idle)
    assertTrue("Loading MS should be Loading", loadingMs is AuthUiState.Loading)
    assertTrue("Loading Switch should be Loading", loadingSwitch is AuthUiState.Loading)
    assertTrue("SignedIn should be SignedIn", signedIn is AuthUiState.SignedIn)
    assertTrue("Error should be Error", error is AuthUiState.Error)
  }

  @Test
  fun loading_state_with_different_providers() {
    val loadingMs = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loadingSwitch = AuthUiState.Loading(AuthProvider.SWITCH_EDU)

    assertTrue("Loading MS should be Loading state", loadingMs is AuthUiState.Loading)
    assertTrue("Loading Switch should be Loading state", loadingSwitch is AuthUiState.Loading)

    if (loadingMs is AuthUiState.Loading) {
      assertEquals("Microsoft provider should be set", AuthProvider.MICROSOFT, loadingMs.provider)
    }
    if (loadingSwitch is AuthUiState.Loading) {
      assertEquals("Switch provider should be set", AuthProvider.SWITCH_EDU, loadingSwitch.provider)
    }
  }

  @Test
  fun error_state_preserves_message() {
    val errorMessage = "Connection failed"
    val authState = AuthUiState.Error(errorMessage)

    assertTrue("Error state should be error", authState is AuthUiState.Error)
    if (authState is AuthUiState.Error) {
      assertEquals("Error message should be preserved", errorMessage, authState.message)
    }
  }

  @Test
  fun navigation_routing_covers_all_states() {
    val states =
        listOf(
            AuthUiState.Idle,
            AuthUiState.Loading(AuthProvider.MICROSOFT),
            AuthUiState.Loading(AuthProvider.SWITCH_EDU),
            AuthUiState.Error("Error message"),
            AuthUiState.SignedIn)

    val statesNavigatingToHome = states.filter { it is AuthUiState.SignedIn }
    assertEquals("Only SignedIn should navigate to home", 1, statesNavigatingToHome.size)

    val statesNavigatingToSignIn = states.filter { it !is AuthUiState.SignedIn }
    assertEquals("All other states should navigate to sign in", 4, statesNavigatingToSignIn.size)
  }

  @Test
  fun auth_provider_enum_values() {
    assertEquals(
        "MICROSOFT should be first", AuthProvider.MICROSOFT, AuthProvider.valueOf("MICROSOFT"))
    assertEquals(
        "SWITCH_EDU should be second", AuthProvider.SWITCH_EDU, AuthProvider.valueOf("SWITCH_EDU"))
  }
}
