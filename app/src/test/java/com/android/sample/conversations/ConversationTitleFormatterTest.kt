package com.android.sample.conversations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTitleFormatterTest {

  @Test
  fun localTitleFrom_head_length_less_than_maxLen_returns_head() {
    // Covers condition: if (head.length <= maxLen) head
    val question = "What is EPFL?"
    val result = ConversationTitleFormatter.localTitleFrom(question, maxLen = 60)
    assertEquals("What is EPFL?", result)
  }

  @Test
  fun localTitleFrom_head_length_greater_than_maxLen_truncates() {
    // Covers condition: else head.take(maxLen)
    val question = "This is a very long question that exceeds the maximum length of 60 characters"
    val result = ConversationTitleFormatter.localTitleFrom(question, maxLen = 30)
    assertEquals(30, result.length)
    assertTrue(result.startsWith("This is a very long question"))
  }

  @Test
  fun localTitleFrom_head_exactly_maxLen_returns_head() {
    // Covers boundary: head.length == maxLen
    val question = "A".repeat(60)
    val result = ConversationTitleFormatter.localTitleFrom(question, maxLen = 60)
    assertEquals(60, result.length)
    assertEquals(question, result)
  }

  @Test
  fun localTitleFrom_filters_blank_words() {
    // Covers filter { it.isNotBlank() } condition
    val question = "What  is   EPFL?"
    val result = ConversationTitleFormatter.localTitleFrom(question)
    assertEquals("What is EPFL?", result)
  }
}
