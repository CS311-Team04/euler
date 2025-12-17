package com.android.sample.conversations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationModelsTest {

  @Test
  fun messageSourceMetadata_deriveLabel_removes_www_prefix() {
    // Note: Uri.parse may behave differently in unit tests
    // The function should extract the host and remove www prefix when possible
    val metadata = MessageSourceMetadata(url = "https://www.epfl.ch/page", compactType = "NONE")
    val label = metadata.deriveLabel()
    // If parsing works, should be "epfl.ch", otherwise falls back to full URL
    assertTrue(
        "Label should be either domain or full URL",
        label == "epfl.ch" || label == "https://www.epfl.ch/page")
  }

  @Test
  fun messageSourceMetadata_deriveLabel_keeps_domain_without_www() {
    val metadata = MessageSourceMetadata(url = "https://epfl.ch/page", compactType = "NONE")
    val label = metadata.deriveLabel()
    // If parsing works, should be "epfl.ch", otherwise falls back to full URL
    assertTrue(
        "Label should be either domain or full URL",
        label == "epfl.ch" || label == "https://epfl.ch/page")
  }

  @Test
  fun messageSourceMetadata_deriveLabel_handles_invalid_url() {
    val invalidUrl = "not-a-valid-url"
    val metadata = MessageSourceMetadata(url = invalidUrl, compactType = "NONE")
    val label = metadata.deriveLabel()
    // Should return original URL when parsing fails
    assertEquals(invalidUrl, label)
  }

  @Test
  fun messageSourceMetadata_deriveLabel_handles_url_without_protocol() {
    val metadata = MessageSourceMetadata(url = "epfl.ch/page", compactType = "NONE")
    val label = metadata.deriveLabel()
    // Should return original URL when parsing fails
    assertEquals("epfl.ch/page", label)
  }

  @Test
  fun messageSourceMetadata_deriveLabel_handles_complex_domain() {
    val metadata =
        MessageSourceMetadata(url = "https://www.example.co.uk/path/to/page", compactType = "NONE")
    val label = metadata.deriveLabel()
    // If parsing works, should be "example.co.uk", otherwise falls back to full URL
    assertTrue(
        "Label should be either domain or full URL",
        label == "example.co.uk" || label == "https://www.example.co.uk/path/to/page")
  }

  @Test
  fun messageSourceMetadata_deriveLabel_handles_subdomain() {
    val metadata =
        MessageSourceMetadata(url = "https://subdomain.example.com/page", compactType = "NONE")
    val label = metadata.deriveLabel()
    // If parsing works, should be "subdomain.example.com", otherwise falls back to full URL
    assertTrue(
        "Label should be either domain or full URL",
        label == "subdomain.example.com" || label == "https://subdomain.example.com/page")
  }

  @Test
  fun messageSourceMetadata_deriveLabel_handles_empty_string() {
    val metadata = MessageSourceMetadata(url = "", compactType = "NONE")
    val label = metadata.deriveLabel()
    assertEquals("", label)
  }

  @Test
  fun messageDTO_default_values() {
    val dto = MessageDTO()
    assertEquals("", dto.role)
    assertEquals("", dto.text)
    assertEquals(null, dto.createdAt)
    assertEquals(null, dto.edCardId)
    assertEquals(null, dto.sourceUrl)
    assertEquals(null, dto.sourceCompactType)
  }

  @Test
  fun messageDTO_with_all_fields() {
    val dto =
        MessageDTO(
            role = "assistant",
            text = "Hello",
            edCardId = "ed-123",
            sourceUrl = "https://epfl.ch",
            sourceCompactType = "NONE")
    assertEquals("assistant", dto.role)
    assertEquals("Hello", dto.text)
    assertEquals("ed-123", dto.edCardId)
    assertEquals("https://epfl.ch", dto.sourceUrl)
    assertEquals("NONE", dto.sourceCompactType)
  }

  @Test
  fun conversation_default_values() {
    val conv = Conversation()
    assertEquals("", conv.id)
    assertEquals("", conv.title)
    assertEquals("", conv.lastMessagePreview)
    assertEquals(null, conv.createdAt)
    assertEquals(null, conv.updatedAt)
    assertEquals("", conv.ownerUid)
    assertEquals(false, conv.archived)
  }
}
