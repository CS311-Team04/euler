package com.android.sample.auth

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NetworkHelper
 *
 * These tests cover 100% of the pure network logic without Android dependencies, allowing for high
 * coverage of this critical part
 */
class NetworkHelperTest {

  @Test
  fun `isConnected should return true when has Internet and WiFi`() {
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = true, hasCellular = false, hasEthernet = false))
  }

  @Test
  fun `isConnected should return true when has Internet and Cellular`() {
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = false, hasCellular = true, hasEthernet = false))
  }

  @Test
  fun `isConnected should return true when has Internet and Ethernet`() {
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = false, hasCellular = false, hasEthernet = true))
  }

  @Test
  fun `isConnected should return true when has Internet but no specific transport (emulator fallback)`() {
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = false, hasCellular = false, hasEthernet = false))
  }

  @Test
  fun `isConnected should return false when no Internet`() {
    assertFalse(
        NetworkHelper.isConnected(
            hasInternet = false, hasWifi = true, hasCellular = true, hasEthernet = true))
  }

  @Test
  fun `isConnected should return false when no Internet and no transport`() {
    assertFalse(
        NetworkHelper.isConnected(
            hasInternet = false, hasWifi = false, hasCellular = false, hasEthernet = false))
  }

  @Test
  fun `isConnectedWithValidatedInternet should return true when has validated Internet and WiFi`() {
    assertTrue(
        NetworkHelper.isConnectedWithValidatedInternet(
            hasInternet = true,
            hasValidatedInternet = true,
            hasWifi = true,
            hasCellular = false,
            hasEthernet = false))
  }

  @Test
  fun `isConnectedWithValidatedInternet should return false when no validated Internet`() {
    assertFalse(
        NetworkHelper.isConnectedWithValidatedInternet(
            hasInternet = true,
            hasValidatedInternet = false,
            hasWifi = true,
            hasCellular = false,
            hasEthernet = false))
  }

  @Test
  fun `isConnectedWithValidatedInternet should return false when validated Internet but no connection`() {
    assertFalse(
        NetworkHelper.isConnectedWithValidatedInternet(
            hasInternet = false,
            hasValidatedInternet = true,
            hasWifi = false,
            hasCellular = false,
            hasEthernet = false))
  }

  @Test
  fun `getPrimaryConnectionType should return ETHERNET when available`() {
    assertEquals(
        "ETHERNET",
        NetworkHelper.getPrimaryConnectionType(
            hasWifi = true, hasCellular = true, hasEthernet = true))
  }

  @Test
  fun `getPrimaryConnectionType should return WIFI when Ethernet not available`() {
    assertEquals(
        "WIFI",
        NetworkHelper.getPrimaryConnectionType(
            hasWifi = true, hasCellular = true, hasEthernet = false))
  }

  @Test
  fun `getPrimaryConnectionType should return CELLULAR when only Cellular available`() {
    assertEquals(
        "CELLULAR",
        NetworkHelper.getPrimaryConnectionType(
            hasWifi = false, hasCellular = true, hasEthernet = false))
  }

  @Test
  fun `getPrimaryConnectionType should return null when no transport available`() {
    assertNull(
        NetworkHelper.getPrimaryConnectionType(
            hasWifi = false, hasCellular = false, hasEthernet = false))
  }

  @Test
  fun `isValidForAuthentication should return true when has validated Internet`() {
    assertTrue(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = true,
            hasWifi = false,
            hasCellular = false,
            hasEthernet = false))
  }

  @Test
  fun `isValidForAuthentication should return true when has Internet with WiFi transport`() {
    assertTrue(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = false,
            hasWifi = true,
            hasCellular = false,
            hasEthernet = false))
  }

  @Test
  fun `isValidForAuthentication should return true when has Internet with Cellular transport`() {
    assertTrue(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = false,
            hasWifi = false,
            hasCellular = true,
            hasEthernet = false))
  }

  @Test
  fun `isValidForAuthentication should return true when has Internet with Ethernet transport`() {
    assertTrue(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = false,
            hasWifi = false,
            hasCellular = false,
            hasEthernet = true))
  }

  @Test
  fun `isValidForAuthentication should return false when no Internet`() {
    assertFalse(
        NetworkHelper.isValidForAuthentication(
            hasInternet = false,
            hasValidatedInternet = false,
            hasWifi = true,
            hasCellular = true,
            hasEthernet = true))
  }

  @Test
  fun `isValidForAuthentication should return false when Internet but no validated and no transport`() {
    assertFalse(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = false,
            hasWifi = false,
            hasCellular = false,
            hasEthernet = false))
  }

  // Testing of complex combinations to ensure complete coverage
  @Test
  fun `isConnected should handle multiple transports correctly`() {
    // WiFi + Cellular
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = true, hasCellular = true, hasEthernet = false))

    // WiFi + Ethernet
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = true, hasCellular = false, hasEthernet = true))

    // Cellular + Ethernet
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = false, hasCellular = true, hasEthernet = true))

    // All transports
    assertTrue(
        NetworkHelper.isConnected(
            hasInternet = true, hasWifi = true, hasCellular = true, hasEthernet = true))
  }

  @Test
  fun `isValidForAuthentication should handle multiple transports correctly`() {
    // Multiple transports with Internet but no validation
    assertTrue(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = false,
            hasWifi = true,
            hasCellular = true,
            hasEthernet = false))

    // Multiple transports with validation
    assertTrue(
        NetworkHelper.isValidForAuthentication(
            hasInternet = true,
            hasValidatedInternet = true,
            hasWifi = true,
            hasCellular = true,
            hasEthernet = true))
  }
}
