package com.android.sample.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.authentification.AuthTags
import com.android.sample.home.HomeTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun signIn_to_home_to_settings_back_then_signOut_returns_to_signIn() {
    composeRule.setContent {
      MaterialTheme { AppNav(startOnSignedIn = false, activity = composeRule.activity) }
    }

    // Sign-In screen visible
    composeRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()

    // Switch edu path: simulated async success (1200 ms) via ViewModel
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).performClick()

    composeRule.waitUntil(timeoutMillis = 6_000) {
      composeRule.onAllNodesWithTag(HomeTags.Root).fetchSemanticsNodes().isNotEmpty()
    }

    // Home visible
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).assertIsDisplayed()

    // Open drawer and navigate to Settings
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.onNodeWithText("Settings").performClick()

    // Settings header text
    composeRule.onNodeWithText("Settings").assertIsDisplayed()

    // Back to Home (drawer should reopen via HomeWithDrawer route)
    // Back icon uses contentDescription
    composeRule.onNode(hasContentDescription("Back")).performClick()

    composeRule.waitUntil(timeoutMillis = 3_000) {
      composeRule.onAllNodesWithTag(HomeTags.Root).fetchSemanticsNodes().isNotEmpty()
    }

    // Open drawer and sign out
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.onNodeWithText("Sign Out").performClick()

    // Back to Sign-In
    composeRule.waitUntil(timeoutMillis = 3_000) {
      composeRule.onAllNodesWithTag(AuthTags.Root).fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
  }
}
