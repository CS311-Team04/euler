package com.android.sample.home

import org.junit.Assert.*
import org.junit.Test

/**
 * Deterministic unit tests for HomeViewModel predefined responses functionality. These tests
 * directly test the companion object functions which are pure and deterministic.
 *
 * The ViewModel integration is tested indirectly through the existing HomeViewModelOfflineCacheTest
 * and HomeViewModelOfflineCacheCoverageTest classes.
 */
class HomeViewModelPredefinedResponsesTest {

  // ==================== OFFLINE_SUGGESTIONS TESTS ====================

  @Test
  fun `OFFLINE_SUGGESTIONS contains all 7 Euler questions`() {
    val suggestions = HomeViewModel.OFFLINE_SUGGESTIONS
    assertEquals("Should have 7 suggestions", 7, suggestions.size)
    assertTrue(suggestions.contains("What can Euler do for me?"))
    assertTrue(suggestions.contains("How do I start a new conversation?"))
    assertTrue(suggestions.contains("How do I use offline mode?"))
    assertTrue(suggestions.contains("How do I find my previous chats?"))
    assertTrue(suggestions.contains("How do I use voice input?"))
    assertTrue(suggestions.contains("How do I change the theme?"))
    assertTrue(suggestions.contains("How does Euler handle my privacy?"))
  }

  @Test
  fun `OFFLINE_SUGGESTIONS are non-empty strings`() {
    HomeViewModel.OFFLINE_SUGGESTIONS.forEach { suggestion ->
      assertTrue("Suggestion should not be empty", suggestion.isNotEmpty())
      assertTrue("Suggestion should be trimmed", suggestion == suggestion.trim())
    }
  }

  // ==================== getPredefinedResponse TESTS ====================

  @Test
  fun `getPredefinedResponse returns response for Euler capabilities question`() {
    val response = HomeViewModel.getPredefinedResponse("What can Euler do for me?")
    assertNotNull("Should have response for Euler capabilities", response)
    assertTrue(response!!.contains("answer questions"))
  }

  @Test
  fun `getPredefinedResponse returns response for new conversation question`() {
    val response = HomeViewModel.getPredefinedResponse("How do I start a new conversation?")
    assertNotNull("Should have response for new conversation", response)
    assertTrue(response!!.contains("New chat"))
  }

  @Test
  fun `getPredefinedResponse returns response for offline mode question`() {
    val response = HomeViewModel.getPredefinedResponse("How do I use offline mode?")
    assertNotNull("Should have response for offline mode", response)
    assertTrue(response!!.contains("suggestion bubbles"))
  }

  @Test
  fun `getPredefinedResponse returns response for previous chats question`() {
    val response = HomeViewModel.getPredefinedResponse("How do I find my previous chats?")
    assertNotNull("Should have response for previous chats", response)
    assertTrue(response!!.contains("conversation list"))
  }

  @Test
  fun `getPredefinedResponse returns response for voice input question`() {
    val response = HomeViewModel.getPredefinedResponse("How do I use voice input?")
    assertNotNull("Should have response for voice input", response)
    assertTrue(response!!.contains("mic"))
  }

  @Test
  fun `getPredefinedResponse returns response for theme question`() {
    val response = HomeViewModel.getPredefinedResponse("How do I change the theme?")
    assertNotNull("Should have response for theme", response)
    assertTrue(response!!.contains("Appearance"))
  }

  @Test
  fun `getPredefinedResponse returns response for privacy question`() {
    val response = HomeViewModel.getPredefinedResponse("How does Euler handle my privacy?")
    assertNotNull("Should have response for privacy", response)
    assertTrue(response!!.contains("securely"))
  }

  @Test
  fun `getPredefinedResponse returns null for unknown question`() {
    val response = HomeViewModel.getPredefinedResponse("What is the meaning of life?")
    assertNull("Should return null for unknown question", response)
  }

  @Test
  fun `getPredefinedResponse returns null for empty string`() {
    val response = HomeViewModel.getPredefinedResponse("")
    assertNull("Should return null for empty string", response)
  }

  @Test
  fun `getPredefinedResponse returns null for partial match`() {
    val response = HomeViewModel.getPredefinedResponse("What can Euler")
    assertNull("Should return null for partial match", response)
  }

  @Test
  fun `getPredefinedResponse returns null for similar but different question`() {
    val response = HomeViewModel.getPredefinedResponse("What can Euler do?")
    assertNull("Should return null for similar but different question", response)
  }

  @Test
  fun `all OFFLINE_SUGGESTIONS have matching predefined responses`() {
    HomeViewModel.OFFLINE_SUGGESTIONS.forEach { question ->
      val response = HomeViewModel.getPredefinedResponse(question)
      assertNotNull(
          "OFFLINE_SUGGESTIONS '$question' should have matching predefined response", response)
      assertTrue("Response should not be empty for '$question'", response!!.isNotEmpty())
    }
  }

  @Test
  fun `all 7 predefined questions have correct expected content`() {
    val expectedContents =
        mapOf(
            "What can Euler do for me?" to "answer questions",
            "How do I start a new conversation?" to "New chat",
            "How do I use offline mode?" to "suggestion bubbles",
            "How do I find my previous chats?" to "conversation list",
            "How do I use voice input?" to "mic",
            "How do I change the theme?" to "Appearance",
            "How does Euler handle my privacy?" to "securely")

    expectedContents.forEach { (question, expectedContent) ->
      val response = HomeViewModel.getPredefinedResponse(question)
      assertNotNull("Should have response for: $question", response)
      assertTrue(
          "Response for '$question' should contain '$expectedContent'",
          response!!.contains(expectedContent))
    }
  }

