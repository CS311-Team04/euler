package com.android.sample.sign_in

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
  val state: StateFlow<AuthUiState> = _state.asStateFlow()

  fun onMicrosoftLoginClick() {
    if (_state.value is AuthUiState.Loading) return
    _state.value = AuthUiState.Loading(AuthProvider.MICROSOFT)
  }

  fun onSwitchEduLoginClick() {
    if (_state.value is AuthUiState.Loading) return
    startSignIn(AuthProvider.SWITCH_EDU)
  }

  fun onAuthenticationSuccess() {
    _state.value = AuthUiState.SignedIn
  }

  fun onAuthenticationError(error: String) {
    _state.value = AuthUiState.Error(error)
  }

  private fun startSignIn(provider: AuthProvider) {
    _state.value = AuthUiState.Loading(provider)
    viewModelScope.launch {
      try {
        // Simulate async work for other providers
        kotlinx.coroutines.delay(1200)
        _state.value = AuthUiState.SignedIn
      } catch (t: Throwable) {
        _state.value = AuthUiState.Error(t.message ?: "Unknown error")
      }
    }
  }
}
