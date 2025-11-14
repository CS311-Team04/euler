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
}
