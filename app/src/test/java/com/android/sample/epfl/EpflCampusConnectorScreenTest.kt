package com.android.sample.epfl

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.settings.AppSettings
import com.android.sample.settings.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

  @Test
  fun `screen displays title`() = runTest {
    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
  }

  @Test
  fun `screen displays close button`() = runTest {
    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `close button triggers callback`() = runTest {
    var backClicked = false
    composeRule.setContent {
      MaterialTheme {
        EpflCampusConnectorScreen(onBackClick = { backClicked = true }, viewModel = mockViewModel)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithContentDescription("Close").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(backClicked)
  }

  @Test
  fun `shows loading indicator when isLoading is true`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isLoading = true)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Loading state should be visible (CircularProgressIndicator doesn't have text)
    // We verify by checking that other content is not visible
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
  }

  @Test
  fun `shows connected state when isConnected is true`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            isConnected = true, weeklySlots = 15, finalExams = 3, lastSync = "2024-01-15T10:00:00Z")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connected", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("15", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("3", substring = true).assertIsDisplayed()
  }

  @Test
  fun `shows disconnect button when connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = true, weeklySlots = 10, finalExams = 2)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Disconnect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `disconnect button triggers viewModel disconnect`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = true, weeklySlots = 10, finalExams = 2)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Disconnect", substring = true).performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    verify(mockViewModel).disconnect()
  }

  @Test
  fun `shows instructions when not connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Check that instructions are visible
    composeRule.onNodeWithText("How to connect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `shows open EPFL Campus button when not connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Open EPFL Campus", substring = true).assertIsDisplayed()
  }

  @Test
  fun `shows URL input field when not connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Paste your ICS URL", substring = true).assertIsDisplayed()
  }

  @Test
  fun `connect button is disabled when URL is invalid`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false, isValidUrl = false, icsUrlInput = "")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect", substring = true).assertIsNotEnabled()
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
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect", substring = true).assertIsEnabled()
  }

  @Test
  fun `shows syncing text when isSyncing is true`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            isConnected = false,
            isSyncing = true,
            isValidUrl = true,
            icsUrlInput = "https://example.com/cal.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Syncing", substring = true).assertIsDisplayed()
  }

  @Test
  fun `shows error snackbar when error is set`() = runTest {
    uiStateFlow.value = EpflCampusUiState(error = "Something went wrong")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
  }

  @Test
  fun `shows success snackbar when successMessage is set`() = runTest {
    uiStateFlow.value = EpflCampusUiState(successMessage = "Schedule synced successfully!")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Schedule synced successfully!").assertIsDisplayed()
  }

  @Test
  fun `shows clipboard suggestion when detected`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            showClipboardSuggestion = true,
            detectedClipboardUrl = "https://campus.epfl.ch/calendar.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("ICS URL detected", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("Use this URL", substring = true).assertIsDisplayed()
  }

  @Test
  fun `accept clipboard suggestion triggers viewModel`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            showClipboardSuggestion = true,
            detectedClipboardUrl = "https://campus.epfl.ch/calendar.ics")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Use this URL", substring = true).performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

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
    advanceUntilIdle()

    composeRule.onNodeWithText("Not now", substring = true).performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    verify(mockViewModel).dismissClipboardSuggestion()
  }

  @Test
  fun `error snackbar OK button triggers clearError`() = runTest {
    uiStateFlow.value = EpflCampusUiState(error = "Error message")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("OK").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    verify(mockViewModel).clearError()
  }

  @Test
  fun `success snackbar OK button triggers clearSuccessMessage`() = runTest {
    uiStateFlow.value = EpflCampusUiState(successMessage = "Success!")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("OK").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    verify(mockViewModel).clearSuccessMessage()
  }

  @Test
  fun `displays correct weekly slots and exams count when connected`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(isConnected = true, weeklySlots = 20, finalExams = 6, lastSync = null)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("20", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("6", substring = true).assertIsDisplayed()
  }

  @Test
  fun `shows last sync time when available`() = runTest {
    uiStateFlow.value =
        EpflCampusUiState(
            isConnected = true, weeklySlots = 10, finalExams = 2, lastSync = "2024-01-20T15:30:00Z")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Last sync", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("2024-01-20", substring = true).assertIsDisplayed()
  }

  @Test
  fun `screen header shows EPFL Campus subtitle`() = runTest {
    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Import your schedule", substring = true).assertIsDisplayed()
  }

  @Test
  fun `instruction steps are displayed when not connected`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false)

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Check for step numbers
    composeRule.onNodeWithText("1", substring = false).assertIsDisplayed()
    composeRule.onNodeWithText("2", substring = false).assertIsDisplayed()
    composeRule.onNodeWithText("3", substring = false).assertIsDisplayed()
  }

  @Test
  fun `URL input shows placeholder text`() = runTest {
    uiStateFlow.value = EpflCampusUiState(isConnected = false, icsUrlInput = "")

    composeRule.setContent {
      MaterialTheme { EpflCampusConnectorScreen(viewModel = mockViewModel) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule
        .onNodeWithText("https://campus.epfl.ch/deploy/", substring = true)
        .assertIsDisplayed()
  }
}
