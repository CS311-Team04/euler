package com.android.sample.epfl

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.sample.settings.AppSettings
import com.android.sample.settings.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EpflCampusConnectorScreenTest {

  @get:Rule val composeRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  private lateinit var mockViewModel: EpflCampusViewModel
  private lateinit var uiStateFlow: MutableStateFlow<EpflCampusUiState>

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    AnimationConfig.disableAnimations = true
    TestFlags.fakeEmail = ""
    AppSettings.setLanguage(Language.EN)

    mockViewModel = mock()
    uiStateFlow = MutableStateFlow(EpflCampusUiState())
    whenever(mockViewModel.uiState).thenReturn(uiStateFlow)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
    AnimationConfig.disableAnimations = false
    TestFlags.fakeEmail = null
  }

  // ===== Screen structure tests =====

  @Test
  fun `screen renders with correct structure`() = runTest {
    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("epfl_screen").assertIsDisplayed()
    composeRule.onNodeWithTag("back_button").assertIsDisplayed()
  }

  @Test
  fun `back button triggers callback`() = runTest {
    var backClicked = false
    composeRule.setContent {
      MaterialTheme {
        EpflCampusConnectorScreen(onBackClick = { backClicked = true }, viewModel = mockViewModel)
      }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("back_button").performClick()
    composeRule.waitForIdle()

    assertTrue(backClicked)
  }

  // ===== Loading state tests =====

  @Test
  fun `shows loading indicator when isLoading is true`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isLoading = true)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
  }

  @Test
  fun `hides loading indicator when isLoading is false`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isLoading = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
  }

  // ===== Connected state tests =====

  @Test
  fun `shows connected card when isConnected is true`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            isConnected = true, weeklySlots = 15, finalExams = 3, lastSync = "2024-01-15")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("connected_card").assertIsDisplayed()
    composeRule.onNodeWithTag("disconnect_button").assertIsDisplayed()
  }

  @Test
  fun `disconnect button triggers viewModel disconnect`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = true, weeklySlots = 10, finalExams = 2)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("disconnect_button").performClick()
    composeRule.waitForIdle()

    verify(mockViewModel).disconnect()
  }

  @Test
  fun `displays weekly slots and exams count when connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = true, weeklySlots = 20, finalExams = 6)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    // The numbers should appear in the connected card
    composeRule.onNodeWithText("20", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("6", substring = true).assertIsDisplayed()
  }

  // ===== Not connected state tests =====

  @Test
  fun `shows instructions card when not connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false, isLoading = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("instructions_card").assertIsDisplayed()
    composeRule.onNodeWithTag("open_campus_button").assertIsDisplayed()
    composeRule.onNodeWithTag("url_input").assertIsDisplayed()
    composeRule.onNodeWithTag("connect_button").assertIsDisplayed()
  }

  @Test
  fun `connect button is disabled when URL is invalid`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false, isValidUrl = false, icsUrlInput = "")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("connect_button").assertIsNotEnabled()
  }

  @Test
  fun `connect button is enabled when URL is valid`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            isConnected = false, isValidUrl = true, icsUrlInput = "https://example.com/cal.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("connect_button").assertIsEnabled()
  }

  @Test
  fun `connect button is disabled while syncing`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            isConnected = false,
            isValidUrl = true,
            isSyncing = true,
            icsUrlInput = "https://test.com")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("connect_button").assertIsNotEnabled()
  }

  // ===== Error and Success snackbar tests =====

  @Test
  fun `shows error snackbar when error is set`() = runTest {
    uiStateFlow.value = EpflCampusUiState(error = "Something went wrong")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("error_snackbar").assertIsDisplayed()
    composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
  }

  @Test
  fun `shows success snackbar when successMessage is set`() = runTest {
    uiStateFlow.value = EpflCampusUiState(successMessage = "Schedule synced!")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("success_snackbar").assertIsDisplayed()
    composeRule.onNodeWithText("Schedule synced!").assertIsDisplayed()
  }

  @Test
  fun `error snackbar OK button triggers clearError`() = runTest {
    uiStateFlow.value = EpflCampusUiState(error = "Error message")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("OK").performClick()
    composeRule.waitForIdle()

    verify(mockViewModel).clearError()
  }

  @Test
  fun `success snackbar OK button triggers clearSuccessMessage`() = runTest {
    uiStateFlow.value = EpflCampusUiState(successMessage = "Success!")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("OK").performClick()
    composeRule.waitForIdle()

    verify(mockViewModel).clearSuccessMessage()
  }

  // ===== Clipboard suggestion tests =====

  @Test
  fun `shows clipboard banner when suggestion is available`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            showClipboardSuggestion = true,
            detectedClipboardUrl = "https://campus.epfl.ch/calendar.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("clipboard_banner").assertIsDisplayed()
  }

  @Test
  fun `accept clipboard URL triggers viewModel`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            showClipboardSuggestion = true,
            detectedClipboardUrl = "https://campus.epfl.ch/calendar.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    // Click "Use this URL" button
    composeRule.onNodeWithText("Use this URL").performClick()
    composeRule.waitForIdle()

    verify(mockViewModel).acceptClipboardUrl()
  }

  @Test
  fun `dismiss clipboard suggestion triggers viewModel`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            showClipboardSuggestion = true,
            detectedClipboardUrl = "https://campus.epfl.ch/calendar.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    // Click "Not now" button
    composeRule.onNodeWithText("Not now").performClick()
    composeRule.waitForIdle()

    verify(mockViewModel).dismissClipboardSuggestion()
  }

  // ===== UI state transitions =====

  @Test
  fun `UI updates correctly when state changes from loading to connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isLoading = true)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("loading_indicator").assertIsDisplayed()

    // Update state to connected
    uiStateFlow.value = EpflCampusUiState(isConnected = true, weeklySlots = 5, finalExams = 2)
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("loading_indicator").assertDoesNotExist()
    composeRule.onNodeWithTag("connected_card").assertIsDisplayed()
  }

  @Test
  fun `UI updates correctly when state changes from not connected to connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("instructions_card").assertIsDisplayed()

    // Update state to connected
    uiStateFlow.value = EpflCampusUiState(isConnected = true, weeklySlots = 10, finalExams = 3)
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("instructions_card").assertDoesNotExist()
    composeRule.onNodeWithTag("connected_card").assertIsDisplayed()
  }

  // ===== Input validation tests =====

  @Test
  fun `URL input updates viewModel`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("url_input").performTextInput("https://test.com")
    composeRule.waitForIdle()

    verify(mockViewModel, atLeastOnce()).updateIcsUrl(any())
  }

  // ===== Edge cases =====

  @Test
  fun `handles empty state correctly`() = runTest {
    uiStateFlow.value = EpflCampusUiState()

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    // Default state should show instructions (not connected, not loading)
    composeRule.onNodeWithTag("epfl_screen").assertIsDisplayed()
    composeRule.onNodeWithTag("instructions_card").assertIsDisplayed()
  }

  @Test
  fun `no crash when both error and success message are null`() = runTest {
    uiStateFlow.value = EpflCampusUiState(error = null, successMessage = null)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("error_snackbar").assertDoesNotExist()
    composeRule.onNodeWithTag("success_snackbar").assertDoesNotExist()
  }
}
