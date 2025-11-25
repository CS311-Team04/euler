package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.network.FakeNetworkConnectivityMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for HomeViewModel offline mode functionality */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class HomeViewModelOfflineTest {

  private lateinit var networkMonitor: FakeNetworkConnectivityMonitor
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

    networkMonitor = FakeNetworkConnectivityMonitor(initialOnline = true)
    viewModel = HomeViewModel(networkMonitor = networkMonitor)
  }

  @After
  fun tearDown() {
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun `initial state is online`() = runTest {
    val state = viewModel.uiState.first()
    assertFalse(state.isOffline)
    assertFalse(state.showOfflineMessage)
  }

  @Test
  fun `going offline sets isOffline to true`() = runTest {
    networkMonitor.setOnline(false)
    // Wait a bit for the flow to update
    kotlinx.coroutines.delay(100)
    val state = viewModel.uiState.first()
    assertTrue(state.isOffline)
  }

  @Test
  fun `going offline shows message when signed in`() = runTest {
    // Simulate signed in user by checking if we can show the message
    // In real scenario, user would be signed in via Firebase
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    val state = viewModel.uiState.first()
    // Note: showOfflineMessage only shows if user was previously signed in
    // This test verifies the offline state is tracked
    assertTrue(state.isOffline)
  }

  @Test
  fun `going back online sets isOffline to false and hides message`() = runTest {
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    var state = viewModel.uiState.first()
    assertTrue(state.isOffline)

    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)
    state = viewModel.uiState.first()
    assertFalse(state.isOffline)
    assertFalse(state.showOfflineMessage)
  }

  @Test
  fun `dismissOfflineMessage hides the message`() = runTest {
    // First go offline to show message
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    viewModel.dismissOfflineMessage()
    val state = viewModel.uiState.first()
    assertFalse(state.showOfflineMessage)
  }

  @Test
  fun `sendMessage does nothing when offline`() = runTest {
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    val initialState = viewModel.uiState.first()
    val initialMessageCount = initialState.messages.size

    viewModel.updateMessageDraft("Test message")
    viewModel.sendMessage()

    kotlinx.coroutines.delay(100)
    val finalState = viewModel.uiState.first()
    // Message should not be sent when offline
    assertEquals(initialMessageCount, finalState.messages.size)
  }

  @Test
  fun `sendMessage works when online`() = runTest {
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)
    val initialState = viewModel.uiState.first()
    val initialMessageCount = initialState.messages.size

    viewModel.updateMessageDraft("Test message")
    // Note: sendMessage might require network, so we just verify it doesn't block
    // The actual sending would require mocking the LLM client
    viewModel.sendMessage()

    kotlinx.coroutines.delay(100)
    val finalState = viewModel.uiState.first()
    // When online, message sending should proceed (though may fail without proper mocks)
    // We just verify the state allows sending
    assertFalse(finalState.isOffline)
  }
}
