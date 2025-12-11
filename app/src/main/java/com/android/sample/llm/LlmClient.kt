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
 * Source type for the response
 * - "schedule": answer came from user's EPFL schedule
 * - "rag": answer came from RAG/web sources
 * - "food": answer came from EPFL restaurant menus
 * - "none": no external source used
 */
enum class SourceType {
  SCHEDULE,
  RAG,
  FOOD,
  NONE;

  companion object {
    fun fromString(s: String?): SourceType =
        when (s?.lowercase()) {
          "schedule" -> SCHEDULE
          "rag" -> RAG
          "food" -> FOOD
          else -> NONE
        }
  }
}

/**
 * ED Discussion intent information.
 *
 * @param detected Whether an ED Discussion posting intent was detected
 * @param intent The type of ED intent detected (e.g., "post_question")
 * @param formattedQuestion The formatted question for ED post (when detected is true)
 * @param formattedTitle The formatted title for ED post (when detected is true)
 */
data class EdIntent(
    val detected: Boolean = false,
    val intent: String? = null,
    val formattedQuestion: String? = null,
    val formattedTitle: String? = null
)

/**
 * ED Discussion fetch intent information.
 *
 * @param detected Whether an ED Discussion fetch intent was detected
 * @param query The query string to use for fetching (when detected is true)
 */
data class EdFetchIntent(
    val detected: Boolean = false,
    val query: String? = null,
)

/**
 * Response from the LLM backend.
 *
 * @param reply The text response from the LLM
 * @param url Optional URL source reference
 * @param sourceType The type of source used for the answer
 * @param edIntent ED Discussion intent information
 * @param edFetchIntent ED Discussion fetch intent information
 */
