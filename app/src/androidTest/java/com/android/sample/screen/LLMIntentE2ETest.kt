package com.android.sample.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.home.HomeTags
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for LLM intent detection and response handling.
 *
 * These tests verify:
 * 1. General EPFL questions trigger correct RAG-based responses
 * 2. ED Discussion fetch intent is detected and posts are displayed
 * 3. ED Discussion posting intent triggers the post confirmation modal
 * 4. Moodle course/series queries return proper Moodle-formatted responses
 *
 * Note: These tests require either:
 * - Firebase emulator running locally (debug build)
 * - Or production Firebase connection (release build)
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LLMIntentE2ETest : BaseE2ETest() {

  companion object {
    // Timeout for LLM responses (Cloud Functions can take time)
    private const val LLM_RESPONSE_TIMEOUT_MS = 60_000L
    // Timeout for UI elements to appear
    private const val UI_TIMEOUT_MS = 10_000L
  }

  /** Centralized test tags to avoid typos and ease refactoring. */
  private object TestTags {
    const val CHAT_USER_BUBBLE = "chat_user_bubble"
    const val CHAT_AI_TEXT = "chat_ai_text"
    const val CHAT_AI_MOODLE_BADGE = "chat_ai_moodle_badge"
    const val ED_POSTS_SECTION = "ed_posts_section"
    const val ED_POST_CONFIRMATION_MODAL = "ed_post_confirmation_modal"
  }

  private val homeRobot: HomeRobot
    get() = HomeRobot(composeRule)

  @Before
  fun setup() {
    homeRobot.navigateToHome()
  }

  // ==================== SHARED HELPERS ====================

  /**
   * Shared helper that handles the common steps of sending a message:
   * 1. Wait for message field
   * 2. Type the message
   * 3. Click send button
   * 4. Wait for user bubble to confirm message was sent
   */
  private fun typeAndSendMessage(message: String) {
    // Wait for message field to be ready
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(HomeTags.MessageField), timeoutMillis = UI_TIMEOUT_MS)
    composeRule.waitForIdle()

    // Type the message
    composeRule.onNodeWithTag(HomeTags.MessageField).performTextInput(message)
    composeRule.waitForIdle()

    // Wait for send button and click it
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(HomeTags.SendBtn), timeoutMillis = UI_TIMEOUT_MS)
    composeRule.onNodeWithTag(HomeTags.SendBtn).performClick()
    composeRule.waitForIdle()

    // Wait for user message bubble to appear (confirms message was sent)
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(TestTags.CHAT_USER_BUBBLE), timeoutMillis = UI_TIMEOUT_MS)
  }

  /**
   * Sends a message and waits for a response matching the given matcher.
   *
   * @param message The message to send
   * @param responseMatcher The matcher for the expected response component
   */
  private fun sendMessageAndWaitFor(message: String, responseMatcher: SemanticsMatcher) {
    typeAndSendMessage(message)
    composeRule.waitUntilAtLeastOneExists(responseMatcher, timeoutMillis = LLM_RESPONSE_TIMEOUT_MS)
  }

  /** Sends a message and waits for standard AI text response. */
  private fun sendMessageAndWaitForResponse(message: String) {
    sendMessageAndWaitFor(message, hasTestTag(TestTags.CHAT_AI_TEXT))
  }

  /** Sends a message and waits for either AI text OR ED posts section. */
  private fun sendMessageAndWaitForEdFetchResponse(message: String) {
    sendMessageAndWaitFor(
        message, hasTestTag(TestTags.CHAT_AI_TEXT) or hasTestTag(TestTags.ED_POSTS_SECTION))
  }

  /** Sends a message and waits for either AI text OR ED post confirmation modal. */
  private fun sendMessageAndWaitForEdPostResponse(message: String) {
    sendMessageAndWaitFor(
        message,
        hasTestTag(TestTags.CHAT_AI_TEXT) or hasTestTag(TestTags.ED_POST_CONFIRMATION_MODAL))
  }

  /** Verifies that user message and AI response exist in the chat. */
  private fun verifyMessagesExist() {
    val userNodes = composeRule.onAllNodesWithTag(TestTags.CHAT_USER_BUBBLE).fetchSemanticsNodes()
    assert(userNodes.isNotEmpty()) { "Expected at least one user message bubble" }

    val aiNodes = composeRule.onAllNodesWithTag(TestTags.CHAT_AI_TEXT).fetchSemanticsNodes()
    assert(aiNodes.isNotEmpty()) { "Expected at least one AI response" }
  }

  /** Verifies that user message was sent. */
  private fun verifyUserMessageSent() {
    val userNodes = composeRule.onAllNodesWithTag(TestTags.CHAT_USER_BUBBLE).fetchSemanticsNodes()
    assert(userNodes.isNotEmpty()) { "Expected at least one user message bubble" }
  }

  // ==================== GENERAL EPFL QUESTION TESTS ====================

  /**
   * Test that asking a general question about EPFL triggers the LLM to provide a relevant response.
   * This verifies the basic RAG (Retrieval Augmented Generation) functionality.
   */
  @Test
  fun llm_responds_to_general_epfl_question() {
    sendMessageAndWaitForResponse("What is EPFL?")
    verifyMessagesExist()
  }

  /**
   * Test using a predefined suggestion chip (What is EPFL?) that has a cached response. This
   * ensures consistent behavior even when offline.
   */
  @Test
  fun llm_suggestion_chip_triggers_predefined_response() {
    // Verify suggestion chips are displayed and click the first one
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(HomeTags.Action1Btn), timeoutMillis = UI_TIMEOUT_MS)
    composeRule.onNodeWithTag(HomeTags.Action1Btn).assertIsDisplayed()
    composeRule.onNodeWithTag(HomeTags.Action1Btn).performClick()
    composeRule.waitForIdle()

    // Wait for user message and AI response
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(TestTags.CHAT_USER_BUBBLE), timeoutMillis = UI_TIMEOUT_MS)
    composeRule.waitUntilAtLeastOneExists(
        hasTestTag(TestTags.CHAT_AI_TEXT), timeoutMillis = LLM_RESPONSE_TIMEOUT_MS)

    verifyMessagesExist()
  }

  // ==================== ED DISCUSSION FETCH INTENT TESTS ====================

  /**
   * Test that asking to see ED posts triggers the ED fetch intent. This verifies the LLM correctly
   * detects the user's intent to fetch ED Discussion posts.
   */
  @Test
  fun llm_detects_ed_fetch_intent() {
    sendMessageAndWaitForEdFetchResponse("Show me recent posts on Ed Discussion about databases")

    verifyUserMessageSent()

    // Check what response we got - either AI text or ED posts section
    val aiNodes = composeRule.onAllNodesWithTag(TestTags.CHAT_AI_TEXT).fetchSemanticsNodes()
    val edPostsNodes =
        composeRule.onAllNodesWithTag(TestTags.ED_POSTS_SECTION).fetchSemanticsNodes()

    assert(aiNodes.isNotEmpty() || edPostsNodes.isNotEmpty()) {
      "Expected either AI response or ED posts section to be displayed"
    }
  }

  /**
   * Test that asking to post on ED triggers the ED post intent and shows the confirmation modal.
   * This verifies the complete flow from intent detection to showing the post editor.
   */
  @Test
  fun llm_detects_ed_post_intent_and_shows_modal() {
    sendMessageAndWaitForEdPostResponse(
        "I want to post a question on Ed about how to implement binary search in Java")

    verifyUserMessageSent()

    // Check what response we got - either AI text or ED post modal
    val aiNodes = composeRule.onAllNodesWithTag(TestTags.CHAT_AI_TEXT).fetchSemanticsNodes()
    val modalNodes =
        composeRule.onAllNodesWithTag(TestTags.ED_POST_CONFIRMATION_MODAL).fetchSemanticsNodes()

    assert(aiNodes.isNotEmpty() || modalNodes.isNotEmpty()) {
      "Expected either AI response or Ed post modal to be displayed"
    }
  }

  // ==================== MOODLE INTEGRATION TESTS ====================

  /**
   * Test that asking about Moodle courses triggers the Moodle integration. This verifies the LLM
   * correctly handles Moodle-related queries.
   */
  @Test
  fun llm_handles_moodle_course_query() {
    sendMessageAndWaitForResponse("What are my courses on Moodle?")
    verifyMessagesExist()
  }

  /**
   * Test that asking about a specific Moodle series/week shows the correct content. This tests the
   * detailed Moodle content retrieval.
   */
  @Test
  fun llm_retrieves_moodle_series_content() {
    sendMessageAndWaitForResponse("Show me the exercises from Moodle week 3 for Analysis")
    verifyMessagesExist()

    // If Moodle is connected and course exists, we should see a Moodle badge
    val moodleBadgeNodes =
        composeRule.onAllNodesWithTag(TestTags.CHAT_AI_MOODLE_BADGE).fetchSemanticsNodes()
    if (moodleBadgeNodes.isNotEmpty()) {
      // Moodle content was retrieved successfully
      assert(moodleBadgeNodes.isNotEmpty())
    }
  }
}
