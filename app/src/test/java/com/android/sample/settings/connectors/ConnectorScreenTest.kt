package com.android.sample.settings.connectors

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.settings.Localization
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConnectorScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun connectorsScreen_displaysTitle() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val uiState = ConnectorsUiState(connectors = emptyList())
    val stateFlow = MutableStateFlow(uiState)

    // Mock the uiState flow
    io.mockk.every { mockViewModel.uiState } returns stateFlow

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule.onNodeWithText(Localization.t("connectors")).assertIsDisplayed()
  }

  @Test
  fun connectorsScreen_displaysConnectors() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val connectors =
        listOf(
            Connector(id = "moodle", name = "Moodle", description = "courses", isConnected = false),
            Connector(id = "ed", name = "Ed", description = "Q&A platform", isConnected = false))
    val uiState = ConnectorsUiState(connectors = connectors)
    val stateFlow = MutableStateFlow(uiState)

    io.mockk.every { mockViewModel.uiState } returns stateFlow

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
  }

  @Test
  fun connectorsScreen_showsMoodleDialog_whenMoodleConnectorClicked() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val connectors =
        listOf(
            Connector(id = "moodle", name = "Moodle", description = "courses", isConnected = false))
    val uiState = ConnectorsUiState(connectors = connectors, isMoodleConnectDialogOpen = true)
    val stateFlow = MutableStateFlow(uiState)

    io.mockk.every { mockViewModel.uiState } returns stateFlow
    io.mockk.every { mockViewModel.connectConnector(any()) } returns Unit

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithText(Localization.t("settings_connectors_moodle_title"))
        .assertIsDisplayed()
  }

  @Test
  fun connectorsScreen_showsEdDialog_whenEdConnectorClicked() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val connectors =
        listOf(Connector(id = "ed", name = "Ed", description = "Q&A platform", isConnected = false))
    val uiState = ConnectorsUiState(connectors = connectors, isEdConnectDialogOpen = true)
    val stateFlow = MutableStateFlow(uiState)

    io.mockk.every { mockViewModel.uiState } returns stateFlow
    io.mockk.every { mockViewModel.connectConnector(any()) } returns Unit

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule.onNodeWithText(Localization.t("settings_connectors_ed_title")).assertIsDisplayed()
  }

  @Test
  fun connectorsScreen_showsDisconnectDialog_whenConnectedConnectorClicked() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val connector =
        Connector(id = "moodle", name = "Moodle", description = "courses", isConnected = true)
    val uiState =
        ConnectorsUiState(connectors = listOf(connector), pendingConnectorForDisconnect = connector)
    val stateFlow = MutableStateFlow(uiState)

    io.mockk.every { mockViewModel.uiState } returns stateFlow
    io.mockk.every { mockViewModel.showDisconnectConfirmation(any()) } returns Unit

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule.onNodeWithText(Localization.t("disconnect_confirm_title")).assertIsDisplayed()
  }

  @Test
  fun connectorsScreen_displaysErrorMessages() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val uiState =
        ConnectorsUiState(
            connectors = emptyList(),
            edError = "ED connection failed",
            moodleError = "Moodle connection failed")
    val stateFlow = MutableStateFlow(uiState)

    io.mockk.every { mockViewModel.uiState } returns stateFlow

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("ED connection failed").assertIsDisplayed()
    composeRule.onNodeWithText("Moodle connection failed").assertIsDisplayed()
  }

  @Test
  fun connectorsScreen_hidesDialogs_whenNotOpen() {
    val mockViewModel = mockk<ConnectorsViewModel>(relaxed = true)
    val uiState =
        ConnectorsUiState(
            connectors =
                listOf(
                    Connector(
                        id = "moodle",
                        name = "Moodle",
                        description = "courses",
                        isConnected = false)),
            isMoodleConnectDialogOpen = false,
            isEdConnectDialogOpen = false)
    val stateFlow = MutableStateFlow(uiState)

    io.mockk.every { mockViewModel.uiState } returns stateFlow

    composeRule.setContent { MaterialTheme { ConnectorsScreen(viewModel = mockViewModel) } }

    composeRule.waitForIdle()
    composeRule
        .onNodeWithText(Localization.t("settings_connectors_moodle_title"))
        .assertIsNotDisplayed()
    composeRule
        .onNodeWithText(Localization.t("settings_connectors_ed_title"))
        .assertIsNotDisplayed()
  }
}
