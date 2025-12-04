package com.android.sample.home

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.llm.FakeLlmClient
import com.android.sample.util.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenStopRetryTest {

  @get:Rule val composeRule = createComposeRule()

  @get:Rule val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  @Before
  fun setUp() {
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
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDownMocks() {
    unmockkAll()
    FirebaseAuth.getInstance().signOut()
  }

  private fun createViewModel(): HomeViewModel = HomeViewModel()

  private fun HomeViewModel.editState(transform: (HomeUiState) -> HomeUiState) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    val flow = field.get(this) as MutableStateFlow<HomeUiState>
    flow.value = transform(flow.value)
  }

  private fun HomeViewModel.setPrivateField(name: String, value: Any?) {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  // ============ STOP BUTTON UI TESTS ============

  @Test
  fun stop_button_appears_during_generation_and_stops_when_clicked() = runTest {
    val viewModel = createViewModel()
    val streamingId = "ai-streaming-1"
    viewModel.editState {
      it.copy(
          streamingMessageId = streamingId,
          isSending = true,
          messages =
              listOf(
                  ChatUIModel(id = "user-1", text = "Hello", timestamp = 0L, type = ChatType.USER),
                  ChatUIModel(
                      id = streamingId,
                      text = "Partial",
                      timestamp = 0L,
                      type = ChatType.AI,
                      isThinking = true)))
    }

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.StopBtn).assertIsDisplayed().performClick()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull("Streaming should be stopped", state.streamingMessageId)
    assertFalse("Should not be sending", state.isSending)
  }

  @Test
  fun stop_button_replaces_send_button_during_generation() = runTest {
    val viewModel = createViewModel()

    // Start with send button
    viewModel.editState {
      it.copy(
          streamingMessageId = null,
          isSending = false,
          messageDraft = "Test",
          messages = emptyList())
    }

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag(HomeTags.SendBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.StopBtn).assertDoesNotExist()

    // Start generation
    viewModel.editState {
      it.copy(
          streamingMessageId = "ai-streaming-1",
          isSending = true,
          messageDraft = "",
          messages =
              listOf(
                  ChatUIModel(id = "user-1", text = "Test", timestamp = 0L, type = ChatType.USER),
                  ChatUIModel(
                      id = "ai-streaming-1",
                      text = "",
                      timestamp = 0L,
                      type = ChatType.AI,
                      isThinking = true)))
    }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag(HomeTags.StopBtn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.SendBtn).assertDoesNotExist()
  }

  // ============ RETRY BUTTON UI TESTS ============

  @Test
  fun retry_button_appears_on_last_ai_message_and_retries_when_clicked() = runTest {
    val fakeClient = FakeLlmClient().apply { nextReply = "New response" }
    val viewModel = HomeViewModel(fakeClient)
    viewModel.setPrivateField("lastUserPrompt", "Test question")
    viewModel.editState {
      it.copy(
          messages =
              listOf(
                  ChatUIModel(
                      id = "user-1", text = "Test question", timestamp = 0L, type = ChatType.USER),
                  ChatUIModel(
                      id = "ai-1", text = "Old response", timestamp = 1000L, type = ChatType.AI)),
          streamingMessageId = null)
    }

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag("chat_retry_btn").assertIsDisplayed().performClick()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    val hasPlaceholder = state.messages.any { it.isThinking }
    assertTrue("Should have placeholder for new response", hasPlaceholder)
    assertNotNull("Should have streaming ID", state.streamingMessageId)
  }

  @Test
  fun retry_button_not_displayed_during_streaming() = runTest {
    val viewModel = createViewModel()
    val streamingId = "ai-streaming-1"
    viewModel.editState {
      it.copy(
          messages =
              listOf(
                  ChatUIModel(id = "user-1", text = "Hello", timestamp = 0L, type = ChatType.USER),
                  ChatUIModel(
                      id = streamingId,
                      text = "",
                      timestamp = 0L,
                      type = ChatType.AI,
                      isThinking = true)),
          streamingMessageId = streamingId)
    }

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    composeRule.onNodeWithTag("chat_retry_btn").assertDoesNotExist()
  }

  // ============ INTEGRATION UI TEST ============

  @Test
  fun stop_then_retry_workflow_in_ui() = runTest {
    val fakeClient = FakeLlmClient().apply { nextReply = "Complete response" }
    val viewModel = HomeViewModel(fakeClient)
    viewModel.setPrivateField("lastUserPrompt", "Test question")

    // Start generation
    viewModel.editState {
      it.copy(
          streamingMessageId = "ai-streaming-1",
          isSending = true,
          messageDraft = "",
          messages =
              listOf(
                  ChatUIModel(
                      id = "user-1", text = "Test question", timestamp = 0L, type = ChatType.USER),
                  ChatUIModel(
                      id = "ai-streaming-1",
                      text = "Partial",
                      timestamp = 0L,
                      type = ChatType.AI,
                      isThinking = true)))
    }

    composeRule.setContent { MaterialTheme { HomeScreen(viewModel = viewModel) } }

    // Click stop
    composeRule.onNodeWithTag(HomeTags.StopBtn).assertIsDisplayed().performClick()
    advanceUntilIdle()

    // Verify retry appears
    composeRule.onNodeWithTag(HomeTags.StopBtn).assertDoesNotExist()
    composeRule.onNodeWithTag("chat_retry_btn").assertIsDisplayed()

    // Click retry
    composeRule.onNodeWithTag("chat_retry_btn").performClick()
    advanceUntilIdle()

    // Verify new generation started
    val state = viewModel.uiState.value
    assertNotNull("Should have new streaming ID", state.streamingMessageId)
    assertTrue("Should be sending", state.isSending)
  }
}
