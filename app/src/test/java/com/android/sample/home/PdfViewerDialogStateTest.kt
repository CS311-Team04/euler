package com.android.sample.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PdfViewerDialogStateTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun pdfViewer_showsErrorFallback() {
    composeRule.setContent {
      PdfViewerDialog(
          url = "https://example.com/doc.pdf", onDismiss = {}, testBitmaps = emptyList())
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("pdf_error", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("Open in another app", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun pdfViewer_showsPagesWhenAvailable() {
    val bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).asImageBitmap()
    composeRule.setContent {
      PdfViewerDialog(
          url = "https://example.com/doc.pdf", onDismiss = {}, testBitmaps = listOf(bmp))
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("pdf_page_1", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun pdfViewer_showsLoadingStateWhenForced() {
    composeRule.setContent {
      PdfViewerDialog(url = "https://example.com/doc.pdf", onDismiss = {}, testForceLoading = true)
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Loading PDF...", useUnmergedTree = true).assertIsDisplayed()
    composeRule.onNodeWithText("Open in another app", useUnmergedTree = true).assertIsDisplayed()
  }
}
