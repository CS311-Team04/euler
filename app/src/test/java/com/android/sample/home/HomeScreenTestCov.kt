package com.android.sample.home

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.conversations.Conversation
import com.android.sample.llm.FakeLlmClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  fun suggestions_are_displayed_initially() {
    val viewModel = HomeViewModel(FakeLlmClient())

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithText("What is EPFL").assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action2Btn).assertIsDisplayed()
  }

  @Test
  fun menu_button_opens_and_closes_drawer() {
    val viewModel = HomeViewModel(FakeLlmClient())

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("New chat").assertIsDisplayed()
  }

  @Test
  fun new_chat_from_drawer_resets_state_and_closes() {
    val viewModel = HomeViewModel(FakeLlmClient())
    updateUiState(viewModel) {
      it.copy(currentConversationId = "conv-1", messageDraft = "draft", isDrawerOpen = true)
    }
    injectMessages(viewModel, listOf(sampleMessage("Keep me")))

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.MenuBtn).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(DrawerTags.NewChatRow).performClick()
    composeRule.waitForIdle()

    val state = viewModel.uiState.value
    assertTrue(state.messages.isEmpty())
    assertEquals(null, state.currentConversationId)
    assertEquals("", state.messageDraft)
    assertFalse(state.isDrawerOpen)
  }

  @Test
  fun settings_from_drawer_invokes_callback() {
    var settingsCalled = false
    val viewModel = HomeViewModel(FakeLlmClient())
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

    assertTrue(settingsCalled)
    assertFalse(viewModel.uiState.value.isDrawerOpen)
  }

  @Test
  fun voice_button_triggers_callback_when_visible() {
    var voiceCalled = false
    val viewModel = HomeViewModel(FakeLlmClient())

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, onVoiceChatClick = { voiceCalled = true }) }
    }

    composeRule.onNodeWithTag(HomeTags.VoiceBtn).performClick()
    composeRule.waitForIdle()

    assertTrue(voiceCalled)
  }

  @Test
  fun send_message_updates_ui_state() {
    val fakeClient = FakeLlmClient().apply { nextReply = "Reply" }
    val viewModel = HomeViewModel(fakeClient)

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput("Hello")
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()
    composeRule.waitForIdle()

    val messages = viewModel.uiState.value.messages
    assertTrue(messages.any { it.type == ChatType.USER && it.text == "Hello" })
  }

  @Test
  fun openDrawerOnStart_opens_drawer_and_updates_state() {
    val viewModel = HomeViewModel(FakeLlmClient())

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, openDrawerOnStart = true) }
    }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag(DrawerTags.Root).fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithTag(DrawerTags.Root).assertIsDisplayed()
    assertTrue(viewModel.uiState.value.isDrawerOpen)
  }

  @Test
  fun suggestion_chip_click_triggers_callbacks_and_sends_message() {
    var action1Called = false
    val sentMessages = mutableListOf<String>()
    val fakeClient = FakeLlmClient().apply { nextReply = "Sure" }
    val viewModel = HomeViewModel(fakeClient)

    composeRule.setContent {
      MaterialTheme {
        HomeScreen(
            viewModel = viewModel,
            onAction1Click = { action1Called = true },
            onSendMessage = { sentMessages += it })
      }
    }

    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed().performClick()
    composeRule.waitForIdle()

    assertTrue(action1Called)
    assertTrue(sentMessages.contains("What is EPFL"))
    assertTrue(
        viewModel.uiState.value.messages.any {
          it.type == ChatType.USER && it.text == "What is EPFL"
        })
  }

  @Test
  fun pick_conversation_from_drawer_selects_and_closes() {
    val viewModel = HomeViewModel(FakeLlmClient())
    updateUiState(viewModel) {
      it.copy(
          conversations = listOf(Conversation(id = "remote-1", title = "Linear Algebra")),
          currentConversationId = null)
    }

    composeRule.setContent {
      MaterialTheme { HomeScreen(viewModel = viewModel, openDrawerOnStart = true) }
    }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithText("Linear Algebra").fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithText("Linear Algebra").assertIsDisplayed().performClick()
    composeRule.waitForIdle()

    assertEquals("remote-1", viewModel.uiState.value.currentConversationId)
    assertFalse(viewModel.uiState.value.isDrawerOpen)
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

  private fun sampleMessage(text: String) =
      ChatUIModel(
          id = "msg-${text.hashCode()}",
          text = text,
          timestamp = System.currentTimeMillis(),
          type = ChatType.USER)
}
