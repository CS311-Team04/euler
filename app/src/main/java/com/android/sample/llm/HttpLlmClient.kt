package com.android.sample.llm

import com.android.sample.BuildConfig
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
) : LlmClient {

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  /** Performs a blocking HTTP call on [Dispatchers.IO] and returns the non-empty `reply` field. */
  override suspend fun generateReply(prompt: String): BotReply =
      withContext(Dispatchers.IO) {
        if (endpoint.isBlank()) {
          throw IllegalStateException("LLM HTTP endpoint not configured")
        }

        val escapedPrompt = buildString {
          prompt.forEach { char ->
            when (char) {
              '\\' -> append("\\\\")
              '"' -> append("\\\"")
              '\n' -> append("\\n")
              '\r' -> append("\\r")
              '\t' -> append("\\t")
              '\b' -> append("\\b")
              '\u000C' -> append("\\f")
              else -> append(char)
            }
          }
        }
        val payloadString = """{"question":"$escapedPrompt"}"""
        val payload = payloadString.toRequestBody(jsonMediaType)

        val builder =
            Request.Builder()
                .url(endpoint)
                .post(payload)
                .addHeader("Content-Type", "application/json")

        if (apiKey.isNotBlank()) {
          // adapt header if your backend expects a different header (e.g. "x-api-key")
          builder.addHeader("Authorization", "Bearer $apiKey")
        }

        val request = builder.build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IllegalStateException("LLM HTTP error ${response.code}: ${response.message}")
          }
          val body =
              response.body?.string() ?: throw IllegalStateException("Empty LLM HTTP response")
          return@withContext parseBotReply(body)
        }
      }
}

internal fun parseBotReply(body: String): BotReply {
  if (body.isBlank()) {
    throw IllegalStateException("Empty LLM HTTP response")
  }
  val replyText =
      REPLY_REGEX.find(body)?.groupValues?.getOrNull(1)?.let(::unescapeJsonString)?.trim()?.takeIf {
        it.isNotEmpty()
      } ?: throw IllegalStateException("Empty LLM reply")

  val url =
      PRIMARY_URL_REGEX.find(body)
          ?.groupValues
          ?.getOrNull(1)
          ?.let(::unescapeJsonString)
          ?.trim()
          ?.takeIf { it.isNotEmpty() }
  return BotReply(replyText, url)
}

private fun unescapeJsonString(raw: String): String =
    raw.replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")

private val REPLY_REGEX =
    Pattern.compile("\"reply\"\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        .toRegex()

private val PRIMARY_URL_REGEX =
    Pattern.compile(
            "\"primary_url\"\\s*:\\s*\"([^\"]*)\"", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
        .toRegex()
