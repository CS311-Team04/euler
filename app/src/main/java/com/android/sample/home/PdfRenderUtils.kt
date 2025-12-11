package com.android.sample.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request

private val pdfHttpClient by lazy { OkHttpClient() }

/** Downloads a PDF to the app cache and returns the File. Throws on failure. */
@Throws(Exception::class)
internal fun downloadPdfToCache(context: Context, url: String): File {
  val request = Request.Builder().url(url).build()
  val response = pdfHttpClient.newCall(request).execute()
  if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
  val body = response.body ?: throw IllegalStateException("Empty body")
  val file = File.createTempFile("pdf_dl_", ".pdf", context.cacheDir)
  FileOutputStream(file).use { out -> body.byteStream().use { it.copyTo(out) } }
  return file
}

/** Renders the given page index to a Bitmap. Caller is responsible for closing the page. */
internal fun renderPageToBitmap(renderer: PdfRenderer, pageIndex: Int): Bitmap? =
    renderPageToBitmap(PdfRendererAdapter.from(renderer), pageIndex)

/** Renders all pages to bitmaps. Caller should close renderer separately if needed. */
internal fun renderAllPagesToBitmaps(renderer: PdfRenderer): List<Bitmap> =
    renderAllPagesToBitmaps(PdfRendererAdapter.from(renderer))

// ---- Test-friendly abstractions ----

internal interface PageLike : AutoCloseable {
  val width: Int
  val height: Int

  fun renderTo(bitmap: Bitmap)
}

internal interface RendererLike {
  val pageCount: Int

  fun openPage(index: Int): PageLike
}

internal class PdfRendererAdapter(
    private val pageCountProvider: () -> Int,
    private val pageProvider: (Int) -> PageLike
) : RendererLike {
  override val pageCount: Int
    get() = pageCountProvider()

  override fun openPage(index: Int): PageLike = pageProvider(index)

  companion object {
    internal fun from(
        pageCountProvider: () -> Int,
        pageProvider: (Int) -> PageLike
    ): PdfRendererAdapter = PdfRendererAdapter(pageCountProvider, pageProvider)

    fun from(renderer: PdfRenderer): PdfRendererAdapter =
        from(
            pageCountProvider = { renderer.pageCount },
            pageProvider = { index ->
              val page = renderer.openPage(index)
              object : PageLike {
                override val width: Int = page.width
                override val height: Int = page.height

                override fun renderTo(bitmap: Bitmap) {
                  page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }

                override fun close() = page.close()
              }
            })
  }
}

/** Renders all pages to bitmaps using a test-friendly renderer abstraction. */
internal fun renderAllPagesToBitmaps(renderer: RendererLike): List<Bitmap> {
  val bitmaps = mutableListOf<Bitmap>()
  for (i in 0 until renderer.pageCount) {
    val bitmap = renderPageToBitmap(renderer, i) ?: continue
    bitmaps.add(bitmap)
  }
  return bitmaps
}

private fun renderPageToBitmap(renderer: RendererLike, pageIndex: Int): Bitmap? {
  if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
  renderer.openPage(pageIndex).use { page ->
    val width = page.width * 2
    val height = page.height * 2
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    // Ensure a bright background so PDFs with transparent pages render readably on dark themes.
    Canvas(bitmap).drawColor(Color.WHITE)
    page.renderTo(bitmap)
    return bitmap
  }
}
