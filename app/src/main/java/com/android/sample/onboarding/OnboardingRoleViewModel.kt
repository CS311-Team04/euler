package com.android.sample.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile
import com.android.sample.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingRoleUiState(
    val selectedRole: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isComplete: Boolean = false
) {
  val canContinue: Boolean
    get() = selectedRole != null && !isSaving
}

class OnboardingRoleViewModel(
    private val profileRepository: ProfileDataSource = UserProfileRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(OnboardingRoleUiState())
  val uiState: StateFlow<OnboardingRoleUiState> = _uiState.asStateFlow()

  fun selectRole(role: String) {
    _uiState.value = _uiState.value.copy(selectedRole = role, saveError = null)
  }

  fun continueToNext(onSuccess: () -> Unit) {
    val currentState = _uiState.value
    val selectedRole = currentState.selectedRole ?: return

    _uiState.value = currentState.copy(isSaving = true, saveError = null)

    viewModelScope.launch {
      try {
        // Load existing profile to preserve other fields
        val existingProfile = profileRepository.loadProfile() ?: UserProfile()

        // Update only the roleDescription field
        val updatedProfile = existingProfile.copy(roleDescription = selectedRole)

        // Save to Firestore (merge will preserve other fields)
        profileRepository.saveProfile(updatedProfile)

        _uiState.value = _uiState.value.copy(isComplete = true, isSaving = false)
        onSuccess()
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isSaving = false, saveError = e.message ?: "Failed to save role. Please try again.")
      }
    }
  }

  fun skip(onSuccess: () -> Unit) {
    _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)

    viewModelScope.launch {
      try {
        // Load existing profile to preserve other fields
        val existingProfile = profileRepository.loadProfile() ?: UserProfile()

        // Update with empty roleDescription (or keep existing if already set)
        val updatedProfile = existingProfile.copy(roleDescription = "")

        // Save to Firestore (merge will preserve other fields)
        profileRepository.saveProfile(updatedProfile)

        _uiState.value = _uiState.value.copy(isComplete = true, isSaving = false)
        onSuccess()
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isSaving = false, saveError = e.message ?: "Failed to skip role. Please try again.")
      }
    }
  }
}