data class BotReply(
    val reply: String,
    val url: String?,
    val sourceType: SourceType = SourceType.NONE,
    val edIntent: EdIntent = EdIntent(),
    val edFetchIntent: EdFetchIntent = EdFetchIntent()
) {
  // Backward compatibility properties
  @Deprecated("Use edIntent.detected instead", ReplaceWith("edIntent.detected"))
  val edIntentDetected: Boolean
    get() = edIntent.detected

  @Deprecated("Use edIntent.intent instead", ReplaceWith("edIntent.intent"))
  val edIntentType: String?
    get() = edIntent.intent

  @Deprecated("Use edIntent.formattedQuestion instead", ReplaceWith("edIntent.formattedQuestion"))
  val edFormattedQuestion: String?
    get() = edIntent.formattedQuestion

  @Deprecated("Use edIntent.formattedTitle instead", ReplaceWith("edIntent.formattedTitle"))
  val edFormattedTitle: String?
    get() = edIntent.formattedTitle
}

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
   * Advanced API: prompt + optional rolling summary + recent transcript + profile context.
   *
   * Default implementation simply ignores the extra context and delegates to [generateReply].
   * Implementations that support RAG can override this.
   */
  suspend fun generateReply(
      prompt: String,
      summary: String? = null,
      transcript: String? = null,
      profileContext: String? = null
  ): BotReply = generateReply(prompt)
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
      generateReply(prompt = prompt, summary = null, transcript = null, profileContext = null)

  /**
   * Calls the Cloud Function `answerWithRagFn` and extracts the `reply` field.
   *
   * The payload includes:
   * - "question": the user prompt
   * - "summary": optional rolling summary of the conversation
   * - "recentTranscript": optional recent transcript
   * - "profileContext": optional user profile information
   *
   * When the function fails (timeout, malformed response, empty reply) the optional fallback is
   * invoked before ultimately throwing an [IllegalStateException].
   */
  override suspend fun generateReply(
      prompt: String,
      summary: String?,
      transcript: String?,
      profileContext: String?
  ): BotReply =
      withContext(Dispatchers.IO) {
        val data = buildRequestPayload(prompt, summary, transcript, profileContext)
        val result = callFirebaseFunction(data)

        if (result == null) {
          return@withContext fallback?.generateReply(prompt, summary, transcript, profileContext)
              ?: throw IllegalStateException("LLM service unavailable: timeout or cancelled")
        }

        val map =
            parseResponseMap(result)
                ?: return@withContext fallback?.generateReply(
                    prompt, summary, transcript, profileContext)
                    ?: throw IllegalStateException("Invalid LLM response payload: null data")

        val parsedReply = parseReplyText(map)
        if (parsedReply == null) {
          fallback?.let {
            return@withContext it.generateReply(prompt, summary, transcript, profileContext)
          }
          return@withContext buildBotReply(map, "Voici le document demandé.")
        }

        return@withContext buildBotReply(map, parsedReply)
      }

  private fun buildRequestPayload(
      prompt: String,
      summary: String?,
      transcript: String?,
      profileContext: String?
  ): HashMap<String, Any> {
    val payload =
        hashMapOf<String, Any>(KEY_QUESTION to prompt).apply {
          summary?.let { put(KEY_SUMMARY, it) }
          transcript?.let { put(KEY_TRANSCRIPT, it) }
          profileContext?.let { put(KEY_PROFILE_CONTEXT, it) }
        }
    return payload
  }

  private suspend fun callFirebaseFunction(
      data: HashMap<String, Any>
  ): com.google.firebase.functions.HttpsCallableResult? =
      try {
        withTimeoutOrNull(timeoutMillis) {
          functions.getHttpsCallable(FUNCTION_NAME).call(data).await()
        }
      } catch (e: FirebaseFunctionsException) {
        throw e
      } catch (e: Exception) {
        throw e
      }

  @Suppress("UNCHECKED_CAST")
  private fun parseResponseMap(
      result: com.google.firebase.functions.HttpsCallableResult
  ): Map<String, Any?>? =
      try {
        result.getData() as? Map<String, Any?>
      } catch (e: ClassCastException) {
        null
      }

  private fun parseReplyText(map: Map<String, Any?>): String? =
      try {
        (map[KEY_REPLY] as? String)?.takeIf { it.isNotBlank() }
      } catch (e: ClassCastException) {
        null
      }

  private fun parseEdIntentDetected(map: Map<String, Any?>): Boolean =
      map[KEY_ED_INTENT_DETECTED] as? Boolean ?: false

  private fun parseEdIntentType(map: Map<String, Any?>): String? = map[KEY_ED_INTENT] as? String

  private fun parseSourceType(map: Map<String, Any?>): SourceType =
      SourceType.fromString(map[KEY_SOURCE_TYPE] as? String)

  private fun parseEdFormattedQuestion(map: Map<String, Any?>): String? =
      map[KEY_ED_FORMATTED_QUESTION] as? String

  private fun parseEdFormattedTitle(map: Map<String, Any?>): String? =
      map[KEY_ED_FORMATTED_TITLE] as? String

  private fun parseEdFetchIntentDetected(map: Map<String, Any?>): Boolean =
      map[KEY_ED_FETCH_INTENT_DETECTED] as? Boolean ?: false

  private fun parseEdFetchQuery(map: Map<String, Any?>): String? =
      map[KEY_ED_FETCH_QUERY] as? String

  private fun buildBotReply(map: Map<String, Any?>, replyText: String): BotReply {
    val url = extractUrlFromLlmPayload(map)
    val sourceType = parseSourceType(map)
    val edIntentDetected = parseEdIntentDetected(map)
    val edIntentType = parseEdIntentType(map)

    val edFormattedQuestion = parseEdFormattedQuestion(map)
    val edFormattedTitle = parseEdFormattedTitle(map)
    val edIntent =
        EdIntent(
            detected = edIntentDetected,
            intent = edIntentType,
            formattedQuestion = edFormattedQuestion,
            formattedTitle = edFormattedTitle)

    val edFetchDetected = parseEdFetchIntentDetected(map)
    val edFetchQuery = parseEdFetchQuery(map)

    if (edFetchDetected) {
      Log.d(TAG, "Parsed ED fetch intent: detected=true, query=$edFetchQuery")
    }

    val edFetchIntent =
        EdFetchIntent(
            detected = edFetchDetected,
            query = edFetchQuery,
        )

    return BotReply(replyText, url, sourceType, edIntent, edFetchIntent)
  }

  companion object {
    private const val TAG = "FirebaseFunctionsLlmClient"
    private const val FUNCTION_NAME = "answerWithRagFn"
    private const val DEFAULT_TIMEOUT_MS = 33_000L

    // Request payload keys
    private const val KEY_QUESTION = "question"
    private const val KEY_SUMMARY = "summary"
    private const val KEY_TRANSCRIPT = "recentTranscript"
    private const val KEY_PROFILE_CONTEXT = "profileContext"

    // Response payload keys
    private const val KEY_REPLY = "reply"
    private const val KEY_PRIMARY_URL = "primary_url"
    private const val KEY_SOURCE_TYPE = "source_type"
    private const val KEY_ED_INTENT_DETECTED = "ed_intent_detected"
    private const val KEY_ED_INTENT = "ed_intent"
    private const val KEY_ED_FORMATTED_QUESTION = "ed_formatted_question"
    private const val KEY_ED_FORMATTED_TITLE = "ed_formatted_title"
    private const val KEY_ED_FETCH_INTENT_DETECTED = "ed_fetch_intent_detected"
    private const val KEY_ED_FETCH_QUERY = "ed_fetch_query"

    internal fun extractUrlFromLlmPayload(map: Map<String, Any?>): String? {
      val direct =
          (map[KEY_PRIMARY_URL] as? String)
              ?: (map["primaryUrl"] as? String)
              ?: (map["url"] as? String)
      if (!direct.isNullOrBlank()) return direct

      val moodleFile = map["moodle_file"]
      if (moodleFile is Map<*, *>) {
        val mfUrl =
            (moodleFile["url"] as? String)
                ?: (moodleFile["file_url"] as? String)
                ?: (moodleFile["fileUrl"] as? String)
        if (!mfUrl.isNullOrBlank()) return mfUrl
      }

      val sources = map["sources"]
      if (sources is List<*>) {
        sources.forEach { entry ->
          if (entry is Map<*, *>) {
            val candidate = entry["url"] as? String
            if (!candidate.isNullOrBlank()) return candidate
          }
        }
      }
      return null
    }

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
