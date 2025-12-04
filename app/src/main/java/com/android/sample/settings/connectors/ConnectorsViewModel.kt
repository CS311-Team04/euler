package com.android.sample.settings.connectors

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.settings.Localization
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val MOODLE_CONNECTOR_ID = "moodle"

/** Mock data for connectors. In the future, this will come from a repository. */
private val mockConnectors =
    listOf(
        Connector(
            id = MOODLE_CONNECTOR_ID,
            name = "Moodle",
            description = "courses",
            isConnected = false),
        Connector(
            id = ED_CONNECTOR_ID, name = "Ed", description = "Q&A platform", isConnected = false),
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
    private val moodleRemoteDataSource: MoodleConnectorRemoteDataSource =
        MoodleConnectorRemoteDataSource(functions),
) : ViewModel() {
  private val _uiState =
      MutableStateFlow(
          ConnectorsUiState(
              connectors = mockConnectors, isLoadingEd = true, isLoadingMoodle = true))
  val uiState: StateFlow<ConnectorsUiState> = _uiState.asStateFlow()

  init {
    refreshEdStatus()
    refreshMoodleStatus()
  }

  /** Connects a connector (no confirmation needed). */
  fun connectConnector(connectorId: String) {
    // ED connector navigation is handled in NavGraph via onConnectorClick
    if (connectorId == ED_CONNECTOR_ID) {
      return
    }

    if (connectorId == MOODLE_CONNECTOR_ID) {
      // For Moodle, open the connection dialog (WebView for seamless auth)
      _uiState.update { it.copy(isMoodleConnectDialogOpen = true, moodleConnectError = null) }
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
    if (connectorId == ED_CONNECTOR_ID) {
      disconnectEdConnector()
    } else if (connectorId == MOODLE_CONNECTOR_ID) {
      disconnectMoodleConnector()
    } else {
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
  }

  private fun disconnectEdConnector() {
    viewModelScope.launch {
      _uiState.update { it.copy(pendingConnectorForDisconnect = null) }
      try {
        edRemoteDataSource.disconnect()
        _uiState.update { current ->
          current.copy(
              connectors =
                  current.connectors.map { connector ->
                    if (connector.id == ED_CONNECTOR_ID) {
                      connector.copy(isConnected = false)
                    } else {
                      connector
                    }
                  })
        }
        // Note: Status will be refreshed automatically when ConnectorsScreen is displayed
      } catch (_: Exception) {
        // For now, we don't handle user-facing errors here yet.
      }
    }
  }

  /** Dismisses the disconnect confirmation dialog. */
  fun dismissDisconnectConfirmation() {
    _uiState.update { it.copy(pendingConnectorForDisconnect = null) }
  }

  /** Refreshes the ED connector status from the backend. */
  fun refreshEdStatus() {
    viewModelScope.launch {
      try {
        // Indicate loading, reset ED error
        _uiState.update { it.copy(isLoadingEd = true, edError = null) }

        val config: EdConnectorConfigRemote = edRemoteDataSource.getStatus()

        _uiState.update { state ->
          val updatedConnectors =
              state.connectors.map { connector ->
                if (connector.id == ED_CONNECTOR_ID) {
                  connector.copy(isConnected = config.status == EdConnectorStatusRemote.CONNECTED)
                } else {
                  connector
                }
              }

          state.copy(connectors = updatedConnectors, isLoadingEd = false, edError = null)
        }
      } catch (e: Exception) {
        Log.e("ConnectorsViewModel", "Failed to refresh ED connector status", e)
        _uiState.update { state ->
          state.copy(
              isLoadingEd = false, edError = Localization.t("settings_connectors_ed_status_error"))
        }
      }
    }
  }

  /** Closes the ED connection dialog. */
  fun dismissEdConnectDialog() {
    _uiState.update { it.copy(isEdConnectDialogOpen = false, edConnectError = null) }
  }

  /** Confirms ED connection with token (and optional baseUrl). */
  fun confirmEdConnect(apiToken: String, baseUrl: String?) {
    viewModelScope.launch {
      // Indicate we are connecting
      _uiState.update { it.copy(isEdConnecting = true, edConnectError = null) }

      try {
        val config = edRemoteDataSource.connect(apiToken, baseUrl)
        android.util.Log.d(
            "ED_CONNECT",
            "config after connect: status=${config.status}, lastError=${config.lastError}")

        if (config.status == EdConnectorStatusRemote.CONNECTED) {
          // Success: update state immediately
          _uiState.update { state ->
            val updatedConnectors =
                state.connectors.map { connector ->
                  if (connector.id == ED_CONNECTOR_ID) {
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
          // Note: Status will be refreshed automatically when user returns to ConnectorsScreen
        } else {
          // Backend returned ERROR (e.g.: invalid_credentials)
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
        // Network error / other
        _uiState.update { state ->
          state.copy(
              isEdConnecting = false,
              edConnectError = Localization.t("ed_connect_generic_error"),
          )
        }
      }
    }
  }

  /**
   * Checks clipboard for a potential ED token when the screen becomes visible. If a plausible token
   * is found and the token input is empty, shows the suggestion banner.
   */
  fun checkClipboardForEdToken(context: Context, currentTokenInput: String = "") {
    try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

      val isTokenLike =
          if (clip.isNotBlank()) {
            looksLikeEdToken(clip)
          } else {
            false
          }

      if (clip.isNotBlank() && isTokenLike && currentTokenInput.isBlank()) {
        _uiState.update { it.copy(detectedEdToken = clip, showEdClipboardSuggestion = true) }
      } else {
        // Only clear if we're currently showing a suggestion and conditions changed
        val currentState = _uiState.value
        if (currentState.showEdClipboardSuggestion &&
            (currentTokenInput.isNotBlank() || !isTokenLike)) {
          _uiState.update { it.copy(showEdClipboardSuggestion = false, detectedEdToken = null) }
        }
      }
    } catch (e: Exception) {
      // Clipboard access might fail on some devices, ignore silently
    }
  }

  /**
   * Simple heuristic to check if text looks like an ED token. ED tokens are typically alphanumeric
   * strings, at least 10 characters long.
   */
  private fun looksLikeEdToken(text: String): Boolean {
    val trimmed = text.trim()
    return trimmed.length >= 10 &&
        !trimmed.contains(' ') &&
        !trimmed.contains('\n') &&
        !trimmed.contains('\t') &&
        trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
  }

  /**
   * Accepts the detected clipboard token. This should be called when the user taps "Use this
   * token". Returns the token string to fill into the text field.
   */
  fun acceptEdClipboardToken(): String {
    val token = _uiState.value.detectedEdToken ?: ""
    _uiState.update { it.copy(showEdClipboardSuggestion = false, detectedEdToken = null) }
    return token
  }

  /** Dismisses the clipboard suggestion banner. */
  fun dismissEdClipboardSuggestion() {
    _uiState.update { it.copy(showEdClipboardSuggestion = false, detectedEdToken = null) }
  }

  /** Refreshes the Moodle connector status from the backend. */
  fun refreshMoodleStatus() {
    viewModelScope.launch {
      try {
        // Indicate loading, reset Moodle error
        _uiState.update { it.copy(isLoadingMoodle = true, moodleError = null) }

        val config: MoodleConnectorConfigRemote = moodleRemoteDataSource.getStatus()

        _uiState.update { state ->
          val updatedConnectors =
              state.connectors.map { connector ->
                if (connector.id == MOODLE_CONNECTOR_ID) {
                  connector.copy(
                      isConnected = config.status == MoodleConnectorStatusRemote.CONNECTED)
                } else {
                  connector
                }
              }

          state.copy(connectors = updatedConnectors, isLoadingMoodle = false, moodleError = null)
        }
      } catch (e: Exception) {
        Log.e("ConnectorsViewModel", "Failed to refresh Moodle connector status", e)
        _uiState.update { state ->
          state.copy(
              isLoadingMoodle = false,
              moodleError = Localization.t("settings_connectors_moodle_status_error"))
        }
      }
    }
  }

  private fun disconnectMoodleConnector() {
    viewModelScope.launch {
      _uiState.update { it.copy(pendingConnectorForDisconnect = null) }
      try {
        moodleRemoteDataSource.disconnect()
        _uiState.update { current ->
          current.copy(
              connectors =
                  current.connectors.map { connector ->
                    if (connector.id == MOODLE_CONNECTOR_ID) {
                      connector.copy(isConnected = false)
                    } else {
                      connector
                    }
                  })
        }
      } catch (_: Exception) {
        // For now, we don't handle user-facing errors here yet.
      }
    }
  }

  /** Closes the Moodle connection dialog. */
  fun dismissMoodleConnectDialog() {
    _uiState.update { it.copy(isMoodleConnectDialogOpen = false, moodleConnectError = null) }
  }

  /**
   * Fetches a Moodle token using username and password via Firebase Function. This is more secure
   * as credentials are sent server-side only.
   */
  suspend fun fetchMoodleToken(
      baseUrl: String,
      username: String,
      password: String
  ): Result<String> {
    return try {
      val fetchTokenFn = functions.getHttpsCallable("connectorsMoodleFetchTokenFn")

      val data =
          hashMapOf(
              "baseUrl" to baseUrl.trim(),
              "username" to username.trim(),
              "password" to password.trim())

      Log.d("MOODLE_TOKEN", "Fetching token via Firebase Function")

      val result = fetchTokenFn.call(data).await()
      val responseData = result.getData() as? Map<*, *>
      val token = responseData?.get("token") as? String

      if (token.isNullOrBlank()) {
        Log.e("MOODLE_TOKEN", "No token in Firebase Function response")
        return Result.failure(Exception("No token received from Moodle"))
      }

      Log.d("MOODLE_TOKEN", "Successfully fetched token")
      Result.success(token)
    } catch (e: Exception) {
      Log.e("MOODLE_TOKEN", "Exception fetching token via Firebase Function", e)
      // Extract user-friendly error message
      val errorMessage =
          when {
            e.message?.contains("unauthenticated") == true -> "Authentication required"
            e.message?.contains("invalid-argument") == true -> "Invalid credentials"
            e.message?.contains("Failed to fetch token") == true ->
                "Invalid credentials or Moodle server error"
            else -> e.message ?: "Failed to fetch token"
          }
      Result.failure(Exception(errorMessage))
    }
  }

  /**
   * Fetches Moodle token and connects automatically. This is the main entry point for Moodle
   * connection flow.
   */
  fun connectMoodleWithCredentials(baseUrl: String, username: String, password: String) {
    viewModelScope.launch {
      // Indicate we are fetching token
      _uiState.update { it.copy(isMoodleConnecting = true, moodleConnectError = null) }

      // Fetch token from Moodle API
      val tokenResult = fetchMoodleToken(baseUrl, username, password)

      if (tokenResult.isFailure) {
        // Failed to fetch token
        val error = tokenResult.exceptionOrNull() ?: Exception("Unknown error")
        val errorMessage =
            when {
              error.message?.contains("HTTP") == true ->
                  Localization.t("moodle_connect_api_unreachable")
              else -> Localization.t("moodle_connect_generic_error")
            }

        _uiState.update { state ->
          state.copy(
              isMoodleConnecting = false,
              moodleConnectError = errorMessage,
          )
        }
        return@launch
      }

      // Token fetched successfully, now connect
      val token =
          tokenResult.getOrNull()
              ?: run {
                _uiState.update { state ->
                  state.copy(
                      isMoodleConnecting = false,
                      moodleConnectError = Localization.t("moodle_connect_generic_error"),
                  )
                }
                return@launch
              }
      confirmMoodleConnect(baseUrl, token)
    }
  }

  /** Confirms Moodle connection with base URL and token. */
  fun confirmMoodleConnect(baseUrl: String, token: String) {
    viewModelScope.launch {
      // Indicate we are connecting
      _uiState.update { it.copy(isMoodleConnecting = true, moodleConnectError = null) }

      try {
        val config = moodleRemoteDataSource.connect(baseUrl, token)
        android.util.Log.d(
            "MOODLE_CONNECT",
            "config after connect: status=${config.status}, lastError=${config.lastError}")

        if (config.status == MoodleConnectorStatusRemote.CONNECTED) {
          // Success: set Moodle to "connected" and close the dialog
          _uiState.update { state ->
            val updatedConnectors =
                state.connectors.map { connector ->
                  if (connector.id == MOODLE_CONNECTOR_ID) {
                    connector.copy(isConnected = true)
                  } else {
                    connector
                  }
                }

            state.copy(
                connectors = updatedConnectors,
                isMoodleConnecting = false,
                isMoodleConnectDialogOpen = false,
                moodleConnectError = null,
            )
          }
        } else {
          // Backend returned ERROR
          _uiState.update { state ->
            val friendlyMessageKey =
                when (config.lastError) {
                  "invalid_credentials" -> "moodle_connect_api_unreachable"
                  "api_unreachable" -> "moodle_connect_api_unreachable"
                  else -> "moodle_connect_generic_error"
                }
            val friendlyMessage = Localization.t(friendlyMessageKey)

            state.copy(
                isMoodleConnecting = false,
                moodleConnectError = friendlyMessage,
            )
          }
        }
      } catch (e: Exception) {
        // Network error / other
        _uiState.update { state ->
          state.copy(
              isMoodleConnecting = false,
              moodleConnectError = Localization.t("moodle_connect_generic_error"),
          )
        }
      }
    }
  }
}
