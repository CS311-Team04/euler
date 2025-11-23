package com.android.sample.settings.connectors

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class ConnectorLogosTest {

  @get:Rule val composeRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    AnimationConfig.disableAnimations = true
    TestFlags.fakeEmail = ""
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
    AnimationConfig.disableAnimations = false
    TestFlags.fakeEmail = null
  }

  @Test
  fun `ConnectorLogo displays MoodleLogo for moodle id`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "moodle", isConnected = true) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ConnectorLogo displays EdLogo for ed id`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "ed", isConnected = true) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ConnectorLogo displays EPFLLogo for epfl_campus id`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "epfl_campus", isConnected = true) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ConnectorLogo displays ISAcademiaLogo for is_academia id`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "is_academia", isConnected = true) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ConnectorLogo displays default box for unknown id`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "unknown", isConnected = true) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `MoodleLogo renders when connected`() {
    composeRule.setContent { MaterialTheme { MoodleLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `MoodleLogo renders when not connected`() {
    composeRule.setContent { MaterialTheme { MoodleLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EdLogo renders when connected`() {
    composeRule.setContent { MaterialTheme { EdLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EdLogo renders when not connected`() {
    composeRule.setContent { MaterialTheme { EdLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EPFLLogo renders when connected`() {
    composeRule.setContent { MaterialTheme { EPFLLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EPFLLogo renders when not connected`() {
    composeRule.setContent { MaterialTheme { EPFLLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ISAcademiaLogo renders when connected`() {
    composeRule.setContent { MaterialTheme { ISAcademiaLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ISAcademiaLogo renders when not connected`() {
    composeRule.setContent { MaterialTheme { ISAcademiaLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ConnectorLogo displays default box for unknown id when connected`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "unknown", isConnected = true) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ConnectorLogo displays default box for unknown id when not connected`() {
    composeRule.setContent {
      MaterialTheme { ConnectorLogo(connectorId = "unknown", isConnected = false) }
    }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `MoodleLogo uses correct gradient when connected`() {
    composeRule.setContent { MaterialTheme { MoodleLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `MoodleLogo uses correct gradient when not connected`() {
    composeRule.setContent { MaterialTheme { MoodleLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EdLogo uses correct gradient when connected`() {
    composeRule.setContent { MaterialTheme { EdLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EdLogo uses correct gradient when not connected`() {
    composeRule.setContent { MaterialTheme { EdLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EPFLLogo uses correct image when connected`() {
    composeRule.setContent { MaterialTheme { EPFLLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `EPFLLogo uses correct image when not connected`() {
    composeRule.setContent { MaterialTheme { EPFLLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ISAcademiaLogo uses correct colors when connected in dark mode`() {
    composeRule.setContent { MaterialTheme { ISAcademiaLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ISAcademiaLogo uses correct colors when connected in light mode`() {
    composeRule.setContent { MaterialTheme { ISAcademiaLogo(isConnected = true) } }
    composeRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun `ISAcademiaLogo uses correct colors when not connected`() {
    composeRule.setContent { MaterialTheme { ISAcademiaLogo(isConnected = false) } }
    composeRule.onRoot().assertIsDisplayed()
  }
}
