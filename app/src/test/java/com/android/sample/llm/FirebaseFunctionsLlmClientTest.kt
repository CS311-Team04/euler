package com.android.sample.llm

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseFunctionsLlmClientTest {

  @Test
  fun generateReply_success_returns_reply_text() = runTest {
    val reply = "Hello from Firebase"
    val client = clientWithResult(mapOf("reply" to reply))

    val result = client.generateReply("Question")

    assertEquals(reply, result)
  }

  @Test
  fun generateReply_null_result_uses_fallback() = runTest {
    val fallback = RecordingFallback("fallback-response")
    val client = clientWithResult(resultMap = null, fallback = fallback)

    val result = client.generateReply("Hello")

    assertEquals("fallback-response", result)
    assertEquals(listOf("Hello"), fallback.prompts)
  }

  @Test
  fun generateReply_invalid_payload_triggers_fallback() = runTest {
    val fallback = RecordingFallback("fallback-invalid")
    val client = clientWithResult(resultMap = "not-a-map", fallback = fallback)

    val result = client.generateReply("Hi")

    assertEquals("fallback-invalid", result)
    assertEquals(listOf("Hi"), fallback.prompts)
  }

  @Test
  fun generateReply_empty_reply_invokes_fallback() = runTest {
    val fallback = RecordingFallback("fallback-empty")
    val client = clientWithResult(mapOf("reply" to ""), fallback)

    val result = client.generateReply("Ping")

    assertEquals("fallback-empty", result)
  }

  @Test(expected = IllegalStateException::class)
  fun generateReply_no_fallback_throws_on_invalid_payload() = runTest {
    val client = clientWithResult(resultMap = "invalid", fallback = null)
    client.generateReply("Ping")
  }

  @Test(expected = IllegalStateException::class)
  fun generateReply_no_fallback_throws_on_empty_reply() = runTest {
    val client = clientWithResult(mapOf("reply" to ""), fallback = null)
    client.generateReply("Ping")
  }

  private fun clientWithResult(
      resultMap: Any?,
      fallback: LlmClient? = RecordingFallback("unused")
  ): FirebaseFunctionsLlmClient {
    val result =
        resultMap?.let { mockk<HttpsCallableResult>().apply { every { getData() } returns it } }

    val callable = mockk<HttpsCallableReference>()
    val task =
        if (result != null) {
          Tasks.forResult(result)
        } else {
          @Suppress("UNCHECKED_CAST")
          Tasks.forResult(null) as com.google.android.gms.tasks.Task<HttpsCallableResult>
        }
    every { callable.call(any()) } returns task

    val functions = mockk<FirebaseFunctions>()
    every { functions.getHttpsCallable("answerWithRagFn") } returns callable

    return FirebaseFunctionsLlmClient(
        functions = functions, timeoutMillis = 1000L, fallback = fallback)
  }

  private class RecordingFallback(private val reply: String) : LlmClient {
    val prompts = mutableListOf<String>()

    override suspend fun generateReply(prompt: String): String {
      prompts += prompt
      return reply
    }
  }
}
