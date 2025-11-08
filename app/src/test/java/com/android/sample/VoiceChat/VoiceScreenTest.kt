package com.android.sample.VoiceChat

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun voiceScreen_displays() {
    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_withModifier_displays() {
    composeTestRule.setContent { VoiceScreen(onClose = {}, modifier = Modifier.size(100.dp)) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_rendersWithoutCrash() {
    // Test that the screen can be composed without crashing
    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    // If we get here without exception, the test passes
    assertTrue(true)
  }

  @Test
  fun voiceOverlay_displays() {
    composeTestRule.setContent { VoiceOverlay(onDismiss = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceOverlay_withModifier_displays() {
    composeTestRule.setContent { VoiceOverlay(onDismiss = {}, modifier = Modifier.size(100.dp)) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreenPreviewContent_renders() {
    composeTestRule.setContent { VoiceScreenPreviewContent(level = 0.4f) }
    composeTestRule.onRoot().assertIsDisplayed()
  }
}
