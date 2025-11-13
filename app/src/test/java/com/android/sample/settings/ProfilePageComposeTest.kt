package com.android.sample.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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

    composeRule.onAllNodesWithText("Saved").assertCountEquals(0)

    composeRule.onNodeWithText("Save").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Saved").assertIsDisplayed()
    composeRule.runOnIdle { assertEquals(UserProfile(), savedProfile) }

    composeRule.mainClock.advanceTimeBy(2_600)
    composeRule.waitForIdle()
    composeRule.onAllNodesWithText("Saved").assertCountEquals(0)
  }

  @Test
  fun role_dropdown_allows_selection_and_updates_display_text() {
    composeRule.setContent { MaterialTheme { ProfilePage() } }

    composeRule.onNodeWithText("Select your role").performClick()
    composeRule.onNodeWithText("Teacher").performClick()

    composeRule.onNodeWithText("Teacher", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun existing_profile_locks_full_name_and_email_fields() {
    val profile =
        UserProfile(
            fullName = "Existing Name",
            preferredName = "Existing Preferred",
            email = "locked@epfl.ch",
            faculty = "IC",
            section = "CS",
            roleDescription = "Student")

    composeRule.setContent { MaterialTheme { ProfilePage(initialProfile = profile) } }

    composeRule
        .onNode(hasText("Existing Name") and hasSetTextAction(), useUnmergedTree = true)
        .assertIsNotEnabled()
    composeRule
        .onNode(hasText("locked@epfl.ch") and hasSetTextAction(), useUnmergedTree = true)
        .assertIsNotEnabled()

    composeRule.onNodeWithText("This name can only be set once.").assertIsDisplayed()
    composeRule.onNodeWithText("Managed via your Microsoft account").assertIsDisplayed()

    composeRule
        .onNode(hasText("Existing Preferred") and hasSetTextAction(), useUnmergedTree = true)
        .assertIsEnabled()
  }
}
