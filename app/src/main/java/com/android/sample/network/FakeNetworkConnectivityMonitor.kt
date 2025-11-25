package com.android.sample.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake implementation of NetworkConnectivityMonitor for testing. Allows manual control of the
 * online state.
 */
class FakeNetworkConnectivityMonitor(initialOnline: Boolean = true) : NetworkConnectivityMonitor {
  private val _isOnline = MutableStateFlow(initialOnline)
  override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  override fun isCurrentlyOnline(): Boolean = _isOnline.value

  /** Manually set the online state for testing. */
  fun setOnline(online: Boolean) {
    _isOnline.value = online
  }
}
