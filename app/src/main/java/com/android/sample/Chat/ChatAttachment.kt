package com.android.sample.Chat

import android.net.Uri

/**
 * Lightweight attachment model for chat messages.
 *
 * Currently used for PDF links returned by Moodle intent detection.
 */
data class ChatAttachment(
    val url: String,
    val title: String = deriveTitleFromUrl(url),
    val mimeType: String = "application/pdf"
) {
  companion object {
    fun deriveTitleFromUrl(url: String): String {
      val last = runCatching { Uri.parse(url).lastPathSegment }.getOrNull().orEmpty()
      if (last.isNotBlank()) return last
      val slash = url.substringAfterLast('/', "")
      return slash.ifBlank { "document.pdf" }
    }
  }
}
