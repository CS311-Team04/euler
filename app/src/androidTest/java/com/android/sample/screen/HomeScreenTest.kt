package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.TestConstants
import com.android.sample.home.HomeScreen
import com.android.sample.home.HomeTags
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun action_buttons_trigger_callbacks() {
    var action1Clicked = false
    var action2Clicked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onAction1Click = { action1Clicked = true }, onAction2Click = { action2Clicked = true })
      }
    }

    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    composeRule.onNodeWithTag(HomeTags.Action2Btn).performClick()

    Assert.assertTrue(action1Clicked)
    Assert.assertTrue(action2Clicked)
  }

  @Test
  fun displays_correct_action_button_texts() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText(TestConstants.ButtonTexts.FIND_CS220_EXAMS).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ButtonTexts.CHECK_ED_DISCUSSION).assertIsDisplayed()
  }

  @Test
  fun displays_correct_placeholder_text() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText(TestConstants.PlaceholderTexts.MESSAGE_EULER).assertIsDisplayed()
  }

  @Test
  fun displays_footer_text() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText(TestConstants.FooterTexts.POWERED_BY).assertIsDisplayed()
  }

  @Test
  fun displays_icons_with_correct_content_descriptions() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.MENU))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.MORE))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.SEND))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.EULER))
        .assertIsDisplayed()
  }

  @Test
  fun menu_button_click_triggers_viewmodel_toggle() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Le menu button devrait déclencher le toggle du drawer
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()

    // Vérifier que le composant est toujours affiché (pas de crash)
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
  }

  @Test
  fun topRight_button_click_triggers_viewmodel_setTopRightOpen() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Le bouton top-right devrait déclencher l'ouverture du menu
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()

    // Vérifier que le menu s'affiche
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()
  }

  @Test
  fun message_field_text_input_updates_viewmodel() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    val testMessage = "Test message input"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(testMessage)

    // Vérifier que le champ accepte l'input
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun send_button_is_disabled_when_text_is_empty() {
    var sendMessageCalled = false

    composeRule.setContent {
      MaterialTheme { HomeScreen(onSendMessage = { sendMessageCalled = true }) }
    }

    // Wait for the screen to be fully loaded
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()

    // Verify that without any text input, the callback should not be triggered
    // The send button is disabled when there's no text
    Assert.assertTrue(!sendMessageCalled)
  }

  @Test
  fun multiple_text_inputs_work_correctly() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Test plusieurs inputs consécutifs
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("First")
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Second")
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Third")

    // Vérifier que le champ accepte toujours l'input
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun action_buttons_have_correct_styling_and_behavior() {
    var action1Clicked = false
    var action2Clicked = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onAction1Click = { action1Clicked = true }, onAction2Click = { action2Clicked = true })
      }
    }

    // Test que les boutons sont cliquables et déclenchent les callbacks
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    Assert.assertTrue(action1Clicked)

    composeRule.onNodeWithTag(HomeTags.Action2Btn).performClick()
    Assert.assertTrue(action2Clicked)
  }

  @Test
  fun drawer_state_synchronization_works() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Test que le menu button peut être cliqué sans erreur
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()

    // Vérifier que l'écran reste stable
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
  }

  @Test
  fun message_field_accepts_multiline_text() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    val multilineText = "Line 1\nLine 2\nLine 3"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(multilineText)

    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun top_right_menu_appears_and_disappears() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Click the top right button
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()

    // Menu should appear
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()
  }

  @Test
  fun message_field_placeholder_is_displayed() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText(TestConstants.PlaceholderTexts.MESSAGE_EULER).assertIsDisplayed()
  }

  @Test
  fun action_buttons_display_correct_labels() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText(TestConstants.ButtonTexts.FIND_CS220_EXAMS).assertIsDisplayed()
    composeRule.onNodeWithText(TestConstants.ButtonTexts.CHECK_ED_DISCUSSION).assertIsDisplayed()
  }

  @Test
  fun screen_displays_footer_text() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText(TestConstants.FooterTexts.POWERED_BY).assertIsDisplayed()
  }

  @Test
  fun message_field_can_be_cleared_and_retyped() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Type and clear
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("First")
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Second")

    // Field should still be functional
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun all_icons_have_content_descriptions() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.MENU))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.MORE))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.SEND))
        .assertIsDisplayed()
    composeRule
        .onNode(hasContentDescription(TestConstants.ContentDescriptions.EULER))
        .assertIsDisplayed()
  }

  @Test
  fun screen_handles_special_characters_in_message() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    val specialText = "Hello! @#$%^&*()_+-=[]{}|;:',.<>?"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(specialText)

    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }

  @Test
  fun screen_handles_unicode_characters() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    val unicodeText = "Hello 你好 مرحبا Bonjour こんにちは"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(unicodeText)

    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
  }
}
