package com.android.sample.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.android.sample.profile.ProfileDataSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingRoleScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @get:Rule val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  private lateinit var viewModel: OnboardingRoleViewModel
  private lateinit var mockRepository: ProfileDataSource

  @Before
  fun setUp() {
    mockRepository = mock()
    viewModel = OnboardingRoleViewModel(profileRepository = mockRepository)
  }

  @After
  fun tearDown() {
    // Clean up if needed
  }

  /**
   * Helper function to inject state into the ViewModel using reflection.
   * This allows us to test different UI states without triggering actual ViewModel logic.
   */
  private fun setUiState(state: OnboardingRoleUiState) {
    val field = OnboardingRoleViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val stateFlow = field.get(viewModel) as MutableStateFlow<OnboardingRoleUiState>
    stateFlow.value = state
  }

  @Test
  fun displaysTitle() {
    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("What is your role?").assertIsDisplayed()
  }

  @Test
  fun displaysAllRoleOptions() {
    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Student").assertIsDisplayed()
    composeRule.onNodeWithText("PhD Candidate").assertIsDisplayed()
    composeRule.onNodeWithText("Professor").assertIsDisplayed()
    composeRule.onNodeWithText("Administration").assertIsDisplayed()
  }

  @Test
  fun continueButton_isDisabled_whenNoRoleSelected() {
    setUiState(OnboardingRoleUiState(selectedRole = null, isSaving = false))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Continue").assertIsNotEnabled()
  }

  @Test
  fun continueButton_isEnabled_whenRoleSelected() {
    setUiState(OnboardingRoleUiState(selectedRole = "Student", isSaving = false))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Continue").assertIsEnabled()
  }

  @Test
  fun continueButton_isDisabled_whenSaving() {
    setUiState(OnboardingRoleUiState(selectedRole = "Student", isSaving = true))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Saving...").assertIsDisplayed()
    composeRule.onNodeWithText("Continue").assertDoesNotExist()
  }

  @Test
  fun skipButton_isAlwaysEnabled_whenNotSaving() {
    setUiState(OnboardingRoleUiState(selectedRole = null, isSaving = false))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Skip").assertIsEnabled()
  }

  @Test
  fun skipButton_isDisabled_whenSaving() {
    setUiState(OnboardingRoleUiState(selectedRole = null, isSaving = true))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Saving...").assertIsDisplayed()
    composeRule.onNodeWithText("Skip").assertDoesNotExist()
  }

  @Test
  fun displaysError_whenSaveErrorExists() {
    setUiState(
        OnboardingRoleUiState(
            selectedRole = "Student", isSaving = false, saveError = "Network error"))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Network error").assertIsDisplayed()
  }

  @Test
  fun doesNotDisplayError_whenNoError() {
    setUiState(OnboardingRoleUiState(selectedRole = "Student", isSaving = false, saveError = null))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Error should not be displayed
    composeRule.onNodeWithText("Network error").assertDoesNotExist()
  }

  @Test
  fun clickingRoleOption_callsSelectRole() = runTest {
    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Initially no role selected
    var state = viewModel.uiState.value
    assertNull("No role should be selected initially", state.selectedRole)

    // Click on Student
    composeRule.onNodeWithText("Student").performClick()
    advanceUntilIdle()

    state = viewModel.uiState.value
    assertEquals("Student", state.selectedRole)
  }

  @Test
  fun clickingDifferentRoleOptions_updatesSelection() = runTest {
    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Click Student
    composeRule.onNodeWithText("Student").performClick()
    advanceUntilIdle()
    assertEquals("Student", viewModel.uiState.value.selectedRole)

    // Click Professor
    composeRule.onNodeWithText("Professor").performClick()
    advanceUntilIdle()
    assertEquals("Professor", viewModel.uiState.value.selectedRole)

    // Click PhD Candidate
    composeRule.onNodeWithText("PhD Candidate").performClick()
    advanceUntilIdle()
    assertEquals("PhD Candidate", viewModel.uiState.value.selectedRole)

    // Click Administration
    composeRule.onNodeWithText("Administration").performClick()
    advanceUntilIdle()
    assertEquals("Administration", viewModel.uiState.value.selectedRole)
  }

  @Test
  fun continueButton_callsContinueToNext() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Select a role
    composeRule.onNodeWithText("PhD Candidate").performClick()
    advanceUntilIdle()

    // Click Continue
    composeRule.onNodeWithText("Continue").performClick()
    advanceUntilIdle()

    // Verify saveProfile was called
    verify(mockRepository).saveProfile(any())
  }

  @Test
  fun skipButton_callsSkip() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Click Skip
    composeRule.onNodeWithText("Skip").performClick()
    advanceUntilIdle()

    // Verify saveProfile was called
    verify(mockRepository).saveProfile(any())
  }

  @Test
  fun onContinue_callback_isCalled_afterSuccessfulSave() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    var navigationCalled = false
    composeRule.setContent {
      MaterialTheme {
        OnboardingRoleScreen(
            onContinue = { navigationCalled = true }, viewModel = viewModel)
      }
    }

    // Select role and continue
    composeRule.onNodeWithText("Professor").performClick()
    advanceUntilIdle()

    composeRule.onNodeWithText("Continue").performClick()
    advanceUntilIdle()

    // Wait for coroutine to complete
    composeRule.waitForIdle()

    assertTrue("Navigation callback should be called after successful save", navigationCalled)
  }

  @Test
  fun onContinue_callback_isCalled_afterSkip() = runTest {
    whenever(mockRepository.loadProfile()).thenReturn(null)

    var navigationCalled = false
    composeRule.setContent {
      MaterialTheme {
        OnboardingRoleScreen(
            onContinue = { navigationCalled = true }, viewModel = viewModel)
      }
    }

    // Click Skip
    composeRule.onNodeWithText("Skip").performClick()
    advanceUntilIdle()

    // Wait for coroutine to complete
    composeRule.waitForIdle()

    assertTrue("Navigation callback should be called after skip", navigationCalled)
  }

  @Test
  fun continueButton_showsSavingText_whenSaving() {
    setUiState(OnboardingRoleUiState(selectedRole = "Student", isSaving = true))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Saving...").assertIsDisplayed()
    composeRule.onNodeWithText("Continue").assertDoesNotExist()
  }

  @Test
  fun skipButton_showsSavingText_whenSaving() {
    setUiState(OnboardingRoleUiState(selectedRole = null, isSaving = true))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    composeRule.onNodeWithText("Saving...").assertIsDisplayed()
    composeRule.onNodeWithText("Skip").assertDoesNotExist()
  }

  @Test
  fun errorMessage_isDisplayed_whenSaveFails() = runTest {
    val errorMessage = "Failed to save role. Please try again."
    whenever(mockRepository.loadProfile()).thenReturn(null)
    whenever(mockRepository.saveProfile(any())).thenThrow(RuntimeException("Network error"))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // Select role and try to continue
    composeRule.onNodeWithText("Administration").performClick()
    advanceUntilIdle()

    composeRule.onNodeWithText("Continue").performClick()
    advanceUntilIdle()

    composeRule.waitForIdle()

    // Error should be displayed
    composeRule.onNodeWithText(hasText("Network error") or hasText("Failed to save")).assertIsDisplayed()
  }

  @Test
  fun selectedRoleCard_isHighlighted() {
    setUiState(OnboardingRoleUiState(selectedRole = "Professor", isSaving = false))

    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    // The selected card should be visible (we can't easily test visual highlighting,
    // but we can verify the role text is displayed)
    composeRule.onNodeWithText("Professor").assertIsDisplayed()
    composeRule.onNodeWithText("Continue").assertIsEnabled()
  }

  @Test
  fun allRoleOptions_areClickable() = runTest {
    composeRule.setContent {
      MaterialTheme { OnboardingRoleScreen(onContinue = {}, viewModel = viewModel) }
    }

    val roles = listOf("Student", "PhD Candidate", "Professor", "Administration")

    for (role in roles) {
      composeRule.onNodeWithText(role).performClick()
      advanceUntilIdle()

      assertEquals("Role $role should be selected after click", role, viewModel.uiState.value.selectedRole)
    }
  }
}

