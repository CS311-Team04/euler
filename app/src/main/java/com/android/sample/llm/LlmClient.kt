package com.android.sample.llm

import android.util.Log
import com.android.sample.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class BotReply(val reply: String, val url: String?)

/**
 * Abstraction over the Large Language Model backend.
 *
 * Implementations should:
 * - run network work off the main thread;
 * - throw descriptive exceptions for recoverable errors so the UI can display them;
 * - return natural-language strings ready for display or speech.
 */
interface LlmClient {

    /**
     * Basic API: only the user prompt.
     */
    suspend fun generateReply(prompt: String): BotReply

    /**
     * Advanced API: prompt + optional rolling summary + recent transcript.
     *
     * Default implementation simply ignores the extra context and delegates to [generateReply].
     * Implementations that support RAG can override this.
     */
    suspend fun generateReply(
        prompt: String,
        summary: String?,
        transcript: String?
    ): BotReply = generateReply(prompt)
}

/**
 * Production [LlmClient] relying on Firebase callable Cloud Functions.
 *
 * Uses the `answerWithRagFn` callable function and applies a short timeout.
 * It optionally falls back to [HttpLlmClient] (when configured) if the primary call times out or
 * the response payload is invalid. The fallback is handy for local development with a plain HTTP
 * server.
 */
class FirebaseFunctionsLlmClient(
    private val functions: FirebaseFunctions = defaultFunctions(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
    private val fallback: LlmClient? = null,
) : LlmClient {

    /**
     * Backwards-compatible entry point: no summary / transcript context.
     */
    override suspend fun generateReply(prompt: String): BotReply =
        generateReply(prompt = prompt, summary = null, transcript = null)

    /**
     * Calls the Cloud Function `answerWithRagFn` and extracts the `reply` field.
     *
     * The payload includes:
     * - "question": the user prompt
     * - "summary": optional rolling summary of the conversation
     * - "recentTranscript": optional recent transcript
     *
     * When the function fails (timeout, malformed response, empty reply) the optional fallback is
     * invoked before ultimately throwing an [IllegalStateException].
     */
    override suspend fun generateReply(
        prompt: String,
        summary: String?,
        transcript: String?
    ): BotReply =
        withContext(Dispatchers.IO) {
            val data =
                hashMapOf<String, Any>("question" to prompt).apply {
                    summary?.let { put("summary", it) }
                    transcript?.let { put("recentTranscript", it) }
                }

            val result = try {
                Log.d("FirebaseFunctionsLlmClient", "Calling $FUNCTION_NAME with data: question=${data["question"]?.toString()?.take(50)}..., hasSummary=${data.containsKey("summary")}, hasTranscript=${data.containsKey("recentTranscript")}")
                withTimeoutOrNull(timeoutMillis) {
                    functions.getHttpsCallable(FUNCTION_NAME).call(data).await()
                }
            } catch (e: FirebaseFunctionsException) {
                // Firebase Functions specific exception
                Log.e("FirebaseFunctionsLlmClient", "Firebase Functions error: code=${e.code}, message=${e.message}, details=${e.details}", e)
                throw e // Re-throw to be caught by caller
            } catch (e: Exception) {
                // Other exceptions (network, timeout, etc.)
                Log.e("FirebaseFunctionsLlmClient", "Error calling Firebase Function: ${e.javaClass.simpleName}", e)
                throw e // Re-throw to be caught by caller
            }

            if (result == null) {
                Log.w("FirebaseFunctionsLlmClient", "Function call returned null (timeout or cancelled)")
                return@withContext fallback?.generateReply(prompt)
                    ?: throw IllegalStateException("LLM service unavailable: timeout or cancelled")
            }

            Log.d("FirebaseFunctionsLlmClient", "Received response from $FUNCTION_NAME, data type: ${result.getData()?.javaClass?.simpleName}")

            @Suppress("UNCHECKED_CAST")
            val map = try {
                result.getData() as? Map<String, Any?>
            } catch (e: ClassCastException) {
                Log.e("FirebaseFunctionsLlmClient", "Failed to cast response data to Map", e)
                return@withContext fallback?.generateReply(prompt)
                    ?: throw IllegalStateException("Invalid LLM response payload: ${e.message}")
            } ?: return@withContext fallback?.generateReply(prompt)
                ?: throw IllegalStateException("Invalid LLM response payload: null data")

            val replyText = try {
                (map["reply"] as? String)?.takeIf { it.isNotBlank() }
            } catch (e: ClassCastException) {
                Log.e("FirebaseFunctionsLlmClient", "Failed to cast reply to String", e)
                null
            } ?: return@withContext fallback?.generateReply(prompt)
                ?: throw IllegalStateException("Empty LLM reply")

            val url = try {
                map["primary_url"] as? String
            } catch (e: ClassCastException) {
                Log.w("FirebaseFunctionsLlmClient", "Failed to cast primary_url to String", e)
                null
            }
            BotReply(replyText, url)
        }

    companion object {
        private const val FUNCTION_NAME = "answerWithRagFn"
        private const val DEFAULT_TIMEOUT_MS = 33_000L

    /**
     * Creates a region-scoped [FirebaseFunctions] instance and wires the local emulator when
     * requested via build config flags.
     */
    private fun defaultFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance(BuildConfig.FUNCTIONS_REGION).apply {
          if (BuildConfig.USE_FUNCTIONS_EMULATOR) {
            useEmulator(BuildConfig.FUNCTIONS_HOST, BuildConfig.FUNCTIONS_PORT)
          }
        }
  }
}
