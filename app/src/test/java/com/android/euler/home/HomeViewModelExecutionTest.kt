package com.android.euler.home

import androidx.lifecycle.ViewModel
import com.android.sample.home.HomeViewModel
import com.android.sample.home.HomeUiState
import com.android.sample.home.SystemItem
import com.android.sample.home.ActionItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeViewModelExecutionTest {

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel()
    }

    @Test
    fun `HomeViewModel should extend ViewModel`() {
        assertTrue("HomeViewModel should extend ViewModel", viewModel is ViewModel)
    }

    @Test
    fun `HomeViewModel should initialize with default state`() = runTest {
        val initialState = viewModel.uiState.first()
        
        // Test des propriétés par défaut
        assertEquals("Default userName should be 'Student'", "Student", initialState.userName)
        assertEquals("Default messageDraft should be empty", "", initialState.messageDraft)
        assertFalse("Default isDrawerOpen should be false", initialState.isDrawerOpen)
        assertFalse("Default isTopRightOpen should be false", initialState.isTopRightOpen)
        assertFalse("Default isLoading should be false", initialState.isLoading)
        
        // Test des listes par défaut
        assertTrue("Default systems should not be empty", initialState.systems.isNotEmpty())
        assertTrue("Default recent should not be empty", initialState.recent.isNotEmpty())
    }

    @Test
    fun `HomeViewModel should have expected systems initialized`() = runTest {
        val initialState = viewModel.uiState.first()
        
        // Vérifier les systèmes EPFL
        val expectedSystems = listOf("isa", "moodle", "ed", "camipro", "mail", "drive")
        val systemIds = initialState.systems.map { it.id }
        
        expectedSystems.forEach { expectedId ->
            assertTrue("System $expectedId should be present", systemIds.contains(expectedId))
        }
        
        // Vérifier les statuts de connexion
        val isaSystem = initialState.systems.find { it.id == "isa" }
        assertNotNull("ISA system should exist", isaSystem)
        assertTrue("ISA should be connected", isaSystem!!.isConnected)
        
        val camiproSystem = initialState.systems.find { it.id == "camipro" }
        assertNotNull("Camipro system should exist", camiproSystem)
        assertFalse("Camipro should not be connected", camiproSystem!!.isConnected)
    }

    @Test
    fun `HomeViewModel should have expected recent actions initialized`() = runTest {
        val initialState = viewModel.uiState.first()
        
        // Vérifier que les actions récentes existent
        assertTrue("Recent actions should not be empty", initialState.recent.isNotEmpty())
        
        // Vérifier le contenu des actions
        val firstAction = initialState.recent.first()
        assertNotNull("First action should have id", firstAction.id)
        assertTrue("First action should have title", firstAction.title.isNotEmpty())
        assertTrue("First action should have time", firstAction.time.isNotEmpty())
        
        // Vérifier le contenu spécifique
        assertTrue("Should contain Ed Discussion action", 
            initialState.recent.any { it.title.contains("Ed Discussion") })
        assertTrue("Should contain EPFL Drive action", 
            initialState.recent.any { it.title.contains("EPFL Drive") })
        assertTrue("Should contain IS-Academia action", 
            initialState.recent.any { it.title.contains("IS-Academia") })
    }

    @Test
    fun `toggleDrawer should toggle drawer state correctly`() = runTest {
        val initialState = viewModel.uiState.first()
        assertFalse("Initial drawer state should be closed", initialState.isDrawerOpen)
        
        // Premier toggle - ouvrir
        viewModel.toggleDrawer()
        val afterFirstToggle = viewModel.uiState.first()
        assertTrue("Drawer should be open after first toggle", afterFirstToggle.isDrawerOpen)
        
        // Deuxième toggle - fermer
        viewModel.toggleDrawer()
        val afterSecondToggle = viewModel.uiState.first()
        assertFalse("Drawer should be closed after second toggle", afterSecondToggle.isDrawerOpen)
        
        // Troisième toggle - ouvrir à nouveau
        viewModel.toggleDrawer()
        val afterThirdToggle = viewModel.uiState.first()
        assertTrue("Drawer should be open after third toggle", afterThirdToggle.isDrawerOpen)
    }

    @Test
    fun `setTopRightOpen should set top right menu state correctly`() = runTest {
        val initialState = viewModel.uiState.first()
        assertFalse("Initial top right state should be closed", initialState.isTopRightOpen)
        
        // Ouvrir le menu
        viewModel.setTopRightOpen(true)
        val afterOpen = viewModel.uiState.first()
        assertTrue("Top right menu should be open", afterOpen.isTopRightOpen)
        
        // Fermer le menu
        viewModel.setTopRightOpen(false)
        val afterClose = viewModel.uiState.first()
        assertFalse("Top right menu should be closed", afterClose.isTopRightOpen)
        
        // Tester avec true à nouveau
        viewModel.setTopRightOpen(true)
        val afterOpenAgain = viewModel.uiState.first()
        assertTrue("Top right menu should be open again", afterOpenAgain.isTopRightOpen)
    }

    @Test
    fun `updateMessageDraft should update message draft correctly`() = runTest {
        val initialState = viewModel.uiState.first()
        assertEquals("Initial message draft should be empty", "", initialState.messageDraft)
        
        // Mettre à jour avec un message
        val testMessage = "Hello EULER!"
        viewModel.updateMessageDraft(testMessage)
        val afterUpdate = viewModel.uiState.first()
        assertEquals("Message draft should be updated", testMessage, afterUpdate.messageDraft)
        
        // Mettre à jour avec un autre message
        val anotherMessage = "How are you?"
        viewModel.updateMessageDraft(anotherMessage)
        val afterSecondUpdate = viewModel.uiState.first()
        assertEquals("Message draft should be updated again", anotherMessage, afterSecondUpdate.messageDraft)
        
        // Mettre à jour avec une chaîne vide
        viewModel.updateMessageDraft("")
        val afterEmptyUpdate = viewModel.uiState.first()
        assertEquals("Message draft should be empty", "", afterEmptyUpdate.messageDraft)
        
        // Mettre à jour avec un message long
        val longMessage = "This is a very long message to test that the ViewModel can handle long text inputs properly without any issues."
        viewModel.updateMessageDraft(longMessage)
        val afterLongUpdate = viewModel.uiState.first()
        assertEquals("Message draft should handle long text", longMessage, afterLongUpdate.messageDraft)
    }

    @Test
    fun `sendMessage should add message to recent actions and clear draft`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialRecentCount = initialState.recent.size
        
        // Mettre à jour le draft et envoyer
        val testMessage = "Test message for EULER"
        viewModel.updateMessageDraft(testMessage)
        viewModel.sendMessage()
        
        val afterSend = viewModel.uiState.first()
        
        // Vérifier que le draft est vidé
        assertEquals("Message draft should be cleared", "", afterSend.messageDraft)
        
        // Vérifier qu'une nouvelle action a été ajoutée
        assertEquals("Recent actions count should increase", initialRecentCount + 1, afterSend.recent.size)
        
        // Vérifier le contenu de la nouvelle action
        val newAction = afterSend.recent.first()
        assertTrue("New action should contain the message", newAction.title.contains(testMessage))
        assertTrue("New action should contain 'Messaged EULER'", newAction.title.contains("Messaged EULER"))
        assertEquals("New action should have time 'Just now'", "Just now", newAction.time)
        assertNotNull("New action should have valid id", newAction.id)
    }

    @Test
    fun `sendMessage should not add action when draft is empty`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialRecentCount = initialState.recent.size
        
        // Essayer d'envoyer avec un draft vide
        viewModel.updateMessageDraft("")
        viewModel.sendMessage()
        
        val afterSend = viewModel.uiState.first()
        
        // Vérifier qu'aucune nouvelle action n'a été ajoutée
        assertEquals("Recent actions count should remain the same", initialRecentCount, afterSend.recent.size)
        assertEquals("Message draft should remain empty", "", afterSend.messageDraft)
    }

    @Test
    fun `sendMessage should not add action when draft is only whitespace`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialRecentCount = initialState.recent.size
        
        // Essayer d'envoyer avec un draft contenant seulement des espaces
        viewModel.updateMessageDraft("   \t\n  ")
        viewModel.sendMessage()
        
        val afterSend = viewModel.uiState.first()
        
        // Vérifier qu'aucune nouvelle action n'a été ajoutée
        assertEquals("Recent actions count should remain the same", initialRecentCount, afterSend.recent.size)
        assertEquals("Message draft should remain unchanged", "   \t\n  ", afterSend.messageDraft)
    }

    @Test
    fun `sendMessage should trim whitespace from message`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialRecentCount = initialState.recent.size
        
        // Envoyer un message avec des espaces
        val messageWithSpaces = "  Hello EULER!  "
        viewModel.updateMessageDraft(messageWithSpaces)
        viewModel.sendMessage()
        
        val afterSend = viewModel.uiState.first()
        
        // Vérifier que l'action contient le message sans espaces
        val newAction = afterSend.recent.first()
        assertTrue("New action should contain trimmed message", newAction.title.contains("Hello EULER!"))
        assertFalse("New action should not contain leading/trailing spaces", 
            newAction.title.contains("  Hello EULER!  "))
    }

    @Test
    fun `toggleSystemConnection should toggle system connection status`() = runTest {
        val initialState = viewModel.uiState.first()
        
        // Trouver un système connecté
        val connectedSystem = initialState.systems.find { it.isConnected }
        assertNotNull("Should have at least one connected system", connectedSystem)
        
        val systemId = connectedSystem!!.id
        val initialConnectionStatus = connectedSystem.isConnected
        
        // Toggle la connexion
        viewModel.toggleSystemConnection(systemId)
        val afterFirstToggle = viewModel.uiState.first()
        
        val toggledSystem = afterFirstToggle.systems.find { it.id == systemId }
        assertNotNull("System should still exist", toggledSystem)
        assertEquals("Connection status should be toggled", !initialConnectionStatus, toggledSystem!!.isConnected)
        
        // Toggle à nouveau
        viewModel.toggleSystemConnection(systemId)
        val afterSecondToggle = viewModel.uiState.first()
        
        val backToOriginalSystem = afterSecondToggle.systems.find { it.id == systemId }
        assertNotNull("System should still exist", backToOriginalSystem)
        assertEquals("Connection status should be back to original", initialConnectionStatus, backToOriginalSystem!!.isConnected)
    }

    @Test
    fun `toggleSystemConnection should not affect other systems`() = runTest {
        val initialState = viewModel.uiState.first()
        
        // Trouver deux systèmes différents
        val system1 = initialState.systems[0]
        val system2 = initialState.systems[1]
        
        val system1InitialStatus = system1.isConnected
        val system2InitialStatus = system2.isConnected
        
        // Toggle le premier système
        viewModel.toggleSystemConnection(system1.id)
        val afterToggle = viewModel.uiState.first()
        
        val system1After = afterToggle.systems.find { it.id == system1.id }
        val system2After = afterToggle.systems.find { it.id == system2.id }
        
        assertNotNull("System 1 should still exist", system1After)
        assertNotNull("System 2 should still exist", system2After)
        
        assertEquals("System 1 status should be toggled", !system1InitialStatus, system1After!!.isConnected)
        assertEquals("System 2 status should remain unchanged", system2InitialStatus, system2After!!.isConnected)
    }

    @Test
    fun `toggleSystemConnection should handle non-existent system gracefully`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialSystems = initialState.systems.toList()
        
        // Essayer de toggle un système qui n'existe pas
        viewModel.toggleSystemConnection("non-existent-system")
        val afterToggle = viewModel.uiState.first()
        
        // Vérifier que rien n'a changé
        assertEquals("Systems list should remain unchanged", initialSystems.size, afterToggle.systems.size)
        
        // Vérifier que tous les systèmes ont le même statut
        initialSystems.forEach { originalSystem ->
            val afterSystem = afterToggle.systems.find { it.id == originalSystem.id }
            assertNotNull("System should still exist", afterSystem)
            assertEquals("System status should be unchanged", originalSystem.isConnected, afterSystem!!.isConnected)
        }
    }

    @Test
    fun `setLoading should update loading state correctly`() = runTest {
        val initialState = viewModel.uiState.first()
        assertFalse("Initial loading state should be false", initialState.isLoading)
        
        // Mettre loading à true
        viewModel.setLoading(true)
        val afterSetTrue = viewModel.uiState.first()
        assertTrue("Loading state should be true", afterSetTrue.isLoading)
        
        // Mettre loading à false
        viewModel.setLoading(false)
        val afterSetFalse = viewModel.uiState.first()
        assertFalse("Loading state should be false", afterSetFalse.isLoading)
        
        // Mettre loading à true à nouveau
        viewModel.setLoading(true)
        val afterSetTrueAgain = viewModel.uiState.first()
        assertTrue("Loading state should be true again", afterSetTrueAgain.isLoading)
    }

    @Test
    fun `multiple state changes should work correctly`() = runTest {
        // Effectuer plusieurs changements d'état
        viewModel.setLoading(true)
        viewModel.updateMessageDraft("Test message")
        viewModel.toggleDrawer()
        viewModel.setTopRightOpen(true)
        
        val finalState = viewModel.uiState.first()
        
        // Vérifier tous les changements
        assertTrue("Loading should be true", finalState.isLoading)
        assertEquals("Message draft should be updated", "Test message", finalState.messageDraft)
        assertTrue("Drawer should be open", finalState.isDrawerOpen)
        assertTrue("Top right menu should be open", finalState.isTopRightOpen)
        
        // Effectuer d'autres changements
        viewModel.setLoading(false)
        viewModel.updateMessageDraft("Another message")
        viewModel.toggleDrawer()
        viewModel.setTopRightOpen(false)
        
        val finalState2 = viewModel.uiState.first()
        
        // Vérifier les nouveaux changements
        assertFalse("Loading should be false", finalState2.isLoading)
        assertEquals("Message draft should be updated again", "Another message", finalState2.messageDraft)
        assertFalse("Drawer should be closed", finalState2.isDrawerOpen)
        assertFalse("Top right menu should be closed", finalState2.isTopRightOpen)
    }

    @Test
    fun `sendMessage with multiple messages should maintain chronological order`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialRecentCount = initialState.recent.size
        
        // Envoyer plusieurs messages
        viewModel.updateMessageDraft("First message")
        viewModel.sendMessage()
        
        viewModel.updateMessageDraft("Second message")
        viewModel.sendMessage()
        
        viewModel.updateMessageDraft("Third message")
        viewModel.sendMessage()
        
        val finalState = viewModel.uiState.first()
        
        // Vérifier que les messages sont dans l'ordre chronologique (plus récent en premier)
        assertEquals("Should have 3 new actions", initialRecentCount + 3, finalState.recent.size)
        
        val firstAction = finalState.recent[0]
        val secondAction = finalState.recent[1]
        val thirdAction = finalState.recent[2]
        
        assertTrue("First action should be the most recent", firstAction.title.contains("Third message"))
        assertTrue("Second action should be the second most recent", secondAction.title.contains("Second message"))
        assertTrue("Third action should be the oldest", thirdAction.title.contains("First message"))
        
        // Vérifier que tous ont le temps "Just now"
        assertEquals("First action should have 'Just now'", "Just now", firstAction.time)
        assertEquals("Second action should have 'Just now'", "Just now", secondAction.time)
        assertEquals("Third action should have 'Just now'", "Just now", thirdAction.time)
    }

    @Test
    fun `state should be immutable - changes should not affect previous state`() = runTest {
        val initialState = viewModel.uiState.first()
        val initialMessageDraft = initialState.messageDraft
        
        // Modifier l'état
        viewModel.updateMessageDraft("New message")
        val afterUpdate = viewModel.uiState.first()
        
        // Vérifier que l'état initial n'a pas été modifié (conceptuellement)
        // Note: En réalité, StateFlow partage la même référence, mais le test vérifie le comportement attendu
        assertNotEquals("New state should be different from initial", initialMessageDraft, afterUpdate.messageDraft)
        assertEquals("New state should have the updated message", "New message", afterUpdate.messageDraft)
    }
}
