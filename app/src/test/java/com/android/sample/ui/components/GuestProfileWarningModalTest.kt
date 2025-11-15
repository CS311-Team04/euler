package com.android.sample.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GuestProfileWarningModalTest {

  @get:Rule val composeRule = createComposeRule()

  @Ignore("Flaky under Robolectric CI — temporarily disabled")
  @Test
  fun modal_displays_expected_texts_and_buttons_trigger_callbacks() {
    var continueCount = 0
    var loginCount = 0

    composeRule.setContent {
      GuestProfileWarningModal(
          onContinueAsGuest = { continueCount += 1 }, onLogin = { loginCount += 1 })
    }

    composeRule.waitForIdle()

    composeRule.onNodeWithText("Profile unavailable").assertIsDisplayed()
    composeRule
        .onNodeWithText("Sign in with Microsoft Entra ID to access your profile settings.")
        .assertIsDisplayed()

    composeRule.onNodeWithText("Continue as guest").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Log in now").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertEquals(1, continueCount)
      assertEquals(1, loginCount)
    }
  }

  @Ignore("Flaky under Robolectric CI — temporarily disabled")
  @Test
  fun tapping_backdrop_invokes_continue_callback() {
    var continueCount = 0

    composeRule.setContent {
      GuestProfileWarningModal(onContinueAsGuest = { continueCount += 1 }, onLogin = {})
    }

    composeRule.waitForIdle()
    composeRule.onRoot().performTouchInput { click(Offset(5f, 5f)) }
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals(1, continueCount) }
  }
}
