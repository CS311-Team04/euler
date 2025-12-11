package com.android.sample.home

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PdfRenderUtilsTest {

  @Test
  fun renderAllPagesToBitmaps_usesPageCount_andRendersEachPage() {
    val fakePages =
        listOf(
            FakePage(width = 10, height = 20),
            FakePage(width = 5, height = 8),
        )
    val renderer = FakeRenderer(fakePages)

    val bitmaps = renderAllPagesToBitmaps(renderer)

    assertEquals(2, bitmaps.size)
    assertTrue(bitmaps[0].width == 20 && bitmaps[0].height == 40)
    assertTrue(bitmaps[1].width == 10 && bitmaps[1].height == 16)
  }

  @Test
  fun renderAllPagesToBitmaps_returnsEmpty_whenNoPages() {
    val renderer = FakeRenderer(emptyList())

    val bitmaps = renderAllPagesToBitmaps(renderer)

    assertTrue(bitmaps.isEmpty())
  }

  @Test
  fun renderPageToBitmap_returnsNull_whenOutOfBounds() {
    val renderer = FakeRenderer(emptyList())

    val result = renderAllPagesToBitmaps(renderer)

    assertTrue(result.isEmpty())
  }

  @Test
  fun downloadPdfToCache_succeeds() {
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(200).setBody("%PDF-1.4 dummy"))
    server.start()
    val url = server.url("/doc.pdf").toString()

    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    val file: File = downloadPdfToCache(ctx, url)

    assertTrue(file.exists())
    assertTrue(file.readBytes().isNotEmpty())
    server.shutdown()
  }

  @Test
  fun downloadPdfToCache_throws_onHttpError() {
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(404))
    server.start()
    val url = server.url("/missing.pdf").toString()

    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    assertThrows(IllegalStateException::class.java) { downloadPdfToCache(ctx, url) }
    server.shutdown()
  }


  // --------- fakes for tests ---------
  private class FakePage(override val width: Int, override val height: Int) : PageLike {
    override fun renderTo(bitmap: Bitmap) {
      /* no-op */
    }

    override fun close() {}
  }

  private class FakeRenderer(private val pages: List<PageLike>) : RendererLike {
    override val pageCount: Int
      get() = pages.size

    override fun openPage(index: Int): PageLike = pages[index]
  }
}
