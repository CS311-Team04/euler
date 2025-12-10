package com.android.sample.home

import android.content.Context
import android.graphics.Bitmap
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
internal fun renderPageToBitmap(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
  if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
  renderer.openPage(pageIndex).use { page ->
    val width = page.width * 2
    val height = page.height * 2
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    return bitmap
  }
}

/** Renders all pages to bitmaps. Caller should close renderer separately if needed. */
internal fun renderAllPagesToBitmaps(renderer: PdfRenderer): List<Bitmap> {
  val bitmaps = mutableListOf<Bitmap>()
  for (i in 0 until renderer.pageCount) {
    renderPageToBitmap(renderer, i)?.let { bitmaps.add(it) }
  }
  return bitmaps
}
