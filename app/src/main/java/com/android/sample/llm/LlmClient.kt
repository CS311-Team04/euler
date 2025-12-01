package com.android.sample.llm

import android.util.Log
import com.android.sample.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Response from the LLM backend.
 *
 * @param reply The text response from the LLM
 * @param url Optional URL source reference
 * @param edIntentDetected Whether an ED Discussion posting intent was detected
 * @param edIntent The type of ED intent detected (e.g., "post_question")
 * @param edFormattedQuestion The formatted question for ED post (when edIntentDetected is true)
 * @param edFormattedTitle The formatted title for ED post (when edIntentDetected is true)
 */
data class BotReply(
    val reply: String,
    val url: String?,
    val edIntentDetected: Boolean = false,
    val edIntent: String? = null,
    val edFormattedQuestion: String? = null,
    val edFormattedTitle: String? = null
)

/**
 * Abstraction over the Large Language Model backend.
 *
 * Implementations should:
 * - run network work off the main thread;
 * - throw descriptive exceptions for recoverable errors so the UI can display them;
 * - return natural-language strings ready for display or speech.
 */
interface LlmClient {

  /** Basic API: only the user prompt. */
  suspend fun generateReply(prompt: String): BotReply

  /**
   * Advanced API: prompt + optional rolling summary + recent transcript.
   *
   * Default implementation simply ignores the extra context and delegates to [generateReply].
   * Implementations that support RAG can override this.
   */
  suspend fun generateReply(prompt: String, summary: String?, transcript: String?): BotReply =
      generateReply(prompt)
}

/**
 * Production [LlmClient] relying on Firebase callable Cloud Functions.
 *
 * Uses the `answerWithRagFn` callable function and applies a short timeout. It optionally falls
 * back to [HttpLlmClient] (when configured) if the primary call times out or the response payload
 * is invalid. The fallback is handy for local development with a plain HTTP server.
 */
