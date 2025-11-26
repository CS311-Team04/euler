package com.android.sample.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile
import com.android.sample.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user profile data. Loads and saves profile using Firestore via
 * UserProfileRepository.
 */
class ProfileViewModel(private val profileRepository: ProfileDataSource = UserProfileRepository()) :
    ViewModel() {

  private val _userProfile = MutableStateFlow<UserProfile?>(null)
  val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  init {
    loadProfile()
  }

  /** Loads the user profile from Firestore. Automatically called in init block. */
  fun loadProfile() {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      try {
        val profile = profileRepository.loadProfile()
        _userProfile.value = profile
        Log.d("ProfileViewModel", "Profile loaded: ${profile?.fullName ?: "null"}")
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Failed to load profile", e)
        _errorMessage.value = "Failed to load profile: ${e.message}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Saves the user profile to Firestore. Updates the local state after successful save. */
  fun saveProfile(profile: UserProfile) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      try {
        profileRepository.saveProfile(profile)
        _userProfile.value = profile
        Log.d("ProfileViewModel", "Profile saved: ${profile.fullName}")
      } catch (e: Exception) {
        Log.e("ProfileViewModel", "Failed to save profile", e)
        _errorMessage.value = "Failed to save profile: ${e.message}"
      } finally {
        _isSaving.value = false
      }
    }
  }

  /** Clears the error message. */
  fun clearError() {
    _errorMessage.value = null
  }
}
