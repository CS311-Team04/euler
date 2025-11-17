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
class ProfilePageComposeTest {

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
  fun save_button_triggers_callback() {
    var saved: UserProfile? = null
    composeRule.setContent { MaterialTheme { ProfilePage(onSaveProfile = { saved = it }) } }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Save").performClick()
    composeRule.waitForIdle()

    assertEquals(UserProfile(), saved)
  }

  @Test
  fun saved_banner_shows() {
    val prev = AnimationConfig.disableAnimations
    try {
      AnimationConfig.disableAnimations = false
      composeRule.setContent { MaterialTheme { ProfilePage() } }

      composeRule.onNodeWithText("Save").performClick()
      composeRule.onNodeWithText("Saved").assertIsDisplayed()
    } finally {
      AnimationConfig.disableAnimations = prev
    }
  }

  @Test
  fun saved_banner_hides_when_animations_disabled() {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Save").performClick()
    composeRule.waitForIdle()

    composeRule.onAllNodesWithText("Saved").assertCountEquals(0)
  }

  @Test
  fun dropdown_opens_on_click() {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Select your role").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Teacher").assertIsDisplayed()
  }

  @Test
  fun dropdown_selection_updates_value() {
    composeRule.setContent { MaterialTheme { ProfilePage() } }
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Select your role").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Teacher").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Teacher", useUnmergedTree = true).assertIsDisplayed()
  }
}
