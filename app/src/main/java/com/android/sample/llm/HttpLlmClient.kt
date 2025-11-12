package com.android.sample.llm

import com.android.sample.BuildConfig
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
  override suspend fun generateReply(prompt: String): String =
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

        val request =
            Request.Builder()
                .url(endpoint)
                .post(payload)
                .apply {
                  if (apiKey.isNotBlank()) {
                    addHeader("x-api-key", apiKey)
                  }
                  addHeader("Content-Type", "application/json")
                }
                .build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IllegalStateException("LLM HTTP call failed with ${response.code}")
          }
          val body =
              response.body?.string()?.trim()?.takeIf { it.isNotEmpty() }
                  ?: throw IllegalStateException("LLM HTTP response empty body")

          // Parse JSON reply field using regex - simple approach for "reply":"value"
          val replyPattern = """"reply"\s*:\s*"([^"]*)"""".toRegex()
          val match = replyPattern.find(body)
          val reply =
              match?.groupValues?.getOrNull(1)
                  ?: throw IllegalStateException("LLM HTTP reply empty")

          if (reply.isBlank()) {
            throw IllegalStateException("LLM HTTP reply empty")
          }
          reply
        }
      }
}
