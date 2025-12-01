package com.android.sample.settings.connectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.epfl.EpflScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Initial connector list - status will be updated from repositories */
private fun initialConnectors() =
    listOf(
        Connector(id = "moodle", name = "Moodle", description = "courses", isConnected = false),
        Connector(id = "ed", name = "Ed", description = "Q&A platform", isConnected = false),
        Connector(
            id = "epfl_campus",
            name = "EPFL Campus",
            description = "EPFL schedule",
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
 * - Connection state for each connector (fetched from repositories)
 * - Pending connector for disconnect confirmation
 */
class ConnectorsViewModel(
    private val epflScheduleRepository: EpflScheduleRepository = EpflScheduleRepository()
) : ViewModel() {
  private val _uiState = MutableStateFlow(ConnectorsUiState(connectors = initialConnectors()))
  val uiState: StateFlow<ConnectorsUiState> = _uiState.asStateFlow()

  init {
    // Fetch real connection statuses on initialization
    refreshConnectionStatuses()
  }

  /** Refresh connection statuses from repositories */
  fun refreshConnectionStatuses() {
    viewModelScope.launch {
      // Check EPFL Campus status
      if (epflScheduleRepository.isAuthenticated()) {
        val status = epflScheduleRepository.getStatus()
        if (status is com.android.sample.epfl.ScheduleStatus.Connected) {
          updateConnectorStatus("epfl_campus", true)
        }
      }
    }
  }

  private fun updateConnectorStatus(connectorId: String, isConnected: Boolean) {
    _uiState.update { currentState ->
      currentState.copy(
          connectors =
              currentState.connectors.map {
                if (it.id == connectorId) it.copy(isConnected = isConnected) else it
              })
    }
  }

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
