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
 * - "none": no external source used
 */
enum class SourceType {
    SCHEDULE,
    RAG,
    NONE;

    companion object {
        fun fromString(s: String?): SourceType =
            when (s?.lowercase()) {
                "schedule" -> SCHEDULE
                "rag" -> RAG
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
 * Moodle file information.
 *
 * @param url The download URL for the file
 * @param filename The name of the file
 * @param mimetype The MIME type of the file (e.g., "application/pdf")
 * @param courseName The name of the course (optional)
 * @param fileType The type of file: "lecture", "homework", or "homework_solution" (optional)
 * @param fileNumber The file number (optional)
 * @param week The week number (optional)
 */
data class MoodleFile(
    val url: String,
    val filename: String,
    val mimetype: String,
    val courseName: String? = null,
    val fileType: String? = null,
    val fileNumber: String? = null,
    val week: Int? = null
)

/**
 * Moodle intent information.
 *
 * @param detected Whether a Moodle file fetch intent was detected
 * @param intent The type of Moodle intent detected (e.g., "fetch_lecture")
 * @param file The Moodle file information (when detected is true and file found)
 */
data class MoodleIntent(
    val detected: Boolean = false,
    val intent: String? = null,
    val file: MoodleFile? = null
)

/**
 * Response from the LLM backend.
 *
 * @param reply The text response from the LLM
 * @param url Optional URL source reference
 * @param sourceType The type of source used for the answer
 * @param edIntent ED Discussion intent information
 * @param moodleIntent Moodle intent information
 */
data class BotReply(
    val reply: String,
    val url: String?,
    val sourceType: SourceType = SourceType.NONE,
    val edIntent: EdIntent = EdIntent(),
    val moodleIntent: MoodleIntent = MoodleIntent()
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
                Log.w(TAG, "Function call returned null (timeout or cancelled)")
                return@withContext fallback?.generateReply(prompt, summary, transcript, profileContext)
                    ?: throw IllegalStateException("LLM service unavailable: timeout or cancelled")
            }

            Log.d(
                TAG,
                "Received response from $FUNCTION_NAME, data type: ${result.getData()?.javaClass?.simpleName}")

            val map =
                parseResponseMap(result)
                    ?: return@withContext fallback?.generateReply(
                        prompt, summary, transcript, profileContext)
                        ?: throw IllegalStateException("Invalid LLM response payload: null data")

            val replyText = parseReplyText(map)

            // Check if there's a moodleFile - if so, empty reply is allowed
            val hasMoodleFile = parseMoodleFile(map) != null

            // Only throw exception if reply is empty AND there's no moodleFile
            if (replyText == null && !hasMoodleFile) {
                return@withContext fallback?.generateReply(prompt, summary, transcript, profileContext)
                    ?: throw IllegalStateException("Empty LLM reply")
            }

            buildBotReply(map, replyText ?: "")
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
                profileContext?.let {
                    Log.d(
                        TAG,
                        "buildRequestPayload: including profileContext, length=${it.length}, preview=${it.take(100)}...")
                    put(KEY_PROFILE_CONTEXT, it)
                } ?: Log.d(TAG, "buildRequestPayload: profileContext is null, not including in request")
            }
        // DEBUG: Log full payload structure (without full content to avoid spam)
        Log.d(
            TAG,
            "buildRequestPayload: Payload structure - hasQuestion=${payload.containsKey(KEY_QUESTION)}, hasSummary=${payload.containsKey(KEY_SUMMARY)}, hasTranscript=${payload.containsKey(KEY_TRANSCRIPT)}, hasProfileContext=${payload.containsKey(KEY_PROFILE_CONTEXT)}")
        Log.d(TAG, "buildRequestPayload: Payload keys: ${payload.keys.joinToString()}")
        return payload
    }

