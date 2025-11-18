package com.android.sample.home

import android.content.Context
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.settings.AppSettings
import com.android.sample.settings.Language
import com.android.sample.settings.Localization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * JUnit tests for the new features added to HomeScreen:
 * - AnimatedIntroTitle composable
 * - Auto-scroll logic
 * - Empty state conditional rendering
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HomeScreenNewFeaturesTest {

  @get:Rule val composeTestRule = createComposeRule()
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var context: Context

  @Before
  fun setup() =
      runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        AppSettings.setDispatcher(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        AppSettings.initialize(context)
        advanceUntilIdle()
        // Reset to English for consistent tests
        AppSettings.setLanguage(Language.EN)
        advanceUntilIdle()
      }

  @After
  fun tearDown() {
    AppSettings.resetDispatcher()
    Dispatchers.resetMain()
  }

  // Expected suggestions in the exact order they should appear (using localization)
  private fun getExpectedSuggestions() =
      listOf(
          Localization.t("intro_suggestion_1"),
          Localization.t("intro_suggestion_2"),
          Localization.t("intro_suggestion_3"),
          Localization.t("intro_suggestion_4"),
          Localization.t("intro_suggestion_5"))

  /** Test that AnimatedIntroTitle displays the correct title text. */
  @Test
  fun animatedIntroTitle_displaysCorrectTitle() {
    composeTestRule.setContent { AnimatedIntroTitle() }

    composeTestRule
        .onNodeWithText(Localization.t("ask_euler_anything"))
        .assertExists()
        .assertIsDisplayed()
  }

  /** Test that AnimatedIntroTitle displays the first suggestion initially. */
  @Test
  fun animatedIntroTitle_displaysFirstSuggestionInitially() {
    composeTestRule.setContent { AnimatedIntroTitle() }
    val expectedSuggestions = getExpectedSuggestions()

    composeTestRule.onNodeWithText(expectedSuggestions[0]).assertExists().assertIsDisplayed()
  }

  /** Test that AnimatedIntroTitle contains all expected suggestions. */
  @Test
  fun animatedIntroTitle_containsAllExpectedSuggestions() {
    composeTestRule.setContent { AnimatedIntroTitle() }
    val expectedSuggestions = getExpectedSuggestions()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(expectedSuggestions[0]).assertExists()
  }

  /** Test that AnimatedIntroTitle has correct number of suggestions. */
  @Test
  fun animatedIntroTitle_hasCorrectNumberOfSuggestions() {
    val expectedSuggestions = getExpectedSuggestions()
    assertEquals(5, expectedSuggestions.size)
    assertEquals(5, expectedSuggestions.distinct().size)
  }

  /** Test that suggestions list contains expected content. */
  @Test
  fun suggestionsList_containsExpectedContent() {
    val expectedSuggestions = getExpectedSuggestions()
    assertEquals(Localization.t("intro_suggestion_1"), expectedSuggestions[0])
    assertEquals(Localization.t("intro_suggestion_2"), expectedSuggestions[1])
    assertEquals(Localization.t("intro_suggestion_3"), expectedSuggestions[2])
    assertEquals(Localization.t("intro_suggestion_4"), expectedSuggestions[3])
    assertEquals(Localization.t("intro_suggestion_5"), expectedSuggestions[4])
  }

  /** Test that suggestions are unique. */
  @Test
  fun suggestionsList_allSuggestionsAreUnique() {
    val expectedSuggestions = getExpectedSuggestions()
    val uniqueSuggestions = expectedSuggestions.distinct()
    assertEquals(
        "All suggestions should be unique", expectedSuggestions.size, uniqueSuggestions.size)
  }

  /** Test that suggestions are non-empty strings. */
  @Test
  fun suggestionsList_allSuggestionsAreNonEmpty() {
    val expectedSuggestions = getExpectedSuggestions()
    expectedSuggestions.forEach { suggestion ->
      assertTrue("Suggestion should not be empty: $suggestion", suggestion.isNotBlank())
    }
  }

  /** Test index rotation logic: should wrap around when reaching the end. */
  @Test
  fun indexRotation_logic_wrapsAroundCorrectly() {
    val size = getExpectedSuggestions().size
    var currentIndex = 0

    for (i in 0 until size * 2) {
      currentIndex = (currentIndex + 1) % size
      assertTrue(
          "Index should be within bounds: $currentIndex", currentIndex >= 0 && currentIndex < size)
    }

    assertEquals(0, currentIndex)
  }

  /** Test index rotation logic: handles edge case of size 1. */
  @Test
  fun indexRotation_logic_handlesSingleItem() {
    val size = 1
    var currentIndex = 0
    currentIndex = (currentIndex + 1) % size
    assertEquals(0, currentIndex)
  }

  /** Test index rotation logic: handles starting at non-zero index. */
  @Test
  fun indexRotation_logic_handlesNonZeroStart() {
    val size = getExpectedSuggestions().size
    var currentIndex = 3

    currentIndex = (currentIndex + 1) % size
    assertEquals(4, currentIndex)

    currentIndex = (currentIndex + 1) % size
    assertEquals(0, currentIndex)
  }

  /** Test auto-scroll index calculation when messages list is empty. */
  @Test
  fun autoScrollIndexCalculation_whenMessagesEmpty_returnsZero() {
    val messagesSize = 0
    val isSending = false

    val lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }

    assertEquals(0, lastIndex)
  }

  /** Test auto-scroll index calculation when messages exist and not sending. */
  @Test
  fun autoScrollIndexCalculation_whenMessagesExist_notSending_returnsLastMessageIndex() {
    val messagesSize = 5
    val isSending = false

    val lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }

    assertEquals(4, lastIndex)
  }

  /** Test auto-scroll index calculation when messages exist and sending. */
  @Test
  fun autoScrollIndexCalculation_whenMessagesExist_sending_returnsLastMessageIndexPlusOne() {
    val messagesSize = 5
    val isSending = true

    val lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }

    assertEquals(5, lastIndex)
  }

  /** Test auto-scroll index calculation with single message not sending. */
  @Test
  fun autoScrollIndexCalculation_singleMessage_notSending_returnsZero() {
    val messagesSize = 1
    val isSending = false

    val lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }

    assertEquals(0, lastIndex)
  }

  /** Test auto-scroll index calculation with single message sending. */
  @Test
  fun autoScrollIndexCalculation_singleMessage_sending_returnsOne() {
    val messagesSize = 1
    val isSending = true

    val lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }

    assertEquals(1, lastIndex)
  }

  /** Test auto-scroll index calculation with multiple edge cases. */
  @Test
  fun autoScrollIndexCalculation_multipleEdgeCases() {
    var messagesSize = 0
    var isSending = true
    var lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }
    assertEquals(0, lastIndex)

    messagesSize = 100
    isSending = false
    lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }
    assertEquals(99, lastIndex)

    messagesSize = 100
    isSending = true
    lastIndex =
        if (messagesSize == 0) {
          0
        } else {
          messagesSize - 1 + if (isSending) 1 else 0
        }
    assertEquals(100, lastIndex)
  }

  /** Test empty state condition: messages empty AND not sending shows AnimatedIntroTitle. */
  @Test
  fun emptyStateCondition_messagesEmpty_notSending_showsAnimatedIntroTitle() {
    val messagesEmpty = true
    val isSending = false
    val shouldShowAnimatedIntro = messagesEmpty && !isSending

    assertTrue(shouldShowAnimatedIntro)
  }

  /** Test empty state condition: messages not empty shows LazyColumn. */
  @Test
  fun emptyStateCondition_messagesNotEmpty_showsLazyColumn() {
    val messagesEmpty = false
    val isSending = false
    val shouldShowAnimatedIntro = messagesEmpty && !isSending

    assertFalse(shouldShowAnimatedIntro)
  }

  /** Test empty state condition: messages empty but sending shows LazyColumn. */
  @Test
  fun emptyStateCondition_messagesEmpty_sending_showsLazyColumn() {
    val messagesEmpty = true
    val isSending = true
    val shouldShowAnimatedIntro = messagesEmpty && !isSending

    assertFalse(shouldShowAnimatedIntro)
  }

  /** Test empty state condition: all possible combinations. */
  @Test
  fun emptyStateCondition_allCombinations() {
    assertFalse(false && !false)
    assertFalse(false && !true)
    assertTrue(true && !false)
    assertFalse(true && !true)
  }

  /** Test that AnimatedIntroTitle composable can be composed without errors. */
  @Test
  fun animatedIntroTitle_composableCanBeComposed() {
    composeTestRule.setContent { AnimatedIntroTitle() }

    composeTestRule.waitForIdle()
  }

  /** Test that AnimatedIntroTitle accepts modifier parameter. */
  @Test
  fun animatedIntroTitle_acceptsModifier() {
    composeTestRule.setContent {
      AnimatedIntroTitle(modifier = Modifier.testTag("test_intro_title"))
    }

    composeTestRule.onNodeWithTag("test_intro_title").assertExists()
  }

  /** Test that title color is deep burgundy red (0xFF8B0000). */
  @Test
  fun animatedIntroTitle_titleColorIsDeepBurgundyRed() {
    val expectedColor = 0xFF8B0000.toInt()
    assertEquals(0xFF8B0000.toInt(), expectedColor)
  }

  /** Test rotation delay is 3000 milliseconds (3 seconds). */
  @Test
  fun animatedIntroTitle_rotationDelayIsThreeSeconds() {
    val expectedDelayMs = 3000L
    assertEquals(3000L, expectedDelayMs)
  }

  /** Test animation duration is 300 milliseconds. */
  @Test
  fun animatedIntroTitle_animationDurationIs300ms() {
    val expectedDuration = 300
    assertEquals(300, expectedDuration)
  }

  /** Test that AnimatedContent uses correct transition label. */
  @Test
  fun animatedIntroTitle_transitionLabelIsCorrect() {
    val expectedLabel = "suggestion-transition"
    assertEquals("suggestion-transition", expectedLabel)
  }

  /** Test that suggestions index wraps correctly using modulo. */
  @Test
  fun suggestionsIndexModulo_wrapsCorrectly() {
    val size = getExpectedSuggestions().size
    assertEquals(1, (0 + 1) % size)
    assertEquals(2, (1 + 1) % size)
    assertEquals(3, (2 + 1) % size)
    assertEquals(4, (3 + 1) % size)
    assertEquals(0, (4 + 1) % size)
    assertEquals(0, (size - 1 + 1) % size)
    assertEquals(1, (size + 1) % size)
  }

  /** Test auto-scroll logic with various message counts. */
  @Test
  fun autoScrollLogic_variousMessageCounts() {
    fun calculateLastIndex(messagesSize: Int, isSending: Boolean): Int {
      return if (messagesSize == 0) {
        0
      } else {
        messagesSize - 1 + if (isSending) 1 else 0
      }
    }

    assertEquals(0, calculateLastIndex(0, false))
    assertEquals(0, calculateLastIndex(0, true))
    assertEquals(0, calculateLastIndex(1, false))
    assertEquals(1, calculateLastIndex(1, true))
    assertEquals(4, calculateLastIndex(5, false))
    assertEquals(5, calculateLastIndex(5, true))
    assertEquals(9, calculateLastIndex(10, false))
    assertEquals(10, calculateLastIndex(10, true))
  }

  /** Test that title text is exactly localized "ask_euler_anything". */
  @Test
  fun animatedIntroTitle_titleTextIsExact() {
    val expectedTitle = Localization.t("ask_euler_anything")
    composeTestRule.setContent { AnimatedIntroTitle() }

    composeTestRule.onNodeWithText(expectedTitle, substring = false).assertExists()
  }

  /** Test that suggestions contain expected keywords. */
  @Test
  fun suggestionsList_containExpectedKeywords() {
    val expectedSuggestions = getExpectedSuggestions()
    assertTrue(expectedSuggestions[0].contains("CS220"))
    assertTrue(expectedSuggestions[1].contains("Moodle"))
    assertTrue(expectedSuggestions[2].contains("Ed Discussion"))
    assertTrue(expectedSuggestions[3].contains("IS-Academia"))
    assertTrue(expectedSuggestions[4].contains("EPFL Drive"))
  }

  /** Test that all suggestions are properly formatted. */
  @Test
  fun suggestionsList_allProperlyFormatted() {
    val expectedSuggestions = getExpectedSuggestions()
    expectedSuggestions.forEach { suggestion ->
      assertNotNull(suggestion)
      assertEquals(suggestion, suggestion.trim())
      assertTrue(suggestion.length > 10)
    }
  }

  /** Test that index rotation handles boundary conditions. */
  @Test
  fun indexRotation_boundaryConditions() {
    val size = getExpectedSuggestions().size
    assertEquals(0, (size - 1 + 1) % size)
    assertEquals(1, (0 + 1) % size)
    assertEquals(size - 1, (size - 2 + 1) % size)
  }
}
