package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.Chat.ChatType
import com.android.sample.conversations.CachedResponseRepository
import com.android.sample.network.FakeNetworkConnectivityMonitor
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests suggestion chip offline behavior - chips should work offline with cached responses */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeScreenSuggestionChipOfflineTest {

  private lateinit var net: FakeNetworkConnectivityMonitor
  private lateinit var doc: DocumentReference
  private lateinit var snap: DocumentSnapshot
  private lateinit var vm: HomeViewModel

  @Before
  fun setup() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(
          ctx,
          FirebaseOptions.Builder()
              .setApplicationId("1:123:android:test")
              .setProjectId("test")
              .setApiKey("key")
              .build())
    }
    FirebaseAuth.getInstance().signOut()

    val auth: FirebaseAuth = mock()
    val user: FirebaseUser = mock()
    val db: FirebaseFirestore = mock()
    val col: CollectionReference = mock()
    doc = mock()
    snap = mock()

    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("uid")
    whenever(db.collection("globalCachedResponses")).thenReturn(col)
    whenever(col.document(any())).thenReturn(doc)

    net = FakeNetworkConnectivityMonitor(initialOnline = true)
    vm = HomeViewModel(networkMonitor = net, cacheRepo = CachedResponseRepository(auth, db))
  }

  @After fun tearDown() = FirebaseAuth.getInstance().signOut()

  @Test
  fun `chip click offline with cache sends user message and shows AI response`() = runTest {
    net.setOnline(false)
    delay(50)
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(true)

    vm.sendMessage("What is EPFL") // Simulates chip click
    advanceUntilIdle()
    delay(100)

    val s = vm.uiState.first()
    assertTrue(
        "User msg added", s.messages.any { it.type == ChatType.USER && it.text == "What is EPFL" })
    assertTrue("AI response added", s.messages.any { it.type == ChatType.AI })
  }

  @Test
  fun `chip click offline without cache shows error message`() = runTest {
    net.setOnline(false)
    delay(50)
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    vm.sendMessage("What is EPFL")
    advanceUntilIdle()
    delay(100)

    val s = vm.uiState.first()
    assertTrue("User msg added", s.messages.any { it.type == ChatType.USER })
    assertTrue("Error shown", s.messages.any { it.type == ChatType.AI })
  }

  @Test
  fun `chip click online sends normally`() = runTest {
    net.setOnline(true)
    delay(50)
    vm.sendMessage("What is EPFL")
    advanceUntilIdle()
    delay(100)

    val s = vm.uiState.first()
    assertTrue("User msg added", s.messages.any { it.type == ChatType.USER })
    assertFalse("Online", s.isOffline)
  }

  @Test
  fun `chip click updates draft then sends`() = runTest {
    net.setOnline(false)
    delay(50)
    whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
    whenever(snap.exists()).thenReturn(false)

    vm.updateMessageDraft("What is EPFL")
    vm.sendMessage("What is EPFL") // Chip passes message directly
    advanceUntilIdle()
    delay(100)

    assertTrue(vm.uiState.first().messages.any { it.text == "What is EPFL" })
  }
}
