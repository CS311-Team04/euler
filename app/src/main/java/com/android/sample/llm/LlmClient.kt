package com.android.sample.llm

import com.android.sample.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Abstraction over the Large Language Model backend.
 *
 * Implementations should:
 * - run network work off the main thread;
 * - throw descriptive exceptions for recoverable errors so the UI can display them;
 * - return natural-language strings ready for display or speech.
 */
interface LlmClient {
  suspend fun generateReply(prompt: String): String
}

/**
 * Default [LlmClient] backed by Firebase Cloud Functions.
 *
 * Uses the `answerWithRagFn` callable function (same endpoint as the chat screen) and applies a
 * short timeout. Errors surface as thrown exceptions so callers can surface a meaningful message.
 */
/**
 * Production [LlmClient] relying on Firebase callable Cloud Functions.
 *
 * It optionally falls back to [HttpLlmClient] (when configured) if the primary call times out or
 * the response payload is invalid. The fallback is handy for local development with a plain HTTP
 * server.
 */
class FirebaseFunctionsLlmClient(
    private val functions: FirebaseFunctions = defaultFunctions(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
    private val fallback: LlmClient? =
        if (BuildConfig.LLM_HTTP_ENDPOINT.isNotBlank()) HttpLlmClient() else null,
) : LlmClient {

  /**
   * Calls the Cloud Function and extracts the `reply` field.
   *
   * When the function fails (timeout, malformed response, empty reply) the optional fallback is
   * invoked before ultimately throwing an [IllegalStateException].
   */
  override suspend fun generateReply(prompt: String): String =
      withContext(Dispatchers.IO) {
        val data = hashMapOf("question" to prompt)
        val result =
            withTimeoutOrNull(timeoutMillis) {
              functions.getHttpsCallable(FUNCTION_NAME).call(data).await()
            }

        if (result == null) {
          return@withContext fallback?.generateReply(prompt)
              ?: throw IllegalStateException("LLM service unavailable")
        }

        @Suppress("UNCHECKED_CAST")
        val map =
            result.getData() as? Map<String, Any?>
                ?: return@withContext fallback?.generateReply(prompt)
                    ?: throw IllegalStateException("Invalid LLM response payload")

        (map["reply"] as? String)?.takeIf { it.isNotBlank() }
            ?: return@withContext fallback?.generateReply(prompt)
                ?: throw IllegalStateException("Empty LLM reply")
      }

  companion object {
    private const val FUNCTION_NAME = "answerWithRagFn"
    private const val DEFAULT_TIMEOUT_MS = 5_000L

    /**
     * Creates a region-scoped [FirebaseFunctions] instance and wires the local emulator when
     * requested via build config flags.
     */
    private fun defaultFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("us-central1").apply {
          if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
            useEmulator(BuildConfig.FUNCTIONS_HOST, BuildConfig.FUNCTIONS_PORT)
          }
        }
  }
}
