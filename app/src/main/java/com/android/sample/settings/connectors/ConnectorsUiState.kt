package com.android.sample.settings.connectors

/**
 * Immutable snapshot of everything the ConnectorsScreen needs to render at a given time. Any UI
 * change produces a new instance via copy(...).
 */
data class ConnectorsUiState(
    val connectors: List<Connector> = emptyList(),
    val pendingConnectorForDisconnect: Connector? = null,
    val isLoadingEd: Boolean = false,
    val edError: String? = null,
    val isEdConnectDialogOpen: Boolean = false,
    val isEdConnecting: Boolean = false,
    val edConnectError: String? = null,
    val detectedEdToken: String? = null,
    val showEdClipboardSuggestion: Boolean = false,
    val isLoadingMoodle: Boolean = false,
    val moodleError: String? = null, // status refresh/errors shown on the main screen
    val isMoodleRedirecting: Boolean = false, // Simulates redirect to Moodle
    val isMoodleConnectDialogOpen: Boolean = false,
    val isMoodleConnecting: Boolean = false,
    val moodleConnectError: String? = null, // errors shown in the connect dialog
)