  // ==================== getCanonicalQuestion TESTS ====================

  @Test
  fun `getCanonicalQuestion returns exact match for canonical questions`() {
    HomeViewModel.OFFLINE_SUGGESTIONS.forEach { question ->
      val canonical = HomeViewModel.getCanonicalQuestion(question)
      assertEquals("Should return same string for canonical question", question, canonical)
    }
  }

  @Test
  fun `getCanonicalQuestion handles uppercase input`() {
    val canonical = HomeViewModel.getCanonicalQuestion("WHAT CAN EULER DO FOR ME?")
    assertEquals("What can Euler do for me?", canonical)
  }

  @Test
  fun `getCanonicalQuestion handles lowercase input`() {
    val canonical = HomeViewModel.getCanonicalQuestion("what can euler do for me?")
    assertEquals("What can Euler do for me?", canonical)
  }

  @Test
  fun `getCanonicalQuestion handles mixed case input`() {
    val canonical = HomeViewModel.getCanonicalQuestion("wHaT cAn EuLeR dO fOr Me?")
    assertEquals("What can Euler do for me?", canonical)
  }

  @Test
  fun `getCanonicalQuestion trims leading whitespace`() {
    val canonical = HomeViewModel.getCanonicalQuestion("   What can Euler do for me?")
    assertEquals("What can Euler do for me?", canonical)
  }

  @Test
  fun `getCanonicalQuestion trims trailing whitespace`() {
    val canonical = HomeViewModel.getCanonicalQuestion("What can Euler do for me?   ")
    assertEquals("What can Euler do for me?", canonical)
  }

  @Test
  fun `getCanonicalQuestion trims both leading and trailing whitespace`() {
    val canonical = HomeViewModel.getCanonicalQuestion("  What can Euler do for me?  ")
    assertEquals("What can Euler do for me?", canonical)
  }

  @Test
  fun `getCanonicalQuestion returns input for unknown question`() {
    val input = "Random question here"
    val canonical = HomeViewModel.getCanonicalQuestion(input)
    assertEquals(input, canonical)
  }

  @Test
  fun `getCanonicalQuestion returns trimmed input for unknown question with whitespace`() {
    val canonical = HomeViewModel.getCanonicalQuestion("  Random question  ")
    assertEquals("Random question", canonical)
  }

  @Test
  fun `getCanonicalQuestion handles empty string`() {
    val canonical = HomeViewModel.getCanonicalQuestion("")
    assertEquals("", canonical)
  }

  @Test
  fun `getCanonicalQuestion handles whitespace only string`() {
    val canonical = HomeViewModel.getCanonicalQuestion("   ")
    assertEquals("", canonical)
  }

  // ==================== INTEGRATION TESTS (canonical + predefined) ====================

  @Test
  fun `case insensitive input resolves to predefined response`() {
    val canonical = HomeViewModel.getCanonicalQuestion("HOW DO I USE VOICE INPUT?")
    val response = HomeViewModel.getPredefinedResponse(canonical)
    assertNotNull("Should find response via case-insensitive lookup", response)
    assertTrue(response!!.contains("mic"))
  }

  @Test
  fun `whitespace-padded input resolves to predefined response`() {
    val canonical = HomeViewModel.getCanonicalQuestion("  How do I change the theme?  ")
    val response = HomeViewModel.getPredefinedResponse(canonical)
    assertNotNull("Should find response via whitespace-trimmed lookup", response)
    assertTrue(response!!.contains("Appearance"))
  }

  @Test
  fun `all 7 questions work with case insensitive lookup`() {
    val questionsUppercase = HomeViewModel.OFFLINE_SUGGESTIONS.map { it.uppercase() }

    questionsUppercase.forEachIndexed { index, uppercaseQuestion ->
      val canonical = HomeViewModel.getCanonicalQuestion(uppercaseQuestion)
      assertEquals(
          "Uppercase question should resolve to canonical form",
          HomeViewModel.OFFLINE_SUGGESTIONS[index],
          canonical)

      val response = HomeViewModel.getPredefinedResponse(canonical)
      assertNotNull("Should find response for uppercase question: $uppercaseQuestion", response)
    }
  }

  // ==================== RESPONSE CONTENT TESTS ====================

  @Test
  fun `predefined responses are non-empty`() {
    HomeViewModel.OFFLINE_SUGGESTIONS.forEach { question ->
      val response = HomeViewModel.getPredefinedResponse(question)
      assertNotNull(response)
      assertTrue("Response for '$question' should not be empty", response!!.isNotEmpty())
      assertTrue("Response for '$question' should have meaningful length", response.length > 20)
    }
  }

  @Test
  fun `predefined responses are trimmed`() {
    HomeViewModel.OFFLINE_SUGGESTIONS.forEach { question ->
      val response = HomeViewModel.getPredefinedResponse(question)
      assertNotNull(response)
      assertEquals("Response for '$question' should be trimmed", response!!.trim(), response)
    }
  }
}
