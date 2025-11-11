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
}
