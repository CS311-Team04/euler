package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.llm.FakeLlmClient
import com.android.sample.util.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelStopRetryTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()
  private val testDispatcher
    get() = dispatcherRule.dispatcher

  private fun HomeViewModel.updateUiState(transform: (HomeUiState) -> HomeUiState) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    stateFlow.value = transform(stateFlow.value)
  }

  private fun HomeViewModel.setPrivateField(name: String, value: Any?) {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  private suspend fun HomeViewModel.awaitStreamingCompletion(timeoutMs: Long = 2_000L) {
    var remaining = timeoutMs
    while ((uiState.value.streamingMessageId != null || uiState.value.isSending) && remaining > 0) {
      dispatcherRule.dispatcher.scheduler.advanceUntilIdle()
      kotlinx.coroutines.delay(20)
      remaining -= 20
    }
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()
  }

  @Before
  fun setUpFirebase() {
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
  fun tearDownFirebase() {
    FirebaseAuth.getInstance().signOut()
  }

  // ============ STOP GENERATION TESTS ============

  @Test
  fun stopGeneration_cancels_stream_and_preserves_text() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        val streamingId = "ai-streaming-1"
        val job = Job()
        viewModel.setPrivateField("activeStreamJob", job)

        viewModel.updateUiState {
          it.copy(
              messages =
                  listOf(
                      ChatUIModel(
                          id = "user-1", text = "Hello", timestamp = 0L, type = ChatType.USER),
                      ChatUIModel(
                          id = streamingId,
                          text = "Partial response",
                          timestamp = 0L,
                          type = ChatType.AI,
                          isThinking = true)),
              streamingMessageId = streamingId,
              isSending = true)
        }

        viewModel.stopGeneration()

        assertTrue("Stream job should be cancelled", job.isCancelled)
        val state = viewModel.uiState.value
        val aiMessage = state.messages.firstOrNull { it.id == streamingId }
        assertEquals("Partial text should be preserved", "Partial response", aiMessage!!.text)
        assertTrue("Message should be marked as stopped", aiMessage.wasStopped)
        assertNull("Streaming should be cleared", state.streamingMessageId)
        assertFalse("Should not be sending", state.isSending)
      }

  @Test
  fun stopGeneration_sets_fallback_when_empty() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        val streamingId = "ai-streaming-1"

        viewModel.updateUiState {
          it.copy(
              messages =
                  listOf(
                      ChatUIModel(
                          id = "user-1", text = "Hello", timestamp = 0L, type = ChatType.USER),
                      ChatUIModel(
                          id = streamingId,
                          text = "",
                          timestamp = 0L,
                          type = ChatType.AI,
                          isThinking = true)),
              streamingMessageId = streamingId,
              isSending = true)
        }

        viewModel.stopGeneration()

        val state = viewModel.uiState.value
        val aiMessage = state.messages.firstOrNull { it.id == streamingId }
        assertEquals("Should set fallback text", "Generation stopped", aiMessage!!.text)
      }

  // ============ RETRY TESTS ============

  @Test
  fun retryLastMessage_removes_last_ai_and_restarts_generation() =
      runTest(testDispatcher) {
        val fakeClient = FakeLlmClient().apply { nextReply = "New response" }
        val viewModel = HomeViewModel(fakeClient)
        viewModel.setPrivateField("lastUserPrompt", "Test question")

        viewModel.updateUiState {
          it.copy(
              messages =
                  listOf(
                      ChatUIModel(
                          id = "user-1",
                          text = "Test question",
                          timestamp = 0L,
                          type = ChatType.USER),
                      ChatUIModel(
                          id = "ai-1",
                          text = "Old response",
                          timestamp = 1000L,
                          type = ChatType.AI)))
        }

        viewModel.retryLastMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Should have streaming ID", state.streamingMessageId)
        assertTrue("Should be sending", state.isSending)
        val oldAiRemoved = !state.messages.any { it.text == "Old response" }
        assertTrue("Old AI message should be removed", oldAiRemoved)
      }

  @Test
  fun retryLastMessage_works_in_guest_mode() =
      runTest(testDispatcher) {
        val fakeClient = FakeLlmClient().apply { nextReply = "Retry response" }
        val viewModel = HomeViewModel(fakeClient)
        viewModel.setPrivateField("lastUserPrompt", "Retry this")

        viewModel.updateUiState {
          it.copy(
              messages =
                  listOf(
                      ChatUIModel(
                          id = "user-1", text = "Retry this", timestamp = 0L, type = ChatType.USER),
                      ChatUIModel(
                          id = "ai-1",
                          text = "Old response",
                          timestamp = 1000L,
                          type = ChatType.AI)))
        }

        viewModel.retryLastMessage()
        advanceUntilIdle()
        awaitStreamingCompletion()

        assertTrue("LLM should have been called", fakeClient.prompts.isNotEmpty())
        assertEquals("Should use last prompt", "Retry this", fakeClient.prompts.last())
      }

  @Test
  fun retryLastMessage_when_no_prompt_does_nothing() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        val initialState = viewModel.uiState.value

        viewModel.retryLastMessage()

        val state = viewModel.uiState.value
        assertEquals("Messages unchanged", initialState.messages.size, state.messages.size)
        assertFalse("Should not be sending", state.isSending)
      }

  // ============ INTEGRATION TEST ============

  @Test
  fun stop_then_retry_workflow() =
      runTest(testDispatcher) {
        val fakeClient = FakeLlmClient().apply { nextReply = "Complete response" }
        val viewModel = HomeViewModel(fakeClient)
        viewModel.setPrivateField("lastUserPrompt", "Test question")

        // Start generation
        viewModel.updateMessageDraft("Test question")
        viewModel.sendMessage()
        advanceUntilIdle()

        // Simulate partial streaming
        val streamingId = viewModel.uiState.value.streamingMessageId
        assertNotNull("Should have streaming ID", streamingId)
        viewModel.updateUiState { state ->
          state.copy(
              messages =
                  state.messages.map { msg ->
                    if (msg.id == streamingId) {
                      msg.copy(text = "Partial response", isThinking = false)
                    } else {
                      msg
                    }
                  })
        }

        // Stop generation
        viewModel.stopGeneration()
        advanceUntilIdle()

        val afterStop = viewModel.uiState.value
        assertNull("Streaming should be stopped", afterStop.streamingMessageId)
        val stoppedMessage = afterStop.messages.firstOrNull { it.wasStopped }
        assertNotNull("Should have stopped message", stoppedMessage)

        // Retry
        viewModel.retryLastMessage()
        advanceUntilIdle()
        awaitStreamingCompletion()

        val afterRetry = viewModel.uiState.value
        assertTrue("Should have new response", fakeClient.prompts.size >= 2)
        val newAiMessage =
            afterRetry.messages.lastOrNull { it.type == ChatType.AI && it.text.isNotBlank() }
        assertNotNull("Should have new AI message", newAiMessage)
        assertFalse("New message should not be stopped", newAiMessage!!.wasStopped)
      }
}
