package com.android.sample.pdf

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PdfViewerScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun displaysFilenameInTitle() {
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = "test.pdf",
          navController = mockk(relaxed = true))
    }

    composeRule.onNodeWithText("test.pdf").assertIsDisplayed()
  }

  @Test
  fun displaysCloseButton() {
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = "test.pdf",
          navController = mockk(relaxed = true))
    }

    composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
  }

  @Test
  fun closeButtonCallsPopBackStack() {
    val navController = mockk<NavController>(relaxed = true)
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = "test.pdf",
          navController = navController)
    }

    composeRule.onNodeWithTag("pdf_viewer_close_button").performClick()

    verify { navController.popBackStack() }
  }

  @Test
  fun displaysWebView() {
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = "test.pdf",
          navController = mockk(relaxed = true))
    }

    composeRule.onNodeWithTag("pdf_viewer_webview").assertIsDisplayed()
  }

  @Test
  fun handlesLongFilename() {
    val longFilename = "very-long-filename-that-should-be-truncated-in-the-ui.pdf"
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = longFilename,
          navController = mockk(relaxed = true))
    }

    composeRule.onNodeWithText(longFilename, substring = true).assertIsDisplayed()
  }

  @Test
  fun handlesSpecialCharactersInFilename() {
    val specialFilename = "test file (1).pdf"
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = specialFilename,
          navController = mockk(relaxed = true))
    }

    composeRule.onNodeWithText(specialFilename).assertIsDisplayed()
  }

  @Test
  fun handlesEmptyFilename() {
    composeRule.setContent {
      PdfViewerScreen(
          pdfUrl = "https://example.com/test.pdf",
          filename = "",
          navController = mockk(relaxed = true))
    }

    // Should not crash, just display empty title
    composeRule.onNodeWithTag("pdf_viewer_webview").assertIsDisplayed()
  }
}
