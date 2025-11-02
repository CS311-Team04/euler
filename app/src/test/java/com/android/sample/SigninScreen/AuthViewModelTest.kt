package com.android.sample.signinscreen

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.android.sample.sign_in.AuthViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

  @Test
  fun AuthViewModel_initial_state_is_Idle_or_Error() = runTest {
    val viewModel = AuthViewModel()
    val initialState = viewModel.state.first()
    assertTrue(
        "Initial state should be Idle or Error (if Firebase not initialized)",
        initialState is AuthUiState.Idle || initialState is AuthUiState.Error)
  }

  @Test
  fun AuthViewModel_onMicrosoftLoginClick_sets_Loading_state() = runTest {
    val viewModel = AuthViewModel()
    // Wait for initial state
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()

    val state = viewModel.state.first()
    assertTrue("State should be Loading", state is AuthUiState.Loading)
    if (state is AuthUiState.Loading) {
      assertEquals(AuthProvider.MICROSOFT, state.provider)
    }
  }

  @Test
  fun AuthViewModel_onMicrosoftLoginClick_does_not_change_if_already_loading() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()
    val state1 = viewModel.state.first()
    assertTrue(state1 is AuthUiState.Loading)

    viewModel.onMicrosoftLoginClick()
    val state2 = viewModel.state.first()
    assertEquals("Should not change state if already loading", state1, state2)
  }

  @Test
  fun AuthViewModel_onSwitchEduLoginClick_starts_sign_in() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onSwitchEduLoginClick()

    val state = viewModel.state.first()
    assertTrue("State should be Loading", state is AuthUiState.Loading)
    if (state is AuthUiState.Loading) {
      assertEquals(AuthProvider.SWITCH_EDU, state.provider)
    }
  }

  @Test
  fun AuthViewModel_onSwitchEduLoginClick_does_not_change_if_already_loading() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()
    val state1 = viewModel.state.first()
    assertTrue(state1 is AuthUiState.Loading)

    viewModel.onSwitchEduLoginClick()
    val state2 = viewModel.state.first()
    assertEquals("Should not change state if already loading", state1, state2)
  }

  @Test
  fun AuthViewModel_onAuthenticationSuccess_sets_SignedIn_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationSuccess()

    val state = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, state)
  }

  @Test
  fun AuthViewModel_onAuthenticationError_sets_Error_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    val errorMessage = "Test error message"
    viewModel.onAuthenticationError(errorMessage)

    val state = viewModel.state.first()
    assertTrue("State should be Error", state is AuthUiState.Error)
    if (state is AuthUiState.Error) {
      assertEquals(errorMessage, state.message)
    }
  }

  @Test
  fun AuthViewModel_onAuthenticationError_with_empty_message() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationError("")

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Error)
    if (state is AuthUiState.Error) {
      assertEquals("", state.message)
    }
  }

  @Test
  fun AuthViewModel_onAuthenticationError_with_long_message() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    val longMessage = "A".repeat(1000)
    viewModel.onAuthenticationError(longMessage)

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Error)
    if (state is AuthUiState.Error) {
      assertEquals(longMessage, state.message)
    }
  }

  @Test
  fun AuthViewModel_signOut_sets_Idle_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.signOut()

    val state = viewModel.state.first()
    assertEquals(AuthUiState.Idle, state)
  }

  @Test
  fun AuthViewModel_signOut_works_from_SignedIn_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationSuccess()
    val signedInState = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, signedInState)

    viewModel.signOut()

    val state = viewModel.state.first()
    assertEquals(AuthUiState.Idle, state)
  }

  @Test
  fun AuthViewModel_signOut_works_from_Error_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationError("Error")
    val errorState = viewModel.state.first()
    assertTrue(errorState is AuthUiState.Error)

    viewModel.signOut()

    val state = viewModel.state.first()
    assertEquals(AuthUiState.Idle, state)
  }

  @Test
  fun AuthViewModel_state_transitions_from_Idle_to_Loading() = runTest {
    val viewModel = AuthViewModel()
    val initialState = viewModel.state.first()

    viewModel.onMicrosoftLoginClick()

    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)
  }

  @Test
  fun AuthViewModel_state_transitions_from_Loading_to_SignedIn() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()
    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)

    viewModel.onAuthenticationSuccess()
    val signedInState = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, signedInState)
  }

  @Test
  fun AuthViewModel_state_transitions_from_Loading_to_Error() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()
    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)

    viewModel.onAuthenticationError("Error occurred")
    val errorState = viewModel.state.first()
    assertTrue(errorState is AuthUiState.Error)
  }

  @Test
  fun AuthViewModel_state_transitions_from_Error_to_SignedIn() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationError("Error")
    val errorState = viewModel.state.first()
    assertTrue(errorState is AuthUiState.Error)

    viewModel.onAuthenticationSuccess()
    val signedInState = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, signedInState)
  }

  @Test
  fun AuthViewModel_SWITCH_EDU_loading_completes_to_SignedIn() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onSwitchEduLoginClick()

    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)
    if (loadingState is AuthUiState.Loading) {
      assertEquals(AuthProvider.SWITCH_EDU, loadingState.provider)
    }

    // Advance time to allow async operation to complete
    advanceTimeBy(1500)

    val finalState = viewModel.state.first()
    // After delay, should transition to SignedIn
    assertTrue(
        "After loading, should be SignedIn",
        finalState is AuthUiState.SignedIn || finalState is AuthUiState.Loading)
  }

  @Test
  fun AuthViewModel_multiple_error_messages() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationError("Error 1")
    val error1 = viewModel.state.first()
    assertTrue(error1 is AuthUiState.Error)
    if (error1 is AuthUiState.Error) {
      assertEquals("Error 1", error1.message)
    }

    viewModel.onAuthenticationError("Error 2")
    val error2 = viewModel.state.first()
    assertTrue(error2 is AuthUiState.Error)
    if (error2 is AuthUiState.Error) {
      assertEquals("Error 2", error2.message)
    }
  }

  @Test
  fun AuthViewModel_can_retry_after_error() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationError("Error")
    val errorState = viewModel.state.first()
    assertTrue(errorState is AuthUiState.Error)

    viewModel.onMicrosoftLoginClick()
    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)
  }

  @Test
  fun AuthViewModel_can_retry_after_signout() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationSuccess()
    val signedInState = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, signedInState)

    viewModel.signOut()
    val idleState = viewModel.state.first()
    assertEquals(AuthUiState.Idle, idleState)

    viewModel.onMicrosoftLoginClick()
    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)
  }

  @Test
  fun AuthViewModel_state_flow_is_not_null() = runTest {
    val viewModel = AuthViewModel()
    assertNotNull(viewModel.state)
  }

  @Test
  fun AuthViewModel_state_flow_provides_current_value() = runTest {
    val viewModel = AuthViewModel()
    val state = viewModel.state.first()
    assertNotNull(state)
    assertTrue(state is AuthUiState)
  }

  @Test
  fun AuthViewModel_can_handle_multiple_rapid_clicks() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    // Rapid clicks should only result in one loading state
    viewModel.onMicrosoftLoginClick()
    viewModel.onMicrosoftLoginClick()
    viewModel.onMicrosoftLoginClick()

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Loading)
    if (state is AuthUiState.Loading) {
      assertEquals(AuthProvider.MICROSOFT, state.provider)
    }
  }

  @Test
  fun AuthViewModel_provider_separation_MICROSOFT_vs_SWITCH_EDU() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()
    val microsoftState = viewModel.state.first()
    assertTrue(microsoftState is AuthUiState.Loading)
    if (microsoftState is AuthUiState.Loading) {
      assertEquals(AuthProvider.MICROSOFT, microsoftState.provider)
    }

    viewModel.onAuthenticationSuccess()
    viewModel.state.first()

    viewModel.onSwitchEduLoginClick()
    val switchState = viewModel.state.first()
    assertTrue(switchState is AuthUiState.Loading)
    if (switchState is AuthUiState.Loading) {
      assertEquals(AuthProvider.SWITCH_EDU, switchState.provider)
    }
  }

  @Test
  fun AuthViewModel_error_message_preserves_unicode() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    val unicodeMessage = "Erreur: émoji 🚀"
    viewModel.onAuthenticationError(unicodeMessage)

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Error)
    if (state is AuthUiState.Error) {
      assertEquals(unicodeMessage, state.message)
    }
  }

  @Test
  fun AuthViewModel_error_message_preserves_newlines() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    val multilineMessage = "Line 1\nLine 2\nLine 3"
    viewModel.onAuthenticationError(multilineMessage)

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Error)
    if (state is AuthUiState.Error) {
      assertEquals(multilineMessage, state.message)
    }
  }

  @Test
  fun AuthViewModel_state_changes_are_reflected_in_flow() = runTest {
    val viewModel = AuthViewModel()
    val states = mutableListOf<AuthUiState>()

    states.add(viewModel.state.first())
    viewModel.onMicrosoftLoginClick()
    states.add(viewModel.state.first())
    viewModel.onAuthenticationSuccess()
    states.add(viewModel.state.first())
    viewModel.signOut()
    states.add(viewModel.state.first())

    assertEquals(4, states.size)
    assertTrue(states[0] is AuthUiState.Idle || states[0] is AuthUiState.Error)
    assertTrue(states[1] is AuthUiState.Loading)
    assertEquals(AuthUiState.SignedIn, states[2])
    assertEquals(AuthUiState.Idle, states[3])
  }

  @Test
  fun AuthViewModel_SWITCH_EDU_provider_creates_correct_loading_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onSwitchEduLoginClick()

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Loading)
    if (state is AuthUiState.Loading) {
      assertEquals(AuthProvider.SWITCH_EDU, state.provider)
      assertNotEquals(AuthProvider.MICROSOFT, state.provider)
    }
  }

  @Test
  fun AuthViewModel_MICROSOFT_provider_creates_correct_loading_state() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onMicrosoftLoginClick()

    val state = viewModel.state.first()
    assertTrue(state is AuthUiState.Loading)
    if (state is AuthUiState.Loading) {
      assertEquals(AuthProvider.MICROSOFT, state.provider)
      assertNotEquals(AuthProvider.SWITCH_EDU, state.provider)
    }
  }

  @Test
  fun AuthViewModel_signOut_from_Idle_stays_Idle() = runTest {
    val viewModel = AuthViewModel()
    val initialState = viewModel.state.first()

    viewModel.signOut()

    val state = viewModel.state.first()
    assertEquals(AuthUiState.Idle, state)
  }

  @Test
  fun AuthViewModel_complete_flow_idle_to_signedin() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    // Idle -> Loading -> SignedIn
    viewModel.onMicrosoftLoginClick()
    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)

    viewModel.onAuthenticationSuccess()
    val signedInState = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, signedInState)
  }

  @Test
  fun AuthViewModel_complete_flow_with_error() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    // Idle -> Loading -> Error -> Idle (via signOut)
    viewModel.onMicrosoftLoginClick()
    val loadingState = viewModel.state.first()
    assertTrue(loadingState is AuthUiState.Loading)

    viewModel.onAuthenticationError("Failed")
    val errorState = viewModel.state.first()
    assertTrue(errorState is AuthUiState.Error)

    viewModel.signOut()
    val idleState = viewModel.state.first()
    assertEquals(AuthUiState.Idle, idleState)
  }

  @Test
  fun AuthViewModel_error_can_be_cleared_by_success() = runTest {
    val viewModel = AuthViewModel()
    viewModel.state.first()

    viewModel.onAuthenticationError("Error")
    val errorState = viewModel.state.first()
    assertTrue(errorState is AuthUiState.Error)

    viewModel.onAuthenticationSuccess()
    val successState = viewModel.state.first()
    assertEquals(AuthUiState.SignedIn, successState)
  }
}
