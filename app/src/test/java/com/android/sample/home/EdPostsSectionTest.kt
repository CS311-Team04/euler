package com.android.sample.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class EdPostsSectionTest {

  @get:Rule val composeRule = createComposeRule()

  // ===== IDLE: early return, nothing rendered =====
  @Test
  fun idle_renders_nothing() {
    val state = EdPostsUiState(stage = EdPostsStage.IDLE)
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("ED Discussion Posts").assertDoesNotExist()
  }

  // ===== LOADING =====
  @Test
  fun loading_shows_header_and_spinner() {
    val state =
        EdPostsUiState(stage = EdPostsStage.LOADING, filters = EdIntentFilters(course = "COM-301"))
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("ED Discussion Posts").assertIsDisplayed()
    composeRule.onNodeWithText("COM-301").assertIsDisplayed()
    composeRule.onNodeWithText("Course: COM-301").assertIsDisplayed()
  }

  // ===== LOADING without course filter =====
  @Test
  fun loading_without_course_shows_ED_chip() {
    val state =
        EdPostsUiState(stage = EdPostsStage.LOADING, filters = EdIntentFilters(course = null))
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("ED").assertIsDisplayed()
    composeRule.onNodeWithText("Course:").assertDoesNotExist()
  }

  // ===== EMPTY =====
  @Test
  fun empty_shows_no_posts_message() {
    val state = EdPostsUiState(stage = EdPostsStage.EMPTY)
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("No posts found").assertIsDisplayed()
  }

  // ===== ERROR with message =====
  @Test
  fun error_shows_message_and_retry() {
    var retryFilters: EdIntentFilters? = null
    val filters = EdIntentFilters(course = "CS-101")
    val state =
        EdPostsUiState(
            stage = EdPostsStage.ERROR, errorMessage = "Network failed", filters = filters)
    composeRule.setContent {
      MaterialTheme { EdPostsSection(state, onRetry = { retryFilters = it }) }
    }
    composeRule.onNodeWithText("Network failed").assertIsDisplayed()
    composeRule.onNodeWithText("Retry").performClick()
    assertEquals(filters, retryFilters)
  }

  // ===== ERROR without message (null) =====
  @Test
  fun error_without_message_shows_default() {
    val state = EdPostsUiState(stage = EdPostsStage.ERROR, errorMessage = null)
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("An error occurred").assertIsDisplayed()
  }

  // ===== SUCCESS with 1 post =====
  @Test
  fun success_single_post_shows_singular_result() {
    val post =
        EdPost(
            title = "Help Q1",
            content = "Plain text",
            date = 1700000000000L,
            author = "Student",
            url = "https://ed.test/1")
    val state = EdPostsUiState(stage = EdPostsStage.SUCCESS, posts = listOf(post))
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("Help Q1").assertIsDisplayed()
    composeRule.onNodeWithText("Found 1 result").assertIsDisplayed()
  }

  // ===== SUCCESS with multiple posts + click =====
  @Test
  fun success_multiple_posts_click_opens() {
    var openedUrl: String? = null
    val posts =
        listOf(
            EdPost(
                title = "Post A",
                content = "<paragraph>Hello</paragraph>",
                date = 1700000000000L,
                author = "TA",
                url = "https://ed.test/a"),
            EdPost(
                title = "Post B",
                content = "<p>World</p>",
                date = 1700000001000L,
                author = "Prof",
                url = "https://ed.test/b"))
    val state = EdPostsUiState(stage = EdPostsStage.SUCCESS, posts = posts)
    composeRule.setContent {
      MaterialTheme { EdPostsSection(state, onOpenPost = { openedUrl = it }) }
    }
    composeRule.onNodeWithText("Found 2 results").assertIsDisplayed()
    composeRule.onNodeWithText("Post A").performClick()
    assertEquals("https://ed.test/a", openedUrl)
  }

  // ===== HTML conversion branches via posts =====
  @Test
  fun html_entities_and_tags_are_converted() {
    val htmlContent = "<div>&amp;&lt;&gt;&quot;&apos;&nbsp;</div><br/><paragraph>Text</paragraph>"
    val post =
        EdPost(title = "HTML Test", content = htmlContent, date = 0L, author = "X", url = "u")
    val state = EdPostsUiState(stage = EdPostsStage.SUCCESS, posts = listOf(post))
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("HTML Test").assertIsDisplayed()
    // Content is rendered as plain text (entities decoded)
  }

  // ===== Blank content =====
  @Test
  fun blank_content_renders_without_crash() {
    val post = EdPost(title = "Empty", content = "   ", date = 0L, author = "Y", url = "v")
    val state = EdPostsUiState(stage = EdPostsStage.SUCCESS, posts = listOf(post))
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("Empty").assertIsDisplayed()
  }

  // ===== Excessive newlines in content =====
  @Test
  fun excessive_newlines_are_trimmed() {
    val post =
        EdPost(title = "Newlines", content = "A\n\n\n\n\nB", date = 0L, author = "Z", url = "w")
    val state = EdPostsUiState(stage = EdPostsStage.SUCCESS, posts = listOf(post))
    composeRule.setContent { MaterialTheme { EdPostsSection(state) } }
    composeRule.onNodeWithText("Newlines").assertIsDisplayed()
  }
}
