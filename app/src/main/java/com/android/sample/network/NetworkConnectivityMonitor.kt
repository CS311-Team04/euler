package com.android.sample.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for monitoring network connectivity state. This allows for testable implementations
 * without Android dependencies.
 */
interface NetworkConnectivityMonitor {
  /** Flow that emits true when online, false when offline. */
  val isOnline: StateFlow<Boolean>

  /** Current online status (true if online, false if offline). */
  fun isCurrentlyOnline(): Boolean
}
