package com.android.sample.onboarding

import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile
import com.android.sample.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPersonalInfoViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  private lateinit var mockRepository: ProfileDataSource
  private lateinit var viewModel: OnboardingPersonalInfoViewModel

  @Before
  fun setUp() {
    mockRepository = mock()
    viewModel = OnboardingPersonalInfoViewModel(profileRepository = mockRepository)
  }

  @Test
  fun initialState_isEmpty() = runTest {
    val state = viewModel.uiState.first()
    assertEquals("", state.firstName)
    assertEquals("", state.lastName)
    assertEquals("", state.username)
    assertNull(state.firstNameError)
    assertNull(state.lastNameError)
    assertNull(state.usernameError)
    assertNull(state.submitError)
    assertFalse(state.isSubmitting)
    assertFalse(state.isComplete)
    assertFalse(state.isFormValid)
  }

  @Test
  fun updateFirstName_updatesValue() = runTest {
    viewModel.updateFirstName("John")

    val state = viewModel.uiState.first()
    assertEquals("John", state.firstName)
    assertNull("Error should be cleared when typing", state.firstNameError)
  }

  @Test
  fun updateLastName_updatesValue() = runTest {
    viewModel.updateLastName("Doe")

    val state = viewModel.uiState.first()
    assertEquals("Doe", state.lastName)
    assertNull("Error should be cleared when typing", state.lastNameError)
  }

  @Test
  fun updateUsername_updatesValue() = runTest {
    viewModel.updateUsername("johndoe")

    val state = viewModel.uiState.first()
    assertEquals("johndoe", state.username)
    assertNull("Error should be cleared when typing", state.usernameError)
  }

  @Test
  fun validateFirstName_requiresNonBlank() = runTest {
    viewModel.updateFirstName("")
    var state = viewModel.uiState.first()
    assertNotNull("Should have error for blank first name", state.firstNameError)

    viewModel.updateFirstName("   ")
    state = viewModel.uiState.first()
    assertNotNull("Should have error for whitespace-only first name", state.firstNameError)

    viewModel.updateFirstName("John")
    state = viewModel.uiState.first()
    assertNull("Should not have error for valid first name", state.firstNameError)
  }

  @Test
  fun validateLastName_requiresNonBlank() = runTest {
    viewModel.updateLastName("")
    var state = viewModel.uiState.first()
    assertNotNull("Should have error for blank last name", state.lastNameError)

    viewModel.updateLastName("   ")
    state = viewModel.uiState.first()
    assertNotNull("Should have error for whitespace-only last name", state.lastNameError)

    viewModel.updateLastName("Doe")
    state = viewModel.uiState.first()
    assertNull("Should not have error for valid last name", state.lastNameError)
  }

  @Test
  fun validateUsername_requiresMinimumLength() = runTest {
    viewModel.updateUsername("")
    var state = viewModel.uiState.first()
    assertNotNull("Should have error for blank username", state.usernameError)

    viewModel.updateUsername("ab")
    state = viewModel.uiState.first()
    assertNotNull("Should have error for username < 3 characters", state.usernameError)
    assertTrue("Error should mention minimum length", state.usernameError!!.contains("3"))

    viewModel.updateUsername("abc")
    state = viewModel.uiState.first()
    assertNull("Should not have error for username >= 3 characters", state.usernameError)
  }

  @Test
  fun isFormValid_returnsFalse_whenFieldsEmpty() = runTest {
    val state = viewModel.uiState.first()
    assertFalse("Form should be invalid when empty", state.isFormValid)
  }

  @Test
  fun isFormValid_returnsFalse_whenUsernameTooShort() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("ab")

    val state = viewModel.uiState.first()
    assertFalse("Form should be invalid when username too short", state.isFormValid)
  }

  @Test
  fun isFormValid_returnsTrue_whenAllFieldsValid() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    val state = viewModel.uiState.first()
    assertTrue("Form should be valid when all fields are valid", state.isFormValid)
  }

  @Test
  fun submitPersonalInfo_validatesAllFields() = runTest {
    viewModel.updateFirstName("")
    viewModel.updateLastName("")
    viewModel.updateUsername("")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull("Should have first name error", state.firstNameError)
    assertNotNull("Should have last name error", state.lastNameError)
    assertNotNull("Should have username error", state.usernameError)
    assertFalse("Should not be submitting when invalid", state.isSubmitting)
    assertFalse("Should not be complete when invalid", state.isComplete)
  }

  @Test
  fun submitPersonalInfo_doesNotSubmit_whenFormInvalid() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("ab") // Too short

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    verify(mockRepository, org.mockito.kotlin.never()).saveProfile(any())
    val state = viewModel.uiState.first()
    assertFalse("Should not be submitting when invalid", state.isSubmitting)
  }

  @Test
  fun submitPersonalInfo_savesProfile_whenValid() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("John Doe", savedProfile.fullName)
    assertEquals("johndoe", savedProfile.preferredName)
    assertEquals("", savedProfile.faculty)
    assertEquals("", savedProfile.section)
    assertEquals("", savedProfile.email)
    assertEquals("", savedProfile.phone)
    assertEquals("", savedProfile.roleDescription)
  }

  @Test
  fun submitPersonalInfo_trimsWhitespace() = runTest {
    viewModel.updateFirstName("  John  ")
    viewModel.updateLastName("  Doe  ")
    viewModel.updateUsername("  johndoe  ")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("John Doe", savedProfile.fullName)
    assertEquals("johndoe", savedProfile.preferredName)
  }

  @Test
  fun submitPersonalInfo_setsSubmittingState() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse("Should not be submitting after completion", state.isSubmitting)
    assertTrue("Should be complete after success", state.isComplete)
  }

  @Test
  fun submitPersonalInfo_handlesException() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    val errorMessage = "Save failed"
    whenever(mockRepository.saveProfile(any())).thenThrow(RuntimeException(errorMessage))

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse("Should not be submitting after error", state.isSubmitting)
    assertFalse("Should not be complete after error", state.isComplete)
    assertNotNull("Should have error message", state.submitError)
    assertTrue("Error should contain message", state.submitError!!.contains(errorMessage))
  }

  @Test
  fun submitPersonalInfo_clearsSubmitError_onNewSubmission() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    // First submission fails
    whenever(mockRepository.saveProfile(any())).thenThrow(RuntimeException("First error"))
    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull("Should have error", state.submitError)

    // Second submission succeeds
    whenever(mockRepository.saveProfile(any())).thenAnswer {}
    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertNull("Error should be cleared on new submission", state.submitError)
  }

  @Test
  fun updateFields_clearsSubmitError() = runTest {
    // Set an error first by causing a submission failure
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    val errorMessage = "Test error"
    whenever(mockRepository.saveProfile(any())).thenThrow(RuntimeException(errorMessage))

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull("Should have error after failed submission", state.submitError)

    // Update a field - this should clear the error
    viewModel.updateFirstName("Jane")
    advanceUntilIdle()

    state = viewModel.uiState.first()
    assertNull("Submit error should be cleared when typing", state.submitError)
  }

  @Test
  fun submitPersonalInfo_createsCorrectFullName() = runTest {
    viewModel.updateFirstName("John")
    viewModel.updateLastName("Doe")
    viewModel.updateUsername("johndoe")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("John Doe", savedProfile.fullName)
  }

  @Test
  fun submitPersonalInfo_handlesSingleName() = runTest {
    // Test that fullName construction works correctly when lastName is minimal
    // Note: lastName cannot be empty due to validation, so we use a single character
    viewModel.updateFirstName("Madonna")
    viewModel.updateLastName("X") // Minimal last name to pass validation
    viewModel.updateUsername("madonna")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("Madonna X", savedProfile.fullName)
  }

  @Test
  fun submitPersonalInfo_doesNotSave_whenLastNameEmpty() = runTest {
    // Test that form validation prevents saving when lastName is empty
    viewModel.updateFirstName("Madonna")
    viewModel.updateLastName("")
    viewModel.updateUsername("madonna")

    viewModel.submitPersonalInfo()
    advanceUntilIdle()

    // Should not save because validation fails
    verify(mockRepository, org.mockito.kotlin.never()).saveProfile(any())

    val state = viewModel.uiState.first()
    assertFalse("Should not be submitting when invalid", state.isSubmitting)
    assertFalse("Should not be complete when invalid", state.isComplete)
    assertNotNull("Should have last name error", state.lastNameError)
  }
}
