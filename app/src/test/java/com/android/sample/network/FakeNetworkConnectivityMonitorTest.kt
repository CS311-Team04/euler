package com.android.sample.network

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Tests for FakeNetworkConnectivityMonitor */
class FakeNetworkConnectivityMonitorTest {

  @Test
  fun `initial state is online by default`() {
    val monitor = FakeNetworkConnectivityMonitor()
    assertTrue(monitor.isCurrentlyOnline())
  }

  @Test
  fun `initial state can be set to offline`() {
    val monitor = FakeNetworkConnectivityMonitor(initialOnline = false)
    assertFalse(monitor.isCurrentlyOnline())
  }

  @Test
  fun `setOnline updates the state`() {
    val monitor = FakeNetworkConnectivityMonitor(initialOnline = true)
    assertTrue(monitor.isCurrentlyOnline())

    monitor.setOnline(false)
    assertFalse(monitor.isCurrentlyOnline())

    monitor.setOnline(true)
    assertTrue(monitor.isCurrentlyOnline())
  }

  @Test
  fun `isOnline flow emits current state`() = runTest {
    val monitor = FakeNetworkConnectivityMonitor(initialOnline = true)
    assertTrue(monitor.isOnline.first())

    monitor.setOnline(false)
    assertFalse(monitor.isOnline.first())

    monitor.setOnline(true)
    assertTrue(monitor.isOnline.first())
  }
}
