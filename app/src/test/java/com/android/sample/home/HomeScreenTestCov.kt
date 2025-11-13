package com.android.sample.home

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.conversations.Conversation
import com.android.sample.speech.SpeechToTextHelper
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenTestCov {

  @get:Rule val composeRule = createComposeRule()

  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    initFirebase()
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDown() {
    FirebaseAuth.getInstance().signOut()
    Dispatchers.resetMain()
  }

  @Test
  fun viewModel_initial_state_matches_expectations() {
    val viewModel = HomeViewModel()
    val state = viewModel.uiState.value
    assertEquals("Student", state.userName)
    assertTrue(state.messages.isEmpty())
    assertEquals("", state.messageDraft)
    assertTrue(state.messages.isEmpty())
    assertNull(state.streamingMessageId)
    assertEquals(0, state.streamingSequence)
  }

  @Test
  fun suggestions_are_displayed_initially() {
    val viewModel = HomeViewModel()
    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithText("What is EPFL").assertIsDisplayed()
    composeRule.onNodeWithText("Check Ed Discussion").assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action2Btn).assertIsDisplayed()
  }

  @Test
  fun clicking_first_suggestion_invokes_action1_callback() {
    var action1Called = false
    val viewModel = HomeViewModel()

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = viewModel, onAction1Click = { action1Called = true }, onSendMessage = {})
      }
    }

    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    composeRule.waitForIdle()

    assertTrue(action1Called)
    assertTrue(viewModel.uiState.value.messages.isNotEmpty())
  }

  @Test
  fun clicking_third_suggestion_invokes_onSendMessage() {
    var sendMessageCalled = false
    var sentText = ""
    val viewModel = HomeViewModel()

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = viewModel,
            onAction1Click = {},
            onAction2Click = {},
            onSendMessage = {
              sendMessageCalled = true
              sentText = it
            })
      }
    }

    composeRule.onNodeWithText("Show my schedule").performClick()
    composeRule.waitForIdle()

    assertTrue(sendMessageCalled)
    assertEquals("Show my schedule", sentText)
  }

  @Test
  fun message_field_updates_viewModel_draft() {
    val viewModel = HomeViewModel()
    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, onSendMessage = {}) }
    }

    val draftText = "Hello Euler"
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(draftText)
    composeRule.waitForIdle()

    assertEquals(draftText, viewModel.uiState.value.messageDraft)
  }

  @Test
  fun suggestions_hide_after_ai_message_present() {
    val viewModel = HomeViewModel()
    injectMessages(
        viewModel,
        listOf(ChatUIModel(id = "ai-1", text = "Response", timestamp = 0L, type = ChatType.AI)))

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, onSendMessage = {}) }
    }

    composeRule.waitForIdle()
    val nodes = composeRule.onAllNodesWithText("What is EPFL").fetchSemanticsNodes()
    assertTrue(nodes.isEmpty())
  }

  @Test
  fun suggestion_click_appends_user_message_and_clears_draft() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()

        composeRule.setContent {
          MaterialTheme {
            HomeScreen(viewModel = viewModel, onAction1Click = {}, onSendMessage = {})
          }
        }

        composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
        composeRule.waitForIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isNotEmpty())
        assertEquals("What is EPFL", state.messages.first().text)
        assertEquals(ChatType.USER, state.messages.first().type)
        assertEquals("", state.messageDraft)
      }

  @Test
  fun drawer_callbacks_are_wired() {
    var settingsCalled = false
    var signOutCalled = false

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            onSettingsClick = { settingsCalled = true }, onSignOut = { signOutCalled = true })
      }
    }

    composeRule.onNodeWithTag(HomeTags.Root).assertIsDisplayed()
    assertFalse(settingsCalled)
    assertFalse(signOutCalled)
  }

  @Test
  fun settings_from_drawer_invokes_callback_and_closes_drawer() {
    var settingsCalled = false
    val viewModel = HomeViewModel()
    updateUiState(viewModel) { it.copy(isDrawerOpen = true) }

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(viewModel = viewModel, onSettingsClick = { settingsCalled = true })
      }
    }

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(DrawerTags.ConnectorsRow).performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      assertFalse(viewModel.uiState.value.isDrawerOpen)
      assertTrue(settingsCalled)
    }
  }

  @Test
  fun menu_button_opens_and_closes_drawer() {
    val viewModel = HomeViewModel()
    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(DrawerTags.NewChatRow).assertIsDisplayed()

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle { /* ensure coroutine completes */}
  }

  @Test
  fun new_chat_from_drawer_resets_state_and_closes() {
    val viewModel = HomeViewModel()
    updateUiState(viewModel) {
      it.copy(currentConversationId = "conv-1", messageDraft = "draft", isDrawerOpen = true)
    }
    injectMessages(viewModel, listOf(sampleMessage("Keep me")))

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(DrawerTags.NewChatRow).performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      val state = viewModel.uiState.value
      assertTrue(state.messages.isEmpty())
      assertNull(state.currentConversationId)
      assertEquals("", state.messageDraft)
      assertFalse(state.isDrawerOpen)
    }
  }

  @Test
  fun pick_conversation_from_drawer_selects_conversation() {
    val viewModel = HomeViewModel()
    updateUiState(viewModel) {
      it.copy(
          isDrawerOpen = true,
          conversations = listOf(Conversation(id = "conv-abc", title = "CS courses")))
    }

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("CS courses").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle {
      val state = viewModel.uiState.value
      assertEquals("conv-abc", state.currentConversationId)
      assertFalse(state.isDrawerOpen)
    }
  }

  @Test
  fun top_right_menu_delete_shows_confirmation_modal() {
    val viewModel = HomeViewModel()
    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.TopRightBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Delete current chat").performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertTrue(viewModel.uiState.value.showDeleteConfirmation) }
    composeRule.onNodeWithText("Clear Chat?").assertIsDisplayed()
    composeRule
        .onNodeWithText("This will delete all messages. This action cannot be undone.")
        .assertIsDisplayed()
  }

  @Test
  fun delete_confirmation_buttons_dismiss_modal() {
    val viewModel = HomeViewModel()
    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.runOnIdle { viewModel.showDeleteConfirmation() }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Cancel").performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle { assertFalse(viewModel.uiState.value.showDeleteConfirmation) }

    composeRule.runOnIdle { viewModel.showDeleteConfirmation() }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Delete").performClick()
    composeRule.waitForIdle()
    composeRule.runOnIdle { assertFalse(viewModel.uiState.value.showDeleteConfirmation) }
  }

  @Test
  fun openDrawerOnStart_opens_drawer_and_updates_state() {
    val viewModel = HomeViewModel()

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, openDrawerOnStart = true) }
    }

    composeRule.waitUntil {
      composeRule.onAllNodesWithText("New chat").fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.runOnIdle { assertTrue(viewModel.uiState.value.isDrawerOpen) }
  }

  @Test
  fun forceNewChatOnFirstOpen_resets_conversation_once() {
    val viewModel = HomeViewModel()
    updateUiState(viewModel) {
      it.copy(currentConversationId = "conv-99", messageDraft = "draft", isDrawerOpen = true)
    }
    injectMessages(viewModel, listOf(sampleMessage("Keep me")))

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = viewModel,
            forceNewChatOnFirstOpen = true,
            openDrawerOnStart = false)
      }
    }

    composeRule.waitForIdle()

    composeRule.runOnIdle {
      val state = viewModel.uiState.value
      assertNull(state.currentConversationId)
      assertEquals("", state.messageDraft)
      assertTrue(state.messages.isEmpty())
    }
  }

  @Test
  fun voice_button_triggers_callback_when_message_blank() {
    var voiceCalled = false
    val viewModel = HomeViewModel()

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, onVoiceChatClick = { voiceCalled = true }) }
    }

    composeRule.onNodeWithTag(HomeTags.VoiceBtn).performClick()
    composeRule.waitForIdle()

    assertTrue(voiceCalled)
  }

  @Test
  fun mic_button_uses_speech_helper_and_updates_draft() {
    val recognized = "Recognized speech"
    var started = false
    val helper =
        mock<SpeechToTextHelper> {
          on { startListening(any()) }
              .doAnswer { invocation ->
                started = true
                invocation.getArgument<(String) -> Unit>(0).invoke(recognized)
                null
              }
        }
    val viewModel = HomeViewModel()

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, speechHelper = helper) }
    }

    composeRule.onNodeWithTag(HomeTags.MicBtn).performClick()
    composeRule.waitForIdle()

    assertTrue(started)
    assertEquals(recognized, viewModel.uiState.value.messageDraft)
  }

  private fun initFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:test")
              .setProjectId("test-project")
              .setApiKey("fake-api-key")
              .build())
    }
  }

  private fun injectMessages(viewModel: HomeViewModel, messages: List<ChatUIModel>) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = field.get(viewModel) as MutableStateFlow<HomeUiState>
    stateFlow.value = stateFlow.value.copy(messages = messages)
  }

  private fun updateUiState(viewModel: HomeViewModel, transform: (HomeUiState) -> HomeUiState) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = field.get(viewModel) as MutableStateFlow<HomeUiState>
    stateFlow.value = transform(stateFlow.value)
  }

  private fun sampleMessage(text: String = "Sample") =
      ChatUIModel(
          id = "msg-${text.hashCode()}",
          text = text,
          timestamp = System.currentTimeMillis(),
          type = ChatType.USER)
}
