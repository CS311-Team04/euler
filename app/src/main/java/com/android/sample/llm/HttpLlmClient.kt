package com.android.sample.llm

import com.android.sample.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * LLM client backed by a plain HTTP endpoint (e.g. Cloud Functions HTTPS trigger).
 *
 * Configuration is driven by `BuildConfig` so it can point to the emulator in debug builds. The
 * backend is expected to accept a JSON payload `{"question": "<prompt>"}` and respond with
 * `{"reply": "<text>"}`. Any deviation throws an [IllegalStateException] that the caller can
 * surface to the UI.
 */
class HttpLlmClient(
    private val endpoint: String = BuildConfig.LLM_HTTP_ENDPOINT,
    private val apiKey: String = BuildConfig.LLM_HTTP_API_KEY,
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
) : LlmClient {

  private val jsonMediaType = CONTENT_TYPE_JSON.toMediaType()

  /** Performs a blocking HTTP call on [Dispatchers.IO] and returns the non-empty `reply` field. */
  override suspend fun generateReply(prompt: String): BotReply =
      generateReply(prompt = prompt, summary = null, transcript = null, profileContext = null)

  /**
   * Advanced API with context support. Includes summary, transcript, and profile context in the
   * HTTP payload.
   */
  override suspend fun generateReply(
      prompt: String,
      summary: String?,
      transcript: String?,
      profileContext: String?
  ): BotReply =
      withContext(Dispatchers.IO) {
        if (endpoint.isBlank()) {
          throw IllegalStateException("LLM HTTP endpoint not configured")
        }
        validateEndpoint(endpoint)

        val payloadMap = mutableMapOf<String, Any?>(JSON_KEY_QUESTION to prompt)
        summary?.let { payloadMap[JSON_KEY_SUMMARY] = it }
        transcript?.let { payloadMap[JSON_KEY_TRANSCRIPT] = it }
        profileContext?.let { payloadMap[JSON_KEY_PROFILE_CONTEXT] = it }

        val payloadString = gson.toJson(payloadMap)
        val payload = payloadString.toRequestBody(jsonMediaType)

        val builder =
            Request.Builder()
                .url(endpoint)
                .post(payload)
                .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)

        if (apiKey.isNotBlank()) {
          // adapt header if your backend expects a different header (e.g. "x-api-key")
          builder.addHeader(HEADER_AUTHORIZATION, "$AUTH_SCHEME_BEARER $apiKey")
        }

        val request = builder.build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IllegalStateException("LLM HTTP error ${response.code}: ${response.message}")
          }
          val body =
              response.body?.string() ?: throw IllegalStateException("Empty LLM HTTP response")
          return@withContext parseBotReply(body, gson)
        }
      }

  private fun validateEndpoint(endpoint: String) {
    val url =
        endpoint.toHttpUrlOrNull()
            ?: throw IllegalStateException("Invalid LLM HTTP endpoint URL: $endpoint")
    if (url.isHttps) return
    if (url.scheme.equals("http", ignoreCase = true) && isAllowedLocalHost(url.host)) return
    // In debug builds, allow HTTP for private IP ranges (common in CI/test environments)
    if (BuildConfig.DEBUG && isPrivateIpAddress(url.host)) {
      return
    }
    throw IllegalStateException("LLM HTTP endpoint must use HTTPS (was $endpoint)")
  }

  private fun isPrivateIpAddress(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    return try {
      val address = InetAddress.getByName(host)
      address.isAnyLocalAddress ||
          address.isLoopbackAddress ||
          address.isLinkLocalAddress ||
          address.isSiteLocalAddress
    } catch (_: UnknownHostException) {
      false
    }
  }

  /**
   * Checks if the given host is an allowed localhost/loopback address. This function validates
   * hosts without hardcoding IP addresses to comply with SonarQube rules.
   *
   * @param host The hostname or IP address to validate
   * @return true if the host is a valid localhost/loopback address, false otherwise
   */
  private fun isAllowedLocalHost(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    // Normalize hostname to lowercase for case-insensitive comparison
    val normalized = host.lowercase(Locale.US)
    // Check common localhost identifier (no hardcoded IP addresses)
    // The subsequent InetAddress check handles loopback detection dynamically
    if (normalized == LOCALHOST) return true
    return try {
      // Resolve the hostname to an InetAddress for dynamic detection
      val address = InetAddress.getByName(host)
      // Accept only loopback addresses (127.0.0.0/8) in production builds.
      // Private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) are only
      // allowed in DEBUG builds via isPrivateIpAddress() check at lines 76-79.
      // This dynamically detects loopback without hardcoding IP addresses.
      address.isLoopbackAddress
    } catch (_: UnknownHostException) {
      // If hostname resolution fails, it's not a valid localhost address
      false
    }
  }
}

