package com.android.sample.settings

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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()

  private lateinit var mockRepository: ProfileDataSource
  private lateinit var viewModel: ProfileViewModel

  @Before
  fun setUp() {
    mockRepository = mock()
    viewModel = ProfileViewModel(profileRepository = mockRepository)
  }

  @Test
  fun init_loadsProfile_automatically() = runTest {
    val testProfile =
        UserProfile(
            fullName = "John Doe",
            preferredName = "John",
            faculty = "IC",
            section = "IN",
            email = "john@epfl.ch",
            phone = "+41 21 123 45 67",
            roleDescription = "Student")

    // Set up mock BEFORE creating ViewModel so init block can use it
    whenever(mockRepository.loadProfile()).thenReturn(testProfile)

    // Create ViewModel - init will call loadProfile()
    val testViewModel = ProfileViewModel(profileRepository = mockRepository)

    advanceUntilIdle()

    val profile = testViewModel.userProfile.first()
    assertNotNull("Profile should be loaded", profile)
    assertEquals("John Doe", profile?.fullName)
    assertEquals("John", profile?.preferredName)
    assertFalse("Should not be loading after init", testViewModel.isLoading.first())
  }

  @Test
  fun init_loadsProfile_returnsNull_whenNoProfile() = runTest {
    // Set up mock BEFORE creating ViewModel
    whenever(mockRepository.loadProfile()).thenReturn(null)

    // Create ViewModel - init will call loadProfile()
    val testViewModel = ProfileViewModel(profileRepository = mockRepository)

    advanceUntilIdle()

    val profile = testViewModel.userProfile.first()
    assertNull("Profile should be null when not found", profile)
    assertFalse("Should not be loading after init", testViewModel.isLoading.first())
  }

  @Test
  fun loadProfile_setsLoadingState() = runTest {
    val testProfile = UserProfile(fullName = "Test User")
    whenever(mockRepository.loadProfile()).thenReturn(testProfile)

    viewModel.loadProfile()
    advanceUntilIdle()

    assertFalse("Should not be loading after completion", viewModel.isLoading.first())
    assertEquals("Test User", viewModel.userProfile.first()?.fullName)
  }

  @Test
  fun loadProfile_handlesException() = runTest {
    val errorMessage = "Network error"
    whenever(mockRepository.loadProfile()).thenThrow(RuntimeException(errorMessage))

    viewModel.loadProfile()
    advanceUntilIdle()

    val error = viewModel.errorMessage.first()
    assertNotNull("Error message should be set", error)
    assertTrue("Error should contain message", error!!.contains(errorMessage))
    assertFalse("Should not be loading after error", viewModel.isLoading.first())
  }

  @Test
  fun saveProfile_successfullySaves() = runTest {
    val testProfile =
        UserProfile(
            fullName = "Jane Smith",
            preferredName = "Jane",
            faculty = "SB",
            section = "CH",
            email = "jane@epfl.ch",
            phone = "+41 21 987 65 43",
            roleDescription = "Professor")

    viewModel.saveProfile(testProfile)
    advanceUntilIdle()

    verify(mockRepository).saveProfile(testProfile)
    assertEquals("Jane Smith", viewModel.userProfile.first()?.fullName)
    assertFalse("Should not be saving after completion", viewModel.isSaving.first())
    assertNull("Should have no error after success", viewModel.errorMessage.first())
  }

  @Test
  fun saveProfile_setsSavingState() = runTest {
    val testProfile = UserProfile(fullName = "Test User")
    whenever(mockRepository.saveProfile(testProfile)).thenAnswer {}

    viewModel.saveProfile(testProfile)
    advanceUntilIdle()

    assertFalse("Should not be saving after completion", viewModel.isSaving.first())
  }

  @Test
  fun saveProfile_handlesException() = runTest {
    val testProfile = UserProfile(fullName = "Test User")
    val errorMessage = "Save failed"
    whenever(mockRepository.saveProfile(testProfile)).thenThrow(RuntimeException(errorMessage))

    viewModel.saveProfile(testProfile)
    advanceUntilIdle()

    val error = viewModel.errorMessage.first()
    assertNotNull("Error message should be set", error)
    assertTrue("Error should contain message", error!!.contains(errorMessage))
    assertFalse("Should not be saving after error", viewModel.isSaving.first())
  }

  @Test
  fun saveProfile_updatesLocalState_afterSuccess() = runTest {
    val initialProfile = UserProfile(fullName = "Initial")
    val updatedProfile = UserProfile(fullName = "Updated")

    // Set initial profile
    whenever(mockRepository.loadProfile()).thenReturn(initialProfile)
    viewModel.loadProfile()
    advanceUntilIdle()

    assertEquals("Initial", viewModel.userProfile.first()?.fullName)

    // Save updated profile
    viewModel.saveProfile(updatedProfile)
    advanceUntilIdle()

    assertEquals("Updated", viewModel.userProfile.first()?.fullName)
  }

  @Test
  fun clearError_removesErrorMessage() = runTest {
    val errorMessage = "Test error"
    whenever(mockRepository.loadProfile()).thenThrow(RuntimeException(errorMessage))

    viewModel.loadProfile()
    advanceUntilIdle()

    assertNotNull("Error should be set", viewModel.errorMessage.first())

    viewModel.clearError()

    assertNull("Error should be cleared", viewModel.errorMessage.first())
  }
}
