package com.android.sample.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile
import com.android.sample.profile.UserProfileRepository
import com.android.sample.settings.ProfileFormManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingAcademicUiState(
    val selectedFaculty: String = "",
    val selectedSection: String = "",
    val availableFaculties: List<String> = emptyList(),
    val availableSections: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isComplete: Boolean = false
) {
  val canContinue: Boolean
    get() = selectedFaculty.isNotBlank() && selectedSection.isNotBlank() && !isSaving
}

class OnboardingAcademicViewModel(
    private val profileRepository: ProfileDataSource = UserProfileRepository()
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          OnboardingAcademicUiState(
              availableFaculties = ProfileFormManager.facultyToSections.keys.toList()))

  val uiState: StateFlow<OnboardingAcademicUiState> = _uiState.asStateFlow()

  fun updateFaculty(newFaculty: String) {
    val availableSections = ProfileFormManager.facultyToSections[newFaculty] ?: emptyList()
    _uiState.value =
        _uiState.value.copy(
            selectedFaculty = newFaculty,
            selectedSection = "", // Reset section when faculty changes
            availableSections = availableSections,
            saveError = null)
  }

  fun updateSection(section: String) {
    _uiState.value = _uiState.value.copy(selectedSection = section, saveError = null)
  }

  fun continueToNext(onSuccess: () -> Unit) {
    val currentState = _uiState.value
    val selectedFaculty = currentState.selectedFaculty
    val selectedSection = currentState.selectedSection

    if (selectedFaculty.isBlank() || selectedSection.isBlank()) return

    _uiState.value = currentState.copy(isSaving = true, saveError = null)

    viewModelScope.launch {
      try {
        // Load existing profile to preserve other fields
        val existingProfile = profileRepository.loadProfile() ?: UserProfile()

        // Update only the faculty and section fields
        val updatedProfile =
            existingProfile.copy(faculty = selectedFaculty, section = selectedSection)

        // Save to Firestore (merge will preserve other fields)
        profileRepository.saveProfile(updatedProfile)

        _uiState.value = _uiState.value.copy(isComplete = true, isSaving = false)
        onSuccess()
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isSaving = false,
                saveError = e.message ?: "Failed to save academic information. Please try again.")
      }
    }
  }

  fun skip(onSuccess: () -> Unit) {
    // On Skip: Do NOT perform any database write (to preserve existing data/minimize writes).
    // Just trigger the navigation callback.
    onSuccess()
  }
}
