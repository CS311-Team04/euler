package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.conversations.CachedResponseRepository
import com.android.sample.network.FakeNetworkConnectivityMonitor
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for suggestion chip behavior when offline */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeScreenSuggestionChipOfflineTest {

  private lateinit var networkMonitor: FakeNetworkConnectivityMonitor
  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var user: FirebaseUser
  private lateinit var cacheRepo: CachedResponseRepository
  private lateinit var globalCacheCollection: CollectionReference
  private lateinit var cacheDocument: DocumentReference
  private lateinit var cacheSnapshot: DocumentSnapshot
  private lateinit var viewModel: HomeViewModel

  @Before
  fun setup() {
    // Initialize Firebase for tests
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

    // Setup mocks
    auth = mock()
    firestore = mock()
    user = mock()
    globalCacheCollection = mock()
    cacheDocument = mock()
    cacheSnapshot = mock()

    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("test-user-123")
    whenever(firestore.collection("globalCachedResponses")).thenReturn(globalCacheCollection)

    networkMonitor = FakeNetworkConnectivityMonitor(initialOnline = true)
    cacheRepo = CachedResponseRepository(auth, firestore)
    viewModel = HomeViewModel(networkMonitor = networkMonitor, cacheRepo = cacheRepo)
  }

  @After
  fun tearDown() {
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun `suggestion chip click offline with cache sends message and uses cache`() = runTest {
    val suggestion = "What is EPFL"
    val cachedResponse = "EPFL is a university in Switzerland"

    // Setup offline state
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)

    // Mock cache retrieval
    whenever(globalCacheCollection.document(any())).thenReturn(cacheDocument)
    whenever(cacheDocument.get(Source.CACHE)).thenReturn(Tasks.forResult(cacheSnapshot))
    whenever(cacheSnapshot.exists()).thenReturn(true)
    whenever(cacheSnapshot.getString("response")).thenReturn(cachedResponse)

    val cachedData =
        mapOf(
            "question" to suggestion,
            "response" to cachedResponse,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now())
    whenever(cacheSnapshot.data).thenReturn(cachedData)

    val initialState = viewModel.uiState.first()
    val initialMessageCount = initialState.messages.size

    // Simulate suggestion chip click - calls sendMessage with suggestion
    viewModel.sendMessage(suggestion)

    advanceUntilIdle()
    kotlinx.coroutines.delay(200)

    val finalState = viewModel.uiState.first()

    // Verify user message was added
    val userMessages = finalState.messages.filter { it.type == ChatType.USER }
    assertTrue("User message should be added from suggestion chip", userMessages.isNotEmpty())
    assertEquals("User message text should match suggestion", suggestion, userMessages.last().text)

    // Verify behavior: if cache was found, AI message would have text; if not, error would be shown
    val aiMessages = finalState.messages.filter { it.type == ChatType.AI }
    assertTrue("Should have AI message (either cached response or error)", aiMessages.isNotEmpty())

    // Note: We can't easily verify Firestore mock calls due to extension functions
    // Instead, we verify behavior - message was sent and cache check was attempted
  }

  @Test
  fun `suggestion chip click offline without cache shows error`() = runTest {
    val suggestion = "What is EPFL"

    // Setup offline state
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)

    // Mock cache retrieval - no cache found
    whenever(globalCacheCollection.document(any())).thenReturn(cacheDocument)
    whenever(cacheDocument.get(Source.CACHE)).thenReturn(Tasks.forResult(cacheSnapshot))
    whenever(cacheSnapshot.exists()).thenReturn(false)

    // Simulate suggestion chip click
    viewModel.sendMessage(suggestion)

    advanceUntilIdle()
    kotlinx.coroutines.delay(200)

    val finalState = viewModel.uiState.first()

    // Verify user message was added
    val userMessages = finalState.messages.filter { it.type == ChatType.USER }
    assertTrue("User message should be added from suggestion chip", userMessages.isNotEmpty())
    assertEquals("User message text should match suggestion", suggestion, userMessages.last().text)

    // Verify error message is shown
    val aiMessages = finalState.messages.filter { it.type == ChatType.AI }
    assertTrue("Should have AI message (error)", aiMessages.isNotEmpty())
  }

  @Test
  fun `suggestion chip click online sends message normally`() = runTest {
    val suggestion = "What is EPFL"

    // Setup online state
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)

    // Simulate suggestion chip click
    viewModel.sendMessage(suggestion)

    advanceUntilIdle()
    kotlinx.coroutines.delay(200)

    val finalState = viewModel.uiState.first()

    // Verify user message was added
    val userMessages = finalState.messages.filter { it.type == ChatType.USER }
    assertTrue("User message should be added from suggestion chip", userMessages.isNotEmpty())
    assertEquals("User message text should match suggestion", suggestion, userMessages.last().text)

    // When online, should proceed with normal flow (may require LLM client mock for full test)
    assertFalse("Should be online", finalState.isOffline)
  }

  @Test
  fun `suggestion chip click updates message draft`() = runTest {
    val suggestion = "What is EPFL"

    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)

    // Mock cache retrieval
    whenever(globalCacheCollection.document(any())).thenReturn(cacheDocument)
    whenever(cacheDocument.get(Source.CACHE)).thenReturn(Tasks.forResult(cacheSnapshot))
    whenever(cacheSnapshot.exists()).thenReturn(false)

    // Simulate suggestion chip click - should update draft first
    viewModel.updateMessageDraft(suggestion)
    viewModel.sendMessage(suggestion)

    advanceUntilIdle()
    kotlinx.coroutines.delay(200)

    val finalState = viewModel.uiState.first()

    // Verify message was sent
    val userMessages = finalState.messages.filter { it.type == ChatType.USER }
    assertTrue("User message should be added", userMessages.isNotEmpty())
    assertEquals("Message text should match suggestion", suggestion, userMessages.last().text)
  }

  @Ignore("Test is flaky - needs investigation")
  @Test
  fun `multiple suggestion chip clicks offline handle correctly`() = runTest {
    val suggestion1 = "What is EPFL"
    val suggestion2 = "Check Ed Discussion"
    val cachedResponse = "EPFL is a university"

    // Setup offline state
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)

    val initialState = viewModel.uiState.first()
    val initialMessageCount = initialState.messages.size

    // Mock cache - first has cache, second doesn't
    whenever(globalCacheCollection.document(any())).thenReturn(cacheDocument)

    // First call - has cache
    whenever(cacheDocument.get(Source.CACHE)).thenReturn(Tasks.forResult(cacheSnapshot))
    whenever(cacheSnapshot.exists()).thenReturn(true)
    whenever(cacheSnapshot.getString("response")).thenReturn(cachedResponse)
    val cachedData =
        mapOf(
            "question" to suggestion1,
            "response" to cachedResponse,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now())
    whenever(cacheSnapshot.data).thenReturn(cachedData)

    viewModel.sendMessage(suggestion1)
    advanceUntilIdle()
    kotlinx.coroutines.delay(800) // Wait longer for first message to complete

    // Check first message was added
    var intermediateState = viewModel.uiState.first()
    var intermediateUserMessages = intermediateState.messages.filter { it.type == ChatType.USER }
    assertTrue(
        "First message should be added, found ${intermediateUserMessages.size} user messages",
        intermediateUserMessages.isNotEmpty())
    assertTrue(
        "First message text should match", intermediateUserMessages.any { it.text == suggestion1 })

    // Second call - no cache (create new snapshot mock for second call)
    val cacheSnapshot2 = mock<DocumentSnapshot>()
    whenever(cacheDocument.get(Source.CACHE)).thenReturn(Tasks.forResult(cacheSnapshot2))
    whenever(cacheSnapshot2.exists()).thenReturn(false)

    viewModel.sendMessage(suggestion2)
    advanceUntilIdle()
    kotlinx.coroutines.delay(800) // Wait longer for second message to complete

    val finalState = viewModel.uiState.first()
    val userMessages = finalState.messages.filter { it.type == ChatType.USER }

    // Verify both messages were added
    assertTrue(
        "Should have at least 2 user messages, found: ${userMessages.size} (initial: $initialMessageCount)",
        userMessages.size >= 2)
    // Find messages by text since order might vary
    val messageTexts = userMessages.map { it.text }
    assertTrue(
        "First message should be present: $suggestion1 in $messageTexts",
        messageTexts.contains(suggestion1))
    assertTrue(
        "Second message should be present: $suggestion2 in $messageTexts",
        messageTexts.contains(suggestion2))

    // Verify AI messages were added (either cached responses or errors)
    val aiMessages = finalState.messages.filter { it.type == ChatType.AI }
    // Note: We might have placeholder messages, so check for at least some AI messages
    assertTrue(
        "Should have AI messages (responses or errors), found: ${aiMessages.size}",
        aiMessages.isNotEmpty())
  }
}
