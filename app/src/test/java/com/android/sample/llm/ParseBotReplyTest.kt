package com.android.sample.llm

import com.google.gson.Gson
import org.junit.Assert.assertEquals
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
}
