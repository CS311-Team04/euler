package com.android.sample.Chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatAttachmentTest {

  @Test
  fun deriveTitleFromUrl_returnsLastSegment_orDefault() {
    val withTitle = ChatAttachment.deriveTitleFromUrl("https://example.com/path/myfile.pdf")
    assertEquals("myfile.pdf", withTitle)

    val fallback = ChatAttachment.deriveTitleFromUrl("https://example.com/")
    assertEquals("document.pdf", fallback)
  }
}
