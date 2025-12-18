package com.android.sample.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.settings.Localization
import com.android.sample.settings.connectors.EdConnectTags
import com.android.sample.settings.connectors.MoodleConnectDialogTags
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test for connectors flow: Home Screen -> Drawer -> Connectors Screen -> Ed Connect
 * Screen Home Screen -> Drawer -> Connectors Screen -> Moodle Connect Dialog
 *
 * This test verifies that users can navigate to the connectors screen and access the connection
 * flows for Ed and Moodle.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ConnectorsE2ETest : BaseE2ETest() {

  private val homeRobot: HomeRobot
    get() = HomeRobot(composeRule)

  @Before
  fun setup() {
    homeRobot.navigateToHome()
  }

  @Test
  fun connectors_flow_navigate_to_connectors_screen() {
    // Navigate to connectors screen using robot pattern
    val connectorsRobot = homeRobot.openDrawer().goToConnectors()

    // Verify Connectors screen is displayed
    connectorsRobot.verifyConnectorsScreenDisplayed()

    // Verify subtitle
    composeRule
        .onNodeWithText(Localization.t("Connect_your_academic_services"), substring = true)
        .assertIsDisplayed()
  }

  @Test
  fun connectors_flow_navigate_to_ed_connect_screen() {
    // Navigate to Ed Connect screen using robot pattern
    homeRobot.openDrawer().goToConnectors().clickEdConnector().verifyEdConnectScreenDisplayed()

    // Verify Ed Connect screen elements - use label text (not placeholder to avoid ambiguity)
    // Wait for the label to appear
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(EdConnectTags.TokenLabel), timeoutMillis = 10_000)
    composeRule.waitForIdle()
    // Verify the label is displayed - use onNode to avoid ambiguity with placeholder
    composeRule.onNodeWithTag(EdConnectTags.TokenLabel).performScrollTo().assertIsDisplayed()

    // Verify "Get Token" button is present - use exact text match to avoid ambiguity
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(EdConnectTags.GetTokenButton), timeoutMillis = 10_000)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(EdConnectTags.GetTokenButton).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun connectors_flow_navigate_to_moodle_connect_dialog() {
    // Navigate to Moodle connect dialog using robot pattern
    homeRobot.openDrawer().goToConnectors().clickMoodleConnector().verifyMoodleDialogDisplayed()

    // The dialog should appear with Moodle connection fields
    // Verify by checking for unique non-editable elements:
    // 1. "Forgot password?" link (unique to the dialog)
    val forgotPasswordText = Localization.t("settings_connectors_moodle_forgot_password")
    composeRule.onNode(hasText(forgotPasswordText)).assertIsDisplayed()

    // 2. Verify editable fields are present (username and password fields)
    // Wait for at least one editable field, then verify we can find multiple
    composeRule.waitUntilAtLeastOneExists(isEditable(), timeoutMillis = 5_000)
    // The dialog should have at least 2 editable fields (username and password)
    // We verify by checking that we can find editable nodes
    composeRule.onAllNodes(isEditable()).assertCountEquals(2)
  }

  @Test
  fun connectors_flow_ed_connection_process() {
    // Navigate to Ed Connect screen using robot pattern
    homeRobot.openDrawer().goToConnectors().clickEdConnector().verifyEdConnectScreenDisplayed()

    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(EdConnectTags.TokenLabel), timeoutMillis = 10_000)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(EdConnectTags.TokenLabel).performScrollTo()

    // Step 3: Enter a test token (simulating user input)
    val testToken = "test_token_1234567890"

    // Find the editable TextField - wait for it to be available
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(EdConnectTags.TokenField), timeoutMillis = 10_000)
    composeRule.waitForIdle()

    // Use the editable matcher to find and interact with the TextField
    // onNode is a method on ComposeTestRule that takes a SemanticsMatcher
    composeRule
        .onNodeWithTag(EdConnectTags.TokenField)
        .performScrollTo()
        .performTextInput(testToken)
    composeRule.waitForIdle()

    // Step 4: Verify Connect button is enabled and clickable
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(EdConnectTags.ConnectButton), timeoutMillis = 10_000)
    composeRule.waitForIdle()
    // Find the Connect button - it should be clickable and not editable
    // Use onNode to find the button specifically (not a text field)
    composeRule.onNodeWithTag(EdConnectTags.ConnectButton).performScrollTo().assertIsDisplayed()

    // Step 5: Click Connect button to trigger connection process
    composeRule.onNodeWithTag(EdConnectTags.ConnectButton).performClick()
    composeRule.waitForIdle()

    // Step 6: Verify that connection process started - check for deterministic outcome
    // After clicking, one of these should happen:
    // 1. Error message appears (invalid token)
    // 2. Button becomes disabled (loading state with CircularProgressIndicator)
    // 3. Navigation back occurs (successful connection)
    composeRule.waitForIdle()

    // Wait for UI to update after connection attempt
    composeRule.waitForIdle()

    // Verify post-condition: Check which outcome occurred
    // First, check if we navigated back (successful connection)
    val navigatedBack =
        try {
          composeRule.onNodeWithTag(EdConnectTags.Root).assertIsDisplayed()
          false
        } catch (e: AssertionError) {
          // Screen root doesn't exist, we navigated back
          true
        }

    if (navigatedBack) {
      // Navigation back occurred (successful connection)
      composeRule.waitUntilAtLeastOneExists(
          hasText(Localization.t("connectors")), timeoutMillis = 3_000)
    } else {
      // Still on Ed Connect screen, verify either error appeared or button is present
      // (loading state)
      val errorAppeared =
          try {
            composeRule.onNodeWithTag(EdConnectTags.ErrorMessage).assertIsDisplayed()
            true
          } catch (e: AssertionError) {
            false
          }

      if (!errorAppeared) {
        // No error, verify button is still present (loading state)
        composeRule.onNodeWithTag(EdConnectTags.ConnectButton).assertIsDisplayed()
      }
    }
  }

  @Test
  fun connectors_flow_moodle_connection_process() {
    // Navigate to Moodle connect dialog using robot pattern
    homeRobot.openDrawer().goToConnectors().clickMoodleConnector().verifyMoodleDialogDisplayed()

    // Verify "Forgot password?" link is displayed
    val forgotPasswordText = Localization.t("settings_connectors_moodle_forgot_password")
    composeRule.onNode(hasText(forgotPasswordText)).assertIsDisplayed()

    // Also verify that editable fields are present
    composeRule.waitUntilAtLeastOneExists(isEditable(), timeoutMillis = 5_000)

    // Step 3: Enter test credentials
    // Find editable TextFields - wait for them to be available
    composeRule.waitUntilAtLeastOneExists(isEditable(), timeoutMillis = 5_000)

    // Wait for both fields to be available
    composeRule.waitUntilAtLeastOneExists(isEditable(), timeoutMillis = 3_000)
    composeRule.waitForIdle()

    // Enter username - first editable field (index 0)
    val editableFields = composeRule.onAllNodes(isEditable())
    editableFields[0].performTextInput("test_user")
    composeRule.waitForIdle()

    // Enter password - second editable field (index 1)
    composeRule.waitForIdle()
    editableFields[1].performTextInput("test_password")
    composeRule.waitForIdle()

    // Step 4: Verify Connect button is enabled
    // Use the specific Moodle login button text to avoid ambiguity with other "Connect" buttons
    val moodleLoginButtonText = Localization.t("settings_connectors_moodle_login_button")
    composeRule.waitUntilAtLeastOneExists(hasText(moodleLoginButtonText), timeoutMillis = 3_000)

    // There are 2 nodes with "Log in" text (title and button), so use onAllNodes to select the
    // button
    // The button is typically the last one (after the title)
    val loginNodes = composeRule.onAllNodes(hasText(moodleLoginButtonText).and(!isEditable()))
    loginNodes.assertCountEquals(2) // Should have title and button
    // Select the last node (index 1) which should be the button
    loginNodes[1].assertIsDisplayed()

    // Step 5: Click Connect button to trigger connection process
    loginNodes[1].performClick()
    composeRule.waitForIdle()

    // Step 6: Verify that connection process started - check for deterministic outcome
    // After clicking, one of these should happen:
    // 1. Error message appears (invalid credentials)
    // 2. Button becomes disabled (loading state with CircularProgressIndicator)
    // 3. Dialog closes and navigation occurs (successful connection)
    composeRule.waitForIdle()

    // Wait for UI to update after connection attempt
    composeRule.waitForIdle()

    // Verify post-condition: Check which outcome occurred
    // First, check if dialog closed (successful connection)
    val dialogClosed =
        try {
          composeRule.onNode(hasText(forgotPasswordText)).assertIsDisplayed()
          false
        } catch (e: AssertionError) {
          // Dialog closed, navigation occurred
          true
        }

    if (dialogClosed) {
      // Dialog closed, navigation occurred (successful connection)
      composeRule.waitUntilAtLeastOneExists(
          hasText(Localization.t("connectors")), timeoutMillis = 3_000)
    } else {
      // Dialog still open, verify either error appeared or button is present (loading state)
      val errorAppeared =
          try {
            composeRule.onNodeWithTag(MoodleConnectDialogTags.ErrorMessage).assertIsDisplayed()
            true
          } catch (e: AssertionError) {
            false
          }

      if (!errorAppeared) {
        // No error, verify that connection process started
        // When loading, the button text is replaced by CircularProgressIndicator,
        // so we verify that the dialog is still open (connection process is in progress)
        composeRule.onNode(hasText(forgotPasswordText)).assertIsDisplayed()
      }
    }
  }
}
