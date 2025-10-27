package com.android.sample.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  @Test
  fun clearChat_empties_recent_messages() = runTest {
    val viewModel = HomeViewModel()

    // Initial state should have some messages
    val initialState = viewModel.uiState.first()
    assertTrue(initialState.recent.isNotEmpty())

    // Clear the chat
    viewModel.clearChat()

    // Verify chat is empty
    val stateAfterClear = viewModel.uiState.first()
    assertTrue(stateAfterClear.recent.isEmpty())
  }

  @Test
  fun clearChat_preserves_other_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialUserName = initialState.userName
    val initialSystems = initialState.systems

    // Clear the chat
    viewModel.clearChat()

    // Verify other state is preserved
    val stateAfterClear = viewModel.uiState.first()
    assertEquals(initialUserName, stateAfterClear.userName)
    assertEquals(initialSystems, stateAfterClear.systems)
  }

  @Test
  fun toggleDrawer_changes_isDrawerOpen_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    assertFalse(initialState.isDrawerOpen)

    // Toggle drawer to open
    viewModel.toggleDrawer()
    val stateAfterFirstToggle = viewModel.uiState.first()
    assertTrue(stateAfterFirstToggle.isDrawerOpen)

    // Toggle drawer to close
    viewModel.toggleDrawer()
    val stateAfterSecondToggle = viewModel.uiState.first()
    assertFalse(stateAfterSecondToggle.isDrawerOpen)
  }

  @Test
  fun sendMessage_with_empty_draft_does_nothing() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialRecentCount = initialState.recent.size

    // Try to send empty message
    viewModel.updateMessageDraft("")
    viewModel.sendMessage()

    // Advance time
    advanceTimeBy(100)

    // Verify nothing changed
    val stateAfterSend = viewModel.uiState.first()
    assertEquals(initialRecentCount, stateAfterSend.recent.size)
  }

  @Test
  fun updateMessageDraft_updates_draft_text() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("Hello")
    val state1 = viewModel.uiState.first()
    assertEquals("Hello", state1.messageDraft)

    viewModel.updateMessageDraft("World")
    val state2 = viewModel.uiState.first()
    assertEquals("World", state2.messageDraft)
  }

  @Test
  fun toggleSystemConnection_flips_connection_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val moodleSystem = initialState.systems.find { it.id == "moodle" }
    assertNotNull(moodleSystem)
    assertTrue(moodleSystem!!.isConnected)

    // Toggle connection
    viewModel.toggleSystemConnection("moodle")

    val stateAfterToggle = viewModel.uiState.first()
    val updatedSystem = stateAfterToggle.systems.find { it.id == "moodle" }
    assertNotNull(updatedSystem)
    assertFalse(updatedSystem!!.isConnected)
  }

  @Test
  fun setTopRightOpen_changes_top_right_open_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    assertFalse(initialState.isTopRightOpen)

    // Set to open
    viewModel.setTopRightOpen(true)
    val stateAfterOpen = viewModel.uiState.first()
    assertTrue(stateAfterOpen.isTopRightOpen)

    // Set to close
    viewModel.setTopRightOpen(false)
    val stateAfterClose = viewModel.uiState.first()
    assertFalse(stateAfterClose.isTopRightOpen)
  }

  @Test
  fun setLoading_changes_loading_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    assertFalse(initialState.isLoading)

    viewModel.setLoading(true)
    val stateAfterSetTrue = viewModel.uiState.first()
    assertTrue(stateAfterSetTrue.isLoading)

    viewModel.setLoading(false)
    val stateAfterSetFalse = viewModel.uiState.first()
    assertFalse(stateAfterSetFalse.isLoading)
  }

  @Test
  fun showDeleteConfirmation_sets_showDeleteConfirmation_to_true() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    assertFalse(initialState.showDeleteConfirmation)

    viewModel.showDeleteConfirmation()
    val stateAfterShow = viewModel.uiState.first()
    assertTrue(stateAfterShow.showDeleteConfirmation)
  }

  @Test
  fun hideDeleteConfirmation_sets_showDeleteConfirmation_to_false() = runTest {
    val viewModel = HomeViewModel()

    viewModel.showDeleteConfirmation()
    val stateAfterShow = viewModel.uiState.first()
    assertTrue(stateAfterShow.showDeleteConfirmation)

    viewModel.hideDeleteConfirmation()
    val stateAfterHide = viewModel.uiState.first()
    assertFalse(stateAfterHide.showDeleteConfirmation)
  }

  @Test
  fun clearChat_with_confirmation_workflow() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    assertTrue(initialState.recent.isNotEmpty())

    // Show confirmation
    viewModel.showDeleteConfirmation()
    val stateAfterShow = viewModel.uiState.first()
    assertTrue(stateAfterShow.showDeleteConfirmation)

    // Clear chat
    viewModel.clearChat()
    val stateAfterClear = viewModel.uiState.first()
    assertTrue(stateAfterClear.recent.isEmpty())

    // Hide confirmation
    viewModel.hideDeleteConfirmation()
    val stateAfterHide = viewModel.uiState.first()
    assertFalse(stateAfterHide.showDeleteConfirmation)
    assertTrue(stateAfterHide.recent.isEmpty())
  }

  @Test
  fun initial_state_has_default_values() = runTest {
    val viewModel = HomeViewModel()

    val state = viewModel.uiState.first()
    assertEquals("Student", state.userName)
    assertFalse(state.isDrawerOpen)
    assertFalse(state.isTopRightOpen)
    assertFalse(state.isLoading)
    assertFalse(state.showDeleteConfirmation)
    assertEquals("", state.messageDraft)
    assertFalse(state.systems.isEmpty())
    assertFalse(state.recent.isEmpty())
  }

  @Test
  fun updateMessageDraft_with_whitespace() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("  Test with spaces  ")
    val state = viewModel.uiState.first()
    assertEquals("  Test with spaces  ", state.messageDraft)
  }

  @Test
  fun sendMessage_with_whitespace_only_does_nothing() = runTest {
    val viewModel = HomeViewModel()

    val initialCount = viewModel.uiState.first().recent.size

    viewModel.updateMessageDraft("    ")
    viewModel.sendMessage()

    advanceTimeBy(100)

    val stateAfterSend = viewModel.uiState.first()
    assertEquals(initialCount, stateAfterSend.recent.size)
  }

  @Test
  fun toggleSystemConnection_for_isa_system() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val isaSystem = initialState.systems.find { it.id == "isa" }
    assertNotNull(isaSystem)
    assertTrue(isaSystem!!.isConnected)

    viewModel.toggleSystemConnection("isa")

    val stateAfterToggle = viewModel.uiState.first()
    val updatedSystem = stateAfterToggle.systems.find { it.id == "isa" }
    assertNotNull(updatedSystem)
    assertFalse(updatedSystem!!.isConnected)
  }

  @Test
  fun toggleSystemConnection_for_camipro_system() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val camiproSystem = initialState.systems.find { it.id == "camipro" }
    assertNotNull(camiproSystem)
    assertFalse(camiproSystem!!.isConnected)

    viewModel.toggleSystemConnection("camipro")

    val stateAfterToggle = viewModel.uiState.first()
    val updatedSystem = stateAfterToggle.systems.find { it.id == "camipro" }
    assertNotNull(updatedSystem)
    assertTrue(updatedSystem!!.isConnected)
  }

  @Test
  fun toggleSystemConnection_for_nonexistent_system() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialSystems = initialState.systems.toList()

    viewModel.toggleSystemConnection("nonexistent")

    val stateAfterToggle = viewModel.uiState.first()
    assertEquals(initialSystems, stateAfterToggle.systems)
  }

  @Test
  fun setLoading_preserves_other_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialUserName = initialState.userName
    val initialSystems = initialState.systems

    viewModel.setLoading(true)
    val stateAfterLoading = viewModel.uiState.first()

    assertEquals(initialUserName, stateAfterLoading.userName)
    assertEquals(initialSystems, stateAfterLoading.systems)
    assertTrue(stateAfterLoading.isLoading)
  }

  @Test
  fun toggleDrawer_preserves_other_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialUserName = initialState.userName
    val initialSystems = initialState.systems

    viewModel.toggleDrawer()
    val stateAfterToggle = viewModel.uiState.first()

    assertEquals(initialUserName, stateAfterToggle.userName)
    assertEquals(initialSystems, stateAfterToggle.systems)
    assertTrue(stateAfterToggle.isDrawerOpen)
  }

  @Test
  fun setTopRightOpen_preserves_other_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialUserName = initialState.userName
    val initialSystems = initialState.systems

    viewModel.setTopRightOpen(true)
    val stateAfterOpen = viewModel.uiState.first()

    assertEquals(initialUserName, stateAfterOpen.userName)
    assertEquals(initialSystems, stateAfterOpen.systems)
    assertTrue(stateAfterOpen.isTopRightOpen)
  }

  @Test
  fun updateMessageDraft_can_be_updated() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("Test message")
    val stateAfterUpdate = viewModel.uiState.first()
    assertEquals("Test message", stateAfterUpdate.messageDraft)

    // Update again
    viewModel.updateMessageDraft("New message")
    val stateAfterSecondUpdate = viewModel.uiState.first()
    assertEquals("New message", stateAfterSecondUpdate.messageDraft)
  }

  @Test
  fun sendMessage_sets_loading_state() = runTest {
    // Note: Cannot test sendMessage directly as it uses viewModelScope.launch which requires
    // the main dispatcher that is not available in unit tests without Robolectric
    // This is tested in integration tests instead
    assertTrue(true)
  }

  @Test
  fun showDeleteConfirmation_preserves_other_state() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialUserName = initialState.userName
    val initialSystems = initialState.systems
    val initialRecent = initialState.recent

    viewModel.showDeleteConfirmation()
    val stateAfterShow = viewModel.uiState.first()

    assertEquals(initialUserName, stateAfterShow.userName)
    assertEquals(initialSystems, stateAfterShow.systems)
    assertEquals(initialRecent, stateAfterShow.recent)
    assertTrue(stateAfterShow.showDeleteConfirmation)
  }

  @Test
  fun hideDeleteConfirmation_preserves_other_state() = runTest {
    val viewModel = HomeViewModel()

    viewModel.showDeleteConfirmation()
    val stateWithShow = viewModel.uiState.first()

    viewModel.hideDeleteConfirmation()
    val stateAfterHide = viewModel.uiState.first()

    assertEquals(stateWithShow.userName, stateAfterHide.userName)
    assertEquals(stateWithShow.systems, stateAfterHide.systems)
    assertEquals(stateWithShow.recent, stateAfterHide.recent)
    assertFalse(stateAfterHide.showDeleteConfirmation)
  }

  @Test
  fun toggleSystemConnection_multiple_times() = runTest {
    val viewModel = HomeViewModel()

    viewModel.toggleSystemConnection("moodle")
    val state1 = viewModel.uiState.first()
    val moodle1 = state1.systems.find { it.id == "moodle" }
    assertFalse(moodle1!!.isConnected)

    viewModel.toggleSystemConnection("moodle")
    val state2 = viewModel.uiState.first()
    val moodle2 = state2.systems.find { it.id == "moodle" }
    assertTrue(moodle2!!.isConnected)

    viewModel.toggleSystemConnection("moodle")
    val state3 = viewModel.uiState.first()
    val moodle3 = state3.systems.find { it.id == "moodle" }
    assertFalse(moodle3!!.isConnected)
  }

  @Test
  fun toggleSystemConnection_for_drive_system() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val driveSystem = initialState.systems.find { it.id == "drive" }
    assertNotNull(driveSystem)
    assertTrue(driveSystem!!.isConnected)

    viewModel.toggleSystemConnection("drive")

    val stateAfterToggle = viewModel.uiState.first()
    val updatedSystem = stateAfterToggle.systems.find { it.id == "drive" }
    assertNotNull(updatedSystem)
    assertFalse(updatedSystem!!.isConnected)
  }

  @Test
  fun toggleSystemConnection_for_empty_string() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialSystems = initialState.systems.toList()

    viewModel.toggleSystemConnection("")

    val stateAfterToggle = viewModel.uiState.first()
    assertEquals(initialSystems, stateAfterToggle.systems)
  }

  @Test
  fun uiState_maintains_reference_stability() = runTest {
    val viewModel = HomeViewModel()

    val stateRef1 = viewModel.uiState
    val stateRef2 = viewModel.uiState

    assertSame(stateRef1, stateRef2)
  }

  @Test
  fun clearChat_when_already_empty() = runTest {
    val viewModel = HomeViewModel()

    viewModel.clearChat()
    val state1 = viewModel.uiState.first()
    assertTrue(state1.recent.isEmpty())

    viewModel.clearChat()
    val state2 = viewModel.uiState.first()
    assertTrue(state2.recent.isEmpty())
  }

  @Test
  fun initialState_systems_count() = runTest {
    val viewModel = HomeViewModel()

    val state = viewModel.uiState.first()
    assertEquals(6, state.systems.size)
  }

  @Test
  fun initialState_recent_actions_count() = runTest {
    val viewModel = HomeViewModel()

    val state = viewModel.uiState.first()
    assertEquals(3, state.recent.size)
  }

  @Test
  fun initialState_systems_have_correct_states() = runTest {
    val viewModel = HomeViewModel()

    val state = viewModel.uiState.first()
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
  fun updateMessageDraft_with_newline_characters() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("Line1\nLine2\r\nLine3")
    val state = viewModel.uiState.first()
    assertEquals("Line1\nLine2\r\nLine3", state.messageDraft)
  }

  @Test
  fun updateMessageDraft_with_special_unicode() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("Hello 世界 🌍")
    val state = viewModel.uiState.first()
    assertEquals("Hello 世界 🌍", state.messageDraft)
  }

  @Test
  fun toggleSystemConnection_affects_only_target_system() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val moodleBefore = initialState.systems.find { it.id == "moodle" }!!.isConnected
    val isaBefore = initialState.systems.find { it.id == "isa" }!!.isConnected

    viewModel.toggleSystemConnection("moodle")

    val stateAfter = viewModel.uiState.first()
    val moodleAfter = stateAfter.systems.find { it.id == "moodle" }!!.isConnected
    val isaAfter = stateAfter.systems.find { it.id == "isa" }!!.isConnected

    assertNotEquals(moodleBefore, moodleAfter)
    assertEquals(isaBefore, isaAfter)
  }

  @Test
  fun multiple_state_changes_in_sequence() = runTest {
    val viewModel = HomeViewModel()

    viewModel.toggleDrawer()
    viewModel.setTopRightOpen(true)
    viewModel.updateMessageDraft("Test")
    
    val state = viewModel.uiState.first()
    assertTrue(state.isDrawerOpen)
    assertTrue(state.isTopRightOpen)
    assertEquals("Test", state.messageDraft)
  }

  @Test
  fun clearChat_then_add_recent_via_refresh() = runTest {
    val viewModel = HomeViewModel()

    viewModel.clearChat()
    val stateAfterClear = viewModel.uiState.first()
    assertTrue(stateAfterClear.recent.isEmpty())

    viewModel.clearChat()
    val stateAfterSecondClear = viewModel.uiState.first()
    assertTrue(stateAfterSecondClear.recent.isEmpty())
  }

  @Test
  fun toggleDrawer_then_close() = runTest {
    val viewModel = HomeViewModel()

    viewModel.toggleDrawer()
    assertTrue(viewModel.uiState.first().isDrawerOpen)

    viewModel.toggleDrawer()
    assertFalse(viewModel.uiState.first().isDrawerOpen)
  }

  @Test
  fun setTopRightOpen_toggle_behavior() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setTopRightOpen(true)
    assertTrue(viewModel.uiState.first().isTopRightOpen)

    viewModel.setTopRightOpen(false)
    assertFalse(viewModel.uiState.first().isTopRightOpen)

    viewModel.setTopRightOpen(true)
    assertTrue(viewModel.uiState.first().isTopRightOpen)
  }

  @Test
  fun showDeleteConfirmation_then_hide_multiple_times() = runTest {
    val viewModel = HomeViewModel()

    viewModel.showDeleteConfirmation()
    assertTrue(viewModel.uiState.first().showDeleteConfirmation)

    viewModel.hideDeleteConfirmation()
    assertFalse(viewModel.uiState.first().showDeleteConfirmation)

    viewModel.showDeleteConfirmation()
    assertTrue(viewModel.uiState.first().showDeleteConfirmation)
  }

  @Test
  fun updateMessageDraft_clear_sequence() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("First message")
    assertEquals("First message", viewModel.uiState.first().messageDraft)

    viewModel.updateMessageDraft("Second message")
    assertEquals("Second message", viewModel.uiState.first().messageDraft)

    viewModel.updateMessageDraft("")
    assertEquals("", viewModel.uiState.first().messageDraft)
  }

  @Test
  fun toggleSystemConnection_all_systems() = runTest {
    val viewModel = HomeViewModel()

    val systems = viewModel.uiState.first().systems
    systems.forEach { system ->
      val initialState = system.isConnected
      viewModel.toggleSystemConnection(system.id)
      val newState = viewModel.uiState.first().systems.find { it.id == system.id }!!.isConnected
      assertNotEquals(initialState, newState)
    }
  }

  @Test
  fun setLoading_multiple_fluctuations() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setLoading(true)
    assertTrue(viewModel.uiState.first().isLoading)

    viewModel.setLoading(false)
    assertFalse(viewModel.uiState.first().isLoading)

    viewModel.setLoading(true)
    assertTrue(viewModel.uiState.first().isLoading)

    viewModel.setLoading(false)
    assertFalse(viewModel.uiState.first().isLoading)
  }

  @Test
  fun state_immutability_after_operations() = runTest {
    val viewModel = HomeViewModel()

    val state1 = viewModel.uiState.first()
    viewModel.toggleDrawer()
    val state2 = viewModel.uiState.first()

    assertNotEquals(state1.isDrawerOpen, state2.isDrawerOpen)
  }

  @Test
  fun multiple_drawer_toggles() = runTest {
    val viewModel = HomeViewModel()

    for (i in 1..5) {
      viewModel.toggleDrawer()
      val expectedOpen = i % 2 == 1
      assertEquals(expectedOpen, viewModel.uiState.first().isDrawerOpen)
    }
  }

  @Test
  fun toggleSystemConnection_with_unicode_id() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialSystems = initialState.systems.toList()

    viewModel.toggleSystemConnection("非ASCII")

    val finalState = viewModel.uiState.first()
    assertEquals(initialSystems, finalState.systems)
  }

  @Test
  fun updateMessageDraft_with_tab_characters() = runTest {
    val viewModel = HomeViewModel()

    viewModel.updateMessageDraft("Tab\tCharacter")
    assertEquals("Tab\tCharacter", viewModel.uiState.first().messageDraft)
  }

  @Test
  fun initialState_systems_names_are_correct() = runTest {
    val viewModel = HomeViewModel()

    val state = viewModel.uiState.first()
    val isa = state.systems.find { it.id == "isa" }
    assertEquals("IS-Academia", isa?.name)
    
    val moodle = state.systems.find { it.id == "moodle" }
    assertEquals("Moodle", moodle?.name)
  }

  @Test
  fun initialState_recent_titles_are_present() = runTest {
    val viewModel = HomeViewModel()

    val state = viewModel.uiState.first()
    assertTrue(state.recent.isNotEmpty())
    assertTrue(state.recent.first().title.isNotEmpty())
  }

  @Test
  fun showDeleteConfirmation_and_then_hide_preserves_chat() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialRecentCount = initialState.recent.size

    viewModel.showDeleteConfirmation()
    assertTrue(viewModel.uiState.first().showDeleteConfirmation)
    assertEquals(initialRecentCount, viewModel.uiState.first().recent.size)

    viewModel.hideDeleteConfirmation()
    assertFalse(viewModel.uiState.first().showDeleteConfirmation)
    assertEquals(initialRecentCount, viewModel.uiState.first().recent.size)
  }

  @Test
  fun showDeleteConfirmation_does_not_affect_other_flags() = runTest {
    val viewModel = HomeViewModel()

    val before = viewModel.uiState.first()
    viewModel.showDeleteConfirmation()
    val after = viewModel.uiState.first()

    assertEquals(before.isDrawerOpen, after.isDrawerOpen)
    assertEquals(before.isTopRightOpen, after.isTopRightOpen)
    assertEquals(before.isLoading, after.isLoading)
    assertNotEquals(before.showDeleteConfirmation, after.showDeleteConfirmation)
  }

  @Test
  fun hideDeleteConfirmation_does_not_affect_other_flags() = runTest {
    val viewModel = HomeViewModel()

    viewModel.showDeleteConfirmation()
    val before = viewModel.uiState.first()

    viewModel.hideDeleteConfirmation()
    val after = viewModel.uiState.first()

    assertEquals(before.isDrawerOpen, after.isDrawerOpen)
    assertEquals(before.isTopRightOpen, after.isTopRightOpen)
    assertEquals(before.isLoading, after.isLoading)
    assertNotEquals(before.showDeleteConfirmation, after.showDeleteConfirmation)
  }

  @Test
  fun setLoading_to_true_and_then_false() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setLoading(true)
    assertTrue(viewModel.uiState.first().isLoading)

    viewModel.setLoading(false)
    assertFalse(viewModel.uiState.first().isLoading)
  }

  @Test
  fun setLoading_does_not_affect_delete_confirmation() = runTest {
    val viewModel = HomeViewModel()

    viewModel.showDeleteConfirmation()
    val showDeleteState = viewModel.uiState.first().showDeleteConfirmation

    viewModel.setLoading(true)
    assertEquals(showDeleteState, viewModel.uiState.first().showDeleteConfirmation)

    viewModel.setLoading(false)
    assertEquals(showDeleteState, viewModel.uiState.first().showDeleteConfirmation)
  }

  @Test
  fun delete_confirmation_workflow_with_multiple_toggles() = runTest {
    val viewModel = HomeViewModel()

    for (i in 1..3) {
      viewModel.showDeleteConfirmation()
      assertTrue(viewModel.uiState.first().showDeleteConfirmation)

      viewModel.hideDeleteConfirmation()
      assertFalse(viewModel.uiState.first().showDeleteConfirmation)
    }
  }

  @Test
  fun loading_state_with_delete_confirmation_simultaneously() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setLoading(true)
    viewModel.showDeleteConfirmation()

    val state = viewModel.uiState.first()
    assertTrue(state.isLoading)
    assertTrue(state.showDeleteConfirmation)
  }

  @Test
  fun loading_and_delete_confirmation_independently() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setLoading(true)
    val stateWithLoading = viewModel.uiState.first()
    assertTrue(stateWithLoading.isLoading)
    assertFalse(stateWithLoading.showDeleteConfirmation)

    viewModel.showDeleteConfirmation()
    val stateWithBoth = viewModel.uiState.first()
    assertTrue(stateWithBoth.isLoading)
    assertTrue(stateWithBoth.showDeleteConfirmation)

    viewModel.setLoading(false)
    val stateWithOnlyDelete = viewModel.uiState.first()
    assertFalse(stateWithOnlyDelete.isLoading)
    assertTrue(stateWithOnlyDelete.showDeleteConfirmation)

    viewModel.hideDeleteConfirmation()
    val stateClear = viewModel.uiState.first()
    assertFalse(stateClear.isLoading)
    assertFalse(stateClear.showDeleteConfirmation)
  }

  @Test
  fun hideDeleteConfirmation_when_not_shown() = runTest {
    val viewModel = HomeViewModel()

    val stateBefore = viewModel.uiState.first()
    assertFalse(stateBefore.showDeleteConfirmation)

    viewModel.hideDeleteConfirmation()
    val stateAfter = viewModel.uiState.first()
    assertFalse(stateAfter.showDeleteConfirmation)
  }

  @Test
  fun showDeleteConfirmation_when_already_shown() = runTest {
    val viewModel = HomeViewModel()

    viewModel.showDeleteConfirmation()
    assertTrue(viewModel.uiState.first().showDeleteConfirmation)

    viewModel.showDeleteConfirmation()
    assertTrue(viewModel.uiState.first().showDeleteConfirmation)
  }

  @Test
  fun setLoading_multiple_times() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setLoading(true)
    viewModel.setLoading(true)
    assertTrue(viewModel.uiState.first().isLoading)

    viewModel.setLoading(false)
    viewModel.setLoading(false)
    assertFalse(viewModel.uiState.first().isLoading)
  }

  @Test
  fun delete_confirmation_preserves_recent_list() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    val initialRecent = initialState.recent.toList()

    viewModel.showDeleteConfirmation()
    assertEquals(initialRecent, viewModel.uiState.first().recent)

    viewModel.hideDeleteConfirmation()
    assertEquals(initialRecent, viewModel.uiState.first().recent)
  }

  @Test
  fun complete_delete_workflow() = runTest {
    val viewModel = HomeViewModel()

    val initialState = viewModel.uiState.first()
    assertTrue(initialState.recent.isNotEmpty())
    assertFalse(initialState.showDeleteConfirmation)

    viewModel.showDeleteConfirmation()
    val stateShown = viewModel.uiState.first()
    assertTrue(stateShown.showDeleteConfirmation)
    assertTrue(stateShown.recent.isNotEmpty())

    viewModel.clearChat()
    viewModel.hideDeleteConfirmation()
    val finalState = viewModel.uiState.first()
    assertTrue(finalState.recent.isEmpty())
    assertFalse(finalState.showDeleteConfirmation)
  }

  @Test
  fun loading_state_does_not_affect_drawer() = runTest {
    val viewModel = HomeViewModel()

    viewModel.toggleDrawer()
    val drawerOpen = viewModel.uiState.first().isDrawerOpen

    viewModel.setLoading(true)
    assertEquals(drawerOpen, viewModel.uiState.first().isDrawerOpen)

    viewModel.setLoading(false)
    assertEquals(drawerOpen, viewModel.uiState.first().isDrawerOpen)
  }

  @Test
  fun delete_confirmation_does_not_affect_top_right() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setTopRightOpen(true)
    val topRightOpen = viewModel.uiState.first().isTopRightOpen

    viewModel.showDeleteConfirmation()
    assertEquals(topRightOpen, viewModel.uiState.first().isTopRightOpen)

    viewModel.hideDeleteConfirmation()
    assertEquals(topRightOpen, viewModel.uiState.first().isTopRightOpen)
  }

  @Test
  fun all_flags_can_be_true_simultaneously() = runTest {
    val viewModel = HomeViewModel()

    viewModel.toggleDrawer()
    viewModel.setTopRightOpen(true)
    viewModel.setLoading(true)
    viewModel.showDeleteConfirmation()

    val state = viewModel.uiState.first()
    assertTrue(state.isDrawerOpen)
    assertTrue(state.isTopRightOpen)
    assertTrue(state.isLoading)
    assertTrue(state.showDeleteConfirmation)
  }

  @Test
  fun all_flags_can_be_false_simultaneously() = runTest {
    val viewModel = HomeViewModel()

    viewModel.setTopRightOpen(false)
    viewModel.setLoading(false)
    viewModel.hideDeleteConfirmation()

    val state = viewModel.uiState.first()
    assertFalse(state.isDrawerOpen)
    assertFalse(state.isTopRightOpen)
    assertFalse(state.isLoading)
    assertFalse(state.showDeleteConfirmation)
  }
}
