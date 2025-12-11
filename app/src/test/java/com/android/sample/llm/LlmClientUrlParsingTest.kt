package com.android.sample.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class LlmClientUrlParsingTest {

  @Test
  fun `extractUrl picks primary_url`() {
    val map = mapOf("primary_url" to "https://example.com/a.pdf", "reply" to "")
    assertEquals(
        "https://example.com/a.pdf", FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }

  @Test
  fun `extractUrl picks primaryUrl`() {
    val map = mapOf("primaryUrl" to "https://example.com/b.pdf", "reply" to "")
    assertEquals(
        "https://example.com/b.pdf", FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }

  @Test
  fun `extractUrl picks url`() {
    val map = mapOf("url" to "https://example.com/c.pdf", "reply" to "")
    assertEquals(
        "https://example.com/c.pdf", FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }

  @Test
  fun `extractUrl picks moodle_file url`() {
    val map = mapOf("moodle_file" to mapOf("url" to "https://moodle.com/file.pdf"), "reply" to "")
    assertEquals(
        "https://moodle.com/file.pdf", FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }

  @Test
  fun `extractUrl picks moodle_file file_url`() {
    val map =
        mapOf("moodle_file" to mapOf("file_url" to "https://moodle.com/file2.pdf"), "reply" to "")
    assertEquals(
        "https://moodle.com/file2.pdf", FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }

  @Test
  fun `extractUrl picks first source url`() {
    val map =
        mapOf("sources" to listOf(mapOf("url" to "https://example.com/source.pdf")), "reply" to "")
    assertEquals(
        "https://example.com/source.pdf", FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }

  @Test
  fun `extractUrl returns null when no url present`() {
    val map = mapOf("reply" to "")
    assertEquals(null, FirebaseFunctionsLlmClient.extractUrlFromLlmPayload(map))
  }
}
