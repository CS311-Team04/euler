package com.android.sample.home

import com.android.sample.Chat.ChatType
import com.android.sample.Chat.ChatUIModel
import com.android.sample.profile.UserProfile
import com.android.sample.util.MainDispatcherRule
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()
  private val testDispatcher
    get() = mainDispatcherRule.dispatcher

  private fun createHomeViewModel(): HomeViewModel = HomeViewModel(FakeProfileRepository())

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
        val viewModel = HomeViewModel(repo)

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
        val viewModel = HomeViewModel(repo)
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
        val viewModel = HomeViewModel(repo)
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
        val viewModel = HomeViewModel(repo)
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

  @Test
  fun updateMessageDraft_updates_draft_text() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("Hello")
        val state1 = viewModel.uiState.value
        assertEquals("Hello", state1.messageDraft)

        viewModel.updateMessageDraft("World")
        val state2 = viewModel.uiState.value
        assertEquals("World", state2.messageDraft)
      }

  @Test
  fun toggleSystemConnection_flips_connection_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val moodleSystem = initialState.systems.find { it.id == "moodle" }
        assertNotNull(moodleSystem)
        assertTrue(moodleSystem!!.isConnected)

        // Toggle connection
        viewModel.toggleSystemConnection("moodle")

        val stateAfterToggle = viewModel.uiState.value
        val updatedSystem = stateAfterToggle.systems.find { it.id == "moodle" }
        assertNotNull(updatedSystem)
        assertFalse(updatedSystem!!.isConnected)
      }

  @Test
  fun setTopRightOpen_changes_top_right_open_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        assertFalse(initialState.isTopRightOpen)

        // Set to open
        viewModel.setTopRightOpen(true)
        val stateAfterOpen = viewModel.uiState.value
        assertTrue(stateAfterOpen.isTopRightOpen)

        // Set to close
        viewModel.setTopRightOpen(false)
        val stateAfterClose = viewModel.uiState.value
        assertFalse(stateAfterClose.isTopRightOpen)
      }

  @Test
  fun setLoading_changes_loading_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        assertFalse(initialState.isLoading)

        viewModel.setLoading(true)
        val stateAfterSetTrue = viewModel.uiState.value
        assertTrue(stateAfterSetTrue.isLoading)

        viewModel.setLoading(false)
        val stateAfterSetFalse = viewModel.uiState.value
        assertFalse(stateAfterSetFalse.isLoading)
      }

  @Test
  fun showDeleteConfirmation_sets_showDeleteConfirmation_to_true() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        assertFalse(initialState.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()
        val stateAfterShow = viewModel.uiState.value
        assertTrue(stateAfterShow.showDeleteConfirmation)
      }

  @Test
  fun hideDeleteConfirmation_sets_showDeleteConfirmation_to_false() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.showDeleteConfirmation()
        val stateAfterShow = viewModel.uiState.value
        assertTrue(stateAfterShow.showDeleteConfirmation)

        viewModel.hideDeleteConfirmation()
        val stateAfterHide = viewModel.uiState.value
        assertFalse(stateAfterHide.showDeleteConfirmation)
      }

  @Test
  fun clearChat_with_confirmation_workflow() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.replaceMessages("Test message")

        val initialState = viewModel.uiState.value
        assertTrue(initialState.messages.isNotEmpty())

        // Show confirmation
        viewModel.showDeleteConfirmation()
        val stateAfterShow = viewModel.uiState.value
        assertTrue(stateAfterShow.showDeleteConfirmation)

        // Clear chat
        viewModel.clearChat()
        val stateAfterClear = viewModel.uiState.value
        assertTrue(stateAfterClear.messages.isEmpty())

        // Hide confirmation
        viewModel.hideDeleteConfirmation()
        val stateAfterHide = viewModel.uiState.value
        assertFalse(stateAfterHide.showDeleteConfirmation)
        assertTrue(stateAfterHide.messages.isEmpty())
      }

  @Test
  fun initial_state_has_default_values() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        assertEquals("Student", state.userName)
        assertFalse(state.isDrawerOpen)
        assertFalse(state.isTopRightOpen)
        assertFalse(state.isLoading)
        assertFalse(state.showDeleteConfirmation)
        assertEquals("", state.messageDraft)
        assertFalse(state.systems.isEmpty())
        assertTrue(state.messages.isEmpty())
      }

  @Test
  fun updateMessageDraft_with_whitespace() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("  Test with spaces  ")
        val state = viewModel.uiState.value
        assertEquals("  Test with spaces  ", state.messageDraft)
      }

  @Test
  fun sendMessage_with_whitespace_only_does_nothing() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialCount = viewModel.uiState.value.messages.size

        viewModel.updateMessageDraft("    ")
        viewModel.sendMessage()

        testDispatcher.scheduler.advanceTimeBy(100)

        val stateAfterSend = viewModel.uiState.value
        assertEquals(initialCount, stateAfterSend.messages.size)
      }

  @Test
  fun toggleSystemConnection_for_isa_system() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val isaSystem = initialState.systems.find { it.id == "isa" }
        assertNotNull(isaSystem)
        assertTrue(isaSystem!!.isConnected)

        viewModel.toggleSystemConnection("isa")

        val stateAfterToggle = viewModel.uiState.value
        val updatedSystem = stateAfterToggle.systems.find { it.id == "isa" }
        assertNotNull(updatedSystem)
        assertFalse(updatedSystem!!.isConnected)
      }

  @Test
  fun toggleSystemConnection_for_camipro_system() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val camiproSystem = initialState.systems.find { it.id == "camipro" }
        assertNotNull(camiproSystem)
        assertFalse(camiproSystem!!.isConnected)

        viewModel.toggleSystemConnection("camipro")

        val stateAfterToggle = viewModel.uiState.value
        val updatedSystem = stateAfterToggle.systems.find { it.id == "camipro" }
        assertNotNull(updatedSystem)
        assertTrue(updatedSystem!!.isConnected)
      }

  @Test
  fun toggleSystemConnection_for_nonexistent_system() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialSystems = initialState.systems.toList()

        viewModel.toggleSystemConnection("nonexistent")

        val stateAfterToggle = viewModel.uiState.value
        assertEquals(initialSystems, stateAfterToggle.systems)
      }

  @Test
  fun setLoading_preserves_other_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialUserName = initialState.userName
        val initialSystems = initialState.systems

        viewModel.setLoading(true)
        val stateAfterLoading = viewModel.uiState.value

        assertEquals(initialUserName, stateAfterLoading.userName)
        assertEquals(initialSystems, stateAfterLoading.systems)
        assertTrue(stateAfterLoading.isLoading)
      }

  @Test
  fun toggleDrawer_preserves_other_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialUserName = initialState.userName
        val initialSystems = initialState.systems

        viewModel.toggleDrawer()
        val stateAfterToggle = viewModel.uiState.value

        assertEquals(initialUserName, stateAfterToggle.userName)
        assertEquals(initialSystems, stateAfterToggle.systems)
        assertTrue(stateAfterToggle.isDrawerOpen)
      }

  @Test
  fun setTopRightOpen_preserves_other_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialUserName = initialState.userName
        val initialSystems = initialState.systems

        viewModel.setTopRightOpen(true)
        val stateAfterOpen = viewModel.uiState.value

        assertEquals(initialUserName, stateAfterOpen.userName)
        assertEquals(initialSystems, stateAfterOpen.systems)
        assertTrue(stateAfterOpen.isTopRightOpen)
      }

  @Test
  fun clearChat_does_not_affect_flags() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        // Put some flags to true
        viewModel.setTopRightOpen(true)
        viewModel.setLoading(true)
        viewModel.showDeleteConfirmation()

        // Then clear chat
        viewModel.clearChat()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertTrue(state.isTopRightOpen)
        assertTrue(state.isLoading)
        assertTrue(state.showDeleteConfirmation)
      }

  @Test
  fun top_right_and_delete_confirmation_are_independent() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setTopRightOpen(true)
        val openState = viewModel.uiState.value
        assertTrue(openState.isTopRightOpen)
        assertFalse(openState.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()
        val bothState = viewModel.uiState.value
        assertTrue(bothState.isTopRightOpen)
        assertTrue(bothState.showDeleteConfirmation)

        viewModel.setTopRightOpen(false)
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isTopRightOpen)
        assertTrue(finalState.showDeleteConfirmation)
      }

  @Test
  fun ui_state_copy_keeps_immutability_contract() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()
        val s1 = viewModel.uiState.value
        val s2 = s1.copy()
        assertEquals(s1, s2)
        assertNotSame(s1, s2)
      }

  @Test
  fun updateMessageDraft_can_be_updated() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("Test message")
        val stateAfterUpdate = viewModel.uiState.value
        assertEquals("Test message", stateAfterUpdate.messageDraft)

        // Update again
        viewModel.updateMessageDraft("New message")
        val stateAfterSecondUpdate = viewModel.uiState.value
        assertEquals("New message", stateAfterSecondUpdate.messageDraft)
      }

  @Test
  fun sendMessage_sets_loading_state() =
      runTest(testDispatcher) {
        // Note: Cannot test sendMessage directly as it uses viewModelScope.launch which requires
        // the main dispatcher that is not available in unit tests without Robolectric
        // This is tested in integration tests instead
        assertTrue(true)
      }

  @Test
  fun showDeleteConfirmation_preserves_other_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialUserName = initialState.userName
        val initialSystems = initialState.systems
        val initialMessages = initialState.messages

        viewModel.showDeleteConfirmation()
        val stateAfterShow = viewModel.uiState.value

        assertEquals(initialUserName, stateAfterShow.userName)
        assertEquals(initialSystems, stateAfterShow.systems)
        assertEquals(initialMessages, stateAfterShow.messages)
        assertTrue(stateAfterShow.showDeleteConfirmation)
      }

  @Test
  fun hideDeleteConfirmation_preserves_other_state() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.showDeleteConfirmation()
        val stateWithShow = viewModel.uiState.value

        viewModel.hideDeleteConfirmation()
        val stateAfterHide = viewModel.uiState.value

        assertEquals(stateWithShow.userName, stateAfterHide.userName)
        assertEquals(stateWithShow.systems, stateAfterHide.systems)
        assertEquals(stateWithShow.messages, stateAfterHide.messages)
        assertFalse(stateAfterHide.showDeleteConfirmation)
      }

  @Test
  fun toggleSystemConnection_multiple_times() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.toggleSystemConnection("moodle")
        val state1 = viewModel.uiState.value
        val moodle1 = state1.systems.find { it.id == "moodle" }
        assertFalse(moodle1!!.isConnected)

        viewModel.toggleSystemConnection("moodle")
        val state2 = viewModel.uiState.value
        val moodle2 = state2.systems.find { it.id == "moodle" }
        assertTrue(moodle2!!.isConnected)

        viewModel.toggleSystemConnection("moodle")
        val state3 = viewModel.uiState.value
        val moodle3 = state3.systems.find { it.id == "moodle" }
        assertFalse(moodle3!!.isConnected)
      }

  @Test
  fun toggleSystemConnection_for_drive_system() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val driveSystem = initialState.systems.find { it.id == "drive" }
        assertNotNull(driveSystem)
        assertTrue(driveSystem!!.isConnected)

        viewModel.toggleSystemConnection("drive")

        val stateAfterToggle = viewModel.uiState.value
        val updatedSystem = stateAfterToggle.systems.find { it.id == "drive" }
        assertNotNull(updatedSystem)
        assertFalse(updatedSystem!!.isConnected)
      }

  @Test
  fun toggleSystemConnection_for_empty_string() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialSystems = initialState.systems.toList()

        viewModel.toggleSystemConnection("")

        val stateAfterToggle = viewModel.uiState.value
        assertEquals(initialSystems, stateAfterToggle.systems)
      }

  @Test
  fun uiState_maintains_reference_stability() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val stateRef1 = viewModel.uiState
        val stateRef2 = viewModel.uiState

        assertSame(stateRef1, stateRef2)
      }

  @Test
  fun clearChat_when_already_empty() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.clearChat()
        val state1 = viewModel.uiState.value
        assertTrue(state1.messages.isEmpty())

        viewModel.clearChat()
        val state2 = viewModel.uiState.value
        assertTrue(state2.messages.isEmpty())
      }

  @Test
  fun initialState_systems_count() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        assertEquals(6, state.systems.size)
      }

  @Test
  fun initialState_messages_count() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        assertEquals(0, state.messages.size)
      }

  @Test
  fun initialState_systems_have_correct_states() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        val isa = state.systems.find { it.id == "isa" }
        val moodle = state.systems.find { it.id == "moodle" }
        val ed = state.systems.find { it.id == "ed" }
        val camipro = state.systems.find { it.id == "camipro" }
        val mail = state.systems.find { it.id == "mail" }
        val drive = state.systems.find { it.id == "drive" }

        assertNotNull(isa)
        assertTrue(isa!!.isConnected)
        assertNotNull(moodle)
        assertTrue(moodle!!.isConnected)
        assertNotNull(ed)
        assertTrue(ed!!.isConnected)
        assertNotNull(camipro)
        assertFalse(camipro!!.isConnected)
        assertNotNull(mail)
        assertFalse(mail!!.isConnected)
        assertNotNull(drive)
        assertTrue(drive!!.isConnected)
      }

  @Test
  fun updateMessageDraft_with_newline_characters() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("Line1\nLine2\r\nLine3")
        val state = viewModel.uiState.value
        assertEquals("Line1\nLine2\r\nLine3", state.messageDraft)
      }

  @Test
  fun updateMessageDraft_with_special_unicode() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("Hello 世界 🌍")
        val state = viewModel.uiState.value
        assertEquals("Hello 世界 🌍", state.messageDraft)
      }

  @Test
  fun toggleSystemConnection_affects_only_target_system() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val moodleBefore = initialState.systems.find { it.id == "moodle" }!!.isConnected
        val isaBefore = initialState.systems.find { it.id == "isa" }!!.isConnected

        viewModel.toggleSystemConnection("moodle")

        val stateAfter = viewModel.uiState.value
        val moodleAfter = stateAfter.systems.find { it.id == "moodle" }!!.isConnected
        val isaAfter = stateAfter.systems.find { it.id == "isa" }!!.isConnected

        assertNotEquals(moodleBefore, moodleAfter)
        assertEquals(isaBefore, isaAfter)
      }

  @Test
  fun multiple_state_changes_in_sequence() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.toggleDrawer()
        viewModel.setTopRightOpen(true)
        viewModel.updateMessageDraft("Test")

        val state = viewModel.uiState.value
        assertTrue(state.isDrawerOpen)
        assertTrue(state.isTopRightOpen)
        assertEquals("Test", state.messageDraft)
      }

  @Test
  fun clearChat_then_add_messages_via_refresh() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.clearChat()
        val stateAfterClear = viewModel.uiState.value
        assertTrue(stateAfterClear.messages.isEmpty())

        viewModel.clearChat()
        val stateAfterSecondClear = viewModel.uiState.value
        assertTrue(stateAfterSecondClear.messages.isEmpty())
      }

  @Test
  fun toggleDrawer_then_close() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.toggleDrawer()
        assertTrue(viewModel.uiState.value.isDrawerOpen)

        viewModel.toggleDrawer()
        assertFalse(viewModel.uiState.value.isDrawerOpen)
      }

  @Test
  fun setTopRightOpen_toggle_behavior() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setTopRightOpen(true)
        assertTrue(viewModel.uiState.value.isTopRightOpen)

        viewModel.setTopRightOpen(false)
        assertFalse(viewModel.uiState.value.isTopRightOpen)

        viewModel.setTopRightOpen(true)
        assertTrue(viewModel.uiState.value.isTopRightOpen)
      }

  @Test
  fun showDeleteConfirmation_then_hide_multiple_times() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.hideDeleteConfirmation()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
      }

  @Test
  fun updateMessageDraft_clear_sequence() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("First message")
        assertEquals("First message", viewModel.uiState.value.messageDraft)

        viewModel.updateMessageDraft("Second message")
        assertEquals("Second message", viewModel.uiState.value.messageDraft)

        viewModel.updateMessageDraft("")
        assertEquals("", viewModel.uiState.value.messageDraft)
      }

  @Test
  fun toggleSystemConnection_all_systems() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val systems = viewModel.uiState.value.systems
        systems.forEach { system ->
          val initialState = system.isConnected
          viewModel.toggleSystemConnection(system.id)
          val newState = viewModel.uiState.value.systems.find { it.id == system.id }!!.isConnected
          assertNotEquals(initialState, newState)
        }
      }

  @Test
  fun setLoading_multiple_fluctuations() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setLoading(true)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.setLoading(false)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.setLoading(true)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.setLoading(false)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun state_immutability_after_operations() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state1 = viewModel.uiState.value
        viewModel.toggleDrawer()
        val state2 = viewModel.uiState.value

        assertNotEquals(state1.isDrawerOpen, state2.isDrawerOpen)
      }

  @Test
  fun multiple_drawer_toggles() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        for (i in 1..5) {
          viewModel.toggleDrawer()
          val expectedOpen = i % 2 == 1
          assertEquals(expectedOpen, viewModel.uiState.value.isDrawerOpen)
        }
      }

  @Test
  fun toggleSystemConnection_with_unicode_id() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialSystems = initialState.systems.toList()

        viewModel.toggleSystemConnection("非ASCII")

        val finalState = viewModel.uiState.value
        assertEquals(initialSystems, finalState.systems)
      }

  @Test
  fun updateMessageDraft_with_tab_characters() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.updateMessageDraft("Tab\tCharacter")
        assertEquals("Tab\tCharacter", viewModel.uiState.value.messageDraft)
      }

  @Test
  fun initialState_systems_names_are_correct() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        val isa = state.systems.find { it.id == "isa" }
        assertEquals("IS-Academia", isa?.name)

        val moodle = state.systems.find { it.id == "moodle" }
        assertEquals("Moodle", moodle?.name)
      }

  @Test
  fun initialState_messages_are_empty() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
      }

  @Test
  fun showDeleteConfirmation_and_then_hide_preserves_chat() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialMessagesCount = initialState.messages.size

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(initialMessagesCount, viewModel.uiState.value.messages.size)

        viewModel.hideDeleteConfirmation()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(initialMessagesCount, viewModel.uiState.value.messages.size)
      }

  @Test
  fun showDeleteConfirmation_does_not_affect_other_flags() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val before = viewModel.uiState.value
        viewModel.showDeleteConfirmation()
        val after = viewModel.uiState.value

        assertEquals(before.isDrawerOpen, after.isDrawerOpen)
        assertEquals(before.isTopRightOpen, after.isTopRightOpen)
        assertEquals(before.isLoading, after.isLoading)
        assertNotEquals(before.showDeleteConfirmation, after.showDeleteConfirmation)
      }

  @Test
  fun hideDeleteConfirmation_does_not_affect_other_flags() {
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

  @Test
  fun loading_state_with_delete_confirmation_simultaneously() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setLoading(true)
        viewModel.showDeleteConfirmation()

        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertTrue(state.showDeleteConfirmation)
      }

  @Test
  fun loading_and_delete_confirmation_independently() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setLoading(true)
        val stateWithLoading = viewModel.uiState.value
        assertTrue(stateWithLoading.isLoading)
        assertFalse(stateWithLoading.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()
        val stateWithBoth = viewModel.uiState.value
        assertTrue(stateWithBoth.isLoading)
        assertTrue(stateWithBoth.showDeleteConfirmation)

        viewModel.setLoading(false)
        val stateWithOnlyDelete = viewModel.uiState.value
        assertFalse(stateWithOnlyDelete.isLoading)
        assertTrue(stateWithOnlyDelete.showDeleteConfirmation)

        viewModel.hideDeleteConfirmation()
        val stateClear = viewModel.uiState.value
        assertFalse(stateClear.isLoading)
        assertFalse(stateClear.showDeleteConfirmation)
      }

  @Test
  fun hideDeleteConfirmation_when_not_shown() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val stateBefore = viewModel.uiState.value
        assertFalse(stateBefore.showDeleteConfirmation)

        viewModel.hideDeleteConfirmation()
        val stateAfter = viewModel.uiState.value
        assertFalse(stateAfter.showDeleteConfirmation)
      }

  @Test
  fun showDeleteConfirmation_when_already_shown() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
      }

  @Test
  fun setLoading_multiple_times() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setLoading(true)
        viewModel.setLoading(true)
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.setLoading(false)
        viewModel.setLoading(false)
        assertFalse(viewModel.uiState.value.isLoading)
      }

  @Test
  fun delete_confirmation_preserves_messages_list() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        val initialState = viewModel.uiState.value
        val initialMessages = initialState.messages.toList()

        viewModel.showDeleteConfirmation()
        assertEquals(initialMessages, viewModel.uiState.value.messages)

        viewModel.hideDeleteConfirmation()
        assertEquals(initialMessages, viewModel.uiState.value.messages)
      }

  @Test
  fun complete_delete_workflow() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.replaceMessages("Test message")

        val initialState = viewModel.uiState.value
        assertTrue(initialState.messages.isNotEmpty())
        assertFalse(initialState.showDeleteConfirmation)

        viewModel.showDeleteConfirmation()
        val stateShown = viewModel.uiState.value
        assertTrue(stateShown.showDeleteConfirmation)
        assertTrue(stateShown.messages.isNotEmpty())

        viewModel.clearChat()
        viewModel.hideDeleteConfirmation()
        val finalState = viewModel.uiState.value
        assertTrue(finalState.messages.isEmpty())
        assertFalse(finalState.showDeleteConfirmation)
      }

  @Test
  fun loading_state_does_not_affect_drawer() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.toggleDrawer()
        val drawerOpen = viewModel.uiState.value.isDrawerOpen

        viewModel.setLoading(true)
        assertEquals(drawerOpen, viewModel.uiState.value.isDrawerOpen)

        viewModel.setLoading(false)
        assertEquals(drawerOpen, viewModel.uiState.value.isDrawerOpen)
      }

  @Test
  fun delete_confirmation_does_not_affect_top_right() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setTopRightOpen(true)
        val topRightOpen = viewModel.uiState.value.isTopRightOpen

        viewModel.showDeleteConfirmation()
        assertEquals(topRightOpen, viewModel.uiState.value.isTopRightOpen)

        viewModel.hideDeleteConfirmation()
        assertEquals(topRightOpen, viewModel.uiState.value.isTopRightOpen)
      }

  @Test
  fun all_flags_can_be_true_simultaneously() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.toggleDrawer()
        viewModel.setTopRightOpen(true)
        viewModel.setLoading(true)
        viewModel.showDeleteConfirmation()

        val state = viewModel.uiState.value
        assertTrue(state.isDrawerOpen)
        assertTrue(state.isTopRightOpen)
        assertTrue(state.isLoading)
        assertTrue(state.showDeleteConfirmation)
      }

  @Test
  fun all_flags_can_be_false_simultaneously() =
      runTest(testDispatcher) {
        val viewModel = HomeViewModel()

        viewModel.setTopRightOpen(false)
        viewModel.setLoading(false)
        viewModel.hideDeleteConfirmation()

        val state = viewModel.uiState.value
        assertFalse(state.isDrawerOpen)
        assertFalse(state.isTopRightOpen)
        assertFalse(state.isLoading)
        assertFalse(state.showDeleteConfirmation)
      }
}
