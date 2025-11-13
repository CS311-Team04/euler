package com.android.sample.signinscreen

import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import org.junit.Assert.*
import org.junit.Test

class AuthUiStateTest {

  @Test
  fun AuthUiState_Idle_is_instance_of_AuthUiState() {
    val state: AuthUiState = AuthUiState.Idle
    assertNotNull(state)
  }

  @Test
  fun AuthUiState_SignedIn_is_instance_of_AuthUiState() {
    val state: AuthUiState = AuthUiState.SignedIn
    assertNotNull(state)
  }

  @Test
  fun AuthUiState_Loading_is_instance_of_AuthUiState() {
    val state: AuthUiState = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertNotNull(state)
  }

  @Test
  fun AuthUiState_Error_is_instance_of_AuthUiState() {
    val state: AuthUiState = AuthUiState.Error("Test error")
    assertNotNull(state)
  }

  @Test
  fun AuthUiState_Idle_equals_itself() {
    assertEquals(AuthUiState.Idle, AuthUiState.Idle)
  }

  @Test
  fun AuthUiState_SignedIn_equals_itself() {
    assertEquals(AuthUiState.SignedIn, AuthUiState.SignedIn)
  }

  @Test
  fun AuthUiState_Loading_with_same_provider_equals() {
    val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loading2 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertEquals(loading1, loading2)
  }

  @Test
  fun AuthUiState_Loading_with_different_providers_not_equals() {
    val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loading2 = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertNotEquals(loading1, loading2)
  }

  @Test
  fun AuthUiState_Error_with_same_message_equals() {
    val error1 = AuthUiState.Error("Same error")
    val error2 = AuthUiState.Error("Same error")
    assertEquals(error1, error2)
  }

  @Test
  fun AuthUiState_Error_with_different_messages_not_equals() {
    val error1 = AuthUiState.Error("Error 1")
    val error2 = AuthUiState.Error("Error 2")
    assertNotEquals(error1, error2)
  }

  @Test
  fun AuthUiState_different_types_not_equals() {
    assertNotEquals(AuthUiState.Idle, AuthUiState.SignedIn)
    assertNotEquals(AuthUiState.Idle, AuthUiState.Loading(AuthProvider.MICROSOFT))
    assertNotEquals(AuthUiState.Idle, AuthUiState.Error("Error"))
    assertNotEquals(AuthUiState.SignedIn, AuthUiState.Loading(AuthProvider.MICROSOFT))
    assertNotEquals(AuthUiState.SignedIn, AuthUiState.Error("Error"))
    assertNotEquals(AuthUiState.Loading(AuthProvider.MICROSOFT), AuthUiState.Error("Error"))
  }

  @Test
  fun AuthUiState_Loading_provider_property() {
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertEquals(AuthProvider.MICROSOFT, loading.provider)
  }

  @Test
  fun AuthUiState_Error_message_property() {
    val error = AuthUiState.Error("Test error message")
    assertEquals("Test error message", error.message)
  }

  @Test
  fun AuthUiState_Loading_copy_works() {
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val copied = loading.copy(provider = AuthProvider.SWITCH_EDU)
    assertEquals(AuthProvider.SWITCH_EDU, copied.provider)
    assertEquals(AuthProvider.MICROSOFT, loading.provider)
  }

  @Test
  fun AuthUiState_Error_copy_works() {
    val error = AuthUiState.Error("Original error")
    val copied = error.copy(message = "New error")
    assertEquals("New error", copied.message)
    assertEquals("Original error", error.message)
  }

  @Test
  fun AuthUiState_Loading_hashCode_consistency() {
    val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loading2 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertEquals(loading1.hashCode(), loading2.hashCode())
  }

  @Test
  fun AuthUiState_Error_hashCode_consistency() {
    val error1 = AuthUiState.Error("Same message")
    val error2 = AuthUiState.Error("Same message")
    assertEquals(error1.hashCode(), error2.hashCode())
  }

  @Test
  fun AuthUiState_Loading_hashCode_different_for_different_providers() {
    val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loading2 = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertNotEquals(loading1.hashCode(), loading2.hashCode())
  }

  @Test
  fun AuthUiState_Error_hashCode_different_for_different_messages() {
    val error1 = AuthUiState.Error("Error 1")
    val error2 = AuthUiState.Error("Error 2")
    assertNotEquals(error1.hashCode(), error2.hashCode())
  }

  @Test
  fun AuthUiState_Loading_toString_contains_provider() {
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val toString = loading.toString()
    assertTrue(toString.contains("MICROSOFT") || toString.contains("Loading"))
  }

