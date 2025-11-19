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
import com.android.sample.profile.UserProfile
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
import kotlinx.coroutines.test.advanceTimeBy
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

  private fun createHomeViewModel(): HomeViewModel =
      HomeViewModel(profileRepository = FakeProfileRepository())

  private fun HomeViewModel.replaceMessages(vararg texts: String) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    val current = stateFlow.value
    val messages =
        texts.map { text ->
          ChatUIModel(
              id = UUID.randomUUID().toString(), text = text, timestamp = 0L, type = ChatType.USER)
        }
    stateFlow.value = current.copy(messages = messages)
  }

  private fun HomeViewModel.setUserNameForTest(name: String) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    val current = stateFlow.value
    stateFlow.value = current.copy(userName = name)
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

  @Test
  fun setGuestMode_true_sets_guest_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setGuestMode(true)

        val state = viewModel.uiState.value
        assertTrue(state.isGuest)
        assertEquals("guest", state.userName)
        assertNull(state.profile)
        assertFalse(state.showGuestProfileWarning)
      }

  @Test
  fun setGuestMode_false_restores_default_name_when_blank() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        viewModel.setUserNameForTest("")
        viewModel.setGuestMode(false)

        val state = viewModel.uiState.value
        assertFalse(state.isGuest)
        assertEquals("Student", state.userName)
      }

  @Test
  fun refreshProfile_updates_state_from_repository() =
      runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val profile =
            UserProfile(
                fullName = "Jane Doe",
                preferredName = "JD",
                faculty = "IC",
                section = "CS",
                email = "jane@epfl.ch",
                phone = "+41 79 123 45 67",
                roleDescription = "Student")
        repo.savedProfile = profile
        val viewModel = HomeViewModel(profileRepository = repo)

        viewModel.refreshProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(profile, state.profile)
        assertEquals("JD", state.userName)
        assertFalse(state.isGuest)
      }

  @Test
  fun refreshProfile_with_null_profile_keeps_defaults() =
      runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val viewModel = HomeViewModel(profileRepository = repo)
        viewModel.setUserNameForTest("")

        viewModel.refreshProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.profile)
        assertEquals("Student", state.userName)
        assertFalse(state.isGuest)
      }

  @Test
  fun saveProfile_persists_and_updates_ui_state() =
      runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val viewModel = HomeViewModel(profileRepository = repo)
        val profile =
            UserProfile(
                fullName = "Alice Example",
                preferredName = "Alice",
                faculty = "SV",
                section = "Bio",
                email = "alice@epfl.ch",
                phone = "+41 79 765 43 21",
                roleDescription = "PhD")

        viewModel.saveProfile(profile)
        advanceUntilIdle()

        assertEquals(profile, repo.savedProfile)
        val state = viewModel.uiState.value
        assertEquals(profile, state.profile)
        assertEquals("Alice", state.userName)
        assertFalse(state.isGuest)
      }

  @Test
  fun clearProfile_resets_profile_and_guest_flags() =
      runTest(testDispatcher) {
        val repo = FakeProfileRepository()
        val viewModel = HomeViewModel(profileRepository = repo)
        val profile =
            UserProfile(
                fullName = "Bob Example",
                preferredName = "Bob",
                email = "bob@epfl.ch",
                roleDescription = "Staff")

        viewModel.saveProfile(profile)
        advanceUntilIdle()
        viewModel.setGuestMode(true)

        viewModel.clearProfile()

        val state = viewModel.uiState.value
        assertNull(state.profile)
        assertEquals("Student", state.userName)
        assertFalse(state.isGuest)
        assertFalse(state.showGuestProfileWarning)
      }

  @Test
  fun guest_profile_warning_visiblity_toggles_correctly() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.showGuestProfileWarning()
        assertTrue(viewModel.uiState.value.showGuestProfileWarning)

        viewModel.hideGuestProfileWarning()
        assertFalse(viewModel.uiState.value.showGuestProfileWarning)
      }

  @Test
  fun clearChat_empties_messages() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.replaceMessages("Test message")

        // Initial state should have messages now (user message added synchronously)
        val initialState = viewModel.uiState.value
        assertTrue(initialState.messages.isNotEmpty())

        // Clear the chat
        viewModel.clearChat()

        // Verify chat is empty
        val stateAfterClear = viewModel.uiState.value
        assertTrue(stateAfterClear.messages.isEmpty())
      }

  @Test
  fun clearChat_preserves_other_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialUserName = initialState.userName
        val initialSystems = initialState.systems

        // Clear the chat
        viewModel.clearChat()

        // Verify other state is preserved
        val stateAfterClear = viewModel.uiState.value
        assertEquals(initialUserName, stateAfterClear.userName)
        assertEquals(initialSystems, stateAfterClear.systems)
      }

  @Test
  fun simulateStreamingFromText_populates_message_and_clears_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        val field = HomeViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<HomeUiState>
        val aiId = "ai-${System.nanoTime()}"
        val initial = stateFlow.value
        stateFlow.value =
            initial.copy(
                messages =
                    listOf(
                        ChatUIModel(
                            id = aiId,
                            text = "",
                            timestamp = 0L,
                            type = ChatType.AI,
                            isThinking = true)),
                streamingMessageId = aiId,
                streamingSequence = 0,
                isSending = true)

        viewModel.simulateStreamingForTest(aiId, "Bonjour EPFL")

        val finalState = viewModel.uiState.value
        val aiMessage = finalState.messages.first()
        assertEquals("Bonjour EPFL", aiMessage.text)
        assertFalse(aiMessage.isThinking)
        assertNull(finalState.streamingMessageId)
        assertFalse(finalState.isSending)
        assertTrue(finalState.streamingSequence > initial.streamingSequence)
      }

  @Test
  fun toggleDrawer_changes_isDrawerOpen_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        assertFalse(initialState.isDrawerOpen)

        // Toggle drawer to open
        viewModel.toggleDrawer()
        val stateAfterFirstToggle = viewModel.uiState.value
        assertTrue(stateAfterFirstToggle.isDrawerOpen)

        // Toggle drawer to close
        viewModel.toggleDrawer()
        val stateAfterSecondToggle = viewModel.uiState.value
        assertFalse(stateAfterSecondToggle.isDrawerOpen)
      }

  @Test
  fun sendMessage_with_empty_draft_does_nothing() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialMessagesCount = initialState.messages.size

        // Try to send empty message
        viewModel.updateMessageDraft("")
        viewModel.sendMessage()

        // Advance time
        testDispatcher.scheduler.advanceTimeBy(100)

        // Verify nothing changed
        val stateAfterSend = viewModel.uiState.value
        assertEquals(initialMessagesCount, stateAfterSend.messages.size)
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

  @Test
  fun toggleDrawer_toggles_flag() {
    val viewModel = HomeViewModel()
    assertFalse(viewModel.uiState.value.isDrawerOpen)

    viewModel.toggleDrawer()
    assertTrue(viewModel.uiState.value.isDrawerOpen)

    viewModel.toggleDrawer()
    assertFalse(viewModel.uiState.value.isDrawerOpen)
  }

  @Test
  fun sendMessage_guest_appends_user_and_ai_messages() = runBlocking {
    val fakeClient = FakeLlmClient().apply { nextReply = "Bonjour" }
    val viewModel = HomeViewModel()
    viewModel.setPrivateField("llmClient", fakeClient)

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
    val viewModel = HomeViewModel()
    viewModel.setPrivateField(
        "llmClient",
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
    val viewModel = HomeViewModel()
    viewModel.setPrivateField("llmClient", fakeClient)

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
    val viewModel = HomeViewModel()
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
    val viewModel = HomeViewModel()
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
    val viewModel = HomeViewModel()
    val method = HomeViewModel::class.java.getDeclaredMethod("toUi", MessageDTO::class.java)
    method.isAccessible = true
    val dto = MessageDTO(role = "assistant", text = "Salut")

    val chat = method.invoke(viewModel, dto) as ChatUIModel
    assertEquals(ChatType.AI, chat.type)
    assertEquals("Salut", chat.text)
  }

  @Test
  fun selectConversation_sets_current_and_exits_local_placeholder() {
    val viewModel = HomeViewModel()
    viewModel.setPrivateField("isInLocalNewChat", true)

    viewModel.selectConversation("remote-42")

    assertEquals("remote-42", viewModel.uiState.value.currentConversationId)
    assertFalse(viewModel.getBooleanField("isInLocalNewChat"))
  }

  @Test
  fun clearChat_cancels_active_stream_and_resets_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
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
        val viewModel = HomeViewModel()
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildSiteLabel", String::class.java)
        method.isAccessible = true

        val label = method.invoke(viewModel, "https://www.epfl.ch/campus-services") as String

        assertEquals("EPFL.ch Website", label)
      }

  @Test
  fun buildSiteLabel_uses_host_for_external_sites() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildSiteLabel", String::class.java)
        method.isAccessible = true

        val label = method.invoke(viewModel, "https://kotlinlang.org/docs/home.html") as String

        assertEquals("kotlinlang.org Website", label)
      }

  @Test
  fun buildFallbackTitle_returns_clean_path_segment() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
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
        val viewModel = HomeViewModel()
        val method =
            HomeViewModel::class.java.getDeclaredMethod("buildFallbackTitle", String::class.java)
        method.isAccessible = true

        val title = method.invoke(viewModel, "https://example.com") as String

        assertEquals("example.com", title)
      }

  // ============ Tests for handleSendMessageError (via sendMessage with auth) ============

  @Test
  fun sendMessage_authenticated_withRepositoryFirebaseException_formatsError() =
      runTest(testDispatcher) {
        // Mock authenticated user
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        // Mock exception with code and details during repository operation
        val mockException =
            mock<com.google.firebase.functions.FirebaseFunctionsException> {
              on { message } doReturn "Test error"
              on { code } doReturn
                  com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED
              on { details } doReturn "Access denied"
            }

        runBlocking {
          whenever(repo.startNewConversation(any())).thenAnswer { throw mockException }
        }

        val fakeClient = FakeLlmClient()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("Test message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        val messages = viewModel.uiState.value.messages
        val aiMessage = messages.firstOrNull { it.type == ChatType.AI }

        assertNotNull(aiMessage)
        assertTrue(aiMessage!!.text.contains("Error"))
        assertTrue(aiMessage.text.contains("[PERMISSION_DENIED]"))
        assertTrue(aiMessage.text.contains("Access denied"))
        assertFalse(aiMessage.isThinking)
        assertFalse(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.streamingMessageId)
      }

  @Test
  fun sendMessage_authenticated_withRepositoryException_withDetails_formatsError() =
      runTest(testDispatcher) {
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        // Mock exception with details
        val mockException =
            mock<com.google.firebase.functions.FirebaseFunctionsException> {
              on { message } doReturn "Test error"
              on { code } doReturn
                  com.google.firebase.functions.FirebaseFunctionsException.Code.INTERNAL
              on { details } doReturn "Custom error details"
            }

        runBlocking {
          whenever(repo.startNewConversation(any())).thenAnswer { throw mockException }
        }

        val fakeClient = FakeLlmClient()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("Test message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        val messages = viewModel.uiState.value.messages
        val aiMessage = messages.firstOrNull { it.type == ChatType.AI }

        assertNotNull(aiMessage)
        assertTrue(aiMessage!!.text.contains("Error"))
        assertTrue(aiMessage.text.contains("Custom error details"))
        assertFalse(aiMessage.isThinking)
      }

  @Test
  fun sendMessage_authenticated_withRepositoryException_withNoDetails_usesMessage() =
      runTest(testDispatcher) {
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        // Mock exception without details (null details)
        val mockException =
            mock<com.google.firebase.functions.FirebaseFunctionsException> {
              on { message } doReturn "Original error message"
              on { code } doReturn
                  com.google.firebase.functions.FirebaseFunctionsException.Code.UNAVAILABLE
              on { details } doReturn null
            }

        runBlocking {
          whenever(repo.appendMessage(any(), any(), any())).thenAnswer { throw mockException }
          whenever(repo.startNewConversation(any())).thenReturn("conv-123")
        }

        val fakeClient = FakeLlmClient()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("Test message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        val messages = viewModel.uiState.value.messages
        val aiMessage = messages.firstOrNull { it.type == ChatType.AI }

        assertNotNull(aiMessage)
        assertTrue(aiMessage!!.text.contains("Error"))
        assertTrue(aiMessage.text.contains("[UNAVAILABLE]"))
        assertTrue(aiMessage.text.contains("Original error message"))
      }

  @Test
  fun sendMessage_authenticated_withRegularException_usesExceptionMessage() =
      runTest(testDispatcher) {
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        runBlocking {
          whenever(repo.startNewConversation(any())).thenAnswer {
            throw IllegalStateException("Something went wrong")
          }
        }

        val fakeClient = FakeLlmClient()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("Test message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        val messages = viewModel.uiState.value.messages
        val aiMessage = messages.firstOrNull { it.type == ChatType.AI }

        assertNotNull(aiMessage)
        assertTrue(aiMessage!!.text.contains("Error"))
        assertTrue(aiMessage.text.contains("Something went wrong"))
        assertFalse(aiMessage.text.contains("[")) // No code should be present
      }

  @Test
  fun sendMessage_authenticated_withExceptionWithoutMessage_usesFallback() =
      runTest(testDispatcher) {
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        // Exception with null message
        runBlocking {
          whenever(repo.startNewConversation(any())).thenAnswer {
            throw RuntimeException(null as String?)
          }
        }

        val fakeClient = FakeLlmClient()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("Test message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        val messages = viewModel.uiState.value.messages
        val aiMessage = messages.firstOrNull { it.type == ChatType.AI }

        assertNotNull(aiMessage)
        assertTrue(aiMessage!!.text.contains("Error"))
        assertTrue(aiMessage.text.contains("request failed"))
      }

  @Test
  fun sendMessage_authenticated_errorUpdatesCorrectMessage() =
      runTest(testDispatcher) {
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        // First message succeeds
        runBlocking {
          whenever(repo.startNewConversation(any())).thenReturn("conv-123")
          whenever(repo.appendMessage(any(), any(), any())).thenReturn(Unit)
        }

        val fakeClient = FakeLlmClient().apply { nextReply = "First response" }
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("First message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        // Second message fails
        runBlocking {
          whenever(repo.appendMessage(any(), any(), any())).thenAnswer {
            throw RuntimeException("Test error")
          }
        }

        viewModel.updateMessageDraft("Second message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        val messages = viewModel.uiState.value.messages

        // First AI message should be unchanged
        val firstAiMessage = messages.firstOrNull { it.text.contains("First response") }
        assertNotNull(firstAiMessage)
        assertFalse(firstAiMessage!!.isThinking)

        // Second AI message should have error
        val errorMessage = messages.lastOrNull { it.type == ChatType.AI }
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.text.contains("Error"))
        assertTrue(errorMessage.text.contains("Test error"))
        assertFalse(errorMessage.isThinking)
      }

  @Test
  fun sendMessage_authenticated_errorClearsStreamingState() =
      runTest(testDispatcher) {
        val auth =
            mock<FirebaseAuth> {
              val user = mock<FirebaseUser>()
              on { currentUser } doReturn user
            }

        val repo = mock<ConversationRepository>()

        runBlocking {
          whenever(repo.startNewConversation(any())).thenAnswer {
            throw RuntimeException("Outer error")
          }
        }

        val fakeClient = FakeLlmClient()
        val viewModel = HomeViewModel(fakeClient, auth, repo)

        viewModel.updateMessageDraft("Test message")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.awaitStreamingCompletion()

        // Verify that streaming state is cleared
        assertFalse(viewModel.uiState.value.isSending)
        assertNull(viewModel.uiState.value.streamingMessageId)
      }

  @Test
  fun hideDeleteConfirmation_does_not_affect_other_flags() =
      runTest(testDispatcher) {
        val viewModel = createHomeViewModel()

        viewModel.showDeleteConfirmation()
        val before = viewModel.uiState.value

        viewModel.hideDeleteConfirmation()
        val after = viewModel.uiState.value

        assertEquals(before.isDrawerOpen, after.isDrawerOpen)
        assertEquals(before.isTopRightOpen, after.isTopRightOpen)
        assertEquals(before.isLoading, after.isLoading)
        assertNotEquals(before.showDeleteConfirmation, after.showDeleteConfirmation)
      }

  @Test
  fun setLoading_to_true_and_then_false() =
      runTest(testDispatcher) {
        val viewModel = createHomeViewModel()

        viewModel.setLoading(true)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.setLoading(false)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun setLoading_does_not_affect_delete_confirmation() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.showDeleteConfirmation()
        val showDeleteState = viewModel.uiState.value.showDeleteConfirmation

        viewModel.setLoading(true)
        assertEquals(showDeleteState, viewModel.uiState.value.showDeleteConfirmation)

        viewModel.setLoading(false)
        assertEquals(showDeleteState, viewModel.uiState.value.showDeleteConfirmation)
      }

  @Test
  fun delete_confirmation_workflow_with_multiple_toggles() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        for (i in 1..3) {
          viewModel.showDeleteConfirmation()
          assertTrue(viewModel.uiState.value.showDeleteConfirmation)
          viewModel.hideDeleteConfirmation()
          assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        }
      }
}
