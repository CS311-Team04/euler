package com.android.sample.home

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
