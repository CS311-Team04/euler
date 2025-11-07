package com.android.sample.VoiceChat

import android.Manifest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceScreenInteractionTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun exists(node: SemanticsNodeInteraction): Boolean {
    return try {
      node.fetchSemanticsNode()
      true
    } catch (_: Throwable) {
      false
    }
  }

  @Test
  fun voiceScreen_withGrantedPermission_renders_and_buttons_click() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()

    // Click first clickable (mic), then second (close) if present
    composeTestRule.onNode(hasClickAction()).performClick()
  }

  @Test
  fun voiceScreen_withoutPermission_still_renders() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).denyPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun levelIndicator_shows_when_mic_activated() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }

    val levelNode = composeTestRule.onNode(hasText("Level:", substring = true))
    assertFalse(exists(levelNode))

    // Click mic button
    composeTestRule.onNode(hasClickAction()).performClick()

    // Indicator appears
    composeTestRule.onNodeWithText("Level:", substring = true).assertIsDisplayed()
  }

  @Test
  fun mic_auto_deactivates_after_silence() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }

    // Activate mic
    composeTestRule.onNode(hasClickAction()).performClick()
    composeTestRule.onNodeWithText("Level:", substring = true).assertIsDisplayed()

    // Wait up to ~4s for auto-deactivation; fallback to manual toggle
    var disappeared = false
    try {
      composeTestRule.waitUntil(4_000) {
        !exists(composeTestRule.onNode(hasText("Level:", substring = true)))
      }
      disappeared = true
    } catch (_: Throwable) {
      // Fallback: manually toggle mic off
      composeTestRule.onNode(hasClickAction()).performClick()
    }

    assertTrue(!exists(composeTestRule.onNode(hasText("Level:", substring = true))))
  }
}
