package com.android.sample.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of NetworkConnectivityMonitor that uses ConnectivityManager to monitor
 * network state changes.
 */
class AndroidNetworkConnectivityMonitor(context: Context) : NetworkConnectivityMonitor {
  private val connectivityManager: ConnectivityManager =
      context.getSystemService<ConnectivityManager>()
          ?: throw IllegalStateException("ConnectivityManager not available")

  private val _isOnline = MutableStateFlow(checkConnectivity())
  override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var periodicCheckJob: Job? = null

  private val networkCallback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          updateConnectivity()
        }

        override fun onLost(network: Network) {
          // When network is lost, check immediately and also schedule a re-check
          updateConnectivity()
          // Double-check after a short delay to catch cases where the callback fires before
          // the network is fully disconnected
          scope.launch {
            delay(200)
            updateConnectivity()
          }
        }

        override fun onUnavailable() {
          // Network request was unable to find a network
          _isOnline.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
          updateConnectivity()
        }

        override fun onLinkPropertiesChanged(
            network: Network,
            linkProperties: android.net.LinkProperties
        ) {
          updateConnectivity()
        }
      }

  init {
    // Register for all network changes (not just validated ones)
    // This allows us to detect when networks become unavailable faster
    val request =
        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
    connectivityManager.registerNetworkCallback(request, networkCallback)

    // Also start a periodic check as a fallback (every 1 second)
    // This ensures we catch connectivity changes even if callbacks are delayed
    // This is especially important when WiFi goes out while the app is running
    periodicCheckJob =
        scope.launch(Dispatchers.IO) {
          while (true) {
            delay(1000) // Check every second for faster detection
            val currentState = _isOnline.value
            val newState = checkConnectivity()
            if (currentState != newState) {
              _isOnline.value = newState
            }
          }
        }
  }

  private fun checkConnectivity(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    // Check if network has internet capability
    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    if (!hasInternet) {
      return false
    }

    // Check validation status - this is the most reliable indicator
    // NET_CAPABILITY_VALIDATED means the network has been validated to have internet access
    val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    // If validated, definitely online
    if (hasValidated) {
      return true
    }

    // If not validated, check all networks to see if any have validated internet
    // This handles cases where the active network might not be validated yet
    val allNetworks = connectivityManager.allNetworks
    if (allNetworks.isEmpty()) {
      return false
    }

    // Check if any network has validated internet
    for (net in allNetworks) {
      val netCapabilities = connectivityManager.getNetworkCapabilities(net) ?: continue
      if (netCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          netCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
        return true
      }
    }

    // If we have internet capability but no validated networks, check transport types
    // This is a fallback - if we have a transport type, we might be online but validation is
    // pending
    // The periodic check will catch when validation completes or the network is lost
    val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    val hasEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

    // Only return true if we have a transport type - this means we have a network connection
    // but validation might be pending. The periodic check will update this quickly.
    return hasWifi || hasCellular || hasEthernet
  }

  private fun updateConnectivity() {
    val newState = checkConnectivity()
    if (_isOnline.value != newState) {
      _isOnline.value = newState
    }
  }

  override fun isCurrentlyOnline(): Boolean = checkConnectivity()

  fun unregister() {
    periodicCheckJob?.cancel()
    periodicCheckJob = null
    try {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    } catch (e: Exception) {
      // Ignore if already unregistered
    }
  }
}
