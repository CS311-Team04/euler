package com.android.sample.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class HomeViewModelPdfDetectionTest {

  private lateinit var vm: HomeViewModel

  @Before
  fun setup() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(
          ctx,
          FirebaseOptions.Builder()
              .setApplicationId("1:123:android:test")
              .setProjectId("test")
              .setApiKey("key")
              .build())
    }
    vm = HomeViewModelTestHelpers.createViewModelForPdfDetection()
  }

  @Test
  fun isPdfUrl_acceptsPdfWithTokenQuery() {
    assertTrue(vm.isPdfUrl("https://example.com/files/doc.pdf?token=abc123"))
  }

  @Test
  fun isPdfUrl_rejectsNonPdf() {
    assertFalse(vm.isPdfUrl("https://example.com/page.html?token=abc123"))
  }
}
