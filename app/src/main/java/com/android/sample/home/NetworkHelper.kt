package com.android.sample.auth

/**
 * Helper for network verification logic
 *
 * This class contains pure network decision logic that can be tested without Android dependencies,
 * allowing 100% coverage of this logic.
 */
object NetworkHelper {

  /**
   * Determines if a network connection is available based on network capabilities
   *
   * @param hasInternet Indicates if the network has Internet access
   * @param hasWifi Indicates if the network uses WiFi
   * @param hasCellular Indicates if the network uses Cellular
   * @param hasEthernet Indicates if the network uses Ethernet
   * @return true if a valid network connection is available
   */
  fun isConnected(
      hasInternet: Boolean,
      hasWifi: Boolean,
      hasCellular: Boolean,
      hasEthernet: Boolean
  ): Boolean {
    return when {
      hasInternet && hasWifi -> true
      hasInternet && hasCellular -> true
      hasInternet && hasEthernet -> true
      hasInternet -> true
      else -> false
    }
  }

  /**
   * Determines if a network connection is available with validated Internet
   *
   * @param hasInternet Indicates if the network has Internet access
   * @param hasValidatedInternet Indicates if Internet access is validated
   * @param hasWifi Indicates if the network uses WiFi
   * @param hasCellular Indicates if the network uses Cellular
   * @param hasEthernet Indicates if the network uses Ethernet
   * @return true if a valid network connection with validated Internet is available.
   */
  fun isConnectedWithValidatedInternet(
      hasInternet: Boolean,
      hasValidatedInternet: Boolean,
      hasWifi: Boolean,
      hasCellular: Boolean,
      hasEthernet: Boolean
  ): Boolean {
    return hasValidatedInternet && isConnected(hasInternet, hasWifi, hasCellular, hasEthernet)
  }

  /**
   * Determines the primary network connection type
   *
   * @param hasWifi Indicates whether the network uses WiFi
   * @param hasCellular Indicates whether the network uses Cellular
   * @param hasEthernet Indicates whether the network uses Ethernet
   * @return the primary connection type, or null if none
   */
  fun getPrimaryConnectionType(
      hasWifi: Boolean,
      hasCellular: Boolean,
      hasEthernet: Boolean
  ): String? {
    return when {
      hasEthernet -> "ETHERNET"
      hasWifi -> "WIFI"
      hasCellular -> "CELLULAR"
      else -> null
    }
  }

  /**
   * Checks if a network configuration is valid for authentication
   *
   * @param hasInternet Indicates whether the network has Internet access
   * @param hasValidatedInternet Indicates whether Internet access is validated
   * @param hasWifi Indicates whether the network uses WiFi
   * @param hasCellular Indicates whether the network uses Cellular
   * @param hasEthernet Indicates whether the network uses Ethernet
   * @return true if the configuration is valid for authentication
   */
  fun isValidForAuthentication(
      hasInternet: Boolean,
      hasValidatedInternet: Boolean,
      hasWifi: Boolean,
      hasCellular: Boolean,
      hasEthernet: Boolean
  ): Boolean {

    return when {
      hasValidatedInternet -> true
      hasInternet && (hasWifi || hasCellular || hasEthernet) -> true
      else -> false
    }
  }
}