    private suspend fun callFirebaseFunction(
        data: HashMap<String, Any>
    ): com.google.firebase.functions.HttpsCallableResult? =
        try {
            Log.d(
                TAG,
                "Calling $FUNCTION_NAME with data: question=${data[KEY_QUESTION]?.toString()?.take(50)}..., hasSummary=${data.containsKey(KEY_SUMMARY)}, hasTranscript=${data.containsKey(KEY_TRANSCRIPT)}, hasProfileContext=${data.containsKey(KEY_PROFILE_CONTEXT)}")
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

    private fun parsePrimaryUrl(map: Map<String, Any?>): String? = map[KEY_PRIMARY_URL] as? String

    private fun parseEdIntentDetected(map: Map<String, Any?>): Boolean =
        map[KEY_ED_INTENT_DETECTED] as? Boolean ?: false

    private fun parseEdIntentType(map: Map<String, Any?>): String? = map[KEY_ED_INTENT] as? String

    private fun parseSourceType(map: Map<String, Any?>): SourceType =
        SourceType.fromString(map[KEY_SOURCE_TYPE] as? String)

    private fun parseEdFormattedQuestion(map: Map<String, Any?>): String? =
        map[KEY_ED_FORMATTED_QUESTION] as? String

    private fun parseEdFormattedTitle(map: Map<String, Any?>): String? =
        map[KEY_ED_FORMATTED_TITLE] as? String

    private fun parseMoodleIntentDetected(map: Map<String, Any?>): Boolean =
        map[KEY_MOODLE_INTENT_DETECTED] as? Boolean ?: false

    private fun parseMoodleIntentType(map: Map<String, Any?>): String? =
        map[KEY_MOODLE_INTENT] as? String

    @Suppress("UNCHECKED_CAST")
    private fun parseMoodleFile(map: Map<String, Any?>): MoodleFile? {
        val moodleFileMap = map[KEY_MOODLE_FILE] as? Map<String, Any?> ?: return null

        // Get raw URL - handle different types that Firebase might return
        val rawUrl = moodleFileMap["url"]
        val url =
            when (rawUrl) {
                is String -> {
                    // URL might be encoded, try to decode it
                    try {
                        java.net.URLDecoder.decode(rawUrl, "UTF-8")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to decode URL, using raw: ${e.message}")
                        rawUrl
                    }
                }
                else -> {
                    val urlString = rawUrl?.toString() ?: return null
                    try {
                        java.net.URLDecoder.decode(urlString, "UTF-8")
                    } catch (e: Exception) {
                        urlString
                    }
                }
            }

        // Log the full URL to debug
        Log.d(
            TAG,
            "parseMoodleFile: rawUrl type=${rawUrl?.javaClass?.simpleName}, url length=${url.length}")
        Log.d(TAG, "parseMoodleFile: full URL=$url")

        val filename = moodleFileMap["filename"] as? String ?: return null
        val mimetype = moodleFileMap["mimetype"] as? String ?: "application/pdf"

        // Ensure URL is not truncated or modified - preserve the full URL
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            Log.e(TAG, "parseMoodleFile: URL is empty after trimming")
            return null
        }

        // Percent-encode the path/filename if it contains spaces or other characters
        val encodedUrl =
            try {
                val uri = java.net.URI(trimmedUrl)
                // Rebuild with encoded path/query to satisfy URI parser
                val encodedPath = uri.rawPath ?: ""
                val safePath = if (encodedPath.isNotEmpty()) encodedPath else uri.path.replace(" ", "%20")
                val query = uri.rawQuery ?: ""
                java.net
                    .URI(uri.scheme, uri.userInfo, uri.host, uri.port, safePath, query, uri.rawFragment)
                    .toString()
            } catch (e: Exception) {
                // Fallback: simple replace spaces with %20
                trimmedUrl.replace(" ", "%20")
            }

        // Verify URL is valid
        try {
            java.net.URI(encodedUrl) // Validate URL format
        } catch (e: Exception) {
            Log.e(TAG, "parseMoodleFile: Invalid URL format even after encoding: $encodedUrl", e)
            return null
        }

        Log.d(
            TAG,
            "parseMoodleFile: Successfully parsed - filename=$filename, url length=${encodedUrl.length}")

        return MoodleFile(encodedUrl, filename, mimetype)
    }

    private fun buildBotReply(map: Map<String, Any?>, replyText: String): BotReply {
        val url = parsePrimaryUrl(map)
        val sourceType = parseSourceType(map)
        val edIntentDetected = parseEdIntentDetected(map)
        val edIntentType = parseEdIntentType(map)

        if (edIntentDetected) {
            Log.d(TAG, "ED intent detected: $edIntentType")
        }

        val edFormattedQuestion = parseEdFormattedQuestion(map)
        val edFormattedTitle = parseEdFormattedTitle(map)
        val edIntent =
            EdIntent(
                detected = edIntentDetected,
                intent = edIntentType,
                formattedQuestion = edFormattedQuestion,
                formattedTitle = edFormattedTitle)

        val moodleIntentDetected = parseMoodleIntentDetected(map)
        val moodleIntentType = parseMoodleIntentType(map)

        // Debug: Log the raw moodle_file data from the response
        val moodleFileRaw = map[KEY_MOODLE_FILE]
        Log.d(
            TAG, "Moodle file raw data: $moodleFileRaw (type: ${moodleFileRaw?.javaClass?.simpleName})")

        // If it's a Map, log its contents
        if (moodleFileRaw is Map<*, *>) {
            @Suppress("UNCHECKED_CAST") val moodleFileMap = moodleFileRaw as Map<String, Any?>
            Log.d(TAG, "Moodle file map keys: ${moodleFileMap.keys}")
            moodleFileMap["url"]?.let {
                Log.d(
                    TAG,
                    "Moodle file URL from map: $it (type: ${it.javaClass.simpleName}, length: ${it.toString().length})")
            }
        }

        val moodleFile = parseMoodleFile(map)

        if (moodleIntentDetected) {
            Log.d(TAG, "Moodle intent detected: $moodleIntentType, file=${moodleFile != null}")
            if (moodleFile != null) {
                Log.d(
                    TAG,
                    "Moodle file parsed: url=${moodleFile.url}, filename=${moodleFile.filename}, mimetype=${moodleFile.mimetype}")
                Log.d(
                    TAG,
                    "Moodle file URL length: ${moodleFile.url.length}, starts with: ${moodleFile.url.take(50)}")
            } else {
                Log.w(TAG, "Moodle file is null despite intent being detected. Raw data: $moodleFileRaw")
            }
        }

        val moodleIntent =
            MoodleIntent(detected = moodleIntentDetected, intent = moodleIntentType, file = moodleFile)

        return BotReply(replyText, url, sourceType, edIntent, moodleIntent)
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
        private const val KEY_MOODLE_INTENT_DETECTED = "moodle_intent_detected"
        private const val KEY_MOODLE_INTENT = "moodle_intent"
        private const val KEY_MOODLE_FILE = "moodle_file"

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