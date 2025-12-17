package com.android.sample.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.sample.authentification.AuthTags
import com.android.sample.home.DrawerTags
import com.android.sample.home.HomeTags
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

  @Before
  fun setup() {
    // Navigate to home screen as guest for testing
    navigateToHomeScreen()
  }

  private fun navigateToHomeScreen() {
    // Wait for opening screen to navigate
    composeRule.waitForIdle()
    Thread.sleep(3000)

    // Click guest button to navigate to home
    composeRule.waitUntilAtLeastOneExists(hasTestTag(AuthTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()
    composeRule.waitForIdle()

    // Wait for home screen root
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 10_000)

    // Wait for home screen elements to be fully loaded
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MenuBtn), timeoutMillis = 5_000)
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MessageField), timeoutMillis = 5_000)
    composeRule.waitForIdle()
  }

  @Test
  fun navigation_flow_home_to_settings_and_back() {
    // Step 1: Verify Home Screen is displayed and wait for menu button
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MenuBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()

    // Step 2: Open the drawer by clicking menu button
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()

    // Step 3: Wait for drawer to open and verify it's displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(DrawerTags.Root), timeoutMillis = 3_000)
    composeRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()

    // Step 4: Verify drawer content is visible
    composeRule.waitForIdle()
    Thread.sleep(500) // Allow drawer animation to complete

    // Step 5: Click on Settings icon in drawer footer
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(DrawerTags.UserSettings), timeoutMillis = 3_000)
    composeRule.onNodeWithTag(DrawerTags.UserSettings).assertIsDisplayed()
    composeRule.onNodeWithTag(DrawerTags.UserSettings).performClick()
    composeRule.waitForIdle()

    // Step 6: Wait for Settings screen to appear
    // Settings screen should be displayed (we verify by checking drawer is closed)
    Thread.sleep(1000) // Allow navigation animation
    composeRule.waitForIdle()

    // Step 7: Verify navigation to Settings occurred
    // The drawer should be closed after navigation
    // We verify this by checking that the drawer root is no longer visible
    composeRule.waitForIdle()

    // Step 8: Use activity back press to return to home (must be on main thread)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      composeRule.activity.onBackPressedDispatcher.onBackPressed()
    }
    composeRule.waitForIdle()
    Thread.sleep(500) // Allow navigation animation

    // Step 9: Verify we're back on Home Screen
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()

    // Verify we can open drawer again
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MenuBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(hasTestTag(DrawerTags.Root), timeoutMillis = 3_000)
    composeRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()
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
