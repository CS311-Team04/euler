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

data class OnboardingPersonalInfoUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val usernameError: String? = null,
    val submitError: String? = null,
    val isSubmitting: Boolean = false,
    val isComplete: Boolean = false
) {
  val isFormValid: Boolean
    get() =
        firstName.trim().isNotBlank() &&
            lastName.trim().isNotBlank() &&
            username.trim().isNotBlank() &&
            username.trim().length >= 3 &&
            firstNameError == null &&
            lastNameError == null &&
            usernameError == null
}

class OnboardingPersonalInfoViewModel(
    private val profileRepository: ProfileDataSource = UserProfileRepository()
) : ViewModel() {

  private val _uiState = MutableStateFlow(OnboardingPersonalInfoUiState())
  val uiState: StateFlow<OnboardingPersonalInfoUiState> = _uiState.asStateFlow()

  fun updateFirstName(value: String) {
    _uiState.value =
        _uiState.value.copy(firstName = value, firstNameError = null, submitError = null)
    validateFirstName(value)
  }

  fun updateLastName(value: String) {
    _uiState.value = _uiState.value.copy(lastName = value, lastNameError = null, submitError = null)
    validateLastName(value)
  }

  fun updateUsername(value: String) {
    _uiState.value = _uiState.value.copy(username = value, usernameError = null, submitError = null)
    validateUsername(value)
  }

  private fun validateFirstName(value: String) {
    val error = if (value.trim().isBlank()) "First name is required" else null
    _uiState.value = _uiState.value.copy(firstNameError = error)
  }

  private fun validateLastName(value: String) {
    val error = if (value.trim().isBlank()) "Last name is required" else null
    _uiState.value = _uiState.value.copy(lastNameError = error)
  }

  private fun validateUsername(value: String) {
    val trimmed = value.trim()
    val error =
        when {
          trimmed.isBlank() -> "Username is required"
          trimmed.length < 3 -> "Username must be at least 3 characters"
          else -> null
        }
    _uiState.value = _uiState.value.copy(usernameError = error)
  }

  fun submitPersonalInfo() {
    val currentState = _uiState.value

    // Validate all fields
    validateFirstName(currentState.firstName)
    validateLastName(currentState.lastName)
    validateUsername(currentState.username)

    val updatedState = _uiState.value
    if (!updatedState.isFormValid) {
      return
    }

    _uiState.value = updatedState.copy(isSubmitting = true, submitError = null)

    viewModelScope.launch {
      try {
        // Prepare data for Firestore
        val fullName = "${currentState.firstName.trim()} ${currentState.lastName.trim()}".trim()

        // Create UserProfile object
        val profile =
            UserProfile(
                fullName = fullName,
                preferredName = currentState.username.trim(),
                // Other fields are empty for now, which is normal
                faculty = "",
                section = "",
                email = "",
                phone = "",
                roleDescription = "")

        // Save directly via Firestore (Repository)
        profileRepository.saveProfile(profile)

        // Success!
        _uiState.value = _uiState.value.copy(isComplete = true, isSubmitting = false)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isSubmitting = false,
                submitError = e.message ?: "Failed to save personal information. Please try again.")
      }
    }
  }
}
