package com.android.sample.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.profile.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfilePageComposeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun clicking_save_persists_profile_and_shows_confirmation_banner() {
    var savedProfile: UserProfile? = null

    composeRule.setContent { MaterialTheme { ProfilePage(onSaveProfile = { savedProfile = it }) } }
    composeRule.runOnIdle { composeRule.onAllNodesWithText("Saved").assertCountEquals(0) }

    composeRule.runOnIdle { composeRule.onNodeWithText("Save").performClick() }

    composeRule.runOnIdle {
      composeRule.onNodeWithText("Saved").assertIsDisplayed()
      assertEquals(UserProfile(), savedProfile)
    }

    composeRule.mainClock.advanceTimeBy(2_600)
    composeRule.runOnIdle { composeRule.onAllNodesWithText("Saved").assertCountEquals(0) }
  }

  @Test
  fun role_dropdown_allows_selection_and_updates_display_text() {
    composeRule.setContent { MaterialTheme { ProfilePage() } }

    composeRule.runOnIdle { composeRule.onNodeWithText("Select your role").performClick() }

    composeRule.runOnIdle { composeRule.onNodeWithText("Teacher").performClick() }

    composeRule.runOnIdle {
      composeRule.onNodeWithText("Teacher", useUnmergedTree = true).assertIsDisplayed()
    }
  }
}
