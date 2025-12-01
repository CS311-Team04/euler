package com.android.sample.settings.connectors

/**
 * Mirror of ED connector statuses returned by Firebase Functions.
 *
 * Backend (TypeScript) returns:
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
 * "Remote" model for ED config as returned by callables. This is the type used in the ViewModel /
 * UI.
 */
data class EdConnectorConfigRemote(
    val status: EdConnectorStatusRemote,
    val baseUrl: String?,
    val lastTestAt: String?,
    val lastError: String?
)

/** Raw mapping Any? -> EdConnectorConfigRemote from Firebase Functions result.data. */
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
