package com.android.sample.llm

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseBotReplyTest {

  private val gson = Gson()

  @Test
  fun parseBotReply_valid_response_returns_bot_reply() {
    val body = """{"reply":"Hello world","primary_url":"https://example.com"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Hello world", result.reply)
    assertEquals("https://example.com", result.url)
  }

  @Test
  fun parseBotReply_empty_body_throws() {
    val error = runCatching { parseBotReply("", gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM HTTP response"))
  }

  @Test
  fun parseBotReply_whitespace_body_throws() {
    val error = runCatching { parseBotReply("   ", gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM HTTP response"))
  }

  @Test
  fun parseBotReply_invalid_json_throws() {
    val error = runCatching { parseBotReply("{invalid json}", gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Invalid LLM HTTP response"))
  }

  @Test
  fun parseBotReply_missing_reply_key_throws() {
    val body = """{"primary_url":"https://example.com"}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_empty_reply_value_throws() {
    val body = """{"reply":"","primary_url":"https://example.com"}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_whitespace_reply_throws() {
    val body = """{"reply":"   ","primary_url":"https://example.com"}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_handles_optional_url() {
    val body = """{"reply":"Test reply"}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull(result.url)
  }

  @Test
  fun parseBotReply_trims_reply_and_url() {
    val body = """{"reply":"  Trimmed reply  ","primary_url":"  https://example.com  "}"""
    val result = parseBotReply(body, gson)

    assertEquals("Trimmed reply", result.reply)
    assertEquals("https://example.com", result.url)
  }

  @Test
  fun parseBotReply_handles_null_url() {
    val body = """{"reply":"Test reply","primary_url":null}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull(result.url)
  }

  @Test
  fun parseBotReply_rejects_non_string_reply() {
    val body = """{"reply":123}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_rejects_array_reply() {
    val body = """{"reply":["not","a","string"]}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_rejects_object_reply() {
    val body = """{"reply":{"nested":"object"}}"""
    val error = runCatching { parseBotReply(body, gson) }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun parseBotReply_handles_non_string_url() {
    val body = """{"reply":"Test reply","primary_url":123}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull("Non-string URL should be ignored", result.url)
  }

  @Test
  fun parseBotReply_handles_empty_string_url() {
    val body = """{"reply":"Test reply","primary_url":""}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull("Empty string URL should be ignored", result.url)
  }

  @Test
  fun parseBotReply_handles_whitespace_url() {
    val body = """{"reply":"Test reply","primary_url":"   "}"""
    val result = parseBotReply(body, gson)

    assertEquals("Test reply", result.reply)
    assertNull("Whitespace-only URL should be ignored", result.url)
  }
}
