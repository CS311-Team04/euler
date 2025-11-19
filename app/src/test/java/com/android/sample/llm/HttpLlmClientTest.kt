package com.android.sample.llm

import java.net.HttpURLConnection
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
