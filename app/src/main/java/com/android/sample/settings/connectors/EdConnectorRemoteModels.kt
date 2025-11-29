package com.android.sample.settings.connectors

/**
 * Mirror des statuts du connecteur ED renvoyés par Firebase Functions.
 *
 * Backend (TypeScript) renvoie:
 * - "not_connected"
 * - "connected"
 * - "error"
 */
enum class EdConnectorStatusRemote {
  NOT_CONNECTED,
  CONNECTED,
  ERROR;

  companion object {
    fun fromRaw(value: String?): EdConnectorStatusRemote =
        when (value) {
          "connected" -> CONNECTED
          "error" -> ERROR
          else -> NOT_CONNECTED
        }
  }
}

/**
 * Modèle "remote" pour la config ED telle que renvoyée par les callables. C’est ce type qu’on
 * utilise dans le ViewModel / UI.
 */
data class EdConnectorConfigRemote(
    val status: EdConnectorStatusRemote,
    val baseUrl: String?,
    val lastTestAt: String?,
    val lastError: String?
)

/** Mapping brut Any? -> EdConnectorConfigRemote à partir de result.data des fonctions Firebase. */
@Suppress("UNCHECKED_CAST")
fun mapEdConnectorConfig(raw: Any?): EdConnectorConfigRemote {
  val map = raw as? Map<String, Any?> ?: emptyMap()

  val statusStr = map["status"] as? String
  val baseUrl = map["baseUrl"] as? String
  val lastTestAt = map["lastTestAt"] as? String
  val lastError = map["lastError"] as? String

  return EdConnectorConfigRemote(
      status = EdConnectorStatusRemote.fromRaw(statusStr),
      baseUrl = baseUrl,
      lastTestAt = lastTestAt,
      lastError = lastError)
}
