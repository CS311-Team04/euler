package com.android.sample.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.home.HomeTags
import com.android.sample.settings.SettingsTags
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test for navigation flow within the app: Home Screen -> Open Drawer -> Navigate to
 * Settings -> Return to Home
 *
 * This test verifies the complete navigation flow including drawer interactions and screen
 * transitions.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NavigationFlowE2ETest : BaseE2ETest() {

  private val homeRobot: HomeRobot
    get() = HomeRobot(composeRule)

  @Before
  fun setup() {
    // Navigate to home screen as guest for testing
    homeRobot.navigateToHome()
    // Wait for home screen elements to be fully loaded
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MessageField), timeoutMillis = 5_000)
    composeRule.waitForIdle()
  }

  @Test
  fun navigation_flow_home_to_settings_and_back() {
    // Step 1: Verify Home Screen is displayed
    homeRobot.verifyHomeScreenDisplayed()

    // Step 2-5: Open drawer and navigate to Settings using robot pattern
    val drawerRobot = homeRobot.openDrawer().verifyDrawerDisplayed()
    drawerRobot.goToSettings()

    // Step 6-7: Verify Settings screen is displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(SettingsTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(SettingsTags.Root).assertIsDisplayed()
    composeRule.waitForIdle()

    // Step 8: Use Espresso pressBack() to return to home
    Espresso.pressBack()
    composeRule.waitForIdle()

    // Step 9: Verify we're back on Home Screen
    homeRobot.verifyHomeScreenDisplayed()

    // Verify we can open drawer again
    homeRobot.openDrawer().verifyDrawerDisplayed()
  }

  @Test
  fun navigation_flow_top_right_menu_interaction() {
    // Step 1: Verify Home Screen is displayed and wait for buttons
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.TopRightBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).assertIsDisplayed()

    // Step 2: Click on top right button to open menu
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.waitForIdle()

    // Step 3: Wait for top right menu to appear
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.TopRightMenu), timeoutMillis = 3_000)
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

    // Step 4: Close menu by clicking outside or clicking button again
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.waitForIdle()

    // Step 5: Verify we're still on Home Screen
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }
}
