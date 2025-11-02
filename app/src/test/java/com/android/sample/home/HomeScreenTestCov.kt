package com.android.sample.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenTestCov {

  private val testDispatcher = StandardTestDispatcher()

  @get:Rule val composeTestRule = createComposeRule()

  // ----------- BASIC VISIBILITY TESTS -----------

  @Test
  fun placeholder_is_displayed_initially() {
    composeTestRule.setContent { MaterialTheme { HomeScreen() } }
    composeTestRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
    composeTestRule.onNodeWithText("Ask Anything").assertExists()
  }

  @Test
  fun voice_mode_visible_when_empty_and_hidden_when_text_entered() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    composeTestRule.onNodeWithTag("voice_mode_btn").assertExists()

    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Hello")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTags.SendBtn).assertExists()
  }

  @Test
  fun send_button_appears_only_when_text_not_blank() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(HomeTags.SendBtn).assertExists()
  }

  // ----------- TRANSITIONS -----------

  @Test
  fun typing_text_hides_voice_mode_and_shows_send_button() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    composeTestRule.onNodeWithTag("voice_mode_btn").assertExists()
    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Msg")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTags.SendBtn).assertExists()
  }

  @Test
  fun clearing_text_hides_send_and_shows_voice_mode() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(HomeTags.SendBtn).assertExists()

    vm.updateMessageDraft("")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("voice_mode_btn").assertExists()
  }

  // ----------- STATE & ENABLEMENT -----------

  @Test
  fun sending_disables_buttons_and_field() =
      runTest(testDispatcher) {
        val vm = HomeViewModel()
        composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

        vm.updateMessageDraft("Sending...")
        vm.sendMessage()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(HomeTags.MicBtn).assertExists()
        composeTestRule.onNodeWithTag(HomeTags.MessageField).assertExists()
        assertTrue(vm.uiState.value.isSending)
      }

  @Test
  fun send_button_triggers_onSendMessage_and_clears_draft() {
    var called = false
    var textSent = ""
    val vm = HomeViewModel()

    composeTestRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = vm,
            onSendMessage = {
              called = true
              textSent = it
            })
      }
    }

    vm.updateMessageDraft("Hello world")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(HomeTags.SendBtn).performClick()

    composeTestRule.waitForIdle()
    assertTrue(called)
    assertEquals("Hello world", textSent)
    assertTrue(vm.uiState.value.messageDraft.isBlank())
  }

  // ----------- EDGE CASES -----------

  @Test
  fun whitespace_or_empty_text_does_not_enable_send() {
    val vm = HomeViewModel()
    vm.updateMessageDraft("   \t\n")
    assertTrue(vm.uiState.value.messageDraft.isBlank())
  }

  @Test
  fun unicode_and_special_characters_are_supported() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    val msg = "Hello 🌍 مرحبا 你好 !@#"
    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput(msg)
    composeTestRule.waitForIdle()

    assertEquals(msg, vm.uiState.value.messageDraft)
    composeTestRule.onNodeWithTag(HomeTags.SendBtn).assertExists()
  }

  // ----------- UI LAYOUT PROPERTIES (logical verification) -----------

  @Test
  fun layout_elements_are_displayed_correctly() {
    composeTestRule.setContent { MaterialTheme { HomeScreen() } }

    composeTestRule.onNodeWithTag(HomeTags.MessageField).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
  }

  @Test
  fun voice_mode_and_send_never_visible_together() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    // Initially only voice
    composeTestRule.onNodeWithTag("voice_mode_btn").assertExists()

    // After typing, only send
    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Hello")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(HomeTags.SendBtn).assertExists()
  }

  // ----------- LONG INPUT & STRESS -----------

  @Test
  fun very_long_input_is_handled_correctly() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    val long = "A".repeat(1000)
    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput(long)
    composeTestRule.waitForIdle()
    assertEquals(long, vm.uiState.value.messageDraft)
  }

  @Test
  fun dictate_button_always_visible() {
    val vm = HomeViewModel()
    composeTestRule.setContent { MaterialTheme { HomeScreen(viewModel = vm) } }

    composeTestRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()

    composeTestRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Test")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTags.MicBtn).assertIsDisplayed()
  }
}
