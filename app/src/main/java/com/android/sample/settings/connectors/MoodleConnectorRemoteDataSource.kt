package com.android.sample.settings.connectors

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

private const val FN_STATUS = "connectorsMoodleStatusFn"
private const val FN_CONNECT = "connectorsMoodleConnectFn"
private const val FN_DISCONNECT = "connectorsMoodleDisconnectFn"
private const val FN_TEST = "connectorsMoodleTestFn"

/**
 * Small data source to call Moodle Cloud Functions. Each method returns the current config returned
 * by the backend.
 */
open class MoodleConnectorRemoteDataSource(
    functions: FirebaseFunctions? = null,
) {
  private val functions: FirebaseFunctions =
      requireNotNull(functions) {
        "FirebaseFunctions must not be null in MoodleConnectorRemoteDataSource"
      }

  /** Gets the current Moodle connector status from the backend. */
  open suspend fun getStatus(): MoodleConnectorConfigRemote {
    val result = functions.getHttpsCallable(FN_STATUS).call().await()
    return mapMoodleConnectorConfig(result.getData())
  }

  /** Connects the Moodle connector with the provided base URL and token. */
  open suspend fun connect(baseUrl: String, token: String): MoodleConnectorConfigRemote {
    val payload =
        hashMapOf<String, Any>(
            "baseUrl" to baseUrl,
            "token" to token,
        )

    val result = functions.getHttpsCallable(FN_CONNECT).call(payload).await()
    return mapMoodleConnectorConfig(result.getData())
  }

  /** Disconnects the Moodle connector. */
  open suspend fun disconnect(): MoodleConnectorConfigRemote {
    val result = functions.getHttpsCallable(FN_DISCONNECT).call().await()
    return mapMoodleConnectorConfig(result.getData())
  }

  /** Tests the Moodle connector connection. */
  suspend fun test(): MoodleConnectorConfigRemote {
    val result = functions.getHttpsCallable(FN_TEST).call().await()
    return mapMoodleConnectorConfig(result.getData())
  }
}
