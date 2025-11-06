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
class VoiceChatSheetTest {

  @get:Rule val composeTestRule = createComposeRule()

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
}
