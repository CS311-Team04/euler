package com.android.sample.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.authentification.AuthTags
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MicrosoftFlowE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Ignore("Requires Firebase/Provider UI; skip in default connected test run")
  @Test
  fun microsoft_login_shows_loading_then_returns_to_signin_buttons_enabled_on_error() {
    composeRule.setContent {
      MaterialTheme { AppNav(startOnSignedIn = false, activity = composeRule.activity) }
    }

    // Click Microsoft login
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).performClick()

    // Loading indicator for Microsoft appears, and buttons disabled
    composeRule.onNodeWithTag(AuthTags.MsProgress).assertIsDisplayed()
    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsNotEnabled()

    // We expect that since real Microsoft flow isn't completed, we remain on SignIn and eventually
    // buttons re-enable.
    composeRule.waitUntil(timeoutMillis = 5_000) {
      // We simply wait until the loading indicator disappears
      composeRule.onAllNodesWithTag(AuthTags.MsProgress).fetchSemanticsNodes().isEmpty()
    }

    composeRule.onNodeWithTag(AuthTags.BtnMicrosoft).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.BtnSwitchEdu).assertIsEnabled()
    composeRule.onNodeWithTag(AuthTags.Root).assertIsDisplayed()
  }
}
