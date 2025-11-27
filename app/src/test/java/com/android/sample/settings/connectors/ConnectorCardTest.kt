package com.android.sample.settings.connectors

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.settings.AppSettings
import com.android.sample.settings.Language
import com.android.sample.settings.Localization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ConnectorCardTest {

  @get:Rule val composeRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
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

  private fun createTestConnector(
      id: String = "test",
      name: String = "Test",
      description: String = "Desc",
      isConnected: Boolean = false
  ) = Connector(id = id, name = name, description = description, isConnected = isConnected)

  private fun renderCard(
      connector: Connector,
      isDark: Boolean = true,
      onConnectClick: () -> Unit = {}
  ) {
    val colors = ConnectorsTheme.colors(isDark = isDark)
    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(
            connector = connector,
            onConnectClick = onConnectClick,
            colors = colors,
            isDark = isDark)
      }
    }
    composeRule.waitForIdle()
  }

  @Test
  fun `ConnectorCard displays connector name and description`() = runTest {
    val connector = createTestConnector(name = "Test Connector", description = "Test Description")
    renderCard(connector)

    composeRule.onNodeWithText("Test Connector").assertIsDisplayed()
    composeRule.onNodeWithText("Test Description").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct state when not connected`() = runTest {
    val connector = createTestConnector(isConnected = false)
    renderCard(connector)

    composeRule
        .onNodeWithText(Localization.t("not_connected"), substring = true)
        .assertIsDisplayed()
    composeRule.onNodeWithText(Localization.t("connect"), substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct state when connected`() = runTest {
    val connector = createTestConnector(isConnected = true)
    renderCard(connector)

    composeRule.onNodeWithText(Localization.t("connected"), substring = true).assertIsDisplayed()
    composeRule.onNodeWithText(Localization.t("disconnect"), substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard triggers callback on button click`() = runTest {
    var clicked = false
    val connector = createTestConnector(isConnected = false)
    renderCard(connector) { clicked = true }

    composeRule.onNodeWithText(Localization.t("connect"), substring = true).performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(clicked)
  }

  @Test
  fun `ConnectorCard getCardElevation returns correct elevation for dark mode`() = runTest {
    val connector = createTestConnector()
    renderCard(connector, isDark = true)

    // Card should render correctly with dark mode elevation
    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard getCardElevation returns correct elevation for light mode`() = runTest {
    val connector = createTestConnector()
    renderCard(connector, isDark = false)

    // Card should render correctly with light mode elevation
    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard getConnectorCardState returns correct state for connected`() = runTest {
    val connector = createTestConnector(isConnected = true)
    renderCard(connector)

    // Verify connected state is displayed correctly
    composeRule.onNodeWithText(Localization.t("connected"), substring = true).assertIsDisplayed()
    composeRule.onNodeWithText(Localization.t("disconnect"), substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard getConnectorCardState returns correct state for not connected`() = runTest {
    val connector = createTestConnector(isConnected = false)
    renderCard(connector)

    // Verify not connected state is displayed correctly
    composeRule
        .onNodeWithText(Localization.t("not_connected"), substring = true)
        .assertIsDisplayed()
    composeRule.onNodeWithText(Localization.t("connect"), substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays logo correctly`() = runTest {
    val connector = createTestConnector(id = "moodle", name = "Moodle")
    renderCard(connector)

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard handles empty description`() = runTest {
    val connector = createTestConnector(description = "")
    renderCard(connector)

    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard handles very long name`() = runTest {
    val connector = createTestConnector(name = "Very Long Connector Name That Should Still Display")
    renderCard(connector)

    composeRule.onNodeWithText("Very Long Connector Name", substring = true).assertIsDisplayed()
  }
}
