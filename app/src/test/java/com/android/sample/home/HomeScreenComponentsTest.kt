package com.android.sample.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeScreenComponentsTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun bubbleSendButton_showsLoader_whenSending() {
    composeRule.setContent { BubbleSendButton(enabled = true, isSending = true, onClick = {}) }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("send_loading", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun bubbleSendButton_showsIcon_whenEnabled() {
    composeRule.setContent { BubbleSendButton(enabled = true, isSending = false, onClick = {}) }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("send_icon", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun bubbleSendButton_showsIcon_whenDisabled() {
    composeRule.setContent { BubbleSendButton(enabled = false, isSending = false, onClick = {}) }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("send_icon", useUnmergedTree = true).assertIsDisplayed()
  }
}

