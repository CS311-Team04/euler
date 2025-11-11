package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.conversations.MessageDTO
import com.android.sample.util.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  private val dispatcher
    get() = mainDispatcherRule.dispatcher

  /**
   * Helper used by a few tests to seed the private `_uiState` with deterministic messages. We keep
   * the reflection logic local to the test suite to avoid exposing mutators in the production
   * ViewModel purely for testing.
   */
  private fun HomeViewModel.replaceMessages(vararg texts: String) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    val newMessages =
        texts.map { text ->
          ChatUIModel(
              id = UUID.randomUUID().toString(), text = text, timestamp = 0L, type = ChatType.USER)
        }
    stateFlow.value = stateFlow.value.copy(messages = newMessages)
  }

  private fun HomeViewModel.replaceSystems(vararg systems: SystemItem) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") val stateFlow = field.get(this) as MutableStateFlow<HomeUiState>
    stateFlow.value = stateFlow.value.copy(systems = systems.toList())
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
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
  fun tearDown() {
    Dispatchers.resetMain()
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun initialState_guestMode_isLocalPlaceholder() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        assertNull(state.currentConversationId)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.showDeleteConfirmation)
      }

  @Test
  fun startLocalNewChat_resets_messages_and_flags() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.replaceMessages("hello")
        viewModel.updateMessageDraft("draft")
        viewModel.showDeleteConfirmation()
        viewModel.setTopRightOpen(true)
        viewModel.setLoading(true)

        viewModel.startLocalNewChat()

        val state = viewModel.uiState.value
        assertNull(state.currentConversationId)
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.messageDraft)
        assertFalse(state.showDeleteConfirmation)
        assertTrue(state.isTopRightOpen) // unaffected
        assertTrue(state.isLoading) // unaffected
      }

  @Test
  fun selectConversation_updates_current_id() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.startLocalNewChat()

        viewModel.selectConversation("conversation-123")

        val state = viewModel.uiState.value
        assertEquals("conversation-123", state.currentConversationId)
      }

  @Test
  fun toggleDrawer_toggles_flag() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        assertFalse(viewModel.uiState.value.isDrawerOpen)

        viewModel.toggleDrawer()
        assertTrue(viewModel.uiState.value.isDrawerOpen)

        viewModel.toggleDrawer()
        assertFalse(viewModel.uiState.value.isDrawerOpen)
      }

  @Test
  fun setTopRightOpen_and_hideDeleteConfirmation() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setTopRightOpen(true)
        assertTrue(viewModel.uiState.value.isTopRightOpen)

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.hideDeleteConfirmation()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertTrue(viewModel.uiState.value.isTopRightOpen) // untouched
      }

  @Test
  fun updateMessageDraft_overwrites_text() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("Hello")
        assertEquals("Hello", viewModel.uiState.value.messageDraft)

        viewModel.updateMessageDraft("World")
        assertEquals("World", viewModel.uiState.value.messageDraft)
      }

  @Test
  fun toggleSystemConnection_flips_target_only() =
      runTest(dispatcher) {
        val viewModel =
            HomeViewModel().apply {
              replaceSystems(
                  SystemItem(id = "moodle", name = "Moodle", isConnected = true),
                  SystemItem(id = "isa", name = "IS-Academia", isConnected = true))
            }
        val initial = viewModel.uiState.value
        val moodleInitial = initial.systems.find { it.id == "moodle" }!!.isConnected
        val isaInitial = initial.systems.find { it.id == "isa" }!!.isConnected

        viewModel.toggleSystemConnection("moodle")

        val state = viewModel.uiState.value
        val moodleAfter = state.systems.find { it.id == "moodle" }!!.isConnected
        val isaAfter = state.systems.find { it.id == "isa" }!!.isConnected

        assertNotEquals(moodleInitial, moodleAfter)
        assertEquals(isaInitial, isaAfter)
      }

  @Test
  fun sendMessage_guestWithoutAuth_adds_user_and_error_messages() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.updateMessageDraft("Hello assistant")

        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("messages=${state.messages}", state.messages.isNotEmpty())
        assertEquals(ChatType.USER, state.messages.first().type)
      }

  @Test
  fun deleteCurrentConversation_in_guest_mode_only_resets_locally() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.replaceMessages("hello")
        assertFalse(viewModel.uiState.value.messages.isEmpty())

        viewModel.deleteCurrentConversation()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertNull(state.currentConversationId)
      }

  @Test
  fun localTitleFrom_trims_whitespace_and_limits_words() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        val method =
            HomeViewModel::class
                .java
                .getDeclaredMethod(
                    "localTitleFrom",
                    String::class.java,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!)
        method.isAccessible = true

        val result =
            method.invoke(viewModel, "   Intro to   Quantum Fields   and Beyond   ", 25, 4)
                as String

        assertEquals("Intro to Quantum Fields", result)
      }

  @Test
  fun localTitleFrom_respects_max_length_cutoff() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        val method =
            HomeViewModel::class
                .java
                .getDeclaredMethod(
                    "localTitleFrom",
                    String::class.java,
                    Int::class.javaPrimitiveType!!,
                    Int::class.javaPrimitiveType!!)
        method.isAccessible = true

        val result =
            method.invoke(
                viewModel, "Understanding the fundamentals of electromagnetism in depth", 20, 10)
                as String

        assertEquals("Understanding the fu", result)
      }

  @Test
  fun messageDto_toUi_maps_role_and_timestamp() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        val method = HomeViewModel::class.java.getDeclaredMethod("toUi", MessageDTO::class.java)
        method.isAccessible = true
        val dto = MessageDTO(role = "assistant", text = "Salut")

        val chat = method.invoke(viewModel, dto) as ChatUIModel

        assertEquals(ChatType.AI, chat.type)
        assertEquals("Salut", chat.text)
      }

  @Test
  fun messageDto_toUi_user_role_maps_to_user_type() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        val method = HomeViewModel::class.java.getDeclaredMethod("toUi", MessageDTO::class.java)
        method.isAccessible = true
        val dto = MessageDTO(role = "user", text = "Hello")

        val chat = method.invoke(viewModel, dto) as ChatUIModel

        assertEquals(ChatType.USER, chat.type)
        assertEquals("Hello", chat.text)
      }

  @Test
  fun onSignedOutInternal_resets_state_and_local_chat() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.replaceMessages("Before")
        viewModel.updateMessageDraft("draft")
        val method = HomeViewModel::class.java.getDeclaredMethod("onSignedOutInternal")
        method.isAccessible = true

        method.invoke(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.messageDraft)
        assertFalse(state.showDeleteConfirmation)
      }

  @Test
  fun sendMessage_ignores_blank_input() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.updateMessageDraft("   ")

        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isSending)
      }

  @Test
  fun showDeleteConfirmation_sets_flag_true() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
      }

  @Test
  fun hideDeleteConfirmation_sets_flag_false() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.showDeleteConfirmation()

        viewModel.hideDeleteConfirmation()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
      }

  @Test
  fun setLoading_updates_flag() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setLoading(true)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.setLoading(false)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun startLocalNewChat_preserves_nonDelete_flags() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.setTopRightOpen(true)
        viewModel.setLoading(true)
        viewModel.showDeleteConfirmation()

        viewModel.startLocalNewChat()

        val state = viewModel.uiState.value
        assertTrue(state.isTopRightOpen)
        assertTrue(state.isLoading)
        assertFalse(state.showDeleteConfirmation)
        assertTrue(state.messages.isEmpty())
        assertNull(state.currentConversationId)
      }

  @Test
  fun startLocalNewChat_clears_delete_confirmation_only() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.showDeleteConfirmation()
        viewModel.setTopRightOpen(true)
        viewModel.setLoading(true)

        viewModel.startLocalNewChat()

        val state = viewModel.uiState.value
        assertFalse(state.showDeleteConfirmation)
        assertTrue(state.isTopRightOpen)
        assertTrue(state.isLoading)
      }

  @Test
  fun selectConversation_exitsLocalPlaceholder() =
      runTest(dispatcher) {
        val viewModel = HomeViewModel()
        viewModel.startLocalNewChat()
        assertNull(viewModel.uiState.value.currentConversationId)

        viewModel.selectConversation("conv-999")

        val state = viewModel.uiState.value
        assertEquals("conv-999", state.currentConversationId)
      }
}
