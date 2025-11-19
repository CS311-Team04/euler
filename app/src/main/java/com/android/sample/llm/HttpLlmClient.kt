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
      withContext(Dispatchers.IO) {
        if (endpoint.isBlank()) {
          throw IllegalStateException("LLM HTTP endpoint not configured")
        }
        validateEndpoint(endpoint)

        val payloadString = gson.toJson(mapOf(JSON_KEY_QUESTION to prompt))
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
    throw IllegalStateException("LLM HTTP endpoint must use HTTPS (was $endpoint)")
  }

  private fun isAllowedLocalHost(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    val normalized = host.lowercase(Locale.US)
    if (normalized == EMULATOR_LOOPBACK_HOST || normalized == LOCALHOST) return true
    if (normalized == LOOPBACK_IPV4) return true
    return try {
      val address = InetAddress.getByName(host)
      address.isLoopbackAddress
    } catch (_: UnknownHostException) {
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

  val replyText = json.getTrimmedString(JSON_KEY_REPLY) ?: throw IllegalStateException("Empty LLM reply")
  val url = json.getTrimmedString(JSON_KEY_PRIMARY_URL)
  return BotReply(replyText, url)
}

private fun JsonObject.getTrimmedString(key: String): String? {
  val element: JsonElement = get(key) ?: return null
  if (element.isJsonNull || !element.isJsonPrimitive) return null
  val primitive = element.asJsonPrimitive
  if (!primitive.isString) return null
  val value = primitive.asString.trim()
  return value.takeIf { it.isNotEmpty() }
}

private const val HEADER_CONTENT_TYPE = "Content-Type"
private const val HEADER_AUTHORIZATION = "Authorization"
private const val AUTH_SCHEME_BEARER = "Bearer"
private const val CONTENT_TYPE_JSON = "application/json; charset=utf-8"
private const val JSON_KEY_QUESTION = "question"
private const val JSON_KEY_REPLY = "reply"
private const val JSON_KEY_PRIMARY_URL = "primary_url"
private const val EMULATOR_LOOPBACK_HOST = "10.0.2.2"
private const val LOCALHOST = "localhost"
private const val LOOPBACK_IPV4 = "127.0.0.1"
