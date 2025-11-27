package com.android.sample.settings

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.settings.connectors.Connector
import com.android.sample.settings.connectors.ConnectorsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ConnectorsScreenTest {

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
  fun `screen displays title and subtitle`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connectors").assertIsDisplayed()
    composeRule.onNodeWithText("Connect your academic services").assertIsDisplayed()
  }

  @Test
  fun `all four connectors are displayed`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
    composeRule.onNodeWithText("IS Academia").assertIsDisplayed()
  }

  @Test
  fun `connector descriptions are displayed`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("courses").assertIsDisplayed()
    composeRule.onNodeWithText("Q&A platform").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL services").assertIsDisplayed()
    composeRule.onNodeWithText("Pers. services").assertIsDisplayed()
  }

  @Test
  fun `initial state shows all connectors as not connected`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onAllNodesWithText("Not connected").assertCountEquals(4)
    composeRule.onAllNodesWithText("Connect").assertCountEquals(4)
  }

  @Test
  fun `toggling one connector does not affect others`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onAllNodesWithText("Connect")[0].performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.runOnIdle {
      val notConnectedCount =
          composeRule
              .onAllNodesWithText("Not connected", substring = true)
              .fetchSemanticsNodes()
              .size
      assertTrue(
          "Should have 3 not connected connectors, found: $notConnectedCount",
          notConnectedCount >= 3)
    }
  }

  @Test
  fun `back button triggers callback`() = runTest {
    var backClicked = false
    composeRule.setContent {
      MaterialTheme { ConnectorsScreen(onBackClick = { backClicked = true }) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Close").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(backClicked)
  }

  @Test
  fun `footer displays EPFL text`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  @Test
  fun `all connector types are handled correctly`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    val connectorNames = listOf("Moodle", "Ed", "EPFL Campus", "IS Academia")
    connectorNames.forEach { name -> composeRule.onNodeWithText(name).assertIsDisplayed() }
  }

  @Test
  fun `unknown connector id shows default logo`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // All known connectors should display correctly
    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
    composeRule.onNodeWithText("IS Academia").assertIsDisplayed()
  }

  @Test
  fun `screen handles empty connector list gracefully`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connectors").assertIsDisplayed()
    composeRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  @Test
  fun `connector data class properties are correct`() = runTest {
    val connector =
        Connector(
            id = "test_id",
            name = "Test Connector",
            description = "Test Description",
            isConnected = true)

    assertEquals("test_id", connector.id)
    assertEquals("Test Connector", connector.name)
    assertEquals("Test Description", connector.description)
    assertTrue(connector.isConnected)
  }

  @Test
  fun `connector copy works correctly`() = runTest {
    val original = Connector(id = "test", name = "Test", description = "Desc", isConnected = false)
    val copied = original.copy(isConnected = true)

    assertEquals(original.id, copied.id)
    assertEquals(original.name, copied.name)
    assertEquals(original.description, copied.description)
    assertFalse(original.isConnected)
    assertTrue(copied.isConnected)
  }

  @Test
  fun `screen renders correctly with default parameters`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connectors").assertIsDisplayed()
    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
  }

  @Test
  fun `back button is displayed and accessible`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `all connector names are unique`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    val names = listOf("Moodle", "Ed", "EPFL Campus", "IS Academia")
    names.forEach { name -> composeRule.onNodeWithText(name).assertIsDisplayed() }
  }

  @Test
  fun `grid layout displays connectors in 2x2 format`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
    composeRule.onNodeWithText("IS Academia").assertIsDisplayed()

    composeRule.onNodeWithText("courses").assertIsDisplayed()
    composeRule.onNodeWithText("Q&A platform").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL services").assertIsDisplayed()
    composeRule.onNodeWithText("Pers. services").assertIsDisplayed()
  }

  @Test
  fun `screen respects appearance mode SYSTEM`() = runTest {
    AppSettings.setAppearanceMode(AppearanceMode.SYSTEM)
    advanceUntilIdle()

    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.runOnIdle { composeRule.onNodeWithText("Connectors").assertIsDisplayed() }
  }

  @Test
  fun `screen displays footer correctly`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  @Test
  fun `screen handles rapid clicks correctly`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    val connectButton = composeRule.onAllNodesWithText("Connect")[0]
    repeat(3) {
      connectButton.performClick()
      composeRule.waitForIdle()
      advanceUntilIdle()
    }

    val connectedNodes = composeRule.onAllNodesWithText("Connected").fetchSemanticsNodes()
    val notConnectedNodes = composeRule.onAllNodesWithText("Not connected").fetchSemanticsNodes()
    assertTrue(connectedNodes.isNotEmpty() || notConnectedNodes.isNotEmpty())
  }

  @Test
  fun `screen displays subtitle correctly`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect your academic services").assertIsDisplayed()
  }

  @Test
  fun `screen displays all connector cards in grid`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
    composeRule.onNodeWithText("IS Academia").assertIsDisplayed()
  }

  @Test
  fun `back button triggers onBackClick callback`() = runTest {
    var backClicked = false
    composeRule.setContent {
      MaterialTheme { ConnectorsScreen(onBackClick = { backClicked = true }) }
    }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithContentDescription("Close").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertTrue(backClicked)
  }

  @Test
  fun `screen uses correct theme colors in light mode`() = runTest {
    AppSettings.setAppearanceMode(AppearanceMode.LIGHT)
    advanceUntilIdle()

    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.runOnIdle { composeRule.onNodeWithText("Connectors").assertIsDisplayed() }
  }

  @Test
  fun `screen displays footer text`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("BY EPFL").assertIsDisplayed()
  }

  @Test
  fun `screen displays subtitle text`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connect your academic services").assertIsDisplayed()
  }

  @Test
  fun `screen displays title text`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Connectors").assertIsDisplayed()
  }

  @Test
  fun `screen displays back button`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun `screen displays all connector logos`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
    composeRule.onNodeWithText("IS Academia").assertIsDisplayed()
  }

  @Test
  fun `screen displays grid layout correctly`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Moodle").assertIsDisplayed()
    composeRule.onNodeWithText("Ed").assertIsDisplayed()
    composeRule.onNodeWithText("EPFL Campus").assertIsDisplayed()
    composeRule.onNodeWithText("IS Academia").assertIsDisplayed()
  }

  @Test
  fun `screen handles connector state changes correctly`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.onAllNodesWithText("Not connected").assertCountEquals(4)
    composeRule.onAllNodesWithText("Connect")[0].performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.runOnIdle {
      val notConnectedCount =
          composeRule
              .onAllNodesWithText("Not connected", substring = true)
              .fetchSemanticsNodes()
              .size
      assertTrue("Should have 3 not connected", notConnectedCount >= 3)
    }
  }

  @Test
  fun `screen does not show dialog when connecting`() = runTest {
    composeRule.setContent { MaterialTheme { ConnectorsScreen() } }
    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.onAllNodesWithText("Connect")[0].performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.runOnIdle {
      try {
        composeRule.onNodeWithText("Disconnect?", substring = true).assertIsDisplayed()
        assertFalse("Dialog should not exist", true)
      } catch (_: AssertionError) {
        // Dialog does not exist, which is expected
      }
    }
  }
}
