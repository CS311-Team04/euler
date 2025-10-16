package com.android.sample.sign_in

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.auth.MicrosoftAuth
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
  val state: StateFlow<AuthUiState> = _state.asStateFlow()

  fun onMicrosoftLoginClick(activity: Activity) {
    if (_state.value is AuthUiState.Loading) return
    startMicrosoftSignIn(activity)
  }

  fun onSwitchEduLoginClick() {
    if (_state.value is AuthUiState.Loading) return
    startSignIn(AuthProvider.SWITCH_EDU)
  }

  private fun startMicrosoftSignIn(activity: Activity) {
    _state.value = AuthUiState.Loading(AuthProvider.MICROSOFT)

    MicrosoftAuth.signIn(
        activity = activity,
        onSuccess = { _state.value = AuthUiState.SignedIn },
        onError = { exception ->
          _state.value = AuthUiState.Error(exception.message ?: "Microsoft authentication failed")
        })
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