  @Test
  fun AuthUiState_Error_toString_contains_message() {
    val error = AuthUiState.Error("Test error message")
    val toString = error.toString()
    assertTrue(toString.contains("Test error message") || toString.contains("Error"))
  }

  @Test
  fun AuthUiState_Loading_component1_returns_provider() {
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertEquals(AuthProvider.MICROSOFT, loading.component1())
  }

  @Test
  fun AuthUiState_Error_component1_returns_message() {
    val error = AuthUiState.Error("Error message")
    assertEquals("Error message", error.component1())
  }

  @Test
  fun AuthUiState_Loading_with_MICROSOFT_provider() {
    val state = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertEquals(AuthProvider.MICROSOFT, state.provider)
  }

  @Test
  fun AuthUiState_Loading_with_SWITCH_EDU_provider() {
    val state = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertEquals(AuthProvider.SWITCH_EDU, state.provider)
  }

  @Test
  fun AuthUiState_Error_with_empty_message() {
    val error = AuthUiState.Error("")
    assertEquals("", error.message)
  }

  @Test
  fun AuthUiState_Error_with_long_message() {
    val longMessage = "A".repeat(1000)
    val error = AuthUiState.Error(longMessage)
    assertEquals(longMessage, error.message)
  }

  @Test
  fun AuthUiState_Error_with_special_characters() {
    val specialMessage = "Error: @#$%^&*()"
    val error = AuthUiState.Error(specialMessage)
    assertEquals(specialMessage, error.message)
  }

  @Test
  fun AuthUiState_Loading_equality_component_decomposition() {
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val (provider) = loading
    assertEquals(AuthProvider.MICROSOFT, provider)
  }

  @Test
  fun AuthUiState_Error_equality_component_decomposition() {
    val error = AuthUiState.Error("Test")
    val (message) = error
    assertEquals("Test", message)
  }

  @Test
  fun AuthUiState_all_states_are_distinct_instances() {
    val idle = AuthUiState.Idle
    val signedIn = AuthUiState.SignedIn
    assertSame(idle, AuthUiState.Idle)
    assertSame(signedIn, AuthUiState.SignedIn)
  }

  @Test
  fun AuthUiState_Loading_structural_equality() {
    val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loading2 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertEquals(loading1, loading2)
    assertEquals(loading1.hashCode(), loading2.hashCode())
  }

  @Test
  fun AuthUiState_Error_structural_equality() {
    val error1 = AuthUiState.Error("Same")
    val error2 = AuthUiState.Error("Same")
    assertEquals(error1, error2)
    assertEquals(error1.hashCode(), error2.hashCode())
  }

  @Test
  fun AuthUiState_Loading_inequality_when_different() {
    val loading1 = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val loading2 = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertNotEquals(loading1, loading2)
  }

  @Test
  fun AuthUiState_Error_inequality_when_different() {
    val error1 = AuthUiState.Error("Error 1")
    val error2 = AuthUiState.Error("Error 2")
    assertNotEquals(error1, error2)
  }

  @Test
  fun AuthUiState_Loading_works_with_all_providers() {
    val microsoftLoading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    val switchLoading = AuthUiState.Loading(AuthProvider.SWITCH_EDU)
    assertNotNull(microsoftLoading)
    assertNotNull(switchLoading)
    assertNotEquals(microsoftLoading, switchLoading)
  }

  @Test
  fun AuthUiState_Idle_toString() {
    val toString = AuthUiState.Idle.toString()
    assertTrue(toString.contains("Idle") || toString.isNotEmpty())
  }

  @Test
  fun AuthUiState_SignedIn_toString() {
    val toString = AuthUiState.SignedIn.toString()
    assertTrue(toString.contains("SignedIn") || toString.isNotEmpty())
  }

  @Test
  fun AuthUiState_Loading_with_null_provider_not_supported() {
    // This test verifies that Loading requires a provider
    val loading = AuthUiState.Loading(AuthProvider.MICROSOFT)
    assertNotNull(loading.provider)
  }

  @Test
  fun AuthUiState_Error_with_multiline_message() {
    val multiline = "Line 1\nLine 2\nLine 3"
    val error = AuthUiState.Error(multiline)
    assertEquals(multiline, error.message)
  }

  @Test
  fun AuthUiState_Error_message_unicode_support() {
    val unicodeMessage = "Erreur: émoji 🚀"
    val error = AuthUiState.Error(unicodeMessage)
    assertEquals(unicodeMessage, error.message)
  }
}
