package com.android.sample.conversations

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
}
