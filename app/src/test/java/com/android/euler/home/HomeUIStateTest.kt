package com.android.euler.home

import com.android.sample.home.ActionItem
import com.android.sample.home.HomeUiState
import com.android.sample.home.SystemItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeUIStateTest {

    @Test
    fun `HomeUiState should have correct default values`() {
        val state = HomeUiState()

        assertEquals("Default userName should be 'Student'", "Student", state.userName)
        assertTrue("Default systems should be empty list", state.systems.isEmpty())
        assertTrue("Default recent should be empty list", state.recent.isEmpty())
        assertEquals("Default messageDraft should be empty", "", state.messageDraft)
        assertFalse("Default isDrawerOpen should be false", state.isDrawerOpen)
        assertFalse("Default isTopRightOpen should be false", state.isTopRightOpen)
        assertFalse("Default isLoading should be false", state.isLoading)
    }

    @Test
    fun `HomeUiState should accept custom values`() {
        val systems =
            listOf(
                SystemItem("test1", "Test System 1", true),
                SystemItem("test2", "Test System 2", false)
            )
        val recent =
            listOf(
                ActionItem("action1", "Test Action 1", "1h ago"),
                ActionItem("action2", "Test Action 2", "2h ago")
            )

        val state =
            HomeUiState(
                userName = "Custom User",
                systems = systems,
                recent = recent,
                messageDraft = "Custom message",
                isDrawerOpen = true,
                isTopRightOpen = true,
                isLoading = true
            )

        assertEquals("Custom userName should be set", "Custom User", state.userName)
        assertEquals("Custom systems should be set", systems, state.systems)
        assertEquals("Custom recent should be set", recent, state.recent)
        assertEquals("Custom messageDraft should be set", "Custom message", state.messageDraft)
        assertTrue("Custom isDrawerOpen should be true", state.isDrawerOpen)
        assertTrue("Custom isTopRightOpen should be true", state.isTopRightOpen)
        assertTrue("Custom isLoading should be true", state.isLoading)
    }

    @Test
    fun `HomeUiState should support copy with modifications`() {
        val originalState = HomeUiState()

        // Test copy avec modification d'une propriété
        val modifiedState = originalState.copy(userName = "Modified User")
        assertEquals("Modified userName should be set", "Modified User", modifiedState.userName)
        assertEquals("Other properties should remain unchanged", "", modifiedState.messageDraft)
        assertFalse("Other properties should remain unchanged", modifiedState.isDrawerOpen)

        // Test copy avec modification de plusieurs propriétés
        val systems = listOf(SystemItem("test", "Test System", true))
        val recent = listOf(ActionItem("action", "Test Action", "now"))

        val multiModifiedState =
            originalState.copy(
                userName = "Multi Modified User",
                systems = systems,
                recent = recent,
                messageDraft = "Modified message",
                isDrawerOpen = true
            )

        assertEquals(
            "Multiple properties should be modified",
            "Multi Modified User",
            multiModifiedState.userName
        )
        assertEquals("Systems should be modified", systems, multiModifiedState.systems)
        assertEquals("Recent should be modified", recent, multiModifiedState.recent)
        assertEquals(
            "MessageDraft should be modified",
            "Modified message",
            multiModifiedState.messageDraft
        )
        assertTrue("IsDrawerOpen should be modified", multiModifiedState.isDrawerOpen)
    }

    @Test
    fun `HomeUiState should handle empty and null-like values`() {
        val state =
            HomeUiState(
                userName = "",
                systems = emptyList(),
                recent = emptyList(),
                messageDraft = ""
            )

        assertEquals("Empty userName should be handled", "", state.userName)
        assertTrue("Empty systems should be handled", state.systems.isEmpty())
        assertTrue("Empty recent should be handled", state.recent.isEmpty())
        assertEquals("Empty messageDraft should be handled", "", state.messageDraft)
    }

    @Test
    fun `HomeUiState should be data class with proper equality`() {
        val state1 = HomeUiState(userName = "Test User")
        val state2 = HomeUiState(userName = "Test User")
        val state3 = HomeUiState(userName = "Different User")

        assertEquals("States with same values should be equal", state1, state2)
        assertNotEquals("States with different values should not be equal", state1, state3)
    }

    @Test
    fun `SystemItem should have correct properties`() {
        val system = SystemItem("test-id", "Test System", true)

        assertEquals("System ID should be set", "test-id", system.id)
        assertEquals("System name should be set", "Test System", system.name)
        assertTrue("System connection status should be set", system.isConnected)
    }

    @Test
    fun `SystemItem should support copy with modifications`() {
        val originalSystem = SystemItem("original-id", "Original System", false)

        val modifiedSystem =
            originalSystem.copy(id = "modified-id", name = "Modified System", isConnected = true)

        assertEquals("Modified ID should be set", "modified-id", modifiedSystem.id)
        assertEquals("Modified name should be set", "Modified System", modifiedSystem.name)
        assertTrue("Modified connection status should be set", modifiedSystem.isConnected)

        // Test modification partielle
        val partiallyModifiedSystem = originalSystem.copy(isConnected = true)
        assertEquals("Original ID should remain", "original-id", partiallyModifiedSystem.id)
        assertEquals("Original name should remain", "Original System", partiallyModifiedSystem.name)
        assertTrue("Only connection status should be modified", partiallyModifiedSystem.isConnected)
    }

    @Test
    fun `SystemItem should be data class with proper equality`() {
        val system1 = SystemItem("same-id", "Same System", true)
        val system2 = SystemItem("same-id", "Same System", true)
        val system3 = SystemItem("different-id", "Same System", true)

        assertEquals("Systems with same values should be equal", system1, system2)
        assertNotEquals("Systems with different values should not be equal", system1, system3)
    }

    @Test
    fun `SystemItem should handle edge cases`() {
        // Test avec des chaînes vides
        val emptySystem = SystemItem("", "", false)
        assertEquals("Empty ID should be handled", "", emptySystem.id)
        assertEquals("Empty name should be handled", "", emptySystem.name)
        assertFalse("False connection should be handled", emptySystem.isConnected)

        // Test avec des chaînes longues
        val longId = "very-long-system-id-that-might-be-used-in-real-scenarios"
        val longName = "Very Long System Name That Might Be Used In Real Scenarios"
        val longSystem = SystemItem(longId, longName, true)

        assertEquals("Long ID should be handled", longId, longSystem.id)
        assertEquals("Long name should be handled", longName, longSystem.name)
        assertTrue("True connection should be handled", longSystem.isConnected)
    }

    @Test
    fun `ActionItem should have correct properties`() {
        val action = ActionItem("action-id", "Test Action", "2h ago")

        assertEquals("Action ID should be set", "action-id", action.id)
        assertEquals("Action title should be set", "Test Action", action.title)
        assertEquals("Action time should be set", "2h ago", action.time)
    }

    @Test
    fun `ActionItem should support copy with modifications`() {
        val originalAction = ActionItem("original-id", "Original Action", "1h ago")

        val modifiedAction =
            originalAction.copy(id = "modified-id", title = "Modified Action", time = "2h ago")

        assertEquals("Modified ID should be set", "modified-id", modifiedAction.id)
        assertEquals("Modified title should be set", "Modified Action", modifiedAction.title)
        assertEquals("Modified time should be set", "2h ago", modifiedAction.time)

        // Test modification partielle
        val partiallyModifiedAction = originalAction.copy(time = "just now")
        assertEquals("Original ID should remain", "original-id", partiallyModifiedAction.id)
        assertEquals(
            "Original title should remain",
            "Original Action",
            partiallyModifiedAction.title
        )
        assertEquals("Only time should be modified", "just now", partiallyModifiedAction.time)
    }

    @Test
    fun `ActionItem should be data class with proper equality`() {
        val action1 = ActionItem("same-id", "Same Action", "1h ago")
        val action2 = ActionItem("same-id", "Same Action", "1h ago")
        val action3 = ActionItem("different-id", "Same Action", "1h ago")

        assertEquals("Actions with same values should be equal", action1, action2)
        assertNotEquals("Actions with different values should not be equal", action1, action3)
    }

    @Test
    fun `ActionItem should handle edge cases`() {
        // Test avec des chaînes vides
        val emptyAction = ActionItem("", "", "")
        assertEquals("Empty ID should be handled", "", emptyAction.id)
        assertEquals("Empty title should be handled", "", emptyAction.title)
        assertEquals("Empty time should be handled", "", emptyAction.time)

        // Test avec des chaînes longues
        val longId = "very-long-action-id-that-might-be-generated-by-uuid"
        val longTitle =
            "Very Long Action Title That Might Be Generated By The System In Real Scenarios"
        val longTime = "Very Long Time Description That Might Be Used In Real Scenarios"
        val longAction = ActionItem(longId, longTitle, longTime)

        assertEquals("Long ID should be handled", longId, longAction.id)
        assertEquals("Long title should be handled", longTitle, longAction.title)
        assertEquals("Long time should be handled", longTime, longAction.time)
    }

    @Test
    fun `HomeUiState with systems should work correctly`() {
        val systems =
            listOf(
                SystemItem("system1", "System 1", true),
                SystemItem("system2", "System 2", false),
                SystemItem("system3", "System 3", true)
            )

        val state = HomeUiState(systems = systems)

        assertEquals("Systems should be set correctly", systems, state.systems)
        assertEquals("Should have 3 systems", 3, state.systems.size)

        // Vérifier les systèmes individuels
        val system1 = state.systems.find { it.id == "system1" }
        assertNotNull("System 1 should exist", system1)
        assertEquals("System 1 should have correct name", "System 1", system1!!.name)
        assertTrue("System 1 should be connected", system1.isConnected)

        val system2 = state.systems.find { it.id == "system2" }
        assertNotNull("System 2 should exist", system2)
        assertEquals("System 2 should have correct name", "System 2", system2!!.name)
        assertFalse("System 2 should not be connected", system2.isConnected)
    }

    @Test
    fun `HomeUiState with recent actions should work correctly`() {
        val recent =
            listOf(
                ActionItem("action1", "Action 1", "1h ago"),
                ActionItem("action2", "Action 2", "2h ago"),
                ActionItem("action3", "Action 3", "3h ago")
            )

        val state = HomeUiState(recent = recent)

        assertEquals("Recent actions should be set correctly", recent, state.recent)
        assertEquals("Should have 3 recent actions", 3, state.recent.size)

        // Vérifier les actions individuelles
        val action1 = state.recent.find { it.id == "action1" }
        assertNotNull("Action 1 should exist", action1)
        assertEquals("Action 1 should have correct title", "Action 1", action1!!.title)
        assertEquals("Action 1 should have correct time", "1h ago", action1.time)
    }

    @Test
    fun `HomeUiState should handle complex scenarios`() {
        val systems =
            listOf(
                SystemItem("isa", "IS-Academia", true),
                SystemItem("moodle", "Moodle", true),
                SystemItem("ed", "Ed Discussion", false)
            )

        val recent =
            listOf(
                ActionItem("uuid1", "Posted a question on Ed Discussion", "2h ago"),
                ActionItem("uuid2", "Synced notes with EPFL Drive", "Yesterday")
            )

        val state =
            HomeUiState(
                userName = "EPFL Student",
                systems = systems,
                recent = recent,
                messageDraft = "Hello EULER!",
                isDrawerOpen = true,
                isTopRightOpen = false,
                isLoading = true
            )

        // Vérifier tous les aspects
        assertEquals("Complex state should work correctly", "EPFL Student", state.userName)
        assertEquals("Systems should be preserved", systems, state.systems)
        assertEquals("Recent should be preserved", recent, state.recent)
        assertEquals("Message draft should be preserved", "Hello EULER!", state.messageDraft)
        assertTrue("Drawer state should be preserved", state.isDrawerOpen)
        assertFalse("Top right state should be preserved", state.isTopRightOpen)
        assertTrue("Loading state should be preserved", state.isLoading)
    }

    @Test
    fun `data classes should have proper toString representation`() {
        val system = SystemItem("test-id", "Test System", true)
        val action = ActionItem("action-id", "Test Action", "1h ago")
        val state = HomeUiState(userName = "Test User")

        // Vérifier que toString contient les informations importantes
        assertTrue("System toString should contain ID", system.toString().contains("test-id"))
        assertTrue("System toString should contain name", system.toString().contains("Test System"))

        assertTrue("Action toString should contain ID", action.toString().contains("action-id"))
        assertTrue(
            "Action toString should contain title",
            action.toString().contains("Test Action")
        )

        assertTrue("State toString should contain userName", state.toString().contains("Test User"))
    }

    @Test
    fun `data classes should have proper hashCode`() {
        val system1 = SystemItem("test-id", "Test System", true)
        val system2 = SystemItem("test-id", "Test System", true)
        val system3 = SystemItem("different-id", "Test System", true)

        assertEquals(
            "Same systems should have same hashCode",
            system1.hashCode(),
            system2.hashCode()
        )
        assertNotEquals(
            "Different systems should have different hashCode",
            system1.hashCode(),
            system3.hashCode()
        )

        val action1 = ActionItem("action-id", "Test Action", "1h ago")
        val action2 = ActionItem("action-id", "Test Action", "1h ago")
        val action3 = ActionItem("different-id", "Test Action", "1h ago")

        assertEquals(
            "Same actions should have same hashCode",
            action1.hashCode(),
            action2.hashCode()
        )
        assertNotEquals(
            "Different actions should have different hashCode",
            action1.hashCode(),
            action3.hashCode()
        )
    }
}
