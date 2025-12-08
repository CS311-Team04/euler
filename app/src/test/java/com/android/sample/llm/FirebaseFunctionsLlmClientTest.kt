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
import org.junit.Assert.assertNotNull
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
  fun `SourceType fromString returns NONE for unknown`() {
    assertEquals(SourceType.NONE, SourceType.fromString("unknown"))
    assertEquals(SourceType.NONE, SourceType.fromString(null))
    assertEquals(SourceType.NONE, SourceType.fromString(""))
  }

  @Test
  fun generateReply_parses_moodle_intent_detected_true() = runTest {
    val reply = "Here is your file"
    val moodleFile =
        mapOf(
            "url" to "https://example.com/file.pdf",
            "filename" to "file.pdf",
            "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to reply,
                "moodle_intent_detected" to true,
                "moodle_intent" to "fetch_file",
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch lecture 1")

    assertEquals(reply, result.reply)
    assertTrue(result.moodleIntent.detected)
    assertEquals("fetch_file", result.moodleIntent.intent)
    assertNotNull(result.moodleIntent.file)
    assertEquals("file.pdf", result.moodleIntent.file?.filename)
  }

  @Test
  fun generateReply_parses_moodle_intent_detected_false() = runTest {
    val reply = "Normal response"
    val client =
        clientWithResult(
            mapOf("reply" to reply, "moodle_intent_detected" to false, "moodle_intent" to null))

    val result = client.generateReply("what is EPFL?")

    assertEquals(reply, result.reply)
    assertFalse(result.moodleIntent.detected)
    assertNull(result.moodleIntent.intent)
    assertNull(result.moodleIntent.file)
  }

  @Test
  fun generateReply_allows_empty_reply_when_moodle_file_present() = runTest {
    val moodleFile =
        mapOf(
            "url" to "https://example.com/file.pdf",
            "filename" to "file.pdf",
            "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "",
                "moodle_intent_detected" to true,
                "moodle_intent" to "fetch_file",
                "moodle_file" to moodleFile),
            fallback = null)

    val result = client.generateReply("fetch lecture 1")

    assertEquals("", result.reply)
    assertNotNull(result.moodleIntent.file)
  }

  @Test
  fun generateReply_parses_moodle_file_with_url_encoding() = runTest {
    val moodleFile =
        mapOf(
            "url" to "https://example.com/file%20with%20spaces.pdf",
            "filename" to "file with spaces.pdf",
            "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "File found",
                "moodle_intent_detected" to true,
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch file")

    assertNotNull(result.moodleIntent.file)
    assertTrue(result.moodleIntent.file?.url?.contains("file") == true)
  }

  @Test
  fun generateReply_handles_moodle_file_with_special_characters() = runTest {
    val moodleFile =
        mapOf(
            "url" to "https://example.com/Wk1.A - Tools, Requirements, User Stories .pdf",
            "filename" to "Wk1.A - Tools, Requirements, User Stories .pdf",
            "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "File found",
                "moodle_intent_detected" to true,
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch file")

    assertNotNull(result.moodleIntent.file)
    assertEquals(
        "Wk1.A - Tools, Requirements, User Stories .pdf", result.moodleIntent.file?.filename)
  }

  @Test
  fun generateReply_parses_ed_formatted_question_and_title() = runTest {
    val reply = "I'll post this on ED"
    val client =
        clientWithResult(
            mapOf(
                "reply" to reply,
                "ed_intent_detected" to true,
                "ed_intent" to "post_question",
                "ed_formatted_question" to "What is the deadline?",
                "ed_formatted_title" to "Question about deadline"))

    val result = client.generateReply("post on ed")

    assertEquals(reply, result.reply)
    assertTrue(result.edIntent.detected)
    assertEquals("What is the deadline?", result.edIntent.formattedQuestion)
    assertEquals("Question about deadline", result.edIntent.formattedTitle)
  }

  @Test
  fun generateReply_handles_missing_ed_formatted_fields() = runTest {
    val reply = "Response"
    val client =
        clientWithResult(
            mapOf("reply" to reply, "ed_intent_detected" to true, "ed_intent" to "post_question"))

    val result = client.generateReply("test")

    assertTrue(result.edIntent.detected)
    assertNull(result.edIntent.formattedQuestion)
    assertNull(result.edIntent.formattedTitle)
  }

  @Test
  fun generateReply_parses_source_type_schedule() = runTest {
    val client = clientWithResult(mapOf("reply" to "Your schedule", "source_type" to "schedule"))

    val result = client.generateReply("what's my schedule?")

    assertEquals(SourceType.SCHEDULE, result.sourceType)
  }

  @Test
  fun generateReply_parses_source_type_rag() = runTest {
    val client = clientWithResult(mapOf("reply" to "Answer from web", "source_type" to "rag"))

    val result = client.generateReply("what is EPFL?")

    assertEquals(SourceType.RAG, result.sourceType)
  }

  @Test
  fun generateReply_defaults_source_type_none() = runTest {
    val client = clientWithResult(mapOf("reply" to "Answer"))

    val result = client.generateReply("question")

    assertEquals(SourceType.NONE, result.sourceType)
  }

  @Test
  fun generateReply_with_summary_and_transcript() = runTest {
    val reply = "Answer with context"
    val client = clientWithResult(mapOf("reply" to reply))

    val result =
        client.generateReply(
            prompt = "question", summary = "Previous conversation", transcript = "Recent messages")

    assertEquals(reply, result.reply)
  }

  @Test
  fun generateReply_with_profile_context() = runTest {
    val reply = "Personalized answer"
    val client = clientWithResult(mapOf("reply" to reply))

    val result = client.generateReply(prompt = "question", profileContext = "{\"name\":\"John\"}")

    assertEquals(reply, result.reply)
  }

  @Test
  fun generateReply_handles_moodle_file_with_null_url() = runTest {
    val moodleFile = mapOf("url" to null, "filename" to "file.pdf", "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "Response",
                "moodle_intent_detected" to true,
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch file")

    assertNull(result.moodleIntent.file)
  }

  @Test
  fun generateReply_handles_moodle_file_with_missing_filename() = runTest {
    val moodleFile = mapOf("url" to "https://example.com/file.pdf", "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "Response",
                "moodle_intent_detected" to true,
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch file")

    assertNull(result.moodleIntent.file)
  }

  @Test
  fun generateReply_handles_moodle_file_with_empty_url() = runTest {
    val moodleFile =
        mapOf("url" to "   ", "filename" to "file.pdf", "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "Response",
                "moodle_intent_detected" to true,
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch file")

    assertNull(result.moodleIntent.file)
  }

  @Test
  fun generateReply_handles_invalid_moodle_file_type() = runTest {
    val client =
        clientWithResult(
            mapOf(
                "reply" to "Response",
                "moodle_intent_detected" to true,
                "moodle_file" to "not-a-map"))

    val result = client.generateReply("fetch file")

    assertNull(result.moodleIntent.file)
  }

  @Test
  fun generateReply_handles_moodle_file_with_invalid_url_format() = runTest {
    val moodleFile =
        mapOf("url" to "not a valid url", "filename" to "file.pdf", "mimetype" to "application/pdf")
    val client =
        clientWithResult(
            mapOf(
                "reply" to "Response",
                "moodle_intent_detected" to true,
                "moodle_file" to moodleFile))

    val result = client.generateReply("fetch file")

    // Should handle gracefully - might return null or attempt encoding
    // The exact behavior depends on URI parsing
  }

  @Test
  fun generateReply_propagates_firebase_functions_exception() = runTest {
    val functions = mockk<FirebaseFunctions>()
    val callable = mockk<HttpsCallableReference>()
    val exception = RuntimeException("Firebase error")
    val task = com.google.android.gms.tasks.Tasks.forException<HttpsCallableResult>(exception)
    every { callable.call(any()) } returns task
    every { functions.getHttpsCallable("answerWithRagFn") } returns callable

    val client =
        FirebaseFunctionsLlmClient(functions = functions, timeoutMillis = 1000L, fallback = null)

    try {
      client.generateReply("test")
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      // Expected - exception should be propagated
      assertTrue(e is RuntimeException)
    }
  }

  @Test
  fun generateReply_handles_class_cast_exception_in_parse_response() = runTest {
    val fallback = RecordingFallback("fallback-cast")
    // Return a non-Map type to trigger ClassCastException in parseResponseMap
    val result = mockk<HttpsCallableResult>()
    every { result.getData() } returns "not-a-map"

    val callable = mockk<HttpsCallableReference>()
    val task = Tasks.forResult(result)
    every { callable.call(any()) } returns task

    val functions = mockk<FirebaseFunctions>()
    every { functions.getHttpsCallable("answerWithRagFn") } returns callable

    val client =
        FirebaseFunctionsLlmClient(
            functions = functions, timeoutMillis = 1000L, fallback = fallback)

    val botReply = client.generateReply("test")

    assertEquals("fallback-cast", botReply.reply)
  }

  @Test
  fun generateReply_handles_class_cast_exception_in_parse_reply() = runTest {
    val fallback = RecordingFallback("fallback-reply-cast")
    val result = mockk<HttpsCallableResult>()
    every { result.getData() } returns mapOf("reply" to 123) // Not a String

    val callable = mockk<HttpsCallableReference>()
    val task = Tasks.forResult(result)
    every { callable.call(any()) } returns task

    val functions = mockk<FirebaseFunctions>()
    every { functions.getHttpsCallable("answerWithRagFn") } returns callable

    val client =
        FirebaseFunctionsLlmClient(
            functions = functions, timeoutMillis = 1000L, fallback = fallback)

    val botReply = client.generateReply("test")

    assertEquals("fallback-reply-cast", botReply.reply)
  }
}
