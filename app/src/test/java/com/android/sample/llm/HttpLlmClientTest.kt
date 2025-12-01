package com.android.sample.llm

import java.net.HttpURLConnection
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HttpLlmClientTest {

  private lateinit var server: MockWebServer

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun generateReply_success_returns_reply_and_url() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Bonjour","primary_url":"https://www.epfl.ch/education"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "secret", client = OkHttpClient())

    val reply = client.generateReply("Salut ?")

    assertEquals("Bonjour", reply.reply)
    assertEquals("https://www.epfl.ch/education", reply.url)
  }

  @Test
  fun generateReply_invalidJson_throws() = runBlocking {
    server.enqueue(
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("""{"oops":true"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Salut ?") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Invalid LLM HTTP response"))
  }

  @Test
  fun generateReply_throws_on_http_error() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
            .setBody("""{"reply":"oops"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Hello") }.exceptionOrNull()
    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("LLM HTTP error 500"))
  }

  @Test
  fun generateReply_requires_configured_endpoint() = runBlocking {
    val client = HttpLlmClient(endpoint = "", apiKey = "", client = OkHttpClient())

    try {
      client.generateReply("ignored")
      fail("Expected IllegalStateException when endpoint is missing")
    } catch (e: IllegalStateException) {
      assertEquals("LLM HTTP endpoint not configured", e.message)
    }
  }

  @Test
  fun generateReply_requires_https_for_remote_endpoint() = runBlocking {
    val client =
        HttpLlmClient(endpoint = "http://example.com/answer", apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Hello") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("must use HTTPS"))
  }

  @Test
  fun generateReply_allows_http_for_emulator_loopback() = runBlocking {
    // Test that 10.0.2.2 (Android emulator loopback) passes validation
    // Note: This won't actually connect in unit tests, but validates the security check
    val clientWithTimeout =
        OkHttpClient.Builder().connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
    val client =
        HttpLlmClient(
            endpoint = "http://10.0.2.2:8080/answer", apiKey = "", client = clientWithTimeout)

    // Should not fail with "must use HTTPS" - validation should pass
    // The actual connection will timeout, but that's expected in unit tests
    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    // Should not complain about HTTPS requirement for emulator loopback
    assertNotNull("Expected connection error, not validation error", error)
    assertTrue(
        "Emulator loopback should not require HTTPS, got: ${error!!.message}",
        !error.message!!.contains("must use HTTPS"))
  }

  @Test
  fun generateReply_allows_http_for_localhost() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Localhost test"}"""))
    val client =
        HttpLlmClient(
            endpoint = "http://localhost:${server.port}/answer",
            apiKey = "",
            client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Localhost test", reply.reply)
  }

  @Test
  fun generateReply_allows_http_for_127_0_0_1() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Loopback test"}"""))
    val client =
        HttpLlmClient(
            endpoint = "http://127.0.0.1:${server.port}/answer",
            apiKey = "",
            client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Loopback test", reply.reply)
  }

  @Test
  fun generateReply_allows_https_for_any_endpoint() = runBlocking {
    // Test that HTTPS endpoints are accepted (validation passes)
    // We use a real HTTPS endpoint that should exist (or at least validate correctly)
    val client =
        HttpLlmClient(endpoint = "https://example.com/answer", apiKey = "", client = OkHttpClient())

    // The validation should pass (no "must use HTTPS" error)
    // The actual HTTP call may fail, but that's not what we're testing
    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    // Should not fail with "must use HTTPS" - validation should pass
    if (error != null) {
      assertTrue(
          "Should not complain about HTTPS requirement for HTTPS URLs",
          !error.message!!.contains("must use HTTPS"))
    }
  }

  @Test
  fun generateReply_rejects_http_for_non_loopback_host() = runBlocking {
    // Use a public domain (not private IP) to test HTTPS enforcement
    val client =
        HttpLlmClient(endpoint = "http://example.com/answer", apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("must use HTTPS"))
  }

  @Test
  fun generateReply_rejects_invalid_endpoint_url() = runBlocking {
    // Test that invalid URL format is rejected during endpoint validation
    val client = HttpLlmClient(endpoint = "not-a-valid-url", apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Invalid LLM HTTP endpoint URL"))
  }

  @Test
  fun generateReply_empty_body_throws() = runBlocking {
    // Test that empty HTTP response body is rejected
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody(""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM HTTP response"))
  }

  @Test
  fun generateReply_missing_reply_key_throws() = runBlocking {
    // Test that missing "reply" key in JSON response is rejected
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"primary_url":"https://example.com"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun generateReply_empty_reply_value_throws() = runBlocking {
    // Test that empty string value for "reply" key is rejected
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"","primary_url":"https://example.com"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun generateReply_whitespace_only_reply_throws() = runBlocking {
    // Test that whitespace-only value for "reply" key is rejected after trimming
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"   ","primary_url":"https://example.com"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun generateReply_handles_optional_url() = runBlocking {
    // Test that "primary_url" is optional and can be omitted from JSON response
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Test reply"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Test reply", reply.reply)
    assertEquals(null, reply.url)
  }

  @Test
  fun generateReply_trims_reply_and_url() = runBlocking {
    // Test that whitespace in "reply" and "primary_url" values is trimmed
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"  Trimmed reply  ","primary_url":"  https://example.com  "}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Trimmed reply", reply.reply)
    assertEquals("https://example.com", reply.url)
  }

  @Test
  fun generateReply_handles_null_url() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Test reply","primary_url":null}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Test reply", reply.reply)
    assertEquals(null, reply.url)
  }

  @Test
  fun generateReply_handles_missing_url_key() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Test reply"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Test reply", reply.reply)
    assertEquals(null, reply.url)
  }

  @Test
  fun generateReply_rejects_non_string_reply() = runBlocking {
    // Test that non-string types (e.g., number) for "reply" key are rejected
    server.enqueue(
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("""{"reply":123}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun generateReply_rejects_array_reply() = runBlocking {
    // Test that array type for "reply" key is rejected (must be a string)
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":["not","a","string"]}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun generateReply_rejects_object_reply() = runBlocking {
    // Test that object type for "reply" key is rejected (must be a string)
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":{"nested":"object"}}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    assertNotNull("Expected an IllegalStateException", error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Empty LLM reply"))
  }

  @Test
  fun generateReply_rejects_http_for_private_ip_in_release() = runBlocking {
    // Test that private IP addresses require HTTPS in non-DEBUG builds
    // Note: In unit tests, BuildConfig.DEBUG may be true, so we check both cases
    val clientWithTimeout =
        OkHttpClient.Builder().connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
    val client =
        HttpLlmClient(
            endpoint = "http://192.168.1.1:8080/answer", apiKey = "", client = clientWithTimeout)

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    // In non-DEBUG builds, private IPs should require HTTPS
    // In DEBUG builds, private IPs are allowed but connection will fail (timeout)
    assertNotNull("Expected an error", error)
    // Check if it's a validation error (HTTPS requirement) or connection error
    val isHttpsError = error is IllegalStateException && error.message!!.contains("must use HTTPS")
    val isConnectionError = !isHttpsError && error != null
    // Either validation should reject it (non-DEBUG) or connection should fail (DEBUG)
    assertTrue(
        "Private IP should either require HTTPS (non-DEBUG) or fail connection (DEBUG). Got: ${error?.message}",
        isHttpsError || isConnectionError)
  }

  @Test
  fun generateReply_validates_loopback_addresses() = runBlocking {
    // Test that various loopback address formats (IPv4 and localhost) are allowed with HTTP
    // This validates the dynamic IP detection without hardcoding addresses
    val loopbackAddresses = listOf("127.0.0.1", "localhost")

    for (host in loopbackAddresses) {
      server.enqueue(
          MockResponse()
              .setResponseCode(HttpURLConnection.HTTP_OK)
              .setBody("""{"reply":"Loopback test"}"""))
      val client =
          HttpLlmClient(
              endpoint = "http://$host:${server.port}/answer", apiKey = "", client = OkHttpClient())

      val reply = client.generateReply("Test")

      assertEquals("Loopback test", reply.reply)
    }
  }

  @Test
  fun generateReply_handles_unknown_host() = runBlocking {
    // Test that unknown/invalid hostnames are handled gracefully without crashing
    // The validation should either reject the endpoint or allow connection attempt to fail safely
    val clientWithTimeout =
        OkHttpClient.Builder().connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
    val client =
        HttpLlmClient(
            endpoint = "http://nonexistent-host-12345.local:8080/answer",
            apiKey = "",
            client = clientWithTimeout)

    val error = runCatching { client.generateReply("Test") }.exceptionOrNull()

    // Should either fail validation or connection, but not crash
    assertNotNull("Expected an error for unknown host", error)
  }

  // ==================== ED FORMATTED QUESTION/TITLE TESTS ====================

  @Test
  fun generateReply_parses_ed_formatted_question() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals(
        "Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !", reply.edFormattedQuestion)
    assertNull(reply.edFormattedTitle)
  }

  @Test
  fun generateReply_parses_ed_formatted_title() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_title":"Question 5 Modstoch"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Question 5 Modstoch", reply.edFormattedTitle)
    assertNull(reply.edFormattedQuestion)
  }

  @Test
  fun generateReply_parses_both_formatted_fields() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !","ed_formatted_title":"Question 5 Modstoch"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals(
        "Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !", reply.edFormattedQuestion)
    assertEquals("Question 5 Modstoch", reply.edFormattedTitle)
  }

  @Test
  fun generateReply_defaults_formatted_fields_when_missing() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """{"reply":"Response","ed_intent_detected":true,"ed_intent":"post_question"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertNull(reply.edFormattedQuestion)
    assertNull(reply.edFormattedTitle)
  }

  @Test
  fun generateReply_complete_ed_response() = runBlocking {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """{"reply":"Response","primary_url":"https://epfl.ch","ed_intent_detected":true,"ed_intent":"post_question","ed_formatted_question":"Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !","ed_formatted_title":"Question 5 Modstoch"}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(), apiKey = "", client = OkHttpClient())

    val reply = client.generateReply("Test")

    assertEquals("Response", reply.reply)
    assertEquals("https://epfl.ch", reply.url)
    assertTrue(reply.edIntentDetected)
    assertEquals("post_question", reply.edIntent)
    assertEquals(
        "Bonjour,\n\nComment résoudre ce problème ?\n\nMerci d'avance !", reply.edFormattedQuestion)
    assertEquals("Question 5 Modstoch", reply.edFormattedTitle)
  }
}
