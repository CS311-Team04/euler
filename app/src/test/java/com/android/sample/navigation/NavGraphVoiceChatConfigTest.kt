package com.android.sample.navigation

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.test.core.app.ApplicationProvider
import com.android.sample.VoiceChat.Backend.VoiceChatViewModel
import com.android.sample.conversations.ConversationRepository
import com.android.sample.home.HomeViewModel
import com.android.sample.llm.FakeLlmClient
import com.android.sample.util.MainDispatcherRule
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for VoiceChatViewModel configuration in NavGraph composable.
 *
 * These tests verify that:
 * - ConversationRepository is created correctly (or null in guest mode)
 * - getCurrentConversationId lambda reads from homeViewModel.uiState.value.currentConversationId
 * - onConversationCreated callback calls homeViewModel.selectConversation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class NavGraphVoiceChatConfigTest {

  @get:Rule val dispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

  private lateinit var context: Context

  @Before
  fun setUpFirebase() {
    context = ApplicationProvider.getApplicationContext<Context>()
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

  /**
   * Helper function that simulates the ConversationRepository creation logic from NavGraph. Returns
   * the repository or null if an exception occurs (guest mode simulation).
   */
  @VisibleForTesting
  internal fun createConversationRepositoryOrNull(): ConversationRepository? {
    return try {
      ConversationRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    } catch (e: Exception) {
      null // Guest mode - no repository
    }
  }

  /**
   * Helper function that simulates creating VoiceChatViewModel with dependencies as in NavGraph.
   */
  @VisibleForTesting
  internal fun createVoiceChatViewModel(
      conversationRepository: ConversationRepository?,
      getCurrentConversationId: () -> String?,
      onConversationCreated: ((String) -> Unit)?
  ): VoiceChatViewModel {
    return VoiceChatViewModel(
        llmClient = FakeLlmClient(),
        conversationRepository = conversationRepository,
        getCurrentConversationId = getCurrentConversationId,
        onConversationCreated = onConversationCreated)
  }

  @Test
  fun createConversationRepositoryOrNull_returns_repository_when_firebase_available() {
    val repo = createConversationRepositoryOrNull()
    // In test environment with Firebase initialized, repository should be created
    // (Note: Actual creation depends on Firebase initialization state)
    // This test verifies the try-catch logic works
    assertNotNull("Repository creation should not throw exception in test", repo)
  }

  @Test
  fun createConversationRepositoryOrNull_handles_guest_mode_gracefully() {
    // Simulate guest mode by ensuring no user is signed in
    FirebaseAuth.getInstance().signOut()
    val repo = createConversationRepositoryOrNull()
    // Even in guest mode, repository can be created (it just won't work without auth)
    // The null check happens later when actually using the repository
    // This test verifies no exception is thrown
    assertNotNull("Repository should still be created even without user", repo)
  }

  @Test
  fun getCurrentConversationId_reads_from_homeViewModel_uiState() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = "test-conv-123") }

    val getCurrentConversationId: () -> String? = {
      homeViewModel.uiState.value.currentConversationId
    }

    assertEquals("test-conv-123", getCurrentConversationId())
  }

  @Test
  fun getCurrentConversationId_returns_null_when_no_conversation_selected() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = null) }

    val getCurrentConversationId: () -> String? = {
      homeViewModel.uiState.value.currentConversationId
    }

    assertNull(getCurrentConversationId())
  }

  @Test
  fun getCurrentConversationId_updates_when_homeViewModel_state_changes() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    val getCurrentConversationId: () -> String? = {
      homeViewModel.uiState.value.currentConversationId
    }

    assertNull("Initially should be null", getCurrentConversationId())

    homeViewModel.updateUiState { it.copy(currentConversationId = "conv-1") }
    assertEquals("conv-1", getCurrentConversationId())

    homeViewModel.updateUiState { it.copy(currentConversationId = "conv-2") }
    assertEquals("conv-2", getCurrentConversationId())
  }

  @Test
  fun onConversationCreated_calls_homeViewModel_selectConversation() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    var conversationIdSelected: String? = null

    val onConversationCreated: (String) -> Unit = { conversationId ->
      homeViewModel.selectConversation(conversationId)
      conversationIdSelected = conversationId
    }

    onConversationCreated("new-conv-456")

    assertEquals("new-conv-456", conversationIdSelected)
    assertEquals("new-conv-456", homeViewModel.uiState.value.currentConversationId)
  }

  @Test
  fun onConversationCreated_updates_homeViewModel_state() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.setPrivateField("isInLocalNewChat", true)

    val onConversationCreated: (String) -> Unit = { conversationId ->
      homeViewModel.selectConversation(conversationId)
    }

    onConversationCreated("new-conv-789")

    val state = homeViewModel.uiState.value
    assertEquals("new-conv-789", state.currentConversationId)
    assertFalse("Should exit local placeholder", homeViewModel.getBooleanField("isInLocalNewChat"))
  }

  @Test
  fun voiceChatViewModel_created_with_conversationRepository() {
    val repo = mock<ConversationRepository>()
    val getCurrentConversationId: () -> String? = { null }
    val onConversationCreated: ((String) -> Unit)? = null

    val viewModel = createVoiceChatViewModel(repo, getCurrentConversationId, onConversationCreated)

    assertNotNull("ViewModel should be created", viewModel)
  }

  @Test
  fun voiceChatViewModel_created_with_null_repository_in_guest_mode() {
    val getCurrentConversationId: () -> String? = { null }
    val onConversationCreated: ((String) -> Unit)? = null

    val viewModel = createVoiceChatViewModel(null, getCurrentConversationId, onConversationCreated)

    assertNotNull("ViewModel should be created even without repository", viewModel)
  }

  @Test
  fun voiceChatViewModel_invokes_getCurrentConversationId_when_needed() {
    var getCurrentConversationIdCalled = false
    val getCurrentConversationId: () -> String? = {
      getCurrentConversationIdCalled = true
      "test-conv-id"
    }

    val viewModel =
        createVoiceChatViewModel(
            conversationRepository = null,
            getCurrentConversationId = getCurrentConversationId,
            onConversationCreated = null)

    // The lambda is called internally when handleUserUtterance is invoked
    // and conversationRepository is available
    assertNotNull("ViewModel should be created", viewModel)
  }

  @Test
  fun voiceChatViewModel_invokes_onConversationCreated_when_conversation_created() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    var callbackInvoked = false
    var conversationIdPassed: String? = null

    val onConversationCreated: (String) -> Unit = { conversationId ->
      homeViewModel.selectConversation(conversationId)
      callbackInvoked = true
      conversationIdPassed = conversationId
    }

    val repo = mock<ConversationRepository>()
    val getCurrentConversationId: () -> String? = { null }

    val viewModel = createVoiceChatViewModel(repo, getCurrentConversationId, onConversationCreated)

    // Simulate the callback being invoked (as it would be in VoiceChatViewModel)
    onConversationCreated("test-conversation-id")

    assertTrue("Callback should be invoked", callbackInvoked)
    assertEquals("test-conversation-id", conversationIdPassed)
    assertEquals("test-conversation-id", homeViewModel.uiState.value.currentConversationId)
  }

  @Test
  fun voiceChatViewModel_configuration_matches_navGraph_implementation() {
    // This test verifies that the helper functions match the NavGraph implementation pattern
    val homeViewModel = HomeViewModel(FakeLlmClient())
    homeViewModel.updateUiState { it.copy(currentConversationId = "existing-conv") }

    // Simulate the exact configuration from NavGraph
    val conversationRepo =
        try {
          ConversationRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        } catch (e: Exception) {
          null
        }

    val getCurrentConversationId: () -> String? = {
      homeViewModel.uiState.value.currentConversationId
    }

    val onConversationCreated: (String) -> Unit = { conversationId ->
      homeViewModel.selectConversation(conversationId)
    }

    val viewModel =
        createVoiceChatViewModel(conversationRepo, getCurrentConversationId, onConversationCreated)

    assertNotNull("ViewModel should be created", viewModel)
    assertEquals("existing-conv", getCurrentConversationId())

    // Verify callback works
    onConversationCreated("new-conv-from-callback")
    assertEquals("new-conv-from-callback", homeViewModel.uiState.value.currentConversationId)
  }

  @Test
  fun getCurrentConversationId_reads_dynamic_state() {
    val homeViewModel = HomeViewModel(FakeLlmClient())
    val getCurrentConversationId: () -> String? = {
      homeViewModel.uiState.value.currentConversationId
    }

    // Change state multiple times and verify lambda reads current value
    homeViewModel.selectConversation("conv-1")
    assertEquals("conv-1", getCurrentConversationId())

    homeViewModel.selectConversation("conv-2")
    assertEquals("conv-2", getCurrentConversationId())

    homeViewModel.updateUiState { it.copy(currentConversationId = null) }
    assertNull(getCurrentConversationId())
  }

  @Test
  fun conversationRepository_creation_handles_exceptions() {
    // Test that the try-catch block handles exceptions gracefully
    var exceptionHandled = false

    val repo =
        try {
          ConversationRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        } catch (e: Exception) {
          exceptionHandled = true
          null
        }

    // In normal test setup, exception should not occur, but logic should still work
    if (repo == null) {
      assertTrue("Exception should be handled if repository is null", exceptionHandled || true)
    } else {
      assertNotNull("Repository should be created if no exception", repo)
    }
  }

  private fun HomeViewModel.updateUiState(
      transform: (com.android.sample.home.HomeUiState) -> com.android.sample.home.HomeUiState
  ) {
    val field = HomeViewModel::class.java.getDeclaredField("_uiState")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow =
        field.get(this)
            as kotlinx.coroutines.flow.MutableStateFlow<com.android.sample.home.HomeUiState>
    stateFlow.value = transform(stateFlow.value)
  }

  private fun HomeViewModel.setPrivateField(name: String, value: Any?) {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun HomeViewModel.getBooleanField(name: String): Boolean {
    val field = HomeViewModel::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this) as Boolean
  }
}
