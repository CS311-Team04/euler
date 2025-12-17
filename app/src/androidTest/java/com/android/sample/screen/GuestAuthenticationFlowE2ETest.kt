package com.android.sample.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.TestConstants
import com.android.sample.authentification.AuthTags
import com.android.sample.home.HomeTags
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test for the complete guest authentication flow: Opening Screen -> Sign In Screen ->
 * Guest Login -> Home Screen
 *
 * This test verifies the entire user journey from app launch to home screen when a user chooses to
 * continue as a guest.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class GuestAuthenticationFlowE2ETest : BaseE2ETest() {

  /**
   * Helper function to navigate from app launch to home screen as a guest. Encapsulates the common
   * flow: Opening Screen -> Sign In Screen -> Guest Login -> Home Screen.
   */
  private fun goToHomeAsGuest() {
    // Step 1: Wait for Opening Screen to navigate to Sign In screen
    // The opening screen will automatically navigate to Sign In
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.Root), timeoutMillis = 5_000)

    // Step 2: Click on "Continue as a guest" button
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    composeRule.waitForIdle()

    // Step 3: Wait for navigation to Home Screen
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 10_000)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun guest_authentication_flow_complete_journey() {
    // Step 1: Wait for Opening Screen to navigate to Sign In screen and verify it
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()

    // Verify key elements of Sign In screen are visible
    composeRule.onNodeWithTag(AuthTags.Title).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ScreenTexts.TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsDisplayed()

    // Step 2: Navigate to home as guest
    goToHomeAsGuest()

    // Step 3: Verify Home Screen elements are displayed
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()

    // Verify intro message is shown
    composeRule.onNodeWithText("Ask Euler Anything").assertIsDisplayed()
  }

  @Test
  fun guest_authentication_flow_verifies_guest_mode() {
    // Navigate to home as guest
    goToHomeAsGuest()

    // Verify that suggestion chips are available (guest mode should still show these)
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
  }
}
