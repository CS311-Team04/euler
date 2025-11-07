package com.android.sample.VoiceChat

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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

  @Test
  fun voiceScreen_withGrantedPermission_renders_and_buttons_click() {
    // Grant RECORD_AUDIO permission in Robolectric environment
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()

    // Click the two clickable buttons (mic and close)
    val clickables = composeTestRule.onAllNodes(hasClickAction())
    // Defensive: click first two if present
    if (clickables.fetchSemanticsNodes().size >= 2) {
      clickables[0].performClick()
      clickables[1].performClick()
    }
  }

  @Test
  fun voiceScreen_withoutPermission_still_renders() {
    // Ensure permission is NOT granted
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

    // Initially no indicator
    assertTrue(
        composeTestRule
            .onAllNodes(hasText("Level:", substring = true))
            .fetchSemanticsNodes()
            .isEmpty())

    // Click mic button
    val clickables = composeTestRule.onAllNodes(hasClickAction())
    if (clickables.fetchSemanticsNodes().isNotEmpty()) {
      clickables[0].performClick()
    }

    // Indicator appears
    composeTestRule.onNodeWithText("Level:", substring = true).assertIsDisplayed()
  }

  @Test
  fun mic_auto_deactivates_after_silence() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }

    // Activate mic
    val clickables = composeTestRule.onAllNodes(hasClickAction())
    if (clickables.fetchSemanticsNodes().isNotEmpty()) {
      clickables[0].performClick()
    }
    composeTestRule.onNodeWithText("Level:", substring = true).assertIsDisplayed()

    // Wait > 2.5s to trigger auto-off
    Thread.sleep(3000)
    composeTestRule.waitForIdle()

    // Indicator should disappear
    assertTrue(
        composeTestRule
            .onAllNodes(hasText("Level:", substring = true))
            .fetchSemanticsNodes()
            .isEmpty())
  }
}
