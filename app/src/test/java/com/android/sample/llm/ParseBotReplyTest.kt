package com.android.sample.llm

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the parseBotReply function and related JSON parsing utilities. These tests
 * validate the robustness of JSON response parsing, including edge cases and error handling for
 * various malformed or unexpected JSON structures.
 */
class ParseBotReplyTest {

  private val gson = Gson()

  @Test
  fun parseBotReply_valid_response_returns_bot_reply() {
    // Test successful parsing of valid JSON with both reply and primary_url fields
    val body = """{"reply":"Hello world","primary_url":"https://example.com"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Hello world", result.reply)
    assertEquals("https://example.com", result.url)
  }

  @Test
  fun parseBotReply_empty_body_throws() {
    // Test that empty response body is rejected
    val error = runCatching { parseBotReply("", gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM HTTP response"))
  }

  @Test
  fun parseBotReply_whitespace_body_throws() {
    // Test that whitespace-only response body is rejected (treated as empty after trimming)
    val error = runCatching { parseBotReply("   ", gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM HTTP response"))
  }

  @Test
  fun parseBotReply_invalid_json_throws() {
    // Test that malformed JSON is rejected with appropriate error message
    val error = runCatching { parseBotReply("{invalid json}", gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Invalid LLM HTTP response"))
  }

  @Test
  fun parseBotReply_missing_reply_key_throws() {
    // Test that missing "reply" key in JSON is rejected
    val body = """{"primary_url":"https://example.com"}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_empty_reply_value_throws() {
    // Test that empty string value for "reply" key is rejected
    val body = """{"reply":"","primary_url":"https://example.com"}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_whitespace_reply_throws() {
    // Test that whitespace-only value for "reply" key is rejected after trimming
    val body = """{"reply":"   ","primary_url":"https://example.com"}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_handles_optional_url() {
    // Test that "primary_url" field is optional and can be omitted from JSON
    val body = """{"reply":"Test reply"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull(result.url)
  }

  @Test
  fun parseBotReply_trims_reply_and_url() {
    // Test that leading and trailing whitespace in "reply" and "primary_url" values is trimmed
    val body = """{"reply":"  Trimmed reply  ","primary_url":"  https://example.com  "}"""
    val result = parseBotReply(body, gson)

    assertEquals("Trimmed reply", result.reply)
    assertEquals("https://example.com", result.url)
  }

  @Test
  fun parseBotReply_handles_null_url() {
    // Test that null value for "primary_url" is handled gracefully (returns null)
    val body = """{"reply":"Test reply","primary_url":null}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull(result.url)
  }

  @Test
  fun parseBotReply_rejects_non_string_reply() {
    // Test that non-string types (e.g., number) for "reply" key are rejected
    val body = """{"reply":123}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_rejects_array_reply() {
    // Test that array type for "reply" key is rejected (must be a string)
    val body = """{"reply":["not","a","string"]}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_rejects_object_reply() {
    // Test that object type for "reply" key is rejected (must be a string)
    val body = """{"reply":{"nested":"object"}}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_handles_non_string_url() {
    // Test that non-string types for "primary_url" are ignored (returns null)
    val body = """{"reply":"Test reply","primary_url":123}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull("Non-string URL should be ignored", result.url)
  }

  @Test
  fun parseBotReply_handles_empty_string_url() {
    // Test that empty string value for "primary_url" is ignored (returns null)
    val body = """{"reply":"Test reply","primary_url":""}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull("Empty string URL should be ignored", result.url)
  }

  @Test
  fun parseBotReply_handles_whitespace_url() {
    // Test that whitespace-only value for "primary_url" is ignored after trimming (returns null)
    val body = """{"reply":"Test reply","primary_url":"   "}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull("Whitespace-only URL should be ignored", result.url)
  }

  // ==================== ED INTENT TESTS ====================

  @Test
  fun parseBotReply_parses_ed_intent_detected_true() {
    val body = """{"reply":"ED response","ed_intent_detected":true,"ed_intent":"post_question"}"""
    val result = parseBotReply(body, gson)

    assertEquals("ED response", result.reply)
    assertTrue(result.edIntentDetected)
    assertEquals("post_question", result.edIntent)
  }

  @Test
  fun parseBotReply_parses_ed_intent_detected_false() {
    val body = """{"reply":"Normal response","ed_intent_detected":false,"ed_intent":null}"""
    val result = parseBotReply(body, gson)

    assertEquals("Normal response", result.reply)
    assertFalse(result.edIntentDetected)
    assertNull(result.edIntent)
  }

  @Test
  fun parseBotReply_defaults_ed_intent_when_missing() {
    val body = """{"reply":"Response without ED fields"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Response without ED fields", result.reply)
    assertFalse(result.edIntentDetected)
    assertNull(result.edIntent)
  }

  @Test
  fun parseBotReply_handles_all_ed_intent_types() {
    val types = listOf("post_question", "post_answer", "post_comment")
    for (intentType in types) {
      val body = """{"reply":"Response","ed_intent_detected":true,"ed_intent":"$intentType"}"""
      val result = parseBotReply(body, gson)

      assertTrue("Should detect ED intent for type: $intentType", result.edIntentDetected)
      assertEquals(intentType, result.edIntent)
    }
  }

  @Test
  fun parseBotReply_handles_invalid_ed_intent_detected_type() {
    // Non-boolean ed_intent_detected should default to false
    val body = """{"reply":"Response","ed_intent_detected":"not-a-boolean"}"""
    val result = parseBotReply(body, gson)

    assertFalse(result.edIntentDetected)
  }

  @Test
  fun parseBotReply_handles_invalid_ed_intent_type() {
    // Non-string ed_intent should default to null
    val body = """{"reply":"Response","ed_intent_detected":true,"ed_intent":123}"""
    val result = parseBotReply(body, gson)

    assertTrue(result.edIntentDetected)
    assertNull("Non-string ed_intent should be null", result.edIntent)
  }

  @Test
  fun parseBotReply_full_response_with_all_fields() {
    val body =
        """{"reply":"Full response","primary_url":"https://epfl.ch","ed_intent_detected":true,"ed_intent":"post_question"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Full response", result.reply)
    assertEquals("https://epfl.ch", result.url)
    assertTrue(result.edIntentDetected)
    assertEquals("post_question", result.edIntent)
  }

  // ==================== ED FORMATTED QUESTION/TITLE TESTS ====================

  @Test
  fun parseBotReply_parses_ed_formatted_question() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !"}"""
    val result = parseBotReply(body, gson)

    assertEquals(
        "Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !",
        result.edFormattedQuestion)
    assertNull(result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_parses_ed_formatted_title() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_title":"Question 5 Modstoch"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Question 5 Modstoch", result.edFormattedTitle)
    assertNull(result.edFormattedQuestion)
  }

  @Test
  fun parseBotReply_parses_both_formatted_fields() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !","ed_formatted_title":"Question 5 Modstoch"}"""
    val result = parseBotReply(body, gson)

    assertEquals(
        "Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !",
        result.edFormattedQuestion)
    assertEquals("Question 5 Modstoch", result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_defaults_formatted_fields_when_missing() {
    val body = """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question"}"""
    val result = parseBotReply(body, gson)

    assertNull(result.edFormattedQuestion)
    assertNull(result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_handles_empty_formatted_question() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":""}"""
    val result = parseBotReply(body, gson)

    assertNull("Empty string should be treated as null", result.edFormattedQuestion)
  }

  @Test
  fun parseBotReply_handles_empty_formatted_title() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_title":""}"""
    val result = parseBotReply(body, gson)

    assertNull("Empty string should be treated as null", result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_trims_formatted_question() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"  Trimmed question  "}"""
    val result = parseBotReply(body, gson)

    assertEquals("Trimmed question", result.edFormattedQuestion)
  }

  @Test
  fun parseBotReply_trims_formatted_title() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_title":"  Trimmed title  "}"""
    val result = parseBotReply(body, gson)

    assertEquals("Trimmed title", result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_handles_whitespace_formatted_question() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"   "}"""
    val result = parseBotReply(body, gson)

    assertNull("Whitespace-only should be treated as null", result.edFormattedQuestion)
  }

  @Test
  fun parseBotReply_handles_whitespace_formatted_title() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_title":"   "}"""
    val result = parseBotReply(body, gson)

    assertNull("Whitespace-only should be treated as null", result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_handles_null_formatted_question() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":null}"""
    val result = parseBotReply(body, gson)

    assertNull(result.edFormattedQuestion)
  }

  @Test
  fun parseBotReply_handles_null_formatted_title() {
    val body =
        """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_title":null}"""
    val result = parseBotReply(body, gson)

    assertNull(result.edFormattedTitle)
  }

  @Test
  fun parseBotReply_complete_ed_response() {
    val body =
        """{"reply":"Response","primary_url":"https://epfl.ch","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !","ed_formatted_title":"Question 5 Modstoch"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Response", result.reply)
    assertEquals("https://epfl.ch", result.url)
    assertTrue(result.edIntentDetected)
    assertEquals("post_question", result.edIntent)
    assertEquals(
        "Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !",
        result.edFormattedQuestion)
    assertEquals("Question 5 Modstoch", result.edFormattedTitle)
  }
}
