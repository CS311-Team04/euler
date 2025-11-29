package com.android.sample.settings.connectors

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

private const val FN_STATUS = "edConnectorStatusFn"
private const val FN_CONNECT = "edConnectorConnectFn"
private const val FN_DISCONNECT = "edConnectorDisconnectFn"
private const val FN_TEST = "edConnectorTestFn"

/**
 * Petit data source pour appeler les Cloud Functions ED. Chaque méthode renvoie la config actuelle
 * renvoyée par le backend.
 */
class EdConnectorRemoteDataSource(
    private val functions: FirebaseFunctions,
) {

  suspend fun getStatus(): EdConnectorConfigRemote {
    val result = functions.getHttpsCallable(FN_STATUS).call().await()
    return mapEdConnectorConfig(result.getData())
  }

  suspend fun connect(apiToken: String, baseUrl: String?): EdConnectorConfigRemote {
    val payload =
        hashMapOf<String, Any>(
            "apiToken" to apiToken,
        )
    if (!baseUrl.isNullOrBlank()) {
      payload["baseUrl"] = baseUrl
    }

    val result = functions.getHttpsCallable(FN_CONNECT).call(payload).await()
    return mapEdConnectorConfig(result.getData())
  }

  suspend fun disconnect(): EdConnectorConfigRemote {
    val result = functions.getHttpsCallable(FN_DISCONNECT).call().await()
    return mapEdConnectorConfig(result.getData())
  }

  suspend fun test(): EdConnectorConfigRemote {
    val result = functions.getHttpsCallable(FN_TEST).call().await()
    return mapEdConnectorConfig(result.getData())
  }
}
