package com.android.sample.settings.connectors

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

private const val FN_STATUS = "edConnectorStatusFn"
private const val FN_CONNECT = "edConnectorConnectFn"
private const val FN_DISCONNECT = "edConnectorDisconnectFn"
private const val FN_TEST = "edConnectorTestFn"

/**
 * Small data source to call ED Cloud Functions. Each method returns the current config returned by
 * the backend.
 */
open class EdConnectorRemoteDataSource(
    private val functions: FirebaseFunctions? = null,
) {

  /** Gets the current ED connector status from the backend. */
  open suspend fun getStatus(): EdConnectorConfigRemote {
    val result = functions!!.getHttpsCallable(FN_STATUS).call().await()
    return mapEdConnectorConfig(result.getData())
  }

  /** Connects the ED connector with the provided API token and optional base URL. */
  open suspend fun connect(apiToken: String, baseUrl: String?): EdConnectorConfigRemote {
    val payload =
        hashMapOf<String, Any>(
            "apiToken" to apiToken,
        )
    if (!baseUrl.isNullOrBlank()) {
      payload["baseUrl"] = baseUrl
    }

    val result = functions!!.getHttpsCallable(FN_CONNECT).call(payload).await()
    return mapEdConnectorConfig(result.getData())
  }

  /** Disconnects the ED connector. */
  open suspend fun disconnect(): EdConnectorConfigRemote {
    val result = functions!!.getHttpsCallable(FN_DISCONNECT).call().await()
    return mapEdConnectorConfig(result.getData())
  }

  /** Tests the ED connector connection. */
  suspend fun test(): EdConnectorConfigRemote {
    val result = functions!!.getHttpsCallable(FN_TEST).call().await()
    return mapEdConnectorConfig(result.getData())
  }
}
