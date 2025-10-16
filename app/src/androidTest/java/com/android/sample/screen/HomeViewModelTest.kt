package com.android.sample.home

import org.junit.Assert.*
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun initial_state_is_correct() {
        val vm = HomeViewModel()
        val state = vm.uiState.value

        assertFalse(state.isDrawerOpen)
        assertFalse(state.isTopRightOpen)
        assertEquals("", state.messageDraft)
        assertFalse(state.isLoading)
        assertEquals("Student", state.userName)

        // quelques présences de base
        assertTrue(state.systems.any { it.id == "isa" && it.isConnected })
        assertTrue(state.systems.any { it.id == "camipro" && !it.isConnected })
        assertTrue(state.recent.isNotEmpty())
    }

    @Test
    fun initial_state_contains_exactly_6_systems() {
        val vm = HomeViewModel()
        val state = vm.uiState.value

        assertEquals(6, state.systems.size)
        assertTrue(state.systems.any { it.id == "isa" })
        assertTrue(state.systems.any { it.id == "moodle" })
        assertTrue(state.systems.any { it.id == "ed" })
        assertTrue(state.systems.any { it.id == "camipro" })
        assertTrue(state.systems.any { it.id == "mail" })
        assertTrue(state.systems.any { it.id == "drive" })
    }

    @Test
    fun initial_state_contains_exactly_3_recent_actions() {
        val vm = HomeViewModel()
        val state = vm.uiState.value

        assertEquals(3, state.recent.size)
        assertTrue(state.recent.all { it.id.isNotEmpty() })
        assertTrue(state.recent.all { it.title.isNotEmpty() })
        assertTrue(state.recent.all { it.time.isNotEmpty() })
    }

    @Test
    fun toggleDrawer_flips_drawer_state() {
        val vm = HomeViewModel()
        assertFalse(vm.uiState.value.isDrawerOpen)

        vm.toggleDrawer()
        assertTrue(vm.uiState.value.isDrawerOpen)

        vm.toggleDrawer()
        assertFalse(vm.uiState.value.isDrawerOpen)
    }

    @Test
    fun multiple_toggleDrawer_calls_alternate_correctly() {
        val vm = HomeViewModel()

        vm.toggleDrawer()
        assertTrue(vm.uiState.value.isDrawerOpen)

        vm.toggleDrawer()
        assertFalse(vm.uiState.value.isDrawerOpen)

        vm.toggleDrawer()
        assertTrue(vm.uiState.value.isDrawerOpen)

        vm.toggleDrawer()
        assertFalse(vm.uiState.value.isDrawerOpen)
    }

    @Test
    fun setTopRightOpen_sets_explicit_value() {
        val vm = HomeViewModel()
        vm.setTopRightOpen(true)
        assertTrue(vm.uiState.value.isTopRightOpen)

        vm.setTopRightOpen(false)
        assertFalse(vm.uiState.value.isTopRightOpen)
    }

    @Test
    fun setTopRightOpen_can_be_called_multiple_times_with_same_value() {
        val vm = HomeViewModel()

        vm.setTopRightOpen(true)
        assertTrue(vm.uiState.value.isTopRightOpen)

        vm.setTopRightOpen(true)
        assertTrue(vm.uiState.value.isTopRightOpen)

        vm.setTopRightOpen(false)
        assertFalse(vm.uiState.value.isTopRightOpen)

        vm.setTopRightOpen(false)
        assertFalse(vm.uiState.value.isTopRightOpen)
    }

    @Test
    fun updateMessageDraft_updates_text() {
        val vm = HomeViewModel()
        vm.updateMessageDraft("Hello Euler")
        assertEquals("Hello Euler", vm.uiState.value.messageDraft)
    }

    @Test
    fun updateMessageDraft_can_handle_empty_string() {
        val vm = HomeViewModel()

        vm.updateMessageDraft("Some text")
        assertEquals("Some text", vm.uiState.value.messageDraft)

        vm.updateMessageDraft("")
        assertEquals("", vm.uiState.value.messageDraft)
    }

    @Test
    fun updateMessageDraft_can_handle_long_text() {
        val vm = HomeViewModel()
        val longText =
            "This is a very long message that contains multiple words and sentences. " +
                "It should still be stored correctly in the state."

        vm.updateMessageDraft(longText)
        assertEquals(longText, vm.uiState.value.messageDraft)
    }

    @Test
    fun updateMessageDraft_can_handle_special_characters() {
        val vm = HomeViewModel()
        val specialText = "Hello! @User #hashtag \$money 100% täst émoji 🎉"

        vm.updateMessageDraft(specialText)
        assertEquals(specialText, vm.uiState.value.messageDraft)
    }

    @Test
    fun sendMessage_appends_new_action_and_clears_draft() {
        val vm = HomeViewModel()

        val before = vm.uiState.value.recent
        vm.updateMessageDraft("Find CS220 past exams")
        vm.sendMessage()

        val after = vm.uiState.value.recent
        // le nouveau message est en tête
        assertTrue(after.isNotEmpty())
        assertTrue(after.first().title.contains("Find CS220 past exams"))
        assertTrue(after.first().title.contains("EULER"))
        assertEquals("Just now", after.first().time)
        // list size +1
        assertEquals(before.size + 1, after.size)
        // draft vidé
        assertEquals("", vm.uiState.value.messageDraft)
    }

    @Test
    fun sendMessage_with_blank_draft_does_nothing() {
        val vm = HomeViewModel()

        val initial = vm.uiState.value.recent
        vm.updateMessageDraft("   ")
        vm.sendMessage()

        val now = vm.uiState.value.recent
        assertEquals(initial.size, now.size)
        assertEquals("   ", vm.uiState.value.messageDraft) // reste inchangé car trim() est interne
    }

    @Test
    fun sendMessage_with_empty_string_does_nothing() {
        val vm = HomeViewModel()

        val initial = vm.uiState.value.recent
        val initialSize = initial.size

        vm.updateMessageDraft("")
        vm.sendMessage()

        assertEquals(initialSize, vm.uiState.value.recent.size)
        assertEquals("", vm.uiState.value.messageDraft)
    }

    @Test
    fun sendMessage_multiple_times_adds_multiple_actions() {
        val vm = HomeViewModel()
        val initialSize = vm.uiState.value.recent.size

        vm.updateMessageDraft("Message 1")
        vm.sendMessage()

        vm.updateMessageDraft("Message 2")
        vm.sendMessage()

        vm.updateMessageDraft("Message 3")
        vm.sendMessage()

        assertEquals(initialSize + 3, vm.uiState.value.recent.size)
        assertTrue(vm.uiState.value.recent[0].title.contains("Message 3"))
        assertTrue(vm.uiState.value.recent[1].title.contains("Message 2"))
        assertTrue(vm.uiState.value.recent[2].title.contains("Message 1"))
    }

    @Test
    fun sendMessage_preserves_previous_actions_order() {
        val vm = HomeViewModel()
        val originalFirst = vm.uiState.value.recent.first()

        vm.updateMessageDraft("New message")
        vm.sendMessage()

        val after = vm.uiState.value.recent
        assertEquals(originalFirst.id, after[1].id)
        assertEquals(originalFirst.title, after[1].title)
    }

    @Test
    fun toggleSystemConnection_flips_the_flag_of_targeted_system() {
        val vm = HomeViewModel()

        // vérifie sur un système connu (ex: camipro est false au départ)
        val before = vm.uiState.value.systems.first { it.id == "camipro" }
        assertFalse(before.isConnected)

        vm.toggleSystemConnection("camipro")
        val after = vm.uiState.value.systems.first { it.id == "camipro" }
        assertTrue(after.isConnected)

        // re-toggle -> redevient false
        vm.toggleSystemConnection("camipro")
        val again = vm.uiState.value.systems.first { it.id == "camipro" }
        assertFalse(again.isConnected)
    }

    @Test
    fun toggleSystemConnection_with_connected_system_disconnects_it() {
        val vm = HomeViewModel()

        val before = vm.uiState.value.systems.first { it.id == "isa" }
        assertTrue(before.isConnected)

        vm.toggleSystemConnection("isa")
        val after = vm.uiState.value.systems.first { it.id == "isa" }
        assertFalse(after.isConnected)
    }

    @Test
    fun toggleSystemConnection_does_not_affect_other_systems() {
        val vm = HomeViewModel()

        val moodleBefore = vm.uiState.value.systems.first { it.id == "moodle" }.isConnected
        val edBefore = vm.uiState.value.systems.first { it.id == "ed" }.isConnected

        vm.toggleSystemConnection("isa")

        val moodleAfter = vm.uiState.value.systems.first { it.id == "moodle" }.isConnected
        val edAfter = vm.uiState.value.systems.first { it.id == "ed" }.isConnected

        assertEquals(moodleBefore, moodleAfter)
        assertEquals(edBefore, edAfter)
    }

    @Test
    fun toggleSystemConnection_with_non_existent_id_does_nothing() {
        val vm = HomeViewModel()

        val before = vm.uiState.value.systems
        vm.toggleSystemConnection("non-existent-id")
        val after = vm.uiState.value.systems

        assertEquals(before.size, after.size)
        before.forEachIndexed { index, system ->
            assertEquals(system.isConnected, after[index].isConnected)
        }
    }

    @Test
    fun toggleSystemConnection_multiple_times_for_same_system() {
        val vm = HomeViewModel()

        vm.toggleSystemConnection("mail")
        val state1 = vm.uiState.value.systems.first { it.id == "mail" }.isConnected

        vm.toggleSystemConnection("mail")
        val state2 = vm.uiState.value.systems.first { it.id == "mail" }.isConnected

        vm.toggleSystemConnection("mail")
        val state3 = vm.uiState.value.systems.first { it.id == "mail" }.isConnected

        assertNotEquals(state1, state2)
        assertEquals(state1, state3)
    }

    @Test
    fun setLoading_sets_explicit_loading_state() {
        val vm = HomeViewModel()
        assertFalse(vm.uiState.value.isLoading)

        vm.setLoading(true)
        assertTrue(vm.uiState.value.isLoading)

        vm.setLoading(false)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun setLoading_can_be_called_multiple_times_with_same_value() {
        val vm = HomeViewModel()

        vm.setLoading(true)
        assertTrue(vm.uiState.value.isLoading)

        vm.setLoading(true)
        assertTrue(vm.uiState.value.isLoading)

        vm.setLoading(false)
        assertFalse(vm.uiState.value.isLoading)

        vm.setLoading(false)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun all_operations_preserve_other_state_values() {
        val vm = HomeViewModel()

        val initialUserName = vm.uiState.value.userName
        val initialSystemsCount = vm.uiState.value.systems.size

        vm.toggleDrawer()
        vm.setTopRightOpen(true)
        vm.updateMessageDraft("test")
        vm.setLoading(true)

        assertEquals(initialUserName, vm.uiState.value.userName)
        assertEquals(initialSystemsCount, vm.uiState.value.systems.size)
    }

    @Test
    fun complex_workflow_scenario() {
        val vm = HomeViewModel()

        // Open drawer
        vm.toggleDrawer()
        assertTrue(vm.uiState.value.isDrawerOpen)

        // Toggle some systems
        vm.toggleSystemConnection("camipro")
        vm.toggleSystemConnection("mail")

        // Start loading
        vm.setLoading(true)

        // Type a message
        vm.updateMessageDraft("Check my grades")

        // Open top right panel
        vm.setTopRightOpen(true)

        // Send message
        vm.sendMessage()

        // Verify final state
        assertTrue(vm.uiState.value.isDrawerOpen)
        assertTrue(vm.uiState.value.isTopRightOpen)
        assertTrue(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.messageDraft)
        assertTrue(vm.uiState.value.recent.first().title.contains("Check my grades"))
        assertTrue(vm.uiState.value.systems.first { it.id == "camipro" }.isConnected)
        assertTrue(vm.uiState.value.systems.first { it.id == "mail" }.isConnected)
    }

    @Test
    fun sendMessage_with_whitespace_only_does_nothing() {
        val vm = HomeViewModel()
        val initialSize = vm.uiState.value.recent.size

        vm.updateMessageDraft(" \t\n ")
        vm.sendMessage()

        assertEquals(initialSize, vm.uiState.value.recent.size)
        assertEquals(" \t\n ", vm.uiState.value.messageDraft)
    }

    @Test
    fun sendMessage_trims_whitespace_but_preserves_content() {
        val vm = HomeViewModel()
        val initialSize = vm.uiState.value.recent.size

        vm.updateMessageDraft("  Hello World  ")
        vm.sendMessage()

        assertEquals(initialSize + 1, vm.uiState.value.recent.size)
        assertTrue(vm.uiState.value.recent.first().title.contains("Hello World"))
        assertEquals("", vm.uiState.value.messageDraft)
    }

    @Test
    fun initial_state_system_names_are_correct() {
        val vm = HomeViewModel()
        val state = vm.uiState.value

        assertEquals("IS-Academia", state.systems.first { it.id == "isa" }.name)
        assertEquals("Moodle", state.systems.first { it.id == "moodle" }.name)
        assertEquals("Ed Discussion", state.systems.first { it.id == "ed" }.name)
        assertEquals("Camipro", state.systems.first { it.id == "camipro" }.name)
        assertEquals("EPFL Mail", state.systems.first { it.id == "mail" }.name)
        assertEquals("EPFL Drive", state.systems.first { it.id == "drive" }.name)
    }

    @Test
    fun initial_state_system_connection_states_are_correct() {
        val vm = HomeViewModel()
        val state = vm.uiState.value

        assertTrue(state.systems.first { it.id == "isa" }.isConnected)
        assertTrue(state.systems.first { it.id == "moodle" }.isConnected)
        assertTrue(state.systems.first { it.id == "ed" }.isConnected)
        assertFalse(state.systems.first { it.id == "camipro" }.isConnected)
        assertFalse(state.systems.first { it.id == "mail" }.isConnected)
        assertTrue(state.systems.first { it.id == "drive" }.isConnected)
    }

    @Test
    fun initial_state_recent_actions_have_valid_UUIDs() {
        val vm = HomeViewModel()
        val state = vm.uiState.value

        state.recent.forEach { action ->
            assertTrue("ID should not be empty", action.id.isNotEmpty())
            assertTrue("ID should be longer than 10 chars", action.id.length > 10)
            assertTrue("Title should not be empty", action.title.isNotEmpty())
            assertTrue("Time should not be empty", action.time.isNotEmpty())
        }
    }

    @Test
    fun sendMessage_creates_action_with_correct_format() {
        val vm = HomeViewModel()
        val message = "Test message for EULER"

        vm.updateMessageDraft(message)
        vm.sendMessage()

        val newAction = vm.uiState.value.recent.first()
        assertTrue("Should contain EULER", newAction.title.contains("EULER"))
        assertTrue("Should contain message", newAction.title.contains(message))
        assertTrue("Should contain 'Messaged'", newAction.title.contains("Messaged"))
        assertEquals("Should be 'Just now'", "Just now", newAction.time)
    }

    @Test
    fun updateMessageDraft_handles_null_like_values() {
        val vm = HomeViewModel()

        vm.updateMessageDraft("")
        assertEquals("", vm.uiState.value.messageDraft)

        vm.updateMessageDraft("Valid text")
        assertEquals("Valid text", vm.uiState.value.messageDraft)
    }

    @Test
    fun toggleSystemConnection_with_multiple_systems_preserves_others() {
        val vm = HomeViewModel()
        val originalSystems = vm.uiState.value.systems.toList()

        // Toggle multiple systems
        vm.toggleSystemConnection("isa")
        vm.toggleSystemConnection("camipro")
        vm.toggleSystemConnection("drive")

        val newSystems = vm.uiState.value.systems

        // Check that other systems remain unchanged
        assertEquals(
            originalSystems.first { it.id == "moodle" }.isConnected,
            newSystems.first { it.id == "moodle" }.isConnected
        )
        assertEquals(
            originalSystems.first { it.id == "ed" }.isConnected,
            newSystems.first { it.id == "ed" }.isConnected
        )
        assertEquals(
            originalSystems.first { it.id == "mail" }.isConnected,
            newSystems.first { it.id == "mail" }.isConnected
        )
    }
}
