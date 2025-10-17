package com.android.sample.sign_in

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
  val state: StateFlow<AuthUiState> = _state.asStateFlow()

  private val auth = FirebaseAuth.getInstance()

  init {
    // Check if user is already signed in
    checkAuthState()
  }

  private fun checkAuthState() {
    val currentUser = auth.currentUser
    android.util.Log.d("AuthViewModel", "Checking auth state...")
    android.util.Log.d("AuthViewModel", "Current user: ${currentUser?.uid}")
    android.util.Log.d("AuthViewModel", "Current user email: ${currentUser?.email}")
    android.util.Log.d("AuthViewModel", "Current user displayName: ${currentUser?.displayName}")
    
    if (currentUser != null) {
      android.util.Log.d("AuthViewModel", "User is signed in, setting state to SignedIn")
      _state.value = AuthUiState.SignedIn
    } else {
      android.util.Log.d("AuthViewModel", "No user found, setting state to Idle")
      _state.value = AuthUiState.Idle
    }
  }

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

  fun signOut() {
    auth.signOut()
    _state.value = AuthUiState.Idle
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
