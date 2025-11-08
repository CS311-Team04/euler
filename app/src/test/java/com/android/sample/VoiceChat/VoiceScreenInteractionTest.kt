package com.android.sample.VoiceChat

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
  fun voiceScreen_withGrantedPermission_renders() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun voiceScreen_withoutPermission_renders() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).denyPermissions(Manifest.permission.RECORD_AUDIO)

    composeTestRule.setContent { VoiceScreen(onClose = {}) }
    composeTestRule.onRoot().assertIsDisplayed()
  }

  @Test
  fun closeButton_invokesCallback() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

    var closed = false
    composeTestRule.setContent { VoiceScreen(onClose = { closed = true }) }

    composeTestRule.onNodeWithContentDescription("Close voice screen").performClick()

    org.junit.Assert.assertTrue(closed)
  }
}
