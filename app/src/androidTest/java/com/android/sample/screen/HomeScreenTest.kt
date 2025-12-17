package com.android.sample.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.TestConstants
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeTags
import com.android.sample.home.HomeViewModel
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun ensureFirebaseInitialized() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      val options = TestFirebaseConfig.createFirebaseOptions()
      FirebaseApp.initializeApp(context, options)
    }
  }

  private fun launchHomeScreen() {
    ensureFirebaseInitialized()
    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = HomeViewModel()) } }
  }

  @Test
  fun action1_button_triggers_callback() {
    ensureFirebaseInitialized()
    var action1Clicked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(viewModel = HomeViewModel(), onAction1Click = { action1Clicked = true })
      }
    }

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()

    assertTrue(action1Clicked)
  }

  @Test
  fun action2_button_triggers_callback() {
    ensureFirebaseInitialized()
    var action2Clicked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(viewModel = HomeViewModel(), onAction2Click = { action2Clicked = true })
      }
    }

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action2Btn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Action2Btn).performClick()

    assertTrue(action2Clicked)
  }

  @Test
  fun menu_button_click_toggles_drawer_state() {
    launchHomeScreen()

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MenuBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.onNodeWithText("New chat").assertIsDisplayed()
  }

  @Test
  fun top_right_menu_can_be_opened() {
    launchHomeScreen()

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.TopRightBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()
  }

  @Test
  fun message_field_accepts_text_input() {
    launchHomeScreen()

    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Hello Euler")
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun displays_expected_button_and_placeholder_texts() {
    launchHomeScreen()

    // Use tag-based selectors for suggestion buttons to avoid text matching ambiguity
    // (same text may appear in animated intro and suggestion chips)
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action2Btn).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.PlaceholderTexts.MESSAGE_EULER).assertIsDisplayed()
  }
}