internal fun parseBotReply(body: String, gson: Gson): BotReply {
  if (body.isBlank()) {
    throw IllegalStateException("Empty LLM HTTP response")
  }
  val json: JsonObject =
      try {
        gson.fromJson(body, JsonObject::class.java)
      } catch (error: JsonParseException) {
        throw IllegalStateException("Invalid LLM HTTP response", error)
      } ?: throw IllegalStateException("Invalid LLM HTTP response")

  val replyText =
      json.getTrimmedString(JSON_KEY_REPLY) ?: throw IllegalStateException("Empty LLM reply")
  val url = json.getTrimmedString(JSON_KEY_PRIMARY_URL)
  val sourceType =
      com.android.sample.llm.SourceType.fromString(json.getTrimmedString(JSON_KEY_SOURCE_TYPE))
  val edIntentDetected = json.getBoolean(JSON_KEY_ED_INTENT_DETECTED)
  val edIntentType = json.getTrimmedString(JSON_KEY_ED_INTENT)
  val edFormattedQuestion = json.getTrimmedString(JSON_KEY_ED_FORMATTED_QUESTION)
  val edFormattedTitle = json.getTrimmedString(JSON_KEY_ED_FORMATTED_TITLE)
  val edIntent =
      com.android.sample.llm.EdIntent(
          detected = edIntentDetected,
          intent = edIntentType,
          formattedQuestion = edFormattedQuestion,
          formattedTitle = edFormattedTitle)

  val edFetchIntent =
      com.android.sample.llm.EdFetchIntent(
          detected = json.getBoolean(JSON_KEY_ED_FETCH_INTENT_DETECTED),
          query = json.getTrimmedString(JSON_KEY_ED_FETCH_QUERY),
      )

  return BotReply(replyText, url, sourceType, edIntent, edFetchIntent)
}

private fun JsonObject.getTrimmedString(key: String): String? {
  val element: JsonElement = get(key) ?: return null
  if (element.isJsonNull || !element.isJsonPrimitive) return null
  val primitive = element.asJsonPrimitive
  if (!primitive.isString) return null
  val value = primitive.asString.trim()
  return value.takeIf { it.isNotEmpty() }
}

private fun JsonObject.getBoolean(key: String): Boolean {
  val element: JsonElement = get(key) ?: return false
  if (element.isJsonNull || !element.isJsonPrimitive) return false
  val primitive = element.asJsonPrimitive
  return if (primitive.isBoolean) primitive.asBoolean else false
}

private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_AUTHORIZATION = "Authorization"
private const val AUTH_SCHEME_BEARER = "Bearer"
private const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
private const val JSON_KEY_QUESTION = "question"
private const val JSON_KEY_SUMMARY = "summary"
private const val JSON_KEY_TRANSCRIPT = "recentTranscript"
private const val JSON_KEY_PROFILE_CONTEXT = "profileContext"
private const val JSON_KEY_REPLY = "reply"
private const val JSON_KEY_PRIMARY_URL = "primary_url"
private const val JSON_KEY_SOURCE_TYPE = "source_type"
private const val JSON_KEY_ED_INTENT_DETECTED = "ed_intent_detected"
private const val JSON_KEY_ED_INTENT = "ed_intent"
private const val JSON_KEY_ED_FORMATTED_QUESTION = "ed_formatted_question"
private const val JSON_KEY_ED_FORMATTED_TITLE = "ed_formatted_title"
private const val JSON_KEY_ED_FETCH_INTENT_DETECTED = "ed_fetch_intent_detected"
private const val JSON_KEY_ED_FETCH_QUERY = "ed_fetch_query"
// Standard localhost identifier - safe loopback address (RFC 5735)
private const val LOCALHOST = "localhost"
