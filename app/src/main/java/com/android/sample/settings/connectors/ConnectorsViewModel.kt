package com.android.sample.settings.connectors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.settings.Localization
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
class ConnectorsViewModel(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west6"),
    private val edRemoteDataSource: EdConnectorRemoteDataSource =
        EdConnectorRemoteDataSource(functions),
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            ConnectorsUiState(
                connectors = mockConnectors,
                isLoadingEd = true
            )
        )
  val uiState: StateFlow<ConnectorsUiState> = _uiState.asStateFlow()

    init {
        refreshEdStatus()
    }

  /** Connects a connector (no confirmation needed). */
  fun connectConnector(connectorId: String) {
      if (connectorId == "ed") {
          // Pour ED on ouvre le dialog de connexion (token)
          _uiState.update { it.copy(isEdConnectDialogOpen = true, edConnectError = null) }
          return
      }

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
        if (connectorId == "ed") {
            // Cas spécial ED : on appelle le backend pour vraiment supprimer la config
            viewModelScope.launch {
                // On ferme le dialog tout de suite
                _uiState.update { it.copy(pendingConnectorForDisconnect = null) }

                try {
                    // Appel de la Cloud Function: edConnectorDisconnectFn
                    edRemoteDataSource.disconnect()

                    // Si ça marche, on met ED en "déconnecté" côté UI
                    _uiState.update { current ->
                        current.copy(
                            connectors = current.connectors.map { connector ->
                                if (connector.id == "ed") {
                                    connector.copy(isConnected = false)
                                } else {
                                    connector
                                }
                            }
                        )
                    }
                } catch (_: Exception) {
                    // Pour l'instant on ne gère pas encore d'erreur user-facing ici.
                    // Tu pourras plus tard ajouter un champ errorMessage dans l’UI state si tu veux.
                }
            }
        } else {
            // Comportement local pour les autres connecteurs (Moodle, etc.)
            _uiState.update { currentState ->
                currentState.copy(
                    connectors = currentState.connectors.map {
                        if (it.id == connectorId) {
                            it.copy(isConnected = false)
                        } else {
                            it
                        }
                    },
                    pendingConnectorForDisconnect = null
                )
            }
        }
    }


    /** Dismisses the disconnect confirmation dialog. */
  fun dismissDisconnectConfirmation() {
    _uiState.update { it.copy(pendingConnectorForDisconnect = null) }
  }

    private fun refreshEdStatus() {
        viewModelScope.launch {
            try {
                // on indique qu'on charge, on reset l'erreur ED
                _uiState.update { it.copy(isLoadingEd = true, edError = null) }

                val config: EdConnectorConfigRemote = edRemoteDataSource.getStatus()

                _uiState.update { state ->
                    val updatedConnectors =
                        state.connectors.map { connector ->
                            if (connector.id == "ed") {
                                connector.copy(
                                    isConnected = config.status == EdConnectorStatusRemote.CONNECTED
                                )
                            } else {
                                connector
                            }
                        }

                    state.copy(
                        connectors = updatedConnectors,
                        isLoadingEd = false,
                        edError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoadingEd = false,
                        edError = e.message ?: "Failed to load ED connector status"
                    )
                }
            }
        }
    }

    /** Ferme le dialog de connexion ED. */
    fun dismissEdConnectDialog() {
        _uiState.update { it.copy(isEdConnectDialogOpen = false, edConnectError = null) }
    }

    /** Confirme la connexion ED avec le token (et baseUrl optionnelle). */
    fun confirmEdConnect(apiToken: String, baseUrl: String?) {
        viewModelScope.launch {
            // on indique qu'on est en train de connecter
            _uiState.update { it.copy(isEdConnecting = true, edConnectError = null) }

            try {
                val config = edRemoteDataSource.connect(apiToken, baseUrl)
                android.util.Log.d(
                    "ED_CONNECT",
                    "config after connect: status=${config.status}, lastError=${config.lastError}"
                )

                if (config.status == EdConnectorStatusRemote.CONNECTED) {
                    // succès : on met ED en "connected" et on ferme le dialog
                    _uiState.update { state ->
                        val updatedConnectors =
                            state.connectors.map { connector ->
                                if (connector.id == "ed") {
                                    connector.copy(isConnected = true)
                                } else {
                                    connector
                                }
                            }

                        state.copy(
                            connectors = updatedConnectors,
                            isEdConnecting = false,
                            isEdConnectDialogOpen = false,
                            edConnectError = null,
                        )
                    }
                } else {
                    // backend a répondu ERROR (ex: invalid_credentials)
                    _uiState.update { state ->
                        val friendlyMessageKey =
                            when (config.lastError) {
                                "invalid_credentials" -> "ed_connect_invalid_credentials"
                                "api_unreachable" -> "ed_connect_api_unreachable"
                                else -> "ed_connect_generic_error"
                            }
                        val friendlyMessage = Localization.t(friendlyMessageKey)

                        state.copy(
                            isEdConnecting = false,
                            edConnectError = friendlyMessage,
                        )
                    }
                }
            } catch (e: Exception) {
                // Erreur réseau / autre
                _uiState.update { state ->
                    state.copy(
                        isEdConnecting = false,
                        edConnectError = Localization.t("ed_connect_generic_error"),
                    )
                }
            }
        }
    }
}
