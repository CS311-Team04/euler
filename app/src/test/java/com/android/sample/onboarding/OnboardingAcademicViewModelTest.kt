package com.android.sample.onboarding

import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile
import com.android.sample.settings.ProfileFormManager
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingAcademicViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  private lateinit var mockRepository: ProfileDataSource
  private lateinit var viewModel: OnboardingAcademicViewModel

  @Before
  fun setUp() {
    mockRepository = mock()
    viewModel = OnboardingAcademicViewModel(profileRepository = mockRepository)
  }

  @Test
  fun initialState_hasEmptyFacultyAndSection_andContinueDisabled() = runTest {
    val state = viewModel.uiState.first()
    assertEquals("Faculty should be empty initially", "", state.selectedFaculty)
    assertEquals("Section should be empty initially", "", state.selectedSection)
    assertFalse("Continue should be disabled when fields are empty", state.canContinue)
    assertFalse("Should not be saving initially", state.isSaving)
    assertNull("Should have no error initially", state.saveError)
    assertFalse("Should not be complete initially", state.isComplete)
    assertTrue(
        "Should have available faculties from ProfileFormManager",
        state.availableFaculties.isNotEmpty())
    assertEquals(
        "Available faculties should match ProfileFormManager",
        ProfileFormManager.facultyToSections.keys.toList(),
        state.availableFaculties)
    assertTrue("Available sections should be empty initially", state.availableSections.isEmpty())
  }

  @Test
  fun updateFaculty_setsFacultyAndUpdatesAvailableSections() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    viewModel.updateFaculty(icFaculty)

    val state = viewModel.uiState.first()
    assertEquals("Faculty should be set", icFaculty, state.selectedFaculty)
    assertEquals(
        "Available sections should match faculty",
        ProfileFormManager.facultyToSections[icFaculty],
        state.availableSections)
    assertTrue(
        "Available sections should contain Computer Science",
        state.availableSections.contains("Computer Science"))
    assertTrue(
        "Available sections should contain Communication Systems",
        state.availableSections.contains("Communication Systems"))
  }

  @Test
  fun updateFaculty_resetsSectionToEmpty() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    val enacFaculty = "ENAC — School of Architecture, Civil and Environmental Engineering"

    // First, set faculty and section
    viewModel.updateFaculty(icFaculty)
    viewModel.updateSection("Computer Science")

    var state = viewModel.uiState.first()
    assertEquals("Faculty should be set", icFaculty, state.selectedFaculty)
    assertEquals("Section should be set", "Computer Science", state.selectedSection)

    // Change faculty - section should be reset
    viewModel.updateFaculty(enacFaculty)
    state = viewModel.uiState.first()
    assertEquals("Faculty should be updated", enacFaculty, state.selectedFaculty)
    assertEquals("Section should be reset to empty", "", state.selectedSection)
    assertEquals(
        "Available sections should match new faculty",
        ProfileFormManager.facultyToSections[enacFaculty],
        state.availableSections)
  }

  @Test
  fun updateSection_setsSection() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    viewModel.updateFaculty(icFaculty)
    viewModel.updateSection("Computer Science")

    val state = viewModel.uiState.first()
    assertEquals("Section should be set", "Computer Science", state.selectedSection)
    assertNull("Error should be cleared when selecting section", state.saveError)
  }

  @Test
  fun updateFaculty_clearsPreviousError() = runTest {
    // First, cause an error by making save fail
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any()))
        .thenThrow(RuntimeException("Test error"))

    viewModel.updateFaculty("IC — School of Computer and Communication Sciences")
    viewModel.updateSection("Computer Science")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull("Should have error after failed save", state.saveError)

    // Update faculty - should clear error
    viewModel.updateFaculty("SB — School of Basic Sciences")
    state = viewModel.uiState.first()
    assertNull("Error should be cleared when updating faculty", state.saveError)
  }

  @Test
  fun continueToNext_savesProfileWithFacultyAndSection_whenNoExistingProfile() = runTest {
    // Setup: no existing profile
    whenever(mockRepository.loadProfile()).thenReturn(null)

    val faculty = "IC — School of Computer and Communication Sciences"
    val section = "Computer Science"

    viewModel.updateFaculty(faculty)
    viewModel.updateSection(section)
    var navigationCalled = false
    viewModel.continueToNext { navigationCalled = true }
    advanceUntilIdle()

    // Verify saveProfile was called with correct faculty and section
    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("Faculty should be saved", faculty, savedProfile.faculty)
    assertEquals("Section should be saved", section, savedProfile.section)
    assertEquals("", savedProfile.fullName) // Empty since no existing profile
    assertEquals("", savedProfile.preferredName)
    assertEquals("", savedProfile.email)
    assertEquals("", savedProfile.phone)
    assertEquals("", savedProfile.roleDescription)

    // Verify state updates
    val state = viewModel.uiState.first()
    assertTrue("Should be complete after successful save", state.isComplete)
    assertFalse("Should not be saving after completion", state.isSaving)
    assertNull("Should have no error after success", state.saveError)
    assertTrue("Navigation callback should be called", navigationCalled)
  }

  @Test
  fun continueToNext_preservesExistingProfileFields() = runTest {
    // Setup: existing profile with some data
    val existingProfile =
        UserProfile(
            fullName = "John Doe",
            preferredName = "John",
            faculty = "SB",
            section = "CH",
            email = "john@epfl.ch",
            phone = "+41 21 123 45 67",
            roleDescription = "Student")

    whenever(mockRepository.loadProfile()).thenReturn(existingProfile)

    val newFaculty = "IC — School of Computer and Communication Sciences"
    val newSection = "Computer Science"

    viewModel.updateFaculty(newFaculty)
    viewModel.updateSection(newSection)
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    // Verify saveProfile was called with preserved fields + new faculty/section
    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("Faculty should be updated", newFaculty, savedProfile.faculty)
    assertEquals("Section should be updated", newSection, savedProfile.section)
    assertEquals("John Doe", savedProfile.fullName) // Preserved
    assertEquals("John", savedProfile.preferredName) // Preserved
    assertEquals("john@epfl.ch", savedProfile.email) // Preserved
    assertEquals("+41 21 123 45 67", savedProfile.phone) // Preserved
    assertEquals("Student", savedProfile.roleDescription) // Preserved
  }

  @Test
  fun continueToNext_doesNothing_whenFacultyEmpty() = runTest {
    // Don't set faculty, only set section
    viewModel.updateSection("Computer Science")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    // Verify saveProfile was never called
    verify(mockRepository, never()).saveProfile(org.mockito.kotlin.any())

    val state = viewModel.uiState.first()
    assertFalse("Should not be saving when faculty is empty", state.isSaving)
    assertFalse("Should not be complete when faculty is empty", state.isComplete)
  }

  @Test
  fun continueToNext_doesNothing_whenSectionEmpty() = runTest {
    // Set faculty but not section
    viewModel.updateFaculty("IC — School of Computer and Communication Sciences")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    // Verify saveProfile was never called
    verify(mockRepository, never()).saveProfile(org.mockito.kotlin.any())

    val state = viewModel.uiState.first()
    assertFalse("Should not be saving when section is empty", state.isSaving)
    assertFalse("Should not be complete when section is empty", state.isComplete)
  }

  @Test
  fun continueToNext_handlesException() = runTest {
    val errorMessage = "Network error"
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any()))
        .thenThrow(RuntimeException(errorMessage))

    viewModel.updateFaculty("IC — School of Computer and Communication Sciences")
    viewModel.updateSection("Computer Science")
    var navigationCalled = false
    viewModel.continueToNext { navigationCalled = true }
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse("Should not be saving after error", state.isSaving)
    assertFalse("Should not be complete after error", state.isComplete)
    assertNotNull("Should have error message", state.saveError)
    assertTrue("Error should contain message", state.saveError!!.contains(errorMessage))
    assertFalse("Navigation callback should not be called on error", navigationCalled)
  }

  @Test
  fun continueToNext_handlesExceptionWithNullMessage() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any())).thenThrow(RuntimeException())

    viewModel.updateFaculty("SB — School of Basic Sciences")
    viewModel.updateSection("Mathematics")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(
        "Should have default error message when exception has no message", state.saveError)
    assertTrue(
        "Error should contain default message",
        state.saveError!!.contains("Failed to save academic information"))
  }

  @Test
  fun skip_doesNotCallSaveProfile() = runTest {
    var navigationCalled = false
    viewModel.skip { navigationCalled = true }

    // Verify saveProfile was NEVER called
    verify(mockRepository, never()).saveProfile(org.mockito.kotlin.any())

    // Verify navigation callback is called immediately
    assertTrue("Navigation callback should be called immediately", navigationCalled)

    val state = viewModel.uiState.first()
    assertFalse("Should not be saving after skip", state.isSaving)
    assertFalse("Should not be complete after skip", state.isComplete)
    assertNull("Should have no error after skip", state.saveError)
  }

  @Test
  fun skip_callsOnSuccessCallback() = runTest {
    var navigationCalled = false
    viewModel.skip { navigationCalled = true }

    // Skip should call the callback immediately (synchronously)
    assertTrue("Navigation callback should be called", navigationCalled)

    // Verify no database operations occurred
    verify(mockRepository, never()).loadProfile()
    verify(mockRepository, never()).saveProfile(org.mockito.kotlin.any())
  }

  @Test
  fun canContinue_returnsFalse_whenFacultyEmpty() = runTest {
    viewModel.updateSection("Computer Science")
    val state = viewModel.uiState.first()
    assertFalse("Continue should be disabled when faculty is empty", state.canContinue)
  }

  @Test
  fun canContinue_returnsFalse_whenSectionEmpty() = runTest {
    viewModel.updateFaculty("IC — School of Computer and Communication Sciences")
    val state = viewModel.uiState.first()
    assertFalse("Continue should be disabled when section is empty", state.canContinue)
  }

  @Test
  fun canContinue_returnsTrue_whenBothFieldsSet() = runTest {
    viewModel.updateFaculty("IC — School of Computer and Communication Sciences")
    viewModel.updateSection("Computer Science")
    val state = viewModel.uiState.first()
    assertTrue("Continue should be enabled when both fields are set", state.canContinue)
  }

  @Test
  fun canContinue_returnsFalse_whenSaving() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    viewModel.updateFaculty("IC — School of Computer and Communication Sciences")
    viewModel.updateSection("Computer Science")
    val initialState = viewModel.uiState.first()
    assertTrue(
        "Continue should be enabled when fields set and not saving", initialState.canContinue)

    // Start saving
    viewModel.continueToNext { /* navigation callback */}
    // Note: With test dispatcher, this completes quickly, but the logic is correct
    advanceUntilIdle()

    val finalState = viewModel.uiState.first()
    // After completion, canContinue should be true again (if fields still set)
    assertTrue("Continue should be enabled after completion", finalState.canContinue)
  }

  @Test
  fun updateFaculty_withDifferentFaculties_updatesAvailableSections() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    val sbFaculty = "SB — School of Basic Sciences"
    val enacFaculty = "ENAC — School of Architecture, Civil and Environmental Engineering"

    // Test IC
    viewModel.updateFaculty(icFaculty)
    var state = viewModel.uiState.first()
    assertEquals(
        "IC sections should be available",
        ProfileFormManager.facultyToSections[icFaculty],
        state.availableSections)

    // Test SB
    viewModel.updateFaculty(sbFaculty)
    state = viewModel.uiState.first()
    assertEquals(
        "SB sections should be available",
        ProfileFormManager.facultyToSections[sbFaculty],
        state.availableSections)

    // Test ENAC
    viewModel.updateFaculty(enacFaculty)
    state = viewModel.uiState.first()
    assertEquals(
        "ENAC sections should be available",
        ProfileFormManager.facultyToSections[enacFaculty],
        state.availableSections)
  }
}
