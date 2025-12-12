package com.android.sample.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelPdfDetectionTest {

  private val vm = HomeViewModelTestHelpers.createViewModelForPdfDetection()

  @Test
  fun isPdfUrl_acceptsPdfWithTokenQuery() {
    assertTrue(vm.isPdfUrl("https://example.com/files/doc.pdf?token=abc123"))
  }

  @Test
  fun isPdfUrl_rejectsNonPdf() {
    assertFalse(vm.isPdfUrl("https://example.com/page.html?token=abc123"))
  }
}
