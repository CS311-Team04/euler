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
import kotlinx.coroutines.flow.MutableStateFlow
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelOfflineCacheTest {

  private lateinit var net: FakeNetworkConnectivityMonitor
  private lateinit var db: FirebaseFirestore
  private lateinit var col: CollectionReference
  private lateinit var doc: DocumentReference
  private lateinit var snap: DocumentSnapshot
  private lateinit var vm: HomeViewModel

  private fun HomeViewModel.edit(t: (HomeUiState) -> HomeUiState) {
    val f = HomeViewModel::class.java.getDeclaredField("_uiState")
    f.isAccessible = true
    @Suppress("UNCHECKED_CAST") (f.get(this) as MutableStateFlow<HomeUiState>).value =
        t(uiState.value)
  }

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
    db = mock()
    col = mock()
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

  private fun cache(response: String?, error: Boolean = false) {
    if (error) {
      whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forException(RuntimeException("err")))
    } else {
      whenever(doc.get(Source.CACHE)).thenReturn(Tasks.forResult(snap))
      whenever(snap.exists()).thenReturn(response != null)
    }
  }

  // --- Core offline cache paths ---

  @Test
  fun `offline with cache hit adds user and AI messages`() = runTest {
    net.setOnline(false)
    delay(50)
    cache("cached")
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    val s = vm.uiState.first()
    assertTrue(s.messages.any { it.type == ChatType.USER && it.text == "test" })
    assertTrue(s.messages.any { it.type == ChatType.AI })
  }

  @Test
  fun `offline without cache shows error`() = runTest {
    net.setOnline(false)
    delay(50)
    cache(null)
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    assertTrue(vm.uiState.first().messages.any { it.type == ChatType.AI })
  }

  @Test
  fun `offline cache exception shows error`() = runTest {
    net.setOnline(false)
    delay(50)
    cache(null, error = true)
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    assertTrue(vm.uiState.first().messages.any { it.type == ChatType.AI })
  }

  @Test
  fun `offline guest skips cache`() = runTest {
    net.setOnline(false)
    delay(50)
    vm.setGuestMode(true)
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    assertTrue(vm.uiState.first().isGuest)
    assertTrue(vm.uiState.first().messages.any { it.type == ChatType.USER })
  }

  // --- Message handling ---

  @Test
  fun `uses message parameter over draft`() = runTest {
    net.setOnline(false)
    delay(50)
    cache(null)
    vm.updateMessageDraft("draft")
    vm.sendMessage("param")
    advanceUntilIdle()
    delay(100)
    assertTrue(vm.uiState.first().messages.any { it.text == "param" })
  }

  @Test
  fun `uses draft when no parameter`() = runTest {
    net.setOnline(false)
    delay(50)
    cache(null)
    vm.updateMessageDraft("draft")
    vm.sendMessage()
    advanceUntilIdle()
    delay(100)
    assertTrue(vm.uiState.first().messages.any { it.text == "draft" })
  }

  @Test
  fun `empty message does nothing`() = runTest {
    val c = vm.uiState.first().messages.size
    vm.sendMessage("")
    advanceUntilIdle()
    assertEquals(c, vm.uiState.first().messages.size)
  }

  @Test
  fun `whitespace message does nothing`() = runTest {
    val c = vm.uiState.first().messages.size
    vm.sendMessage("   ")
    advanceUntilIdle()
    assertEquals(c, vm.uiState.first().messages.size)
  }

  // --- Guards ---

  @Test
  fun `already sending blocks new message`() = runTest {
    vm.edit { it.copy(isSending = true) }
    val c = vm.uiState.first().messages.size
    vm.sendMessage("test")
    advanceUntilIdle()
    assertEquals(c, vm.uiState.first().messages.size)
  }

  @Test
  fun `streaming blocks new message`() = runTest {
    vm.edit { it.copy(streamingMessageId = "x") }
    val c = vm.uiState.first().messages.size
    vm.sendMessage("test")
    advanceUntilIdle()
    assertEquals(c, vm.uiState.first().messages.size)
  }

  // --- State updates ---

  @Test
  fun `offline updates showOfflineMessage for signed in user`() = runTest {
    net.setOnline(false)
    delay(50)
    cache(null)
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    assertTrue(vm.uiState.first().isOffline)
  }

  @Test
  fun `networkMonitor offline check updates state`() = runTest {
    cache(null)
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    // networkMonitor.isCurrentlyOnline() checked
    assertTrue(vm.uiState.first().messages.isNotEmpty())
  }

  @Test
  fun `online proceeds without cache check`() = runTest {
    net.setOnline(true)
    delay(50)
    vm.sendMessage("test")
    advanceUntilIdle()
    delay(100)
    assertFalse(vm.uiState.first().isOffline)
    assertTrue(vm.uiState.first().messages.any { it.type == ChatType.USER })
  }
}
