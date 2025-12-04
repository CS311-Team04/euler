package com.android.sample.settings.connectors

/**
 * Mirror of Moodle connector statuses returned by Firebase Functions.
 *
 * Backend (TypeScript) returns:
 * - "not_connected"
 * - "connected"
 * - "error"
 */
enum class MoodleConnectorStatusRemote {
  NOT_CONNECTED,
  CONNECTED,
  ERROR;

  companion object {
    fun fromRaw(value: String?): MoodleConnectorStatusRemote =
        when (value) {
          "connected" -> CONNECTED
          "error" -> ERROR
          else -> NOT_CONNECTED
        }
  }
}

/**
 * "Remote" model for Moodle config as returned by callables. This is the type used in the ViewModel
 * / UI.
 */
data class MoodleConnectorConfigRemote(
    val status: MoodleConnectorStatusRemote,
    val lastTestAt: String?,
    val lastError: String?
)

/** Raw mapping Any? -> MoodleConnectorConfigRemote from Firebase Functions result.data. */
@Suppress("UNCHECKED_CAST")
fun mapMoodleConnectorConfig(raw: Any?): MoodleConnectorConfigRemote {
  val map = raw as? Map<String, Any?> ?: emptyMap()

  val statusStr = map["status"] as? String
  val lastTestAt = map["lastTestAt"] as? String
  val lastError = map["lastError"] as? String

  return MoodleConnectorConfigRemote(
      status = MoodleConnectorStatusRemote.fromRaw(statusStr),
      lastTestAt = lastTestAt,
      lastError = lastError)
}
