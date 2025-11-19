package com.android.sample.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeScreenGuestFlowTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun guest_profile_click_shows_warning_and_continue_hides_modal() {
    val viewModel = HomeViewModel(profileRepository = FakeProfileRepository())
    viewModel.setGuestMode(true)

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, openDrawerOnStart = true) }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Guest").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Profile unavailable").assertIsDisplayed()
    composeRule.runOnIdle { assertTrue(viewModel.uiState.value.showGuestProfileWarning) }

    composeRule.onNodeWithText("Continue as guest").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertFalse(viewModel.uiState.value.showGuestProfileWarning) }
    composeRule.onAllNodesWithText("Profile unavailable").assertCountEquals(0)
  }

  @Test
  fun guest_warning_login_invokes_sign_out_callback() {
    val viewModel = HomeViewModel(profileRepository = FakeProfileRepository())
    viewModel.setGuestMode(true)
    var signOutInvoked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = viewModel, openDrawerOnStart = true, onSignOut = { signOutInvoked = true })
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithText("Guest").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Log in now").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertTrue(signOutInvoked)
      assertFalse(viewModel.uiState.value.showGuestProfileWarning)
    }
  }
}