class FirebaseFunctionsLlmClient(
    private val functions: FirebaseFunctions = defaultFunctions(),
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MS,
    private val fallback: LlmClient? = null,
) : LlmClient {

  /** Backwards-compatible entry point: no summary / transcript context. */
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
        val data = buildRequestPayload(prompt, summary, transcript)
        val result = callFirebaseFunction(data)

        if (result == null) {
          Log.w(TAG, "Function call returned null (timeout or cancelled)")
          return@withContext fallback?.generateReply(prompt)
              ?: throw IllegalStateException("LLM service unavailable: timeout or cancelled")
        }

        Log.d(
            TAG,
            "Received response from $FUNCTION_NAME, data type: ${result.getData()?.javaClass?.simpleName}")

        val map =
            parseResponseMap(result)
                ?: return@withContext fallback?.generateReply(prompt)
                    ?: throw IllegalStateException("Invalid LLM response payload: null data")

        val replyText =
            parseReplyText(map)
                ?: return@withContext fallback?.generateReply(prompt)
                    ?: throw IllegalStateException("Empty LLM reply")

        buildBotReply(map, replyText)
      }

  private fun buildRequestPayload(
      prompt: String,
      summary: String?,
      transcript: String?
  ): HashMap<String, Any> =
      hashMapOf<String, Any>(KEY_QUESTION to prompt).apply {
        summary?.let { put(KEY_SUMMARY, it) }
        transcript?.let { put(KEY_TRANSCRIPT, it) }
      }

  private suspend fun callFirebaseFunction(
      data: HashMap<String, Any>
  ): com.google.firebase.functions.HttpsCallableResult? =
      try {
        Log.d(
            TAG,
            "Calling $FUNCTION_NAME with data: question=${data[KEY_QUESTION]?.toString()?.take(50)}..., hasSummary=${data.containsKey(KEY_SUMMARY)}, hasTranscript=${data.containsKey(KEY_TRANSCRIPT)}")
        withTimeoutOrNull(timeoutMillis) {
          functions.getHttpsCallable(FUNCTION_NAME).call(data).await()
        }
      } catch (e: FirebaseFunctionsException) {
        Log.e(
            TAG,
            "Firebase Functions error: code=${e.code}, message=${e.message}, details=${e.details}",
            e)
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error calling Firebase Function: ${e.javaClass.simpleName}", e)
        throw e
      }

  @Suppress("UNCHECKED_CAST")
  private fun parseResponseMap(
      result: com.google.firebase.functions.HttpsCallableResult
  ): Map<String, Any?>? =
      try {
        result.getData() as? Map<String, Any?>
      } catch (e: ClassCastException) {
        Log.e(TAG, "Failed to cast response data to Map", e)
        null
      }

  private fun parseReplyText(map: Map<String, Any?>): String? =
      try {
        (map[KEY_REPLY] as? String)?.takeIf { it.isNotBlank() }
      } catch (e: ClassCastException) {
        Log.e(TAG, "Failed to cast reply to String", e)
        null
      }

  private fun parsePrimaryUrl(map: Map<String, Any?>): String? =
      try {
        map[KEY_PRIMARY_URL] as? String
      } catch (e: ClassCastException) {
        Log.w(TAG, "Failed to cast primary_url to String", e)
        null
      }

  private fun parseEdIntentDetected(map: Map<String, Any?>): Boolean =
      try {
        map[KEY_ED_INTENT_DETECTED] as? Boolean ?: false
      } catch (e: ClassCastException) {
        Log.w(TAG, "Failed to cast ed_intent_detected to Boolean", e)
        false
      }

  private fun parseEdIntentType(map: Map<String, Any?>): String? =
      try {
        map[KEY_ED_INTENT] as? String
      } catch (e: ClassCastException) {
        Log.w(TAG, "Failed to cast ed_intent to String", e)
        null
      }

  private fun parseEdFormattedQuestion(map: Map<String, Any?>): String? =
      try {
        map[KEY_ED_FORMATTED_QUESTION] as? String
      } catch (e: ClassCastException) {
        Log.w(TAG, "Failed to cast ed_formatted_question to String", e)
        null
      }

  private fun parseEdFormattedTitle(map: Map<String, Any?>): String? =
      try {
        map[KEY_ED_FORMATTED_TITLE] as? String
      } catch (e: ClassCastException) {
        Log.w(TAG, "Failed to cast ed_formatted_title to String", e)
        null
      }

  private fun buildBotReply(map: Map<String, Any?>, replyText: String): BotReply {
    val url = parsePrimaryUrl(map)
    val edIntentDetected = parseEdIntentDetected(map)
    val edIntent = parseEdIntentType(map)

    if (edIntentDetected) {
      Log.d(TAG, "ED intent detected: $edIntent")
    }

    val edFormattedQuestion = parseEdFormattedQuestion(map)
    val edFormattedTitle = parseEdFormattedTitle(map)
    return BotReply(
        replyText, url, edIntentDetected, edIntent, edFormattedQuestion, edFormattedTitle)
  }

  companion object {
    private const val TAG = "FirebaseFunctionsLlmClient"
    private const val FUNCTION_NAME = "answerWithRagFn"
    private const val DEFAULT_TIMEOUT_MS = 33_000L

    // Request payload keys
    private const val KEY_QUESTION = "question"
    private const val KEY_SUMMARY = "summary"
    private const val KEY_TRANSCRIPT = "recentTranscript"

    // Response payload keys
    private const val KEY_REPLY = "reply"
    private const val KEY_PRIMARY_URL = "primary_url"
    private const val KEY_ED_INTENT_DETECTED = "ed_intent_detected"
    private const val KEY_ED_INTENT = "ed_intent"
    private const val KEY_ED_FORMATTED_QUESTION = "ed_formatted_question"
    private const val KEY_ED_FORMATTED_TITLE = "ed_formatted_title"

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
