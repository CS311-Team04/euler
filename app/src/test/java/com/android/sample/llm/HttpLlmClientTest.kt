package com.android.sample.llm

import java.net.HttpURLConnection
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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
  fun generateReply_returnsServerReply() = runTest {
    server.enqueue(
        MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"reply":"Bonjour"}"""))
    val endpoint = server.url("/answer").toString()
    val client =
        HttpLlmClient(
            endpoint = endpoint,
            apiKey = "secret",
            client = OkHttpClient(),
        )

    val reply = client.generateReply("Salut ?")

    assertEquals("Bonjour", reply)

    val recorded = server.takeRequest()
    assertEquals("/answer", recorded.path)
    assertEquals("secret", recorded.getHeader("x-api-key"))
    assertEquals("""{"question":"Salut ?"}""", recorded.body.readUtf8())
  }

  @Test
  fun generateReply_throwsWhenEndpointMissing() = runTest {
    val client = HttpLlmClient(endpoint = "", apiKey = "", client = OkHttpClient())

    try {
      client.generateReply("ignored")
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("LLM HTTP endpoint not configured", e.message)
    }
  }

  @Test
  fun generateReply_throwsOnHttpError() = runTest {
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(),
            apiKey = "",
            client = OkHttpClient(),
        )

    try {
      client.generateReply("test")
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("LLM HTTP call failed with 500", e.message)
    }
  }

  @Test
  fun generateReply_throwsOnEmptyBody() = runTest {
    server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_OK))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(),
            apiKey = "",
            client = OkHttpClient(),
        )

    try {
      client.generateReply("test")
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("LLM HTTP response empty body", e.message)
    }
  }

  @Test
  fun generateReply_throwsOnMissingReplyField() = runTest {
    server.enqueue(
        MockResponse().setResponseCode(HttpURLConnection.HTTP_OK).setBody("""{"reply":""}"""))
    val client =
        HttpLlmClient(
            endpoint = server.url("/answer").toString(),
            apiKey = "",
            client = OkHttpClient(),
        )

    try {
      client.generateReply("test")
      fail("Expected IllegalStateException")
    } catch (e: IllegalStateException) {
      assertEquals("LLM HTTP reply empty", e.message)
    }
  }
}
