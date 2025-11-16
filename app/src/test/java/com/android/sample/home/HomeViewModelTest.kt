package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.conversations.Conversation
import com.android.sample.conversations.ConversationRepository
import com.android.sample.conversations.MessageDTO
import com.android.sample.llm.BotReply
import com.android.sample.llm.FakeLlmClient
import com.android.sample.llm.LlmClient
import com.android.sample.util.MainDispatcherRule
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  @get:Rule val dispatcherRule = MainDispatcherRule()
  private val testDispatcher
    get() = dispatcherRule.dispatcher

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

  @Test
  fun toggleDrawer_toggles_flag() {
    val viewModel = HomeViewModel(FakeLlmClient())
    assertFalse(viewModel.uiState.value.isDrawerOpen)

    viewModel.toggleDrawer()
    assertTrue(viewModel.uiState.value.isDrawerOpen)

    viewModel.toggleDrawer()
    assertFalse(viewModel.uiState.value.isDrawerOpen)
  }

  @Test
  fun sendMessage_guest_appends_user_and_ai_messages() = runBlocking {
    val fakeClient = FakeLlmClient().apply { nextReply = "Bonjour" }
    val viewModel = HomeViewModel(fakeClient)

    viewModel.updateMessageDraft("Salut ?")
    viewModel.sendMessage()
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    viewModel.awaitStreamingCompletion()

    val messages = viewModel.uiState.value.messages
    assertTrue(messages.any { it.type == ChatType.USER && it.text == "Salut ?" })
    assertTrue(messages.any { it.type == ChatType.AI && it.text.contains("Bonjour") })
    assertFalse(viewModel.uiState.value.isSending)
    assertNull(viewModel.uiState.value.streamingMessageId)
  }

  @Test
  fun sendMessage_guest_appends_source_card_when_llm_returns_url() = runBlocking {
    val viewModel =
        HomeViewModel(
            object : LlmClient {
              override suspend fun generateReply(prompt: String): BotReply =
                  BotReply("Voici un lien utile.", "https://www.epfl.ch/education/projects")
            })

    viewModel.updateMessageDraft("Où trouver des projets ?")
    viewModel.sendMessage()
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    viewModel.awaitStreamingCompletion()

    val messages = viewModel.uiState.value.messages
    val sourceCard = messages.lastOrNull { it.source != null }
    assertNotNull("Expected a source card message", sourceCard)
    assertEquals("https://www.epfl.ch/education/projects", sourceCard!!.source?.url)
    assertEquals("EPFL.ch Website", sourceCard.source?.siteLabel)
  }

  @Test
  fun sendMessage_failure_surfaces_error_message() = runBlocking {
    val fakeClient = FakeLlmClient().apply { failure = IllegalStateException("boom") }
    val viewModel = HomeViewModel(fakeClient)

    viewModel.updateMessageDraft("Test")
    viewModel.sendMessage()
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    viewModel.awaitStreamingCompletion()

    val aiMessage = viewModel.uiState.value.messages.firstOrNull { it.type == ChatType.AI }
    assertNotNull(aiMessage)
    assertTrue(aiMessage!!.text.contains("Erreur") || aiMessage.text.contains("error", true))
  }

  @Test
  fun startLocalNewChat_resets_transient_flags() {
    val viewModel = HomeViewModel(FakeLlmClient())
    viewModel.updateUiState { it.copy(isDrawerOpen = true, showDeleteConfirmation = true) }
    viewModel.setTopRightOpen(true)
    viewModel.updateMessageDraft("draft")

    viewModel.startLocalNewChat()

    val state = viewModel.uiState.value
    assertTrue(state.isDrawerOpen) // drawer state unchanged by startLocalNewChat()
    assertFalse(state.showDeleteConfirmation)
    assertEquals("", state.messageDraft)
    assertTrue(state.messages.isEmpty())
  }

  @Test
  fun deleteCurrentConversation_guest_resets_locally() {
    val viewModel = HomeViewModel(FakeLlmClient())
    viewModel.replaceMessages("hello")

    viewModel.deleteCurrentConversation()
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.messages.isEmpty())
    assertNull(state.currentConversationId)
  }

  @Test
  fun deleteCurrentConversation_authNotReady_hides_confirmation() =
      runTest(testDispatcher) {
        val auth = mock<FirebaseAuth> { on { currentUser } doReturn mock<FirebaseUser>() }
        val repo = mock<ConversationRepository>()
        runBlocking { whenever(repo.deleteConversation("conv-1")).thenAnswer {} }
        val viewModel = HomeViewModel(FakeLlmClient(), auth, repo)

        viewModel.updateUiState {
          it.copy(currentConversationId = "conv-1", showDeleteConfirmation = true)
        }

        viewModel.deleteCurrentConversation()
        dispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
      }

  @Test
  fun startData_populates_conversations_and_auto_selects() =
      runTest(testDispatcher) {
        val auth = mock<FirebaseAuth> { on { currentUser } doReturn mock<FirebaseUser>() }
        val repo = mock<ConversationRepository>()
        val conversationsFlow = MutableSharedFlow<List<Conversation>>(replay = 1)
        whenever(repo.conversationsFlow()).thenReturn(conversationsFlow)
        whenever(repo.messagesFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = HomeViewModel(FakeLlmClient(), auth, repo)
        viewModel.setPrivateField("isInLocalNewChat", false)

        viewModel.invokeStartData()
        advanceUntilIdle()

        conversationsFlow.emit(listOf(Conversation(id = "conv-1", title = "First")))
        advanceUntilIdle()

        assertEquals("conv-1", viewModel.uiState.value.currentConversationId)
      }

  @Test
  fun auth_listener_sign_in_triggers_startData() =
      runTest(testDispatcher) {
        val auth = mock<FirebaseAuth>()
        // Configure auth to have no current user initially (guest mode)
        whenever(auth.currentUser).thenReturn(null)
        
        val repo = mock<ConversationRepository>()
        val conversationsFlow = MutableSharedFlow<List<Conversation>>(replay = 1)
        whenever(repo.conversationsFlow()).thenReturn(conversationsFlow)
        whenever(repo.messagesFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = HomeViewModel(FakeLlmClient(), auth, repo)
        viewModel.setPrivateField("lastUid", null)

        val listenerField = HomeViewModel::class.java.getDeclaredField("authListener")
        listenerField.isAccessible = true
        val listener = listenerField.get(viewModel) as FirebaseAuth.AuthStateListener

        // Now simulate sign-in by updating the same mock auth
        val user = mock<FirebaseUser>()
        whenever(auth.currentUser).thenReturn(user)
        whenever(user.uid).thenReturn("user-123")

        listener.onAuthStateChanged(auth)
        advanceUntilIdle()

        runBlocking { verify(repo, timeout(1_000)).conversationsFlow() }
      }

  @Test
  fun deleteCurrentConversation_signedIn_calls_repository() =
      runTest(testDispatcher) {
        val fakeClient = FakeLlmClient()
        val auth = mock<FirebaseAuth> { on { currentUser } doReturn mock<FirebaseUser>() }
        val repo = mock<ConversationRepository>()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateUiState { it.copy(currentConversationId = "conv-1") }

        viewModel.deleteCurrentConversation()
        dispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        runBlocking { verify(repo).deleteConversation("conv-1") }
      }

  @Test
  fun messageDto_toUi_maps_role_and_timestamp() {
    val viewModel = HomeViewModel(FakeLlmClient())
    val method = HomeViewModel::class.java.getDeclaredMethod("toUi", MessageDTO::class.java)
    method.isAccessible = true
    val dto = MessageDTO(role = "assistant", text = "Salut")

    val chat = method.invoke(viewModel, dto) as ChatUIModel
    assertEquals(ChatType.AI, chat.type)
    assertEquals("Salut", chat.text)
  }

  @Test
  fun selectConversation_sets_current_and_exits_local_placeholder() {
    val viewModel = HomeViewModel(FakeLlmClient())
    viewModel.setPrivateField("isInLocalNewChat", true)

    viewModel.selectConversation("remote-42")

    assertEquals("remote-42", viewModel.uiState.value.currentConversationId)
    assertFalse(viewModel.getBooleanField("isInLocalNewChat"))
  }

  @Test
  fun clearChat_cancels_active_stream_and_resets_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel(FakeLlmClient())
        val streamingId = "ai-1"
        viewModel.updateUiState {
          it.copy(
              messages =
                  listOf(
                      ChatUIModel(
                          id = "user-1", text = "Hello", timestamp = 0L, type = ChatType.USER),
                      ChatUIModel(
                          id = streamingId,
                          text = "thinking",
                          timestamp = 0L,
                          type = ChatType.AI,
                          isThinking = true)),
              streamingMessageId = streamingId,
              isSending = true,
              streamingSequence = 3)
        }
        val job = Job()
        viewModel.setPrivateField("activeStreamJob", job)

        viewModel.clearChat()

        assertTrue(job.isCancelled)
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertNull(state.streamingMessageId)
        assertFalse(state.isSending)
        assertEquals(4, state.streamingSequence)
      }

  @Test
  fun startData_withEmptyRemoteList_keeps_null_selection() =
      runTest(testDispatcher) {
        val auth = mock<FirebaseAuth> { on { currentUser } doReturn mock<FirebaseUser>() }
        val repo = mock<ConversationRepository>()
        val conversationsFlow = MutableSharedFlow<List<Conversation>>(replay = 1)
        whenever(repo.conversationsFlow()).thenReturn(conversationsFlow)
        whenever(repo.messagesFlow(any())).thenReturn(flowOf(emptyList()))
        val viewModel = HomeViewModel(FakeLlmClient(), auth, repo)
        viewModel.setPrivateField("isInLocalNewChat", false)

        viewModel.invokeStartData()
        advanceUntilIdle()

        conversationsFlow.emit(emptyList())
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.currentConversationId)
      }

  @Test
  fun sendMessage_signedIn_creates_conversation_and_updates_title() =
      runTest(testDispatcher) {
        val fakeClient = FakeLlmClient().apply { nextReply = "AI reply" }
        val auth = mock<FirebaseAuth> { on { currentUser } doReturn mock<FirebaseUser>() }
        val repo = mock<ConversationRepository>()
        runBlocking { whenever(repo.startNewConversation(any())).thenReturn("conv-123") }
        runBlocking { whenever(repo.appendMessage(any(), any(), any())).thenReturn(Unit) }
        runBlocking { whenever(repo.updateConversationTitle(any(), any())).thenReturn(Unit) }
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        val functions = mock<FirebaseFunctions>()
        val callable = mock<HttpsCallableReference>()
        val result = mock<HttpsCallableResult>()
        whenever(functions.getHttpsCallable("generateTitleFn")).thenReturn(callable)
        whenever(callable.call(any<Map<String, String>>())).thenReturn(Tasks.forResult(result))
        whenever(result.getData()).thenReturn(mapOf("title" to "Generated title"))
        viewModel.setFunctions(functions)

        viewModel.updateMessageDraft("Hello from EPFL student")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()
        advanceUntilIdle()

        assertEquals("conv-123", viewModel.uiState.value.currentConversationId)
        runBlocking { verify(repo).startNewConversation("Hello from EPFL student") }
        runBlocking { verify(repo).appendMessage("conv-123", "user", "Hello from EPFL student") }
        runBlocking { verify(repo).updateConversationTitle("conv-123", "Generated title") }
      }

  @Test
  fun sendMessage_signedIn_title_generation_failure_keeps_quick_title() =
      runTest(testDispatcher) {
        val fakeClient = FakeLlmClient().apply { nextReply = "AI reply" }
        val auth = mock<FirebaseAuth> { on { currentUser } doReturn mock<FirebaseUser>() }
        val repo = mock<ConversationRepository>()
        runBlocking { whenever(repo.startNewConversation(any())).thenReturn("conv-999") }
        runBlocking { whenever(repo.appendMessage(any(), any(), any())).thenReturn(Unit) }
        runBlocking { whenever(repo.updateConversationTitle(any(), any())).thenReturn(Unit) }
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        val functions = mock<FirebaseFunctions>()
        val callable = mock<HttpsCallableReference>()
        whenever(functions.getHttpsCallable("generateTitleFn")).thenReturn(callable)
        whenever(callable.call(any<Map<String, String>>()))
            .thenReturn(Tasks.forException(Exception("boom")))
        viewModel.setFunctions(functions)

        viewModel.updateMessageDraft("Short prompt")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()
        advanceUntilIdle()

        runBlocking { verify(repo).startNewConversation("Short prompt") }
        runBlocking { verify(repo, never()).updateConversationTitle(any(), any()) }
      }

  @Test
  fun buildSiteLabel_formats_epfl_domains() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel(FakeLlmClient())
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildSiteLabel", String::class.java)
        method.isAccessible = true

        val label = method.invoke(viewModel, "https://www.epfl.ch/campus-services") as String

        assertEquals("EPFL.ch Website", label)
      }

  @Test
  fun buildSiteLabel_uses_host_for_external_sites() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel(FakeLlmClient())
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildSiteLabel", String::class.java)
        method.isAccessible = true

        val label = method.invoke(viewModel, "https://kotlinlang.org/docs/home.html") as String

        assertEquals("kotlinlang.org Website", label)
      }

  @Test
  fun buildFallbackTitle_returns_clean_path_segment() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel(FakeLlmClient())
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildFallbackTitle", String::class.java)
        method.isAccessible = true

        val title =
            method.invoke(viewModel, "https://www.epfl.ch/education/projet-de-semestre") as String

        assertEquals("Projet de semestre", title)
      }

  @Test
  fun buildFallbackTitle_defaults_to_host_when_no_path() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel(FakeLlmClient())
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildFallbackTitle", String::class.java)
        method.isAccessible = true

        val title = method.invoke(viewModel, "https://example.com") as String

        assertEquals("example.com", title)
      }

  private fun HomeViewModel.setPrivateField(name: String, value: Any?) {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun HomeViewModel.invokeStartData() {
    val method = HomeViewModel::class.java.getDeclaredMethod("startData")
    method.isAccessible = true
    method.invoke(this)
  }

  private fun HomeViewModel.replaceMessages(vararg texts: String) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    val newMessages =
        texts.map {
          ChatUIModel(
              id = UUID.randomUUID().toString(), text = it, timestamp = 0L, type = ChatType.USER)
        }
    stateFlow.value = stateFlow.value.copy(messages = newMessages)
  }

  private fun HomeViewModel.updateUiState(transform: (HomeUiState) -> HomeUiState) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    stateFlow.value = transform(stateFlow.value)
  }

  private fun HomeViewModel.getBooleanField(name: String): Boolean {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.getBoolean(this)
  }

  private fun HomeViewModel.setFunctions(fake: FirebaseFunctions) {
    val delegateField = HomeViewModel::class.java.getDeclaredField("functions\$delegate")
    delegateField.isAccessible = true
    delegateField.set(this, lazyOf(fake))
  }

  private suspend fun HomeViewModel.awaitStreamingCompletion(timeoutMs: Long = 2_000L) {
    var remaining = timeoutMs
    while ((uiState.value.streamingMessageId != null || uiState.value.isSending) && remaining > 0) {
      dispatcherRule.dispatcher.scheduler.advanceUntilIdle()
      delay(20)
      remaining -= 20
    }
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()
  }
}
