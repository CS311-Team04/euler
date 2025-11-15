package com.android.sample.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.home.FakeProfileRepository
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeTags
import com.android.sample.home.HomeViewModel
import com.android.sample.llm.FakeLlmClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for HomeScreen. Tests the complete user flow from screen rendering to message
 * sending and receiving.
 */
@Ignore("Disabled in CI — constructor mismatch")
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeScreenE2ETest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var fakeLlmClient: FakeLlmClient

  @Before
  fun setup() {
    ensureFirebaseInitialized()
    fakeLlmClient = FakeLlmClient()
  }

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
    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel =
                HomeViewModel(
                    profileRepository = FakeProfileRepository(), llmClient = fakeLlmClient))
      }
    }
  }

  @Test
  fun homeScreen_displaysAllMainElements() {
    launchHomeScreen()

    // Wait for the root element to be displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Root), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()

    // Check that the menu button is displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MenuBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()

    // Check that the top right button is displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.TopRightBtn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).assertIsDisplayed()

    // Check that the message field is displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MessageField), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()

    // Check that suggestion chips are displayed
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()

    // Check that the intro title is displayed when there are no messages
    composeRule.onNodeWithText("Ask Euler Anything").assertIsDisplayed()
  }

  @Test
  fun homeScreen_userCanSendMessageAndReceiveResponse() {
    // Configure fake LLM to return a test response
    fakeLlmClient.nextReply = "Hello! I'm Euler, your EPFL assistant. How can I help you today?"

    launchHomeScreen()

    // Wait for the message field to be ready
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.MessageField), timeoutMillis = 5_000)

    // Type a message in the message field
    val testMessage = "Hello Euler"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(testMessage)

    // Wait for send button to appear (it should appear when text is entered)
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.SendBtn), timeoutMillis = 5_000)

    // Click the send button
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()

    // Wait for the message to appear in the chat
    composeRule.waitUntilAtLeastOneExists(
        hasText(testMessage, substring = true), timeoutMillis = 10_000)
    composeRule.onNode(hasText(testMessage, substring = true)).assertIsDisplayed()

    // Verify that a response was requested from the LLM
    assert(fakeLlmClient.prompts.isNotEmpty()) {
      "Expected at least one prompt to be sent to the LLM"
    }
  }
}
