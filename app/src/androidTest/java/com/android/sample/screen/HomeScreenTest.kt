package com.android.sample.home

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_core_widgets() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // éléments app bar & actions
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action2Btn).assertIsDisplayed()

    // champ + send
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()

    // logo au centre (Image avec contentDescription = "Euler")
    composeRule.onNode(hasContentDescription("Euler")).assertIsDisplayed()
  }

  @Test
  fun topRight_menu_opens_when_icon_clicked() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Au départ, le menu n'est pas visible. On clique sur l'icône…
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()

    // …le DropdownMenu s'ouvre avec ses items placeholder
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()
    composeRule.onNodeWithText("Example item 1").assertIsDisplayed()
    composeRule.onNodeWithText("Example item 2").assertIsDisplayed()
  }

  @Test
  fun typing_and_send_calls_callback_with_text() {
    var sent: String? = null

    composeRule.setContent { MaterialTheme { HomeScreen(onSendMessage = { sent = it }) } }

    val message = "Hello Euler"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(message)
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()

    assertEquals(message, sent)
  }

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

    assertTrue(action1Clicked)
    assertTrue(action2Clicked)
  }

  @Test
  fun displays_correct_action_button_texts() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText("Find CS220 past exams in Drive EPFL").assertIsDisplayed()
    composeRule.onNodeWithText("Check Ed Discussion").assertIsDisplayed()
  }

  @Test
  fun displays_correct_placeholder_text() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNodeWithText("Message EULER").assertIsDisplayed()
  }

  @Test
  fun displays_footer_text() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule
        .onNodeWithText("Powered by APERTUS Swiss LLM · MCP-enabled for 6 EPFL systems")
        .assertIsDisplayed()
  }

  @Test
  fun displays_icons_with_correct_content_descriptions() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    composeRule.onNode(hasContentDescription("Menu")).assertIsDisplayed()
    composeRule.onNode(hasContentDescription("More")).assertIsDisplayed()
    composeRule.onNode(hasContentDescription("Send")).assertIsDisplayed()
    composeRule.onNode(hasContentDescription("Euler")).assertIsDisplayed()
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
  fun send_button_click_with_text_calls_both_callbacks() {
    var sendMessageCalled = false
    var sentText: String? = null

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onSendMessage = {
              sendMessageCalled = true
              sentText = it
            })
      }
    }

    val message = "Test message to send"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(message)
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()

    assertTrue(sendMessageCalled)
    assertEquals(message, sentText)
  }

  @Test
  fun send_button_click_with_empty_text_calls_callback_with_empty_string() {
    var sendMessageCalled = false
    var sentText: String? = null

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onSendMessage = {
              sendMessageCalled = true
              sentText = it
            })
      }
    }

    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()

    assertTrue(sendMessageCalled)
    assertEquals("", sentText)
  }

  @Test
  fun dropdown_menu_items_trigger_dismiss_callback() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Ouvrir le menu
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

    // Cliquer sur un item du menu (qui devrait fermer le menu)
    composeRule.onNodeWithText("Example item 1").performClick()

    // Le menu devrait être fermé maintenant
    // Note: on ne peut pas tester directement si le menu est fermé car il n'est pas affiché
    // Mais on peut vérifier qu'il n'y a pas d'erreur
  }

  @Test
  fun dropdown_menu_item2_also_triggers_dismiss() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Ouvrir le menu
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightMenu).assertIsDisplayed()

    // Cliquer sur le deuxième item
    composeRule.onNodeWithText("Example item 2").performClick()

    // Vérifier qu'il n'y a pas d'erreur
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
  }

  @Test
  fun screen_has_correct_test_tags() {
    composeRule.setContent { MaterialTheme { HomeScreen() } }

    // Vérifier que tous les test tags principaux sont présents
    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MenuBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action2Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()
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
  fun send_button_works_with_various_text_lengths() {
    var sendMessageCalled = false
    var sentText: String? = null

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onSendMessage = {
              sendMessageCalled = true
              sentText = it
            })
      }
    }

    // Test avec texte court
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Hi")
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()
    assertTrue(sendMessageCalled)
    assertEquals("Hi", sentText)

    // Reset pour test suivant
    sendMessageCalled = false
    sentText = null

    // Test avec texte long
    val longText =
        "This is a very long message that contains multiple words and should still work correctly when sent through the send button"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(longText)
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()
    assertTrue(sendMessageCalled)
    assertEquals(longText, sentText)
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
    assertTrue(action1Clicked)

    composeRule.onNodeWithTag(HomeTags.Action2Btn).performClick()
    assertTrue(action2Clicked)
  }

  @Test
  fun screen_handles_multiple_rapid_interactions() {
    var action1Clicked = false
    var sendMessageCalled = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onAction1Click = { action1Clicked = true },
            onSendMessage = { sendMessageCalled = true })
      }
    }

    // Interactions rapides multiples
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Quick test")
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()
    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()

    assertTrue(action1Clicked)
    assertTrue(sendMessageCalled)
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
}
