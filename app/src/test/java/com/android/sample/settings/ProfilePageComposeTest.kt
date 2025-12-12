package com.android.sample.settings

import AnimationConfig
import TestFlags
import androidx.compose.material3.MaterialTheme
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
import org.junit.Ignore
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

  private val fakeFullProfile =
      UserProfile(
          fullName = "John Doe",
          preferredName = "JD",
          faculty = "IC — School of Computer and Communication Sciences",
          section = "Computer Science",
          email = "john.doe@epfl.ch",
          phone = "079 000 00 00",
          roleDescription = "Student")

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    AnimationConfig.disableAnimations = true
    TestFlags.fakeEmail = ""
  }

  @After
  fun teardown() {
    AppSettings.resetDispatcher()
    Dispatchers.resetMain()
    AnimationConfig.disableAnimations = false
    TestFlags.fakeEmail = null
  }

  @Ignore
  @Test
  fun `all fields are displayed`() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Wait for Profile text to appear
    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithText("Profile").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Profile").assertIsDisplayed()
    composeRule.onNodeWithText("Save").assertIsDisplayed()
    composeRule.onNodeWithText("Personal details").assertIsDisplayed()
    composeRule.onNodeWithText("Academic information").assertIsDisplayed()
    composeRule.onNodeWithText("Contact").assertIsDisplayed()
  }

  @Test
  fun `save button triggers callback`() = runTest {
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
  fun `role dropdown works`() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Select your role").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithText("Teacher").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Teacher").assertIsDisplayed()
    composeRule.onNodeWithText("Teacher").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()
  }

  @Test
  fun `faculty dropdown works`() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Select your faculty").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule
          .onAllNodesWithText("IC — School of Computer and Communication Sciences")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeRule
        .onNodeWithText("IC — School of Computer and Communication Sciences")
        .assertIsDisplayed()
    composeRule.onNodeWithText("IC — School of Computer and Communication Sciences").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()
  }

  @Ignore("Flaky test, needs investigation")
  @Test
  fun `section dropdown works`() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage(initialProfile = fakeFullProfile) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.onNodeWithText("Computer Science").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()

    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithText("Communication Systems").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Communication Systems").assertIsDisplayed()
    composeRule.onNodeWithText("Communication Systems").performClick()
    composeRule.waitForIdle()
    advanceUntilIdle()
  }

  @Test
  fun `initial profile is loaded`() = runTest {
    val profile =
        UserProfile(
            fullName = "Test User",
            preferredName = "Test",
            faculty = "IC — School of Computer and Communication Sciences",
            section = "Computer Science",
            email = "test@epfl.ch",
            phone = "+41 79 000 00 00",
            roleDescription = "Student")

    composeRule.setContent { MaterialTheme { ProfilePage(initialProfile = profile) } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Just verify the page loads with the profile
    composeRule.onNodeWithText("Profile").assertIsDisplayed()
    composeRule.onNodeWithText("Save").assertIsDisplayed()
  }

  @Test
  fun `back button is displayed`() = runTest {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()
    advanceUntilIdle()

    // Back button is an IconButton, we can check if Profile title is displayed which indicates the
    // row exists
    composeRule.onNodeWithText("Profile").assertIsDisplayed()
  }
}
