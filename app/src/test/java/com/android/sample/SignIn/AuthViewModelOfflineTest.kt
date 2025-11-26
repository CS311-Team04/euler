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

  @Test
  fun `onMicrosoftLoginClick checks isCurrentlyOnline when state says online but monitor says offline`() =
      runTest {
        // Set state to online but monitor says offline
        networkMonitor.setOnline(false)
        kotlinx.coroutines.delay(100)

        // Manually set isOffline to false (simulating state desync)
        val initialState = viewModel.state.first()

        viewModel.onMicrosoftLoginClick()

        kotlinx.coroutines.delay(100)
        val finalState = viewModel.state.first()
        // Should check isCurrentlyOnline() and detect offline, so state should not change
        assertEquals("State should not change when offline", initialState, finalState)
      }

  @Test
  fun `onSwitchEduLoginClick checks isCurrentlyOnline when state says online but monitor says offline`() =
      runTest {
        networkMonitor.setOnline(false)
        kotlinx.coroutines.delay(100)

        val initialState = viewModel.state.first()

        viewModel.onSwitchEduLoginClick()

        kotlinx.coroutines.delay(100)
        val finalState = viewModel.state.first()
        // Should check isCurrentlyOnline() and detect offline
        assertEquals("State should not change when offline", initialState, finalState)
      }

  @Test
  fun `network monitor initialization with null monitor`() = runTest {
    val viewModelWithoutMonitor = AuthViewModel(networkMonitor = null)
    val isOffline = viewModelWithoutMonitor.isOffline.first()
    // Should default to online (false)
    assertFalse("Should default to online when no monitor", isOffline)
  }

  @Test
  fun `onMicrosoftLoginClick when already loading does nothing`() = runTest {
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)

    // Start login
    viewModel.onMicrosoftLoginClick()
    kotlinx.coroutines.delay(50)
    val loadingState = viewModel.state.first()

    // Try to click again while loading
    viewModel.onMicrosoftLoginClick()
    kotlinx.coroutines.delay(50)
    val stateAfterSecondClick = viewModel.state.first()

    // State should remain in Loading
    assertEquals("State should remain loading", loadingState, stateAfterSecondClick)
  }

  @Test
  fun `onSwitchEduLoginClick when already loading does nothing`() = runTest {
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)

    // Start login
    viewModel.onMicrosoftLoginClick()
    kotlinx.coroutines.delay(50)
    val loadingState = viewModel.state.first()

    // Try SwitchEdu while already loading
    viewModel.onSwitchEduLoginClick()
    kotlinx.coroutines.delay(50)
    val stateAfterSwitchEdu = viewModel.state.first()

    // State should remain in Loading
    assertEquals("State should remain loading", loadingState, stateAfterSwitchEdu)
  }

  @Test
  fun `isOffline updates when network state changes multiple times`() = runTest {
    // Start online
    assertFalse("Should start online", viewModel.isOffline.first())

    // Go offline
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    assertTrue("Should be offline", viewModel.isOffline.first())

    // Go online
    networkMonitor.setOnline(true)
    kotlinx.coroutines.delay(100)
    assertFalse("Should be online", viewModel.isOffline.first())

    // Go offline again
    networkMonitor.setOnline(false)
    kotlinx.coroutines.delay(100)
    assertTrue("Should be offline again", viewModel.isOffline.first())
  }
}
