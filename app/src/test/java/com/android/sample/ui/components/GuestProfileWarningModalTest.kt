package com.android.sample.ui.components

import AnimationConfig
import TestFlags
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GuestProfileWarningModalTest {

  @get:Rule val composeRule = createComposeRule()

  @Before
  fun setup() {
    AnimationConfig.disableAnimations = true
    TestFlags.fakeEmail = ""
  }

  @After
  fun teardown() {
    AnimationConfig.disableAnimations = false
    TestFlags.fakeEmail = null
  }

  @Test
  fun modal_shows_expected_texts() {
    composeRule.setContent { GuestProfileWarningModal(onContinueAsGuest = {}, onLogin = {}) }

    composeRule.onNodeWithText("Profile unavailable").assertIsDisplayed()
    composeRule
        .onNodeWithText("Sign in with Microsoft Entra ID to access your profile settings.")
        .assertIsDisplayed()
  }

  @Test
  fun continue_button_triggers_callback() {
    var continueCount = 0
    composeRule.setContent {
      GuestProfileWarningModal(onContinueAsGuest = { continueCount += 1 }, onLogin = {})
    }

    composeRule.onNodeWithText("Continue as guest").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals(1, continueCount) }
  }

  @Test
  fun login_button_triggers_callback() {
    var loginCount = 0
    composeRule.setContent {
      GuestProfileWarningModal(onContinueAsGuest = {}, onLogin = { loginCount += 1 })
    }

    composeRule.onNodeWithText("Log in now").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals(1, loginCount) }
  }

  @Test
  fun tapping_backdrop_invokes_continue_callback() {
    var continueCount = 0

    composeRule.setContent {
      GuestProfileWarningModal(onContinueAsGuest = { continueCount += 1 }, onLogin = {})
    }
    composeRule.waitForIdle()

    composeRule.onRoot().performTouchInput { click(Offset(5f, 5f)) }

    composeRule.runOnIdle { assertEquals(1, continueCount) }
  }
}
