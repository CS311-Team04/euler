package com.android.sample.sign_in

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.authentification.AuthProvider
import com.android.sample.authentification.AuthUiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
  val state: StateFlow<AuthUiState> = _state.asStateFlow()

  private val auth = FirebaseAuth.getInstance()
  private val firestore = FirebaseFirestore.getInstance()

  init {
    // Check if user is already signed in
    checkAuthState()

    // Listen for auth state changes
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      android.util.Log.d("AuthViewModel", "Auth state changed - current user: ${user?.uid}")
      if (user != null) {
        android.util.Log.d("AuthViewModel", "User signed in: ${user.email}")
        _state.value = AuthUiState.SignedIn
        setLoggedStatus(true)
      } else {
        android.util.Log.d("AuthViewModel", "User signed out")
        _state.value = AuthUiState.Idle
        // Note: We don't set logged to false here because signOut() already handles it
      }
    }
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
    setLoggedStatus(true)
  }

  fun onAuthenticationError(error: String) {
    _state.value = AuthUiState.Error(error)
  }

  fun signOut() {
    android.util.Log.d("AuthViewModel", "Starting sign out process...")
    val currentUser = auth.currentUser
    android.util.Log.d("AuthViewModel", "Current user before signout: ${currentUser?.uid}")
    android.util.Log.d("AuthViewModel", "Current user email before signout: ${currentUser?.email}")

    // Update logged status BEFORE signing out (while user still exists)
    setLoggedStatus(false)

    auth.signOut()

    // Check if signout was successful
    val userAfterSignout = auth.currentUser
    android.util.Log.d("AuthViewModel", "Current user after signout: ${userAfterSignout?.uid}")

    if (userAfterSignout == null) {
      android.util.Log.d("AuthViewModel", "Sign out successful - no current user")
    } else {
      android.util.Log.w(
          "AuthViewModel", "Sign out failed - user still exists: ${userAfterSignout.uid}")
    }

    _state.value = AuthUiState.Idle
  }

  private fun setLoggedStatus(isLogged: Boolean) {
    val currentUser = auth.currentUser
    if (currentUser != null) {
      viewModelScope.launch {
        try {
          val userDoc = firestore.collection("users").document(currentUser.uid)
          userDoc
              .update("logged", isLogged)
              .addOnSuccessListener {
                android.util.Log.d(
                    "AuthViewModel", "Successfully updated logged status to: $isLogged")
              }
              .addOnFailureListener { exception ->
                android.util.Log.e(
                    "AuthViewModel", "Failed to update logged status: ${exception.message}")
              }
        } catch (e: Exception) {
          android.util.Log.e("AuthViewModel", "Error updating logged status: ${e.message}")
        }
      }
    }
  }

  private fun startSignIn(provider: AuthProvider) {
    _state.value = AuthUiState.Loading(provider)
    viewModelScope.launch {
      try {
        // Simulate async work for other providers
        kotlinx.coroutines.delay(1200)
        _state.value = AuthUiState.SignedIn
        setLoggedStatus(true)
      } catch (t: Throwable) {
        _state.value = AuthUiState.Error(t.message ?: "Unknown error")
      }
    }
  }
}
