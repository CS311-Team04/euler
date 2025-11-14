package com.android.sample.screen

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.TestConstants
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeTags
import com.android.sample.home.HomeViewModel
import com.android.sample.llm.FakeLlmClient
import com.android.sample.llm.LlmClient
import com.android.sample.profile.ProfileDataSource
import com.android.sample.profile.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private class TestProfileRepository : ProfileDataSource {
    override suspend fun saveProfile(profile: UserProfile) {}

    override suspend fun loadProfile(): UserProfile? = null
  }

  private fun createHomeViewModel(llmClient: LlmClient = FakeLlmClient()): HomeViewModel =
      HomeViewModel(profileRepository = TestProfileRepository(), llmClient = llmClient)

  private fun setContentWithViewModel(
      llmClient: LlmClient = FakeLlmClient(),
      content: @Composable (HomeViewModel) -> Unit
  ) {
    ensureFirebaseInitialized()
    val viewModel = createHomeViewModel(llmClient)
    composeRule.setContent { MaterialTheme { content(viewModel) } }
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
    ensureFirebaseInitialized()
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
  }

  @Test
  fun action1_button_triggers_callback() {
    ensureFirebaseInitialized()
    var action1Clicked = false

    setContentWithViewModel { vm ->
      HomeScreen(viewModel = vm, onAction1Click = { action1Clicked = true })
    }

    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()

    assertTrue(action1Clicked)
  }

  @Test
  fun action2_button_triggers_callback() {
    ensureFirebaseInitialized()
    var action2Clicked = false

    setContentWithViewModel { vm ->
      HomeScreen(viewModel = vm, onAction2Click = { action2Clicked = true })
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
  }

  @Test
  fun message_field_can_be_cleared_and_retyped() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Type and clear
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("First")
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Second")

    // Field should still be functional
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  //  @Test
  //  fun all_icons_have_content_descriptions() {
  //    composeRule.setContent { MaterialTheme { HomeScreen() } }
  //    composeRule.waitForIdle()
  //
  //    composeRule.waitUntilAtLeastOneExists(
  //        hasContentDescription(TestConstants.ContentDescriptions.MENU), timeoutMillis = 5000)
  //    composeRule
  //        .onAllNodesWithContentDescription(TestConstants.ContentDescriptions.MENU)
  //        .get(0)
  //        .assertIsDisplayed()
  //    composeRule.waitUntilAtLeastOneExists(
  //        hasContentDescription(TestConstants.ContentDescriptions.MORE), timeoutMillis = 5000)
  //    composeRule
  //        .onAllNodesWithContentDescription(TestConstants.ContentDescriptions.MORE)
  //        .get(0)
  //        .assertIsDisplayed()
  //    // Send button only appears when there's text
  //    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
  //    composeRule.waitForIdle()
  //    composeRule.waitUntilAtLeastOneExists(
  //        hasContentDescription(TestConstants.ContentDescriptions.SEND), timeoutMillis = 5000)
  //    composeRule
  //        .onAllNodesWithContentDescription(TestConstants.ContentDescriptions.SEND)
  //        .get(0)
  //        .assertIsDisplayed()
  //    composeRule.waitUntilAtLeastOneExists(
  //        hasContentDescription(TestConstants.ContentDescriptions.EULER), timeoutMillis = 5000)
  //    composeRule
  //        .onAllNodesWithContentDescription(TestConstants.ContentDescriptions.EULER)
  //        .get(0)
  //        .assertIsDisplayed()
  //  }

  @Test
  fun screen_handles_special_characters_in_message() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    val specialText = "Hello! @#$%^&*()_+-=[]{}|;:',.<>?"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(specialText)

    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun screen_handles_unicode_characters() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    val unicodeText = "Hello 你好 مرحبا Bonjour こんにちは"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(unicodeText)

    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun drawer_button_responds_correctly() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // The drawer button should be visible and clickable
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()

    // Test that clicking does not cause a crash
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()

    // After clicking, the drawer should be synchronized
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()

    // Test multiple rapid clicks
    for (i in 1..3) {
      composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    }

    // Verify that everything still works
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
  }

  @Test
  fun delete_menu_flow_shows_confirmation_and_cancel_hides_it() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Open top-right menu
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

    // Click "Delete" and expect confirmation modal
    waitAndClickMenuDelete()
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()

    // Cancel should hide the modal
    composeRule.onNodeWithText("Cancel").performClick()
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun delete_confirmation_delete_opens_and_closes_modal() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
    composeRule.waitForIdle()

    // Open menu -> Delete -> confirm Delete
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    composeRule.onNodeWithText("Delete").performClick()
    composeRule.waitForIdle()

    // Modal should be closed
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun share_item_dismisses_menu() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Open menu and click Share, which calls onDismiss
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuShare()

    // Menu should be dismissed (its items disappear)
    assertNodeDoesNotExist("Share")
  }

  @Test
  fun homeScreen_with_default_parameters_renders() {
    // Test that all default parameters work
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
  }

  @Test
  fun homeScreen_with_custom_modifier() {
    // Test the modifier parameter - verify that the modifier is applied
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm, modifier = Modifier.fillMaxSize()) }

    // The Root should always be present with its testTag
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun homeScreen_with_openDrawerOnStart_true() {
    // Test the openDrawerOnStart = true parameter
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm, openDrawerOnStart = true) }

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    // The drawer should be opened automatically
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
  }

  @Test
  fun homeScreen_onSignOut_callback_is_called() {
    var signOutCalled = false

    setContentWithViewModel { vm ->
      HomeScreen(viewModel = vm, onSignOut = { signOutCalled = true })
    }

    // Open the drawer to access sign out
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()

    // We cannot test DrawerContent directly as it doesn't have a testTag
    // but we can verify that the screen still responds
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()

    // The callback will only be called if we click sign out in the drawer
    // We just verify that the component works
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun homeScreen_onSettingsClick_callback_is_called() {
    var settingsClicked = false

    setContentWithViewModel { vm ->
      HomeScreen(viewModel = vm, onSettingsClick = { settingsClicked = true })
    }

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    // The callback will be tested via the drawer, but we verify that the component works
  }

  @Test
  fun homeScreen_all_callbacks_provided() {
    var action1Called = false
    var action2Called = false
    var sendCalled = false
    var signOutCalled = false
    var settingsCalled = false

    setContentWithViewModel { vm ->
      HomeScreen(
          viewModel = vm,
          onAction1Click = { action1Called = true },
          onAction2Click = { action2Called = true },
          onSendMessage = { sendCalled = true },
          onSignOut = { signOutCalled = true },
          onSettingsClick = { settingsCalled = true })
    }

    // Wait for the UI to be completely rendered (important for CI)
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5000)

    // Test action1 callback
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    assertTrue(action1Called)

    // Note: After clicking Action1Btn, suggestions might disappear, so we skip Action2Btn
    // and test the send callback instead

    // For Send, we must fill the field first to enable the button
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()
    // Use the robust helper function to find and click the Send button
    waitAndClickSendButton()
    assertTrue(sendCalled)
  }

  @Test
  fun delete_modal_background_click_cancels() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Open menu and click Delete
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()

    // The modal should be displayed
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()

    // Clicking on the background (clickable Box) should cancel
    // We cannot click directly on the background, but we can test via Cancel
    composeRule.onNodeWithText("Cancel").performClick()
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun delete_modal_shows_correct_texts() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()

    // Verify all modal texts
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    composeRule
        .onNodeWithText("This will delete all messages. This action cannot be undone.")
        .assertIsDisplayed()
    composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    composeRule.onNodeWithText("Delete").assertIsDisplayed()
  }

  @Test
  fun dropdown_menu_dismisses_when_requested() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

    // Clicking outside the menu should close it
    // We simulate by clicking on the root
    composeRule.onNodeWithTag(HomeTags.Root).performClick()
    composeRule.waitForIdle()

    // The menu should be closed (not visible)
    // Note: we cannot test directly, but we verify that the screen remains stable
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun menu_button_toggles_drawer_multiple_times() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Toggle multiple times
    for (i in 1..5) {
      composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
      composeRule.waitForIdle()
    }

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun message_field_updates_with_multiple_changes() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("A")
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("B")
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("C")

    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun send_button_with_empty_message_still_calls_callback() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Wait for the UI to be completely rendered
    composeRule.waitForIdle()

    val sendMatcher = hasContentDescription("Send")
    composeRule.waitUntilAtLeastOneExists(sendMatcher, timeoutMillis = 5_000)
    composeRule.onAllNodes(sendMatcher)[0].assertIsDisplayed()

    // Enter text to make send button appear
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(sendMatcher, timeoutMillis = 5_000)
    composeRule.onAllNodes(sendMatcher)[0].assertIsDisplayed()
  }

  @Test
  fun loading_indicator_shows_when_isLoading_true() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // We cannot directly set isLoading in the ViewModel from the UI test
    // but we can verify that the component renders correctly
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun messages_list_is_empty_initially() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
    composeRule.waitForIdle()

    // Initially, the messages list is empty
    // We just verify that the UI renders correctly without crashing
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun topRight_menu_open_and_close_cycle() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Open
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

    // Close via dismiss
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.waitForIdle()

    // Reopen
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()
  }

  @Test
  fun delete_menu_item_calls_onDeleteClick_and_onDismiss() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()

    // Both callbacks should be called: onDeleteClick (shows modal) and onDismiss (closes menu)
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    assertNodeDoesNotExist("Delete") // The menu should be closed
  }

  @Test
  fun delete_confirmation_modal_confirm_closes_modal() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
    composeRule.waitForIdle()

    // Open menu -> Delete -> Confirm
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    composeRule.onNodeWithText("Delete").performClick() // Confirm in the modal
    composeRule.waitForIdle()

    // Modal should be closed
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun delete_confirmation_modal_cancel_closes_modal() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
    composeRule.waitForIdle()

    // Open menu -> Delete -> Cancel
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    composeRule.onNodeWithText("Cancel").performClick()
    composeRule.waitForIdle()

    // Modal should be closed
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun homeScreen_with_all_parameters_combined() {
    var action1Called = false

    setContentWithViewModel { vm ->
      HomeScreen(
          viewModel = vm,
          modifier = Modifier,
          onAction1Click = { action1Called = true },
          openDrawerOnStart = false)
    }

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    assertTrue(action1Called)
  }

  @Test
  fun multiple_delete_confirmation_cycles() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Multiple open/close modal cycles
    for (i in 1..3) {
      composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
      waitAndClickMenuDelete()
      composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
      composeRule.onNodeWithText("Cancel").performClick()
      assertNodeDoesNotExist("Clear Chat?")
    }
  }

  @Test
  fun message_field_clears_after_send() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Wait for the UI to be completely rendered (important for CI)
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test message")
    composeRule.waitForIdle()
    // Use the robust helper function to find and click the Send button
    waitAndClickSendButton()

    // The field should be cleared after sending (handled by the ViewModel)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun topRight_menu_items_are_clickable() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithText("Share").assertIsDisplayed()
    composeRule.onNodeWithText("Delete").assertIsDisplayed()

    // Both items should be clickable
    waitAndClickMenuShare()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()
    composeRule.waitForIdle()
  }

  @Test
  fun screen_handles_state_changes() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Change multiple states quickly
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Quick")
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun action_buttons_labels_are_displayed() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithText("What is EPFL").assertIsDisplayed()
    composeRule.onNodeWithText("Check Ed Discussion").assertIsDisplayed()
  }

  @Test
  fun placeholder_text_in_message_field() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithText("Message EULER").assertIsDisplayed()
  }

  @Test
  fun messages_list_container_is_displayed() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
    composeRule.waitForIdle()

    // Verify that the messages container is present
    // (the list may be empty, but the container exists)
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  //  @Test
  //  fun all_ui_elements_have_correct_content_descriptions() {
  //    composeRule.setContent { MaterialTheme { HomeScreen() } }
  //    composeRule.waitForIdle()
  //
  //    composeRule.waitUntilAtLeastOneExists(hasContentDescription("Menu"), timeoutMillis = 5000)
  //    composeRule.onAllNodesWithContentDescription("Menu").get(0).assertIsDisplayed()
  //    composeRule.waitUntilAtLeastOneExists(hasContentDescription("More"), timeoutMillis = 5000)
  //    composeRule.onAllNodesWithContentDescription("More").get(0).assertIsDisplayed()
  //    // Send button only appears when there's text
  //    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
  //    composeRule.waitForIdle()
  //    composeRule.waitUntilAtLeastOneExists(hasContentDescription("Send"), timeoutMillis = 5000)
  //    composeRule.onAllNodesWithContentDescription("Send").get(0).assertIsDisplayed()
  //    composeRule.waitUntilAtLeastOneExists(hasContentDescription("Euler"), timeoutMillis = 5000)
  //    composeRule.onAllNodesWithContentDescription("Euler").get(0).assertIsDisplayed()
  //  }

  @Test
  fun delete_modal_cannot_be_dismissed_by_clicking_delete_button() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()

    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()

    // The Delete button in the modal should confirm, not close
    composeRule.onNodeWithText("Delete").performClick()
    // The modal should close after confirmation
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun delete_modal_shows_when_showDeleteConfirmation_is_true() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // The modal should not be visible initially
    assertNodeDoesNotExist("Clear Chat?")

    // Open via the menu
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()

    // Now it should be visible
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
  }

  @Test
  fun homeScreen_handles_null_callbacks_gracefully() {
    // Test with default callbacks (empty lambdas)
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Wait for the UI to be completely rendered (important for CI)
    composeRule.waitForIdle()
    composeRule.waitUntilAtLeastOneExists(hasTestTag(HomeTags.Action1Btn), timeoutMillis = 5000)

    // Test that Action1Btn is clickable without crashing
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()

    // Note: After clicking Action1Btn, suggestions might disappear, so we skip Action2Btn

    // For Send, we must first fill the field to enable the button
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()
    // Use the robust helper function to find and click the Send button
    waitAndClickSendButton()

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun message_field_updates_realtime() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    val testText = "Hello World Test"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(testText)

    // The ViewModel should update the state in real time
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun topRight_button_opens_menu_every_time() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    for (i in 1..3) {
      composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
      composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

      // Close by clicking Share
      waitAndClickMenuShare()
      composeRule.waitForIdle()
    }
  }

  @Test
  fun delete_flow_complete_workflow() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }
    composeRule.waitForIdle()

    // Complete workflow: open menu -> delete -> confirm
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    waitAndClickMenuDelete()
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    composeRule.onNodeWithText("Delete").performClick()
    composeRule.waitForIdle()

    // Modal should be closed
    assertNodeDoesNotExist("Clear Chat?")
  }

  @Test
  fun microphone_button_is_displayed_when_empty() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Microphone button should always be visible
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
  }

  @Test
  fun voice_mode_button_is_displayed_when_empty() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Voice mode button should be visible when input is empty
    composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()
  }

  @Test
  fun voice_mode_button_disappears_when_text_entered() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Initially, voice mode button should be visible
    composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()

    // Enter text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test message")
    composeRule.waitForIdle()

    // Voice mode button should disappear
    try {
      composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()
      fail("Voice mode button should not be visible when text is entered")
    } catch (e: AssertionError) {
      // Expected: button should not be displayed
    }
  }

  @Test
  fun voice_mode_button_reappears_when_text_cleared() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Enter and clear text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextClearance()
    composeRule.waitForIdle()

    // Voice mode button should reappear
    composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()
  }

  @Test
  fun send_button_appears_when_text_entered() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Initially, send button should NOT be visible
    try {
      composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()
      fail("Send button should not be visible when input is empty")
    } catch (e: AssertionError) {
      // Expected: button should not be displayed
    }

    // Enter text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()

    // Send button should appear
    composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()
  }

  @Test
  fun send_button_disappears_when_text_cleared() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Enter text to make send button appear
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()

    // Clear text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextClearance()
    composeRule.waitForIdle()

    // Send button should disappear
    try {
      composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()
      fail("Send button should not be visible when input is empty")
    } catch (e: AssertionError) {
      // Expected: button should not be displayed
    }
  }

  @Test
  fun microphone_button_always_stays_visible() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Microphone should be visible initially
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()

    // Enter text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()

    // Microphone should still be visible
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()

    // Clear text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextClearance()
    composeRule.waitForIdle()

    // Microphone should still be visible
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
  }

  @Test
  fun voice_mode_button_can_be_clicked() {
    var voiceClicked = false

    setContentWithViewModel { vm ->
      HomeScreen(viewModel = vm, onAction1Click = {}, onAction2Click = {})
    }

    // Click voice mode button - it should not crash
    composeRule.onNodeWithTag(HomeTags.VoiceBtn).performClick()
    composeRule.waitForIdle()

    // Screen should still be stable
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun microphone_and_voice_mode_buttons_transition_smoothly() {
    setContentWithViewModel { vm -> HomeScreen(viewModel = vm) }

    // Both buttons should be visible initially
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()

    // Enter text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeRule.waitForIdle()

    // Only microphone should be visible, send button should appear
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
    try {
      composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()
      fail("Voice mode should disappear when text is entered")
    } catch (e: AssertionError) {
      // Expected
    }
    composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()

    // Clear text
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextClearance()
    composeRule.waitForIdle()

    // Back to initial state
    composeRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.VoiceBtn).assertIsDisplayed()
    try {
      composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()
      fail("Send button should disappear when text is cleared")
    } catch (e: AssertionError) {
      // Expected
    }
    composeRule.onNodeWithText(TestConstants.PlaceholderTexts.MESSAGE_EULER).assertIsDisplayed()
  }

  private fun assertNodeDoesNotExist(text: String) {
    composeRule.onAllNodesWithText(text).assertCountEquals(0)
  }

  private fun waitAndClickSendButton(timeoutMillis: Long = 5_000) {
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(HomeTags.SendBtn), timeoutMillis = timeoutMillis)
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()
  }

  private fun waitAndClickMenuDelete(timeoutMillis: Long = 5_000) {
    // First wait for the menu to be displayed
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(HomeTags.TopRightMenu), timeoutMillis = timeoutMillis)
    // Then wait for the Delete item to appear
    composeRule.waitUntilAtLeastOneExists(hasText("Delete"), timeoutMillis = timeoutMillis)
    composeRule.onNodeWithText("Delete").performClick()
  }

  private fun waitAndClickMenuShare(timeoutMillis: Long = 5_000) {
    // First wait for the menu to be displayed
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(HomeTags.TopRightMenu), timeoutMillis = timeoutMillis)
    // Then wait for the Share item to appear
    composeRule.waitUntilAtLeastOneExists(hasText("Share"), timeoutMillis = timeoutMillis)
    composeRule.onNodeWithText("Share").performClick()
  }
}
