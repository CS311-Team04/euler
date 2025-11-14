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
import com.android.sample.llm.FakeLlmClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
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
      val options =
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:integration-test")
              .setProjectId("integration-test")
              .setApiKey("fake-api-key")
              .build()
      FirebaseApp.initializeApp(context, options)
    }
  }

  private fun launchHomeScreen() {
    ensureFirebaseInitialized()
    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = HomeViewModel(FakeLlmClient())) }
    }
  }

  @Test
  fun action1_button_triggers_callback() {
    ensureFirebaseInitialized()
    var action1Clicked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = HomeViewModel(FakeLlmClient()), onAction1Click = { action1Clicked = true })
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
        HomeScreen(
            viewModel = HomeViewModel(FakeLlmClient()), onAction2Click = { action2Clicked = true })
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

    composeRule.onNodeWithText(TestConstants.ButtonTexts.WHAT_IS_EPFL).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ButtonTexts.CHECK_ED_DISCUSSION).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.PlaceholderTexts.MESSAGE_EULER).assertIsDisplayed()
  }
}
