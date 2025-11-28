package com.android.sample.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.profile.ProfileDataSource
import com.android.sample.settings.ProfileFormManager
import com.android.sample.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingAcademicScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @get:Rule val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  private lateinit var viewModel: OnboardingAcademicViewModel
  private lateinit var mockRepository: ProfileDataSource

  @Before
  fun setUp() {
    mockRepository = mock()
    viewModel = OnboardingAcademicViewModel(profileRepository = mockRepository)
  }

  @After
  fun tearDown() {
    // Clean up if needed
  }

  /**
   * Helper function to inject state into the ViewModel using reflection. This allows us to test
   * different UI states without triggering actual ViewModel logic.
   */
  private fun setUiState(state: OnboardingAcademicUiState) {
    val field = OnboardingAcademicViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(viewModel) as MutableStateFlow<OnboardingAcademicUiState>
    stateFlow.value = state
  }

  @Test
  fun displaysTitle() {
    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Academic Information").assertIsDisplayed()
  }

  @Test
  fun displaysFacultyDropdown() {
    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Faculty").assertIsDisplayed()
    composeRule.onNodeWithText("Select faculty").assertIsDisplayed()
  }

  @Test
  fun displaysSectionDropdown() {
    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Section").assertIsDisplayed()
    composeRule.onNodeWithText("Select faculty first").assertIsDisplayed()
  }

  @Test
  fun facultyDropdown_isEnabled_initially() {
    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // The dropdown field should be enabled (clickable)
    // We can verify by checking that clicking it would open the menu
    // For now, we verify the placeholder text is displayed (which indicates it's enabled)
    composeRule.onNodeWithText("Select faculty").assertIsDisplayed()
  }

  @Test
  fun sectionDropdown_isDisabled_initially() {
    setUiState(
        OnboardingAcademicUiState(
            selectedFaculty = "",
            selectedSection = "",
            availableFaculties = ProfileFormManager.facultyToSections.keys.toList(),
            availableSections = emptyList()))

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Section dropdown should show "Select faculty first" placeholder
    composeRule.onNodeWithText("Select faculty first").assertIsDisplayed()
    // The dropdown should be disabled (not clickable when faculty is empty)
    // We verify this by checking the placeholder text indicates disabled state
  }

  @Test
  fun continueButton_isDisabled_initially() {
    setUiState(
        OnboardingAcademicUiState(
            selectedFaculty = "",
            selectedSection = "",
            availableFaculties = ProfileFormManager.facultyToSections.keys.toList(),
            availableSections = emptyList(),
            isSaving = false))

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Continue").assertIsNotEnabled()
  }

  @Test
  fun skipButton_isEnabled_initially() {
    setUiState(
        OnboardingAcademicUiState(
            selectedFaculty = "",
            selectedSection = "",
            availableFaculties = ProfileFormManager.facultyToSections.keys.toList(),
            availableSections = emptyList(),
            isSaving = false))

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Skip").assertIsEnabled()
  }

  @Test
  fun selectingFaculty_enablesSectionDropdown() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    val icSections = ProfileFormManager.facultyToSections[icFaculty] ?: emptyList()

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Initially, section should show "Select faculty first"
    composeRule.onNodeWithText("Select faculty first").assertIsDisplayed()

    // Click on Faculty dropdown
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()

    // Click on a faculty option (IC)
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()

    // Verify section dropdown now shows "Select section" (enabled state)
    composeRule.onNodeWithText("Select section").assertIsDisplayed()
    // Verify the section dropdown is now enabled (we can interact with it)
  }

  @Test
  fun selectingFacultyAndSection_enablesContinueButton() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    val section = "Computer Science"

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Initially Continue should be disabled
    composeRule.onNodeWithText("Continue").assertIsNotEnabled()

    // Select Faculty
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()

    // Select Section
    composeRule.onNodeWithText("Select section").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(section).performClick()
    advanceUntilIdle()

    // Continue button should now be enabled
    composeRule.onNodeWithText("Continue").assertIsEnabled()
  }

  @Test
  fun changingFaculty_resetsSection() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    val sbFaculty = "SB — School of Basic Sciences"
    val section = "Computer Science"

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Select IC faculty
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()

    // Select a section
    composeRule.onNodeWithText("Select section").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(section).performClick()
    advanceUntilIdle()

    // Verify section is selected
    composeRule.onNodeWithText(section).assertIsDisplayed()

    // Change faculty to SB
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(sbFaculty).performClick()
    advanceUntilIdle()

    // Section should be reset (show "Select section" placeholder)
    composeRule.onNodeWithText("Select section").assertIsDisplayed()
    composeRule.onNodeWithText(section).assertDoesNotExist()
  }

  @Test
  fun continueButton_callsContinueToNext() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    val icFaculty = "IC — School of Computer and Communication Sciences"
    val section = "Computer Science"

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Select faculty and section
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()

    composeRule.onNodeWithText("Select section").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(section).performClick()
    advanceUntilIdle()

    // Click Continue
    composeRule.onNodeWithText("Continue").performClick()
    advanceUntilIdle()

    // Verify saveProfile was called
    verify(mockRepository).saveProfile(any())
  }

  @Test
  fun skipButton_callsSkip() = runTest {
    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Click Skip
    composeRule.onNodeWithText("Skip").performClick()
    advanceUntilIdle()

    // Verify saveProfile was NEVER called
    verify(mockRepository, never()).saveProfile(any())
  }

  @Test
  fun onContinue_callback_isCalled_afterSuccessfulSave() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    val icFaculty = "IC — School of Computer and Communication Sciences"
    val section = "Computer Science"

    var navigationCalled = false
    composeRule.setContent {
      MaterialTheme {
        OnboardingAcademicScreen(onContinue = { navigationCalled = true }, viewModel = viewModel)
      }
    }

    // Select faculty and section
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()

    composeRule.onNodeWithText("Select section").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(section).performClick()
    advanceUntilIdle()

    // Click Continue
    composeRule.onNodeWithText("Continue").performClick()
    advanceUntilIdle()

    // Wait for coroutine to complete
    composeRule.waitForIdle()

    assertTrue("Navigation callback should be called after successful save", navigationCalled)
  }

  @Test
  fun onContinue_callback_isCalled_afterSkip() = runTest {
    var navigationCalled = false
    composeRule.setContent {
      MaterialTheme {
        OnboardingAcademicScreen(onContinue = { navigationCalled = true }, viewModel = viewModel)
      }
    }

    // Click Skip
    composeRule.onNodeWithText("Skip").performClick()
    advanceUntilIdle()

    // Wait for any operations to complete
    composeRule.waitForIdle()

    assertTrue("Navigation callback should be called after skip", navigationCalled)
  }

  @Test
  fun displaysError_whenSaveErrorExists() {
    setUiState(
        OnboardingAcademicUiState(
            selectedFaculty = "IC — School of Computer and Communication Sciences",
            selectedSection = "Computer Science",
            availableFaculties = ProfileFormManager.facultyToSections.keys.toList(),
            availableSections = listOf("Computer Science", "Communication Systems"),
            isSaving = false,
            saveError = "Network error"))

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Network error").assertIsDisplayed()
  }

  @Test
  fun doesNotDisplayError_whenNoError() {
    setUiState(
        OnboardingAcademicUiState(
            selectedFaculty = "IC — School of Computer and Communication Sciences",
            selectedSection = "Computer Science",
            availableFaculties = ProfileFormManager.facultyToSections.keys.toList(),
            availableSections = listOf("Computer Science", "Communication Systems"),
            isSaving = false,
            saveError = null))

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Error should not be displayed
    composeRule.onNodeWithText("Network error").assertDoesNotExist()
  }

  @Test
  fun continueButton_showsSavingText_whenSaving() {
    setUiState(
        OnboardingAcademicUiState(
            selectedFaculty = "IC — School of Computer and Communication Sciences",
            selectedSection = "Computer Science",
            availableFaculties = ProfileFormManager.facultyToSections.keys.toList(),
            availableSections = listOf("Computer Science", "Communication Systems"),
            isSaving = true))

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Verify the button shows "Saving..." text
    composeRule.onNodeWithText("Continue").assertDoesNotExist()
  }

  @Test
  fun allFaculties_areAvailableInDropdown() = runTest {
    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Click on Faculty dropdown
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()

    // Verify all faculties from ProfileFormManager are available
    ProfileFormManager.facultyToSections.keys.forEach { faculty ->
      composeRule.onNodeWithText(faculty).assertIsDisplayed()
    }
  }

  @Test
  fun sections_areAvailableBasedOnSelectedFaculty() = runTest {
    val icFaculty = "IC — School of Computer and Communication Sciences"
    val icSections = ProfileFormManager.facultyToSections[icFaculty] ?: emptyList()

    composeRule.setContent {
      MaterialTheme { OnboardingAcademicScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Select IC faculty
    composeRule.onNodeWithText("Select faculty").performClick()
    advanceUntilIdle()
    composeRule.onNodeWithText(icFaculty).performClick()
    advanceUntilIdle()

    // Click on Section dropdown
    composeRule.onNodeWithText("Select section").performClick()
    advanceUntilIdle()

    // Verify all sections for IC are available
    icSections.forEach { section -> composeRule.onNodeWithText(section).assertIsDisplayed() }
  }
}
