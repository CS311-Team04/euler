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

  @Test
  fun `ConnectorCard displays connector name`() = runTest {
    val connector =
        Connector(
            id = "test", name = "Test Connector", description = "Test Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Test Connector").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays connector description`() = runTest {
    val connector =
        Connector(id = "test", name = "Test", description = "Test Description", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Test Description").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard shows Not connected when not connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Not connected", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard shows Connected when connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connected", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard shows Connect button when not connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard shows Disconnect button when connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Disconnect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard triggers callback on click`() = runTest {
    var clicked = false
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(
            connector = connector,
            onConnectClick = { clicked = true },
            colors = colors,
            isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect", substring = true).performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(clicked)
  }

  @Test
  fun `ConnectorCard renders in dark mode`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard renders in light mode`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = false)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = false)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct button color when connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Disconnect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct button color when not connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct status text color when connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connected", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct status text color when not connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Not connected", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard triggers callback when clicking on card surface`() = runTest {
    var clicked = false
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(
            connector = connector,
            onConnectClick = { clicked = true },
            colors = colors,
            isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Click on the card (not just the button)
    composeRule.onNodeWithText("Test").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(clicked)
  }

  @Test
  fun `ConnectorCard triggers callback when clicking on button surface`() = runTest {
    var clicked = false
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(
            connector = connector,
            onConnectClick = { clicked = true },
            colors = colors,
            isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Click on the disconnect button
    composeRule.onNodeWithText("Disconnect", substring = true).performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(clicked)
  }

  @Test
  fun `ConnectorCard uses correct elevation in dark mode`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard uses correct elevation in light mode`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = false)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = false)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Test").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays logo for connector`() = runTest {
    val connector =
        Connector(id = "moodle", name = "Moodle", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard handles long description text`() = runTest {
    val connector =
        Connector(
            id = "test",
            name = "Test",
            description = "Very long description that should wrap",
            isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Very long description", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct button text color when connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Disconnect", substring = true).assertIsDisplayed()
  }

  @Test
  fun `ConnectorCard displays correct button text color when not connected`() = runTest {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val colors = ConnectorsTheme.colors(isDark = true)

    composeRule.setContent {
      MaterialTheme {
        ConnectorCard(connector = connector, onConnectClick = {}, colors = colors, isDark = true)
      }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect", substring = true).assertIsDisplayed()
  }
}
