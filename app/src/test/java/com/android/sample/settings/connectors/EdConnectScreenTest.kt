package com.android.sample.settings.connectors

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.android.sample.settings.AppSettings
import com.android.sample.settings.Language
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EdConnectScreenTest {

  @get:Rule val composeRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:test")
              .setProjectId("test-project")
              .setApiKey("fake-api-key")
              .build())
    }
    Dispatchers.setMain(testDispatcher)
    AnimationConfig.disableAnimations = true
    TestFlags.fakeEmail = ""
    AppSettings.setLanguage(Language.EN)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
    AnimationConfig.disableAnimations = false
    TestFlags.fakeEmail = null
  }

  private fun createMockViewModel(uiState: ConnectorsUiState): ConnectorsViewModel {
    val viewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val stateFlow = MutableStateFlow(uiState)
    every { viewModel.uiState } returns stateFlow
    return viewModel
  }

  private fun defaultColors(isDark: Boolean = false) =
      ConnectorsColors(
          background =
              if (isDark) androidx.compose.ui.graphics.Color.Black
              else androidx.compose.ui.graphics.Color.White,
          textPrimary =
              if (isDark) androidx.compose.ui.graphics.Color.White
              else androidx.compose.ui.graphics.Color.Black,
          textSecondary =
              if (isDark) androidx.compose.ui.graphics.Color.LightGray
              else androidx.compose.ui.graphics.Color.Gray,
          textSecondary50 =
              if (isDark) androidx.compose.ui.graphics.Color.Gray
              else androidx.compose.ui.graphics.Color.LightGray,
          glassBackground =
              if (isDark) androidx.compose.ui.graphics.Color.DarkGray
              else androidx.compose.ui.graphics.Color.White,
          glassBorder = androidx.compose.ui.graphics.Color.Gray,
          shadowSpot = androidx.compose.ui.graphics.Color.Black,
          shadowAmbient = androidx.compose.ui.graphics.Color.Black,
          onPrimaryColor = androidx.compose.ui.graphics.Color.White,
          connectedGreen = androidx.compose.ui.graphics.Color.Green,
          connectedGreenBackground = androidx.compose.ui.graphics.Color.Green,
          accentRed = androidx.compose.ui.graphics.Color.Red)

  @Test
  fun `screen displays all main elements`() = runTest {
    val uiState =
        ConnectorsUiState(connectors = listOf(Connector(ED_CONNECTOR_ID, "Ed", "Q&A", false)))
    val viewModel = createMockViewModel(uiState)
    composeRule.setContent { MaterialTheme { EdConnectScreen(viewModel = viewModel) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("Open ED Token Page").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `screen shows loading state`() = runTest {
    val uiState =
        ConnectorsUiState(
            connectors = listOf(Connector(ED_CONNECTOR_ID, "Ed", "Q&A", false)),
            isEdConnecting = true)
    val viewModel = createMockViewModel(uiState)
    composeRule.setContent { MaterialTheme { EdConnectScreen(viewModel = viewModel) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Open ED Token Page").assertIsDisplayed()
  }

  @Test
  fun `screen shows error message`() = runTest {
    val uiState =
        ConnectorsUiState(
            connectors = listOf(Connector(ED_CONNECTOR_ID, "Ed", "Q&A", false)),
            edConnectError = "Connection failed")
    val viewModel = createMockViewModel(uiState)
    composeRule.setContent { MaterialTheme { EdConnectScreen(viewModel = viewModel) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connection failed").assertIsDisplayed()
  }

  @Test
  fun `screen shows clipboard banner with short token`() = runTest {
    val uiState =
        ConnectorsUiState(
            connectors = listOf(Connector(ED_CONNECTOR_ID, "Ed", "Q&A", false)),
            showEdClipboardSuggestion = true,
            detectedEdToken = "short_token_12345")
    val viewModel = createMockViewModel(uiState)
    composeRule.setContent { MaterialTheme { EdConnectScreen(viewModel = viewModel) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("ED token detected!", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("short_token_12345").assertIsDisplayed()
  }

  @Test
  fun `screen shows clipboard banner with long token preview`() = runTest {
    val longToken = "a".repeat(50)
    val uiState =
        ConnectorsUiState(
            connectors = listOf(Connector(ED_CONNECTOR_ID, "Ed", "Q&A", false)),
            showEdClipboardSuggestion = true,
            detectedEdToken = longToken)
    val viewModel = createMockViewModel(uiState)
    composeRule.setContent { MaterialTheme { EdConnectScreen(viewModel = viewModel) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("ED token detected!", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("...", substring = true).assertIsDisplayed()
  }

  @Test
  fun `top bar displays correctly`() = runTest {
    val colors = defaultColors()
    composeRule.setContent { MaterialTheme { EdConnectTopBar({}, colors) } }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Connect to ED", substring = true).assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `info card displays correctly`() = runTest {
    val colors = defaultColors()
    composeRule.setContent { MaterialTheme { EdConnectorInfoCard(colors) } }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
  }

  @Test
  fun `how to connect card displays correctly`() = runTest {
    val colors = defaultColors()
    composeRule.setContent { MaterialTheme { HowToConnectCard(colors) } }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("How to connect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `clipboard banner displays with buttons`() = runTest {
    val colors = defaultColors()
    composeRule.setContent {
      MaterialTheme { EdClipboardSuggestionBanner("test_token_12345", colors, {}, {}) }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("ED token detected!", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("Not now", substring = true).assertIsDisplayed()
    composeRule.onNodeWithText("Use this token", substring = true).assertIsDisplayed()
  }

  @Test
  fun `clipboard banner handles long token`() = runTest {
    val colors = defaultColors()
    val longToken = "a".repeat(50)
    composeRule.setContent {
      MaterialTheme { EdClipboardSuggestionBanner(longToken, colors, {}, {}) }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("...", substring = true).assertIsDisplayed()
  }

  @Test
  fun `back button triggers callback`() = runTest {
    var backClicked = false
    val colors = defaultColors()
    composeRule.setContent { MaterialTheme { EdConnectTopBar({ backClicked = true }, colors) } }
    composeRule.waitForIdle()
    composeRule.onNodeWithContentDescription("Close").performClick()
    assert(backClicked)
  }
}
