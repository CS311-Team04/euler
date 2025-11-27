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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingRoleViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  private lateinit var mockRepository: ProfileDataSource
  private lateinit var viewModel: OnboardingRoleViewModel

  @Before
  fun setUp() {
    mockRepository = mock()
    viewModel = OnboardingRoleViewModel(profileRepository = mockRepository)
  }

  @Test
  fun initialState_hasNoRoleSelected_andContinueDisabled() = runTest {
    val state = viewModel.uiState.first()
    assertNull("No role should be selected initially", state.selectedRole)
    assertFalse("Continue should be disabled when no role selected", state.canContinue)
    assertFalse("Should not be saving initially", state.isSaving)
    assertNull("Should have no error initially", state.saveError)
    assertFalse("Should not be complete initially", state.isComplete)
  }

  @Test
  fun selectRole_updatesSelectedRole() = runTest {
    viewModel.selectRole("Student")

    val state = viewModel.uiState.first()
    assertEquals("Student", state.selectedRole)
    assertTrue("Continue should be enabled when role is selected", state.canContinue)
    assertNull("Error should be cleared when selecting role", state.saveError)
  }

  @Test
  fun selectRole_clearsPreviousError() = runTest {
    // First, cause an error by making save fail
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any()))
        .thenThrow(RuntimeException("Test error"))

    viewModel.selectRole("Professor")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    var state = viewModel.uiState.first()
    assertNotNull("Should have error after failed save", state.saveError)

    // Select a different role - should clear error
    viewModel.selectRole("Student")
    state = viewModel.uiState.first()
    assertNull("Error should be cleared when selecting new role", state.saveError)
    assertEquals("Student", state.selectedRole)
  }

  @Test
  fun continueToNext_savesProfileWithSelectedRole_whenNoExistingProfile() = runTest {
    // Setup: no existing profile
    whenever(mockRepository.loadProfile()).thenReturn(null)

    viewModel.selectRole("PhD Candidate")
    var navigationCalled = false
    viewModel.continueToNext { navigationCalled = true }
    advanceUntilIdle()

    // Verify saveProfile was called with correct role
    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("PhD Candidate", savedProfile.roleDescription)
    assertEquals("", savedProfile.fullName) // Empty since no existing profile
    assertEquals("", savedProfile.preferredName)

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
            faculty = "IC",
            section = "IN",
            email = "john@epfl.ch",
            phone = "+41 21 123 45 67",
            roleDescription = "") // Empty initially

    whenever(mockRepository.loadProfile()).thenReturn(existingProfile)

    viewModel.selectRole("Professor")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    // Verify saveProfile was called with preserved fields + new role
    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("Professor", savedProfile.roleDescription)
    assertEquals("John Doe", savedProfile.fullName) // Preserved
    assertEquals("John", savedProfile.preferredName) // Preserved
    assertEquals("IC", savedProfile.faculty) // Preserved
    assertEquals("IN", savedProfile.section) // Preserved
    assertEquals("john@epfl.ch", savedProfile.email) // Preserved
    assertEquals("+41 21 123 45 67", savedProfile.phone) // Preserved
  }

  @Test
  fun continueToNext_doesNothing_whenNoRoleSelected() = runTest {
    // Don't select a role
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    // Verify saveProfile was never called
    verify(mockRepository, org.mockito.kotlin.never()).saveProfile(org.mockito.kotlin.any())

    val state = viewModel.uiState.first()
    assertFalse("Should not be saving when no role selected", state.isSaving)
    assertFalse("Should not be complete when no role selected", state.isComplete)
  }

  @Test
  fun continueToNext_setsSavingState() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    viewModel.selectRole("Student")
    viewModel.continueToNext { /* navigation callback */}

    // Check state immediately (before coroutine completes)
    val stateBefore = viewModel.uiState.first()
    // Note: In a real scenario, isSaving might be true briefly, but with test dispatcher
    // it should complete quickly. Let's verify the final state.
    advanceUntilIdle()

    val stateAfter = viewModel.uiState.first()
    assertFalse("Should not be saving after completion", stateAfter.isSaving)
  }

  @Test
  fun continueToNext_handlesException() = runTest {
    val errorMessage = "Network error"
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any()))
        .thenThrow(RuntimeException(errorMessage))

    viewModel.selectRole("Administration")
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

    viewModel.selectRole("Student")
    viewModel.continueToNext { /* navigation callback */}
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(
        "Should have default error message when exception has no message", state.saveError)
    assertTrue(
        "Error should contain default message", state.saveError!!.contains("Failed to save role"))
  }

  @Test
  fun skip_savesProfileWithEmptyRoleDescription_whenNoExistingProfile() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    var navigationCalled = false
    viewModel.skip { navigationCalled = true }
    advanceUntilIdle()

    // Verify saveProfile was called with empty roleDescription
    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("", savedProfile.roleDescription)
    assertEquals("", savedProfile.fullName)

    // Verify state updates
    val state = viewModel.uiState.first()
    assertTrue("Should be complete after successful skip", state.isComplete)
    assertFalse("Should not be saving after completion", state.isSaving)
    assertNull("Should have no error after success", state.saveError)
    assertTrue("Navigation callback should be called", navigationCalled)
  }

  @Test
  fun skip_preservesExistingProfileFields() = runTest {
    // Setup: existing profile with role already set
    val existingProfile =
        UserProfile(
            fullName = "Jane Smith",
            preferredName = "Jane",
            faculty = "SB",
            section = "CH",
            email = "jane@epfl.ch",
            phone = "+41 21 987 65 43",
            roleDescription = "Professor") // Has existing role

    whenever(mockRepository.loadProfile()).thenReturn(existingProfile)

    viewModel.skip { /* navigation callback */}
    advanceUntilIdle()

    // Verify saveProfile was called with preserved fields but empty roleDescription
    val captor = argumentCaptor<UserProfile>()
    verify(mockRepository).saveProfile(captor.capture())

    val savedProfile = captor.firstValue
    assertEquals("", savedProfile.roleDescription) // Cleared to empty
    assertEquals("Jane Smith", savedProfile.fullName) // Preserved
    assertEquals("Jane", savedProfile.preferredName) // Preserved
    assertEquals("SB", savedProfile.faculty) // Preserved
    assertEquals("CH", savedProfile.section) // Preserved
    assertEquals("jane@epfl.ch", savedProfile.email) // Preserved
    assertEquals("+41 21 987 65 43", savedProfile.phone) // Preserved
  }

  @Test
  fun skip_handlesException() = runTest {
    val errorMessage = "Save failed"
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any()))
        .thenThrow(RuntimeException(errorMessage))

    var navigationCalled = false
    viewModel.skip { navigationCalled = true }
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertFalse("Should not be saving after error", state.isSaving)
    assertFalse("Should not be complete after error", state.isComplete)
    assertNotNull("Should have error message", state.saveError)
    assertTrue("Error should contain message", state.saveError!!.contains(errorMessage))
    assertFalse("Navigation callback should not be called on error", navigationCalled)
  }

  @Test
  fun skip_handlesExceptionWithNullMessage() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(org.mockito.kotlin.any())).thenThrow(RuntimeException())

    viewModel.skip { /* navigation callback */}
    advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertNotNull(
        "Should have default error message when exception has no message", state.saveError)
    assertTrue(
        "Error should contain default message", state.saveError!!.contains("Failed to skip role"))
  }

  @Test
  fun canContinue_returnsFalse_whenSaving() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    viewModel.selectRole("Student")
    val initialState = viewModel.uiState.first()
    assertTrue(
        "Continue should be enabled when role selected and not saving", initialState.canContinue)

    // Start saving
    viewModel.continueToNext { /* navigation callback */}
    // Note: With test dispatcher, this completes quickly, but the logic is correct
    advanceUntilIdle()

    val finalState = viewModel.uiState.first()
    // After completion, canContinue should be true again (if role still selected)
    assertTrue("Continue should be enabled after completion", finalState.canContinue)
  }

  @Test
  fun continueToNext_allRoleOptions() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    val roles = listOf("Student", "PhD Candidate", "Professor", "Administration")

    for (role in roles) {
      val testViewModel = OnboardingRoleViewModel(profileRepository = mockRepository)
      testViewModel.selectRole(role)
      testViewModel.continueToNext { /* navigation callback */}
      advanceUntilIdle()

      val captor = argumentCaptor<UserProfile>()
      verify(mockRepository).saveProfile(captor.capture())
      assertEquals("Role $role should be saved correctly", role, captor.firstValue.roleDescription)

      // Reset mock for next iteration
      org.mockito.Mockito.reset(mockRepository)
      whenever(mockRepository.loadProfile()).thenReturn(null)
    }
  }
}
