package com.android.sample.llm

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseFunctionsLlmClientTest {

  @Test
  fun generateReply_success_returns_reply_text() = runTest {
    val reply = "Hello from Firebase"
    val client = clientWithResult(mapOf("reply" to reply))

    val result = client.generateReply("Question")

    assertEquals(reply, result.reply)
    assertNull(result.url)
  }

  @Test
  fun generateReply_null_result_uses_fallback() = runTest {
    val fallback = RecordingFallback("fallback-response")
    val client = clientWithResult(resultMap = null, fallback = fallback)

    val result = client.generateReply("Hello")

    assertEquals("fallback-response", result.reply)
    assertEquals(listOf("Hello"), fallback.prompts)
  }

  @Test
  fun generateReply_invalid_payload_triggers_fallback() = runTest {
    val fallback = RecordingFallback("fallback-invalid")
    val client = clientWithResult(resultMap = "not-a-map", fallback = fallback)

    val result = client.generateReply("Hi")

    assertEquals("fallback-invalid", result.reply)
    assertEquals(listOf("Hi"), fallback.prompts)
  }

  @Test
  fun generateReply_empty_reply_invokes_fallback() = runTest {
    val fallback = RecordingFallback("fallback-empty")
    val client = clientWithResult(mapOf("reply" to ""), fallback)

    val result = client.generateReply("Ping")

    assertEquals("fallback-empty", result.reply)
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

  @Test
  fun generateReply_success_includes_primary_url_when_present() = runTest {
    val reply = "Answer with link"
    val url = "https://www.epfl.ch/education/projects"
    val client = clientWithResult(mapOf("reply" to reply, "primary_url" to url))

    val result = client.generateReply("Question")

    assertEquals(reply, result.reply)
    assertEquals(url, result.url)
  }

  @Test
  fun generateReply_parses_ed_intent_detected_true() = runTest {
    val reply = "I detected you want to post on ED"
    val client =
        clientWithResult(
            mapOf("reply" to reply, "ed_intent_detected" to true, "ed_intent" to "post_question"))

    val result = client.generateReply("poste sur ed")

    assertEquals(reply, result.reply)
    assertTrue(result.edIntentDetected)
    assertEquals("post_question", result.edIntentType)
  }

  @Test
  fun generateReply_parses_ed_intent_detected_false() = runTest {
    val reply = "Normal response"
    val client =
        clientWithResult(
            mapOf("reply" to reply, "ed_intent_detected" to false, "ed_intent" to null))

    val result = client.generateReply("what is EPFL?")

    assertEquals(reply, result.reply)
    assertFalse(result.edIntentDetected)
    assertNull(result.edIntentType)
  }

  @Test
  fun generateReply_defaults_ed_intent_when_missing() = runTest {
    val reply = "Response without ED fields"
    val client = clientWithResult(mapOf("reply" to reply))

    val result = client.generateReply("Hello")

    assertEquals(reply, result.reply)
    assertFalse(result.edIntentDetected)
    assertNull(result.edIntentType)
  }

  @Test
  fun generateReply_handles_invalid_ed_intent_types() = runTest {
    val reply = "Response"
    val client =
        clientWithResult(
            mapOf("reply" to reply, "ed_intent_detected" to "not-a-boolean", "ed_intent" to 123))

    val result = client.generateReply("test")

    assertEquals(reply, result.reply)
    assertFalse(result.edIntentDetected) // Should default to false
    assertNull(result.edIntentType) // Should default to null
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

    override suspend fun generateReply(prompt: String): BotReply {
      prompts += prompt
      return BotReply(reply, null, SourceType.NONE, EdIntent())
    }
  }

  // SourceType tests
  @Test
  fun `SourceType fromString returns SCHEDULE for schedule`() {
    assertEquals(SourceType.SCHEDULE, SourceType.fromString("schedule"))
    assertEquals(SourceType.SCHEDULE, SourceType.fromString("SCHEDULE"))
  }

  @Test
  fun `SourceType fromString returns RAG for rag`() {
    assertEquals(SourceType.RAG, SourceType.fromString("rag"))
    assertEquals(SourceType.RAG, SourceType.fromString("RAG"))
  }

  @Test
  fun `SourceType fromString returns FOOD for food`() {
    assertEquals(SourceType.FOOD, SourceType.fromString("food"))
    assertEquals(SourceType.FOOD, SourceType.fromString("FOOD"))
    assertEquals(SourceType.FOOD, SourceType.fromString("Food"))
  }

  @Test
  fun `SourceType fromString returns NONE for unknown`() {
    assertEquals(SourceType.NONE, SourceType.fromString("unknown"))
    assertEquals(SourceType.NONE, SourceType.fromString(null))
    assertEquals(SourceType.NONE, SourceType.fromString(""))
  }

  @Test
  fun `SourceType enum contains all expected values`() {
    val values = SourceType.values()
    assertEquals(4, values.size)
    assertTrue(values.contains(SourceType.SCHEDULE))
    assertTrue(values.contains(SourceType.RAG))
    assertTrue(values.contains(SourceType.FOOD))
    assertTrue(values.contains(SourceType.NONE))
  }

  @Test
  fun generateReply_parses_source_type_food() = runTest {
    val reply = "Here are today's menus"
    val client = clientWithResult(mapOf("reply" to reply, "source_type" to "food"))

    val result = client.generateReply("what's for lunch?")

    assertEquals(reply, result.reply)
    assertEquals(SourceType.FOOD, result.sourceType)
  }
}
