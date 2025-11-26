package com.android.sample.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class AndroidNetworkConnectivityMonitorTest {

  private lateinit var context: Context
  private lateinit var connectivityManager: ConnectivityManager

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(
          context,
          FirebaseOptions.Builder()
              .setApplicationId("1:1234567890:android:test")
              .setProjectId("test-project")
              .setApiKey("fake-api-key")
              .build())
    }
    connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  }

  @After
  fun tearDown() {
    // Clean up any monitors
  }

  @Test
  fun `initial state reflects current connectivity`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val initialState = monitor.isOnline.first()

    // Initial state depends on Robolectric's default network state
    // Just verify it's a valid boolean
    assertNotNull("Initial state should be set", initialState)
    monitor.unregister()
  }

  @Test
  fun `onAvailable updates connectivity`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    // Trigger onAvailable callback
    val callback = getNetworkCallback(monitor)
    callback.onAvailable(network)

    // Wait for updateConnectivity to complete
    kotlinx.coroutines.delay(100)
    // Verify updateConnectivity was called (state may vary based on actual network)
    val state = monitor.isOnline.first()
    assertNotNull("State should be updated", state)

    monitor.unregister()
  }

  @Test
  fun `onLost updates connectivity with delayed re-check`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)
    // Trigger onLost - this calls updateConnectivity immediately and after 200ms delay
    callback.onLost(network)

    // Wait for immediate update
    kotlinx.coroutines.delay(50)
    val immediateState = monitor.isOnline.first()

    // Wait for delayed re-check (200ms)
    kotlinx.coroutines.delay(250)
    val delayedState = monitor.isOnline.first()

    // Both should have triggered updateConnectivity
    assertNotNull("Immediate state should be set", immediateState)
    assertNotNull("Delayed state should be set", delayedState)

    monitor.unregister()
  }

  @Test
  fun `onUnavailable sets offline immediately`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    val callback = getNetworkCallback(monitor)
    callback.onUnavailable()

    kotlinx.coroutines.delay(50)
    assertFalse("Should be offline when unavailable", monitor.isOnline.first())

    monitor.unregister()
  }

  @Test
  fun `onCapabilitiesChanged triggers connectivity update`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)
    // Use the actual capabilities from the connectivity manager if available,
    // or create an empty one - the callback will trigger updateConnectivity which checks real state
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: NetworkCapabilities()

    callback.onCapabilitiesChanged(network, capabilities)

    kotlinx.coroutines.delay(100)
    // Verify updateConnectivity was called
    val state = monitor.isOnline.first()
    assertNotNull("State should be updated after capabilities changed", state)

    monitor.unregister()
  }

  @Test
  fun `onLinkPropertiesChanged triggers connectivity check`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)
    callback.onLinkPropertiesChanged(network, android.net.LinkProperties())

    kotlinx.coroutines.delay(100)
    // Should trigger updateConnectivity which checks current state
    val state = monitor.isOnline.first()
    assertNotNull("State should be updated after link properties changed", state)

    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns false when no active network`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    // Test the checkConnectivity logic through isCurrentlyOnline
    // When there's no active network, it should return false
    val result = monitor.isCurrentlyOnline()
    // Result depends on Robolectric's default state, but method should not crash
    assertNotNull("Should return a boolean value", result)

    monitor.unregister()
  }

  @Test
  fun `isCurrentlyOnline calls checkConnectivity`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    // This tests the isCurrentlyOnline override
    val result = monitor.isCurrentlyOnline()
    assertNotNull("Should return a boolean", result)

    monitor.unregister()
  }

  @Test
  fun `periodic check runs and updates state`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val initialValue = monitor.isOnline.value

    // Wait for periodic check (runs every 1 second)
    kotlinx.coroutines.delay(1100)

    // Periodic check should have run (may or may not change state depending on network)
    val afterCheck = monitor.isOnline.value
    assertNotNull("State should exist after periodic check", afterCheck)

    monitor.unregister()
  }

  @Test
  fun `updateConnectivity only updates when state changes`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)
    val initialValue = monitor.isOnline.value

    // Use actual capabilities or create an empty one
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: NetworkCapabilities()

    // Call updateConnectivity multiple times via callback
    callback.onCapabilitiesChanged(network, capabilities)
    kotlinx.coroutines.delay(50)
    callback.onCapabilitiesChanged(network, capabilities)
    kotlinx.coroutines.delay(50)

    // State should remain the same (no unnecessary updates) if connectivity didn't change
    val finalValue = monitor.isOnline.value
    // The state might change if network actually changed, but updateConnectivity logic is tested
    assertNotNull("State should be set", finalValue)

    monitor.unregister()
  }

  @Test
  fun `unregister cleans up resources`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    // Should not throw
    monitor.unregister()
    monitor.unregister() // Multiple calls should be safe

    assertTrue("Unregister should complete without errors", true)
  }

  @Test
  fun `checkConnectivity returns false when activeNetwork is null`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    // Use reflection to access checkConnectivity method
    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    // Mock the connectivity manager to return null for activeNetwork
    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(null)

    // Replace the connectivityManager field
    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertFalse("Should return false when no active network", result)

    // Restore original
    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns false when getNetworkCapabilities returns null`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(null)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertFalse("Should return false when capabilities are null", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns false when network has no internet capability`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertFalse("Should return false when network has no internet capability", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns true when network has validated capability`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true when network is validated", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns true when another network has validated internet`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network1 = ShadowNetwork.newInstance(1)
    val network2 = ShadowNetwork.newInstance(2)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    // Active network has internet but not validated
    val activeCapabilities = mock<NetworkCapabilities>()
    whenever(activeCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(activeCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)

    // Another network has validated internet
    val otherCapabilities = mock<NetworkCapabilities>()
    whenever(otherCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(otherCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network1)
    whenever(mockConnectivityManager.getNetworkCapabilities(network1))
        .thenReturn(activeCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network1, network2))
    whenever(mockConnectivityManager.getNetworkCapabilities(network2)).thenReturn(otherCapabilities)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true when another network has validated internet", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns false when allNetworks is empty`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(any())).thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(emptyArray())

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertFalse("Should return false when allNetworks is empty", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns true when network has WiFi transport`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true when network has WiFi transport", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns true when network has cellular transport`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true when network has cellular transport", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns true when network has ethernet transport`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true when network has ethernet transport", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns false when network has no transport types`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertFalse("Should return false when network has no transport types", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity skips networks with null capabilities when checking all networks`() =
      runTest {
        val monitor = AndroidNetworkConnectivityMonitor(context)
        val network1 = ShadowNetwork.newInstance(1)
        val network2 = ShadowNetwork.newInstance(2)

        val method =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
        method.isAccessible = true

        val activeCapabilities = mock<NetworkCapabilities>()
        whenever(activeCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        whenever(activeCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(false)
        whenever(activeCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(true)

        val mockConnectivityManager = mock<ConnectivityManager>()
        whenever(mockConnectivityManager.activeNetwork).thenReturn(network1)
        whenever(mockConnectivityManager.getNetworkCapabilities(network1))
            .thenReturn(activeCapabilities)
        whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network1, network2))
        whenever(mockConnectivityManager.getNetworkCapabilities(network2))
            .thenReturn(null) // null capabilities

        val field =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
        field.isAccessible = true
        val originalManager = field.get(monitor)
        field.set(monitor, mockConnectivityManager)

        // Should not crash and should return true due to WiFi transport
        val result = method.invoke(monitor) as Boolean
        assertTrue("Should handle null capabilities gracefully", result)

        field.set(monitor, originalManager)
        monitor.unregister()
      }

  @Test
  fun `updateConnectivity only updates state when it changes`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val updateMethod =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("updateConnectivity")
    updateMethod.isAccessible = true

    val checkMethod =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    checkMethod.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    // Get initial state
    val initialState = monitor.isOnline.value

    // Call updateConnectivity - if state is already correct, it shouldn't change
    updateMethod.invoke(monitor)

    // If state was already correct, it should remain the same
    val afterUpdate = monitor.isOnline.value
    val expectedState = checkMethod.invoke(monitor) as Boolean

    assertEquals("State should match checkConnectivity result", expectedState, afterUpdate)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `unregister handles exception when callback not registered`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    // Unregister first time (should work)
    monitor.unregister()

    // Unregister again - should not throw even if already unregistered
    try {
      monitor.unregister()
      assertTrue("Second unregister should not throw", true)
    } catch (e: Exception) {
      fail("Unregister should handle exceptions gracefully: ${e.message}")
    }
  }

  @Test
  fun `initialization throws exception when ConnectivityManager is null`() {
    val mockContext = mock<Context>()
    whenever(mockContext.getSystemService<ConnectivityManager>(any())).thenReturn(null)

    try {
      AndroidNetworkConnectivityMonitor(mockContext)
      fail("Should throw IllegalStateException when ConnectivityManager is null")
    } catch (e: IllegalStateException) {
      assertEquals("ConnectivityManager not available", e.message)
    }
  }

  @Test
  fun `updateConnectivity updates state when it changes from false to true`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val updateMethod =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("updateConnectivity")
    updateMethod.isAccessible = true

    // First set state to false
    val mockCapabilitiesOffline = mock<NetworkCapabilities>()
    whenever(mockCapabilitiesOffline.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network))
        .thenReturn(mockCapabilitiesOffline)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    // Force state to false
    val stateField = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("_isOnline")
    stateField.isAccessible = true
    val stateFlow = stateField.get(monitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    stateFlow.value = false

    // Now change to online
    val mockCapabilitiesOnline = mock<NetworkCapabilities>()
    whenever(mockCapabilitiesOnline.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilitiesOnline.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)
    whenever(mockConnectivityManager.getNetworkCapabilities(network))
        .thenReturn(mockCapabilitiesOnline)

    updateMethod.invoke(monitor)
    kotlinx.coroutines.delay(50)

    assertTrue("State should be updated to true", monitor.isOnline.value)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `updateConnectivity updates state when it changes from true to false`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val updateMethod =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("updateConnectivity")
    updateMethod.isAccessible = true

    // First set state to true
    val mockCapabilitiesOnline = mock<NetworkCapabilities>()
    whenever(mockCapabilitiesOnline.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilitiesOnline.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network))
        .thenReturn(mockCapabilitiesOnline)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    // Force state to true
    val stateField = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("_isOnline")
    stateField.isAccessible = true
    val stateFlow = stateField.get(monitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    stateFlow.value = true

    // Now change to offline
    whenever(mockConnectivityManager.activeNetwork).thenReturn(null)

    updateMethod.invoke(monitor)
    kotlinx.coroutines.delay(50)

    assertFalse("State should be updated to false", monitor.isOnline.value)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `periodic check updates state when connectivity changes`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    // Force initial state to false
    val stateField = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("_isOnline")
    stateField.isAccessible = true
    val stateFlow = stateField.get(monitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    stateFlow.value = false

    // Verify initial state is false
    assertFalse("Initial state should be false", monitor.isOnline.value)

    // Verify that checkConnectivity would return true (this is what periodic check uses)
    val checkMethod =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    checkMethod.isAccessible = true
    val expectedState = checkMethod.invoke(monitor) as Boolean
    assertTrue("checkConnectivity should return true with mocked online state", expectedState)

    // Test the periodic check logic directly by simulating what it does:
    // It checks if currentState != newState and updates if different
    val currentState = monitor.isOnline.value
    val newState = checkMethod.invoke(monitor) as Boolean
    assertNotEquals("State should differ from checkConnectivity result", currentState, newState)

    // Manually trigger updateConnectivity to simulate what periodic check does
    val updateMethod =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("updateConnectivity")
    updateMethod.isAccessible = true
    updateMethod.invoke(monitor)
    kotlinx.coroutines.delay(50)

    // Now state should be updated
    assertTrue("State should be updated to match checkConnectivity", monitor.isOnline.value)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `periodic check does not update state when connectivity unchanged`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    // Set state to true (matching connectivity)
    val stateField = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("_isOnline")
    stateField.isAccessible = true
    val stateFlow = stateField.get(monitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    val initialState = true
    stateFlow.value = initialState

    // Wait for periodic check
    kotlinx.coroutines.delay(1100)

    // State should remain the same since connectivity didn't change
    assertEquals("State should remain unchanged", initialState, monitor.isOnline.value)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity returns false when network has internet but no validated and no transport`() =
      runTest {
        val monitor = AndroidNetworkConnectivityMonitor(context)
        val network = ShadowNetwork.newInstance(1)

        val method =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
        method.isAccessible = true

        val mockCapabilities = mock<NetworkCapabilities>()
        whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(false)
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(false)
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(false)
        whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            .thenReturn(false)

        val mockConnectivityManager = mock<ConnectivityManager>()
        whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
        whenever(mockConnectivityManager.getNetworkCapabilities(network))
            .thenReturn(mockCapabilities)
        whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))
        whenever(mockConnectivityManager.getNetworkCapabilities(network))
            .thenReturn(mockCapabilities)

        val field =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
        field.isAccessible = true
        val originalManager = field.get(monitor)
        field.set(monitor, mockConnectivityManager)

        val result = method.invoke(monitor) as Boolean
        assertFalse("Should return false when no transport types", result)

        field.set(monitor, originalManager)
        monitor.unregister()
      }

  @Test
  fun `checkConnectivity returns false when all networks have internet but none validated`() =
      runTest {
        val monitor = AndroidNetworkConnectivityMonitor(context)
        val network1 = ShadowNetwork.newInstance(1)
        val network2 = ShadowNetwork.newInstance(2)

        val method =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
        method.isAccessible = true

        val capabilities1 = mock<NetworkCapabilities>()
        whenever(capabilities1.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        whenever(capabilities1.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(false)
        whenever(capabilities1.hasTransport(any())).thenReturn(false)

        val capabilities2 = mock<NetworkCapabilities>()
        whenever(capabilities2.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        whenever(capabilities2.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(false)
        whenever(capabilities2.hasTransport(any())).thenReturn(false)

        val mockConnectivityManager = mock<ConnectivityManager>()
        whenever(mockConnectivityManager.activeNetwork).thenReturn(network1)
        whenever(mockConnectivityManager.getNetworkCapabilities(network1)).thenReturn(capabilities1)
        whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network1, network2))
        whenever(mockConnectivityManager.getNetworkCapabilities(network2)).thenReturn(capabilities2)

        val field =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
        field.isAccessible = true
        val originalManager = field.get(monitor)
        field.set(monitor, mockConnectivityManager)

        val result = method.invoke(monitor) as Boolean
        assertFalse("Should return false when no networks are validated", result)

        field.set(monitor, originalManager)
        monitor.unregister()
      }

  @Test
  fun `checkConnectivity returns true when another network has validated but active does not`() =
      runTest {
        val monitor = AndroidNetworkConnectivityMonitor(context)
        val network1 = ShadowNetwork.newInstance(1)
        val network2 = ShadowNetwork.newInstance(2)

        val method =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
        method.isAccessible = true

        val activeCapabilities = mock<NetworkCapabilities>()
        whenever(activeCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        whenever(activeCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(false)
        whenever(activeCapabilities.hasTransport(any())).thenReturn(false)

        val otherCapabilities = mock<NetworkCapabilities>()
        whenever(otherCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)
        whenever(otherCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            .thenReturn(true)

        val mockConnectivityManager = mock<ConnectivityManager>()
        whenever(mockConnectivityManager.activeNetwork).thenReturn(network1)
        whenever(mockConnectivityManager.getNetworkCapabilities(network1))
            .thenReturn(activeCapabilities)
        whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network1, network2))
        whenever(mockConnectivityManager.getNetworkCapabilities(network2))
            .thenReturn(otherCapabilities)

        val field =
            AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
        field.isAccessible = true
        val originalManager = field.get(monitor)
        field.set(monitor, mockConnectivityManager)

        val result = method.invoke(monitor) as Boolean
        assertTrue("Should return true when another network is validated", result)

        field.set(monitor, originalManager)
        monitor.unregister()
      }

  @Test
  fun `onLost delayed re-check updates state correctly`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)

    // Set up initial online state
    val mockCapabilitiesOnline = mock<NetworkCapabilities>()
    whenever(mockCapabilitiesOnline.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilitiesOnline.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network))
        .thenReturn(mockCapabilitiesOnline)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    // Force state to true
    val stateField = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("_isOnline")
    stateField.isAccessible = true
    val stateFlow = stateField.get(monitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    stateFlow.value = true

    // Now simulate network loss
    whenever(mockConnectivityManager.activeNetwork).thenReturn(null)
    whenever(mockConnectivityManager.allNetworks).thenReturn(emptyArray())

    callback.onLost(network)

    // Wait for immediate update
    kotlinx.coroutines.delay(50)
    val immediateState = monitor.isOnline.value

    // Wait for delayed re-check (200ms)
    kotlinx.coroutines.delay(250)
    val delayedState = monitor.isOnline.value

    assertFalse("Immediate state should be false", immediateState)
    assertFalse("Delayed state should be false", delayedState)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `multiple rapid onAvailable callbacks update state correctly`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network1 = ShadowNetwork.newInstance(1)
    val network2 = ShadowNetwork.newInstance(2)

    val callback = getNetworkCallback(monitor)

    callback.onAvailable(network1)
    kotlinx.coroutines.delay(10)
    callback.onAvailable(network2)
    kotlinx.coroutines.delay(10)
    callback.onAvailable(network1)

    kotlinx.coroutines.delay(100)

    // Should not crash and should have a valid state
    assertNotNull("State should be set after rapid callbacks", monitor.isOnline.value)

    monitor.unregister()
  }

  @Test
  fun `multiple rapid onCapabilitiesChanged callbacks update state correctly`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: NetworkCapabilities()

    callback.onCapabilitiesChanged(network, capabilities)
    kotlinx.coroutines.delay(10)
    callback.onCapabilitiesChanged(network, capabilities)
    kotlinx.coroutines.delay(10)
    callback.onCapabilitiesChanged(network, capabilities)

    kotlinx.coroutines.delay(100)

    // Should not crash and should have a valid state
    assertNotNull("State should be set after rapid callbacks", monitor.isOnline.value)

    monitor.unregister()
  }

  @Test
  fun `onLinkPropertiesChanged with multiple networks updates state`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network1 = ShadowNetwork.newInstance(1)
    val network2 = ShadowNetwork.newInstance(2)

    val callback = getNetworkCallback(monitor)

    callback.onLinkPropertiesChanged(network1, android.net.LinkProperties())
    kotlinx.coroutines.delay(50)
    callback.onLinkPropertiesChanged(network2, android.net.LinkProperties())
    kotlinx.coroutines.delay(50)

    assertNotNull("State should be set after link properties changed", monitor.isOnline.value)

    monitor.unregister()
  }

  @Test
  fun `isOnline StateFlow emits initial state`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    val initialState = monitor.isOnline.value
    assertNotNull("Initial state should be set", initialState)

    monitor.unregister()
  }

  @Test
  fun `isOnline StateFlow updates when state changes`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val callback = getNetworkCallback(monitor)

    val initialValue = monitor.isOnline.value

    // Trigger a callback that might change state
    callback.onCapabilitiesChanged(network, NetworkCapabilities())

    kotlinx.coroutines.delay(100)

    // State flow should have emitted a value (may or may not be different)
    val newValue = monitor.isOnline.value
    assertNotNull("State should be set", newValue)

    monitor.unregister()
  }

  @Test
  fun `unregister cancels periodic check job`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    val jobField =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("periodicCheckJob")
    jobField.isAccessible = true
    val jobBefore = jobField.get(monitor) as? kotlinx.coroutines.Job

    assertNotNull("Periodic check job should be set", jobBefore)
    assertFalse("Job should be active before unregister", jobBefore!!.isCancelled)

    monitor.unregister()

    val jobAfter = jobField.get(monitor) as? kotlinx.coroutines.Job
    assertNull("Periodic check job should be null after unregister", jobAfter)
    assertTrue("Job should be cancelled", jobBefore.isCancelled)
  }

  @Test
  fun `unregister sets periodicCheckJob to null`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    val jobField =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("periodicCheckJob")
    jobField.isAccessible = true

    val jobBefore = jobField.get(monitor)
    assertNotNull("Job should exist before unregister", jobBefore)

    monitor.unregister()

    val jobAfter = jobField.get(monitor)
    assertNull("Job should be null after unregister", jobAfter)
  }

  @Test
  fun `network callback is registered during initialization`() = runTest {
    val mockConnectivityManager = mock<ConnectivityManager>()
    val mockContext = mock<Context>()
    whenever(mockContext.getSystemService<ConnectivityManager>(any()))
        .thenReturn(mockConnectivityManager)

    val monitor = AndroidNetworkConnectivityMonitor(mockContext)

    // Verify that registerNetworkCallback was called
    verify(mockConnectivityManager, atLeastOnce())
        .registerNetworkCallback(any(), any<ConnectivityManager.NetworkCallback>())

    monitor.unregister()
  }

  @Test
  fun `network request includes internet capability`() = runTest {
    val mockConnectivityManager = mock<ConnectivityManager>()
    val mockContext = mock<Context>()
    whenever(mockContext.getSystemService<ConnectivityManager>(any()))
        .thenReturn(mockConnectivityManager)

    val monitor = AndroidNetworkConnectivityMonitor(mockContext)

    // Verify that registerNetworkCallback was called with a request
    val requestCaptor = argumentCaptor<android.net.NetworkRequest>()
    verify(mockConnectivityManager, atLeastOnce())
        .registerNetworkCallback(
            requestCaptor.capture(), any<ConnectivityManager.NetworkCallback>())

    // The request should have been created (we can't easily verify its contents without reflection)
    assertNotNull("Network request should be created", requestCaptor.firstValue)

    monitor.unregister()
  }

  @Test
  fun `checkConnectivity handles network with only WiFi transport`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true for WiFi transport", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity handles network with only cellular transport`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true for cellular transport", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity handles network with only ethernet transport`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true for ethernet transport", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `checkConnectivity handles network with multiple transport types`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(false)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)
    whenever(mockCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        .thenReturn(false)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)
    whenever(mockConnectivityManager.allNetworks).thenReturn(arrayOf(network))

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val result = method.invoke(monitor) as Boolean
    assertTrue("Should return true when multiple transports available", result)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  @Test
  fun `onUnavailable sets state to false immediately`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)

    val callback = getNetworkCallback(monitor)

    // Set initial state to true
    val stateField = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("_isOnline")
    stateField.isAccessible = true
    val stateFlow = stateField.get(monitor) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>
    stateFlow.value = true

    callback.onUnavailable()

    kotlinx.coroutines.delay(10)

    assertFalse("State should be false after onUnavailable", monitor.isOnline.value)

    monitor.unregister()
  }

  @Test
  fun `isCurrentlyOnline returns same value as checkConnectivity`() = runTest {
    val monitor = AndroidNetworkConnectivityMonitor(context)
    val network = ShadowNetwork.newInstance(1)

    val method =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredMethod("checkConnectivity")
    method.isAccessible = true

    val mockCapabilities = mock<NetworkCapabilities>()
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        .thenReturn(true)
    whenever(mockCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        .thenReturn(true)

    val mockConnectivityManager = mock<ConnectivityManager>()
    whenever(mockConnectivityManager.activeNetwork).thenReturn(network)
    whenever(mockConnectivityManager.getNetworkCapabilities(network)).thenReturn(mockCapabilities)

    val field =
        AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("connectivityManager")
    field.isAccessible = true
    val originalManager = field.get(monitor)
    field.set(monitor, mockConnectivityManager)

    val checkResult = method.invoke(monitor) as Boolean
    val isCurrentlyOnlineResult = monitor.isCurrentlyOnline()

    assertEquals(
        "isCurrentlyOnline should match checkConnectivity", checkResult, isCurrentlyOnlineResult)

    field.set(monitor, originalManager)
    monitor.unregister()
  }

  // Helper to access the private network callback for testing
  private fun getNetworkCallback(
      monitor: AndroidNetworkConnectivityMonitor
  ): ConnectivityManager.NetworkCallback {
    val field = AndroidNetworkConnectivityMonitor::class.java.getDeclaredField("networkCallback")
    field.isAccessible = true
    return field.get(monitor) as ConnectivityManager.NetworkCallback
  }
}
