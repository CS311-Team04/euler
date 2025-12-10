package com.android.sample.home

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PdfViewerDialogTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun pdfViewer_showsAllPages_whenProvidedBitmaps() {
    val bmp1 = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).asImageBitmap()
    val bmp2 = Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888).asImageBitmap()

    composeRule.setContent {
      PdfViewerDialog(
          url = "https://example.com/doc.pdf", onDismiss = {}, testBitmaps = listOf(bmp1, bmp2))
    }

    composeRule.onNodeWithContentDescription("PDF page 1").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("PDF page 2").assertIsDisplayed()
  }
}
