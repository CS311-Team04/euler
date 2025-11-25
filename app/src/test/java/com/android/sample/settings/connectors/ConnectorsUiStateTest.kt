package com.android.sample.settings.connectors

import org.junit.Assert.*
import org.junit.Test

class ConnectorsUiStateTest {

  @Test
  fun `default state has empty connectors list and null pending`() {
    val state = ConnectorsUiState()

    assertTrue(state.connectors.isEmpty())
    assertNull(state.pendingConnectorForDisconnect)
  }

  @Test
  fun `state with connectors stores them correctly`() {
    val connectors =
        listOf(
            Connector(id = "1", name = "Test1", description = "Desc1", isConnected = false),
            Connector(id = "2", name = "Test2", description = "Desc2", isConnected = true))
    val state = ConnectorsUiState(connectors = connectors)

    assertEquals(2, state.connectors.size)
    assertEquals(connectors, state.connectors)
  }

  @Test
  fun `state with pending connector stores it correctly`() {
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val state = ConnectorsUiState(pendingConnectorForDisconnect = connector)

    assertEquals(connector, state.pendingConnectorForDisconnect)
  }

  @Test
  fun `state copy works correctly`() {
    val connectors =
        listOf(Connector(id = "1", name = "Test1", description = "Desc1", isConnected = false))
    val connector = Connector(id = "test", name = "Test", description = "Desc", isConnected = true)
    val original =
        ConnectorsUiState(connectors = connectors, pendingConnectorForDisconnect = connector)

    val copied = original.copy(pendingConnectorForDisconnect = null)

    assertEquals(original.connectors, copied.connectors)
    assertNull(copied.pendingConnectorForDisconnect)
    assertNotNull(original.pendingConnectorForDisconnect)
  }

  @Test
  fun `state copy with new connectors works correctly`() {
    val original = ConnectorsUiState()
    val newConnectors =
        listOf(Connector(id = "1", name = "Test1", description = "Desc1", isConnected = false))

    val copied = original.copy(connectors = newConnectors)

    assertEquals(newConnectors, copied.connectors)
    assertEquals(original.pendingConnectorForDisconnect, copied.pendingConnectorForDisconnect)
  }
}
