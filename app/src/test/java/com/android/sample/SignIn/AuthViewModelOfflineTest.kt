package com.android.sample.sign_in

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

/** Tests for AuthViewModel offline mode functionality */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AuthViewModelOfflineTest {

  private lateinit var networkMonitor: FakeNetworkConnectivityMonitor
  private lateinit var viewModel: AuthViewModel

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
    viewModel = AuthViewModel(networkMonitor = networkMonitor)
  }

  @After
  fun tearDown() {
    FirebaseAuth.getInstance().signOut()
  }

  @Test
  fun `initial state is online`() = runTest {
    val isOffline = viewModel.isOffline.first()
    assertFalse(isOffline)
  }

  @Test
  fun `going offline sets isOffline to true`() = runTest {
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    val isOffline = viewModel.isOffline.first()
    assertTrue(isOffline)
  }

  @Test
  fun `going back online sets isOffline to false`() = runTest {
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    assertTrue(viewModel.isOffline.first())

    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)
    assertFalse(viewModel.isOffline.first())
  }

  @Test
  fun `onMicrosoftLoginClick does nothing when offline`() = runTest {
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    val initialState = viewModel.state.first()

    viewModel.onMicrosoftLoginClick()

    kotlinx.coroutines.delay(100)
    val finalState = viewModel.state.first()
    // State should not change to Loading when offline
    assertEquals(initialState, finalState)
  }

  @Test
  fun `onSwitchEduLoginClick does nothing when offline`() = runTest {
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    val initialState = viewModel.state.first()

    viewModel.onSwitchEduLoginClick()

    kotlinx.coroutines.delay(100)
    val finalState = viewModel.state.first()
    // State should not change to Loading when offline
    assertEquals(initialState, finalState)
  }

  @Test
  fun `onMicrosoftLoginClick works when online`() = runTest {
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)
    val initialState = viewModel.state.first()

    viewModel.onMicrosoftLoginClick()

    kotlinx.coroutines.delay(100)
    val finalState = viewModel.state.first()
    // When online, login should proceed
    assertNotEquals(initialState, finalState)
  }

  @Test
  fun `onSwitchEduLoginClick works when online`() = runTest {
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)
    val initialState = viewModel.state.first()

    viewModel.onSwitchEduLoginClick()

    kotlinx.coroutines.delay(100)
    val finalState = viewModel.state.first()
    // When online, login should proceed
    assertNotEquals(initialState, finalState)
  }
}
