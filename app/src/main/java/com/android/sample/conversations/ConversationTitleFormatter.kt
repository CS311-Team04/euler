package com.android.sample.conversations

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Shared utility for generating provisional conversation titles from user messages.
 *
 * This ensures that text mode and voice mode use the same title generation logic, preventing drift
 * if the title logic is tweaked later.
 */
object ConversationTitleFormatter {

  /**
   * Creates a quick provisional title from the first prompt.
   *
   * @param question The user's message/question
   * @param maxLen Maximum length of the title (default: 60 characters)
   * @param maxWords Maximum number of words to include (default: 8)
   * @return A shortened version of the question suitable for use as a conversation title
   */
  fun localTitleFrom(question: String, maxLen: Int = 60, maxWords: Int = 8): String {
    val cleaned = question.replace(Regex("\\s+"), " ").trim()
    val head = cleaned.split(" ").filter { it.isNotBlank() }.take(maxWords).joinToString(" ")
    return (if (head.length <= maxLen) head else head.take(maxLen)).trim()
  }

  /**
   * Ask `generateTitleFn` for a better title; fallback to [localTitleFrom] on errors.
   *
   * @param functions Firebase Functions instance
   * @param question The user's message/question
   * @param tag Log tag for debugging (optional)
   * @return Generated title or fallback to local title extraction
   */
  suspend fun fetchTitle(
      functions: FirebaseFunctions,
      question: String,
      tag: String = "ConversationTitleFormatter"
  ): String {
    return try {
      val res =
          functions.getHttpsCallable("generateTitleFn").call(mapOf("question" to question)).await()
      val t = (res.getData() as? Map<*, *>)?.get("title") as? String
      (t?.takeIf { it.isNotBlank() } ?: localTitleFrom(question)).also {
        Log.d(tag, "fetchTitle(): generated='$it'")
      }
    } catch (_: Exception) {
      Log.d(tag, "fetchTitle(): fallback to local extraction")
      localTitleFrom(question)
    }
  }
}
