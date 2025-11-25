package com.android.sample.settings.connectors

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Mock data for connectors. In the future, this will come from a repository. */
private val mockConnectors =
    listOf(
        Connector(id = "moodle", name = "Moodle", description = "courses", isConnected = false),
        Connector(id = "ed", name = "Ed", description = "Q&A platform", isConnected = false),
        Connector(
            id = "epfl_campus",
            name = "EPFL Campus",
            description = "EPFL services",
            isConnected = false),
        Connector(
            id = "is_academia",
            name = "IS Academia",
            description = "Pers. services",
            isConnected = false))

/**
 * ViewModel for ConnectorsScreen.
 *
 * Manages:
 * - List of connectors
 * - Connection state for each connector
 * - Pending connector for disconnect confirmation
 */
class ConnectorsViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(ConnectorsUiState(connectors = mockConnectors))
  val uiState: StateFlow<ConnectorsUiState> = _uiState.asStateFlow()

  /** Connects a connector (no confirmation needed). */
  fun connectConnector(connectorId: String) {
    _uiState.update { currentState ->
      currentState.copy(
          connectors =
              currentState.connectors.map {
                if (it.id == connectorId) {
                  it.copy(isConnected = true)
                } else {
                  it
                }
              })
    }
  }

  /** Shows the disconnect confirmation dialog for a connector. */
  fun showDisconnectConfirmation(connector: Connector) {
    _uiState.update { it.copy(pendingConnectorForDisconnect = connector) }
  }

  /** Disconnects a connector after confirmation. */
  fun disconnectConnector(connectorId: String) {
    _uiState.update { currentState ->
      currentState.copy(
          connectors =
              currentState.connectors.map {
                if (it.id == connectorId) {
                  it.copy(isConnected = false)
                } else {
                  it
                }
              },
          pendingConnectorForDisconnect = null)
    }
  }

  /** Dismisses the disconnect confirmation dialog. */
  fun dismissDisconnectConfirmation() {
    _uiState.update { it.copy(pendingConnectorForDisconnect = null) }
  }
}
