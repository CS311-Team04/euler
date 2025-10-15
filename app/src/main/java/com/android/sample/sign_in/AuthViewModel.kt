package com.android.sample.authentification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onMicrosoftLoginClick() {
        if (_state.value is AuthUiState.Loading) return
        startSignIn(AuthProvider.MICROSOFT)
    }

    fun onSwitchEduLoginClick() {
        if (_state.value is AuthUiState.Loading) return
        startSignIn(AuthProvider.SWITCH_EDU)
    }

    private fun startSignIn(provider: AuthProvider) {
        _state.value = AuthUiState.Loading(provider)
        viewModelScope.launch {
            try {
                // Simulate async work or hook real auth here
                delay(1200)
                _state.value = AuthUiState.SignedIn
            } catch (t: Throwable) {
                _state.value = AuthUiState.Error(t.message ?: "Unknown error")
            }
        }
    }
}
