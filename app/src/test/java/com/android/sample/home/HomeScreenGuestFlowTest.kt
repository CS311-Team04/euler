package com.android.sample.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeScreenGuestFlowTest {

  @get:Rule val composeRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun guest_profile_click_shows_warning_and_continue_hides_modal() = runTest {
    val viewModel = HomeViewModel(FakeProfileRepository())
    viewModel.setGuestMode(true)

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, openDrawerOnStart = true) }
    }

    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.onNodeWithText("Guest").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Profile unavailable").assertIsDisplayed()
    composeRule.runOnIdle { assertTrue(viewModel.uiState.value.showGuestProfileWarning) }

    composeRule.onNodeWithText("Continue as guest").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.runOnIdle { assertFalse(viewModel.uiState.value.showGuestProfileWarning) }
    composeRule.onAllNodesWithText("Profile unavailable").assertCountEquals(0)
  }

  @Test
  fun guest_warning_login_invokes_sign_out_callback() = runTest {
    val viewModel = HomeViewModel(FakeProfileRepository())
    viewModel.setGuestMode(true)
    var signOutInvoked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = viewModel, openDrawerOnStart = true, onSignOut = { signOutInvoked = true })
      }
    }

    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.onNodeWithText("Guest").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Log in now").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.runOnIdle {
      assertTrue(signOutInvoked)
      assertFalse(viewModel.uiState.value.showGuestProfileWarning)
    }
  }
}
