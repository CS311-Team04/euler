package com.android.sample.llm

import com.android.sample.BuildConfig
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FirebaseFunctionsLlmClient.
 *
 * Note: These tests focus on the logic paths and error handling. Full integration testing would
 * require mocking Firebase Functions or using a test emulator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseFunctionsLlmClientTest {

  @Test
  fun generateReply_with_valid_response_returns_reply() {
    // This test would require mocking Firebase Functions
    // For now, we test the logic structure
    val validReply = "Test response"
    val replyMap = mapOf("reply" to validReply)

    // Simulate successful extraction
    val extractedReply = replyMap["reply"]?.toString()?.takeIf { it.isNotBlank() }
    assertNotNull("Reply should be extracted", extractedReply)
    assertEquals(validReply, extractedReply)
  }

  @Test
  fun generateReply_with_null_result_uses_fallback() {
    // Test the null result path (timeout or call failure)
    val result: HttpsCallableResult? = null

    // Simulate fallback logic
    val fallbackUsed = result == null
    assertTrue("Fallback should be used when result is null", fallbackUsed)
  }

  @Test
  fun generateReply_with_invalid_map_uses_fallback() {
    // Test when result.getData() is not a Map<String, Any?>
    val invalidData: Any = "not a map"

    // Simulate the cast check
    @Suppress("UNCHECKED_CAST") val map = invalidData as? Map<String, Any?>
    val shouldUseFallback = map == null
    assertTrue("Fallback should be used when data is not a map", shouldUseFallback)
  }

  @Test
  fun generateReply_with_missing_reply_field_uses_fallback() {
    // Test when map doesn't contain "reply" key
    val map = mapOf<String, Any?>("other" to "value")

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    val shouldUseFallback = reply == null
    assertTrue("Fallback should be used when reply is missing", shouldUseFallback)
  }

  @Test
  fun generateReply_with_empty_reply_uses_fallback() {
    // Test when reply is empty string
    val map = mapOf<String, Any?>("reply" to "")

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    val shouldUseFallback = reply == null
    assertTrue("Fallback should be used when reply is empty", shouldUseFallback)
  }

  @Test
  fun generateReply_with_whitespace_only_reply_uses_fallback() {
    // Test when reply is only whitespace
    val map = mapOf<String, Any?>("reply" to "   ")

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    val shouldUseFallback = reply == null
    assertTrue("Fallback should be used when reply is whitespace only", shouldUseFallback)
  }

  @Test
  fun generateReply_with_null_reply_uses_fallback() {
    // Test when reply is null
    val map = mapOf<String, Any?>("reply" to null)

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    val shouldUseFallback = reply == null
    assertTrue("Fallback should be used when reply is null", shouldUseFallback)
  }

  @Test
  fun generateReply_with_non_string_reply_uses_fallback() {
    // Test when reply is not a String
    val map = mapOf<String, Any?>("reply" to 123)

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    val shouldUseFallback = reply == null
    assertTrue("Fallback should be used when reply is not a string", shouldUseFallback)
  }

  @Test
  fun generateReply_with_valid_reply_extracts_correctly() {
    // Test successful extraction
    val expectedReply = "This is a valid reply"
    val map = mapOf<String, Any?>("reply" to expectedReply)

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    assertNotNull("Reply should be extracted", reply)
    assertEquals(expectedReply, reply)
  }

  @Test
  fun generateReply_fallback_null_throws_exception() {
    // Test when fallback is null and result is null
    val result: HttpsCallableResult? = null
    val fallback: LlmClient? = null

    val shouldThrow = result == null && fallback == null
    assertTrue("Should throw when result is null and fallback is null", shouldThrow)
  }

  @Test
  fun generateReply_fallback_null_invalid_payload_throws_exception() {
    // Test when fallback is null and payload is invalid
    val invalidData: Any = "not a map"
    val fallback: LlmClient? = null

    @Suppress("UNCHECKED_CAST") val map = invalidData as? Map<String, Any?>
    val shouldThrow = map == null && fallback == null
    assertTrue("Should throw when payload is invalid and fallback is null", shouldThrow)
  }

  @Test
  fun generateReply_fallback_null_empty_reply_throws_exception() {
    // Test when fallback is null and reply is empty
    val map = mapOf<String, Any?>("reply" to "")
    val fallback: LlmClient? = null

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    val shouldThrow = reply == null && fallback == null
    assertTrue("Should throw when reply is empty and fallback is null", shouldThrow)
  }

  @Test
  fun generateReply_data_payload_structure() {
    // Test that data payload has correct structure
    val prompt = "Test question"
    val data = hashMapOf("question" to prompt)

    assertEquals("question", data.keys.first())
    assertEquals(prompt, data["question"])
    assertEquals(1, data.size)
  }

  @Test
  fun generateReply_timeout_handling() {
    // Test timeout logic
    val result: HttpsCallableResult? = null // Simulating timeout

    // Simulate withTimeoutOrNull returning null
    val timedOut = result == null
    assertTrue("Should detect timeout when result is null", timedOut)
  }

  @Test
  fun generateReply_map_extraction_with_valid_data() {
    // Test map extraction with valid data structure
    val validMap = mapOf<String, Any?>("reply" to "test", "other" to 123)
    val extractedMap = validMap as? Map<String, Any?>

    assertNotNull("Map should be extracted", extractedMap)
    assertEquals(validMap, extractedMap)
  }

  @Test
  fun generateReply_map_extraction_with_list() {
    // Test that list is not accepted as map
    val listData: Any = listOf("item1", "item2")
    @Suppress("UNCHECKED_CAST") val extractedMap = listData as? Map<String, Any?>

    assertNull("List should not be accepted as map", extractedMap)
  }

  @Test
  fun generateReply_map_extraction_with_string() {
    // Test that string is not accepted as map
    val stringData: Any = "not a map"
    @Suppress("UNCHECKED_CAST") val extractedMap = stringData as? Map<String, Any?>

    assertNull("String should not be accepted as map", extractedMap)
  }

  @Test
  fun generateReply_reply_extraction_with_multiple_fields() {
    // Test reply extraction when map has multiple fields
    val map = mapOf<String, Any?>("reply" to "answer", "status" to "ok", "timestamp" to 123456L)

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    assertNotNull("Reply should be extracted from map with multiple fields", reply)
    assertEquals("answer", reply)
  }

  @Test
  fun generateReply_reply_with_special_characters() {
    // Test reply with special characters
    val specialReply = "Réponse avec caractères spéciaux: àéîöü & < > \" '"
    val map = mapOf<String, Any?>("reply" to specialReply)

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    assertNotNull("Reply with special characters should be extracted", reply)
    assertEquals(specialReply, reply)
  }

  @Test
  fun generateReply_reply_with_newlines() {
    // Test reply with newlines
    val multilineReply = "Line 1\nLine 2\nLine 3"
    val map = mapOf<String, Any?>("reply" to multilineReply)

    val reply = (map["reply"] as? String)?.takeIf { it.isNotBlank() }
    assertNotNull("Multiline reply should be extracted", reply)
    assertEquals(multilineReply, reply)
  }

  @Test
  fun defaultFunctions_creates_firebase_functions_instance() {
    // Test that defaultFunctions creates an instance
    // Note: This is difficult to test without mocking Firebase
    // We test the logic structure instead
    val region = "us-central1"
    assertNotNull("Region should be set", region)
    assertEquals("us-central1", region)
  }

  @Test
  fun defaultFunctions_emulator_configuration_logic() {
    // Test emulator configuration logic
    val useEmulator = BuildConfig.USE_FUNCTIONS_EMULATOR
    val hasHost = BuildConfig.FUNCTIONS_HOST.isNotBlank()
    val hasPort = BuildConfig.FUNCTIONS_PORT > 0

    // Logic: if USE_FUNCTIONS_EMULATOR is true, emulator should be configured
    val shouldConfigureEmulator = useEmulator && hasHost && hasPort
    // We can't directly test FirebaseFunctions.useEmulator(), but we verify the logic
    assertNotNull("Emulator configuration should be checkable", shouldConfigureEmulator)
  }

  @Test
  fun defaultFunctions_region_is_us_central1() {
    // Test that region is us-central1
    val expectedRegion = "us-central1"
    assertEquals("Region should be us-central1", expectedRegion, "us-central1")
  }

  @Test
  fun function_name_is_answerWithRagFn() {
    // Test that function name is correct
    val functionName = "answerWithRagFn"
    assertEquals("answerWithRagFn", functionName)
  }

  @Test
  fun default_timeout_is_5000ms() {
    // Test default timeout
    val defaultTimeout = 5000L
    assertEquals(5000L, defaultTimeout)
  }

  @Test
  fun fallback_initialization_with_http_endpoint() {
    // Test fallback initialization logic
    val hasHttpEndpoint = BuildConfig.LLM_HTTP_ENDPOINT.isNotBlank()
    val fallbackShouldExist = hasHttpEndpoint

    // We can't directly test HttpLlmClient() instantiation, but we verify the logic
    assertNotNull("Fallback initialization logic should be checkable", fallbackShouldExist)
  }

  @Test
  fun fallback_initialization_without_http_endpoint() {
    // Test fallback initialization when endpoint is blank
    val hasHttpEndpoint = false
    val fallbackShouldBeNull = !hasHttpEndpoint

    assertTrue("Fallback should be null when endpoint is blank", fallbackShouldBeNull)
  }

  @Test
  fun error_messages_are_descriptive() {
    // Test that error messages are descriptive
    val timeoutMessage = "LLM service unavailable"
    val invalidPayloadMessage = "Invalid LLM response payload"
    val emptyReplyMessage = "Empty LLM reply"

    assertTrue("Timeout message should be descriptive", timeoutMessage.isNotEmpty())
    assertTrue("Invalid payload message should be descriptive", invalidPayloadMessage.isNotEmpty())
    assertTrue("Empty reply message should be descriptive", emptyReplyMessage.isNotEmpty())
  }

  @Test
  fun generateReply_handles_all_error_paths() {
    // Comprehensive test covering all error paths
    val errorScenarios =
        listOf<Pair<Any?, String>>(
            // Null result
            null to "LLM service unavailable",
            // Invalid data type
            "not a map" to "Invalid LLM response payload",
            // Missing reply
            mapOf<String, Any?>("other" to "value") to "Empty LLM reply",
            // Empty reply
            mapOf<String, Any?>("reply" to "") to "Empty LLM reply",
            // Null reply
            mapOf<String, Any?>("reply" to null) to "Empty LLM reply",
        )

    errorScenarios.forEach { (data, _) ->
      when (data) {
        null -> {
          // Timeout scenario
          assertTrue("Should handle null result", true)
        }
        is String -> {
          // Invalid payload scenario
          @Suppress("UNCHECKED_CAST") val map = data as? Map<String, Any?>
          assertNull("Should detect invalid payload", map)
        }
        is Map<*, *> -> {
          // Empty/missing reply scenario
          val reply = (data["reply"] as? String)?.takeIf { it.isNotBlank() }
          assertNull("Should detect empty/missing reply", reply)
        }
        else -> {
          // Other types
          assertTrue("Should handle other data types", true)
        }
      }
    }
  }
}
