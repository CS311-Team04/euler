package com.android.sample.settings

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.profile.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfilePageComposeTest {

  @get:Rule val composeRule = createComposeRule()
  private val testDispatcher = UnconfinedTestDispatcher()

  companion object {
    init {
      // Ensure TestFlags is set before any test runs to prevent Firebase initialization
      TestFlags.fakeEmail = ""
    }
  }

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    // Set flags BEFORE any composition happens to prevent Firebase initialization
    AnimationConfig.disableAnimations = true
    TestFlags.fakeEmail = ""
  }

  @After
  fun teardown() {
    // Reset AppSettings to prevent coroutines from other tests affecting this test
    AppSettings.resetDispatcher()
    Dispatchers.resetMain()
    AnimationConfig.disableAnimations = false
    TestFlags.fakeEmail = null
  }

  @Test
  fun save_button_triggers_callback() = runTest {
    var saved: UserProfile? = null
    composeRule.setContent { MaterialTheme { ProfilePage(onSaveProfile = { saved = it }) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Save").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    assertEquals(UserProfile(), saved)
  }

  @Test
  fun saved_banner_shows() = runTest {
    val prevAnimations = AnimationConfig.disableAnimations
    val prevFakeEmail = TestFlags.fakeEmail
    try {
      // Keep TestFlags.fakeEmail set to prevent Firebase initialization
      TestFlags.fakeEmail = ""
      AnimationConfig.disableAnimations = false
      composeRule.setContent { MaterialTheme { ProfilePage() } }
      composeRule.waitForIdle()
      advanceUntilIdle()

      composeRule.onNodeWithText("Save").performClick()
      composeRule.waitForIdle()
      advanceUntilIdle()
      composeRule.onNodeWithText("Saved").assertIsDisplayed()
    } finally {
      AnimationConfig.disableAnimations = prevAnimations
      TestFlags.fakeEmail = prevFakeEmail
    }
  }

  @Test
  fun saved_banner_hides_when_animations_disabled() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Save").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onAllNodesWithText("Saved").assertCountEquals(0)
  }

  @Test
  fun dropdown_opens_on_click() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Select your role").performClick()
    // Wait for dropdown to appear
    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithText("Teacher").fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithText("Teacher").assertIsDisplayed()
  }

  @Test
  fun dropdown_selection_updates_value() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Select your role").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()
    composeRule.onNodeWithText("Teacher").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Teacher", useUnmergedTree = true).assertIsDisplayed()
  }
}
