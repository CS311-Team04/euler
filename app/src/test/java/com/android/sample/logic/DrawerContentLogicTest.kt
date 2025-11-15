package com.android.sample.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for DrawerContentLogic. Tests drawer content business logic without Android
 * dependencies.
 */
class DrawerContentLogicTest {

  @Test
  fun `formatUserName returns Student for empty string`() {
    val result = DrawerContentLogic.formatUserName("")

    assertEquals("Student", result)
  }

  @Test
  fun `formatUserName returns Student for whitespace only`() {
    val result = DrawerContentLogic.formatUserName("   ")

    assertEquals("Student", result)
  }

  @Test
  fun `formatUserName trims whitespace`() {
    val result = DrawerContentLogic.formatUserName("  John Doe  ")

    assertEquals("John Doe", result)
  }

  @Test
  fun `formatUserName capitalizes first letter of each word`() {
    val result = DrawerContentLogic.formatUserName("john doe smith")

    assertEquals("John Doe Smith", result)
  }

  @Test
  fun `formatUserName preserves existing capitalization`() {
    val result = DrawerContentLogic.formatUserName("John DOE")

    assertEquals("John Doe", result) // Only first letter is capitalized
  }

  @Test
  fun `formatUserName handles single word`() {
    val result = DrawerContentLogic.formatUserName("john")

    assertEquals("John", result)
  }

  @Test
  fun `formatUserName handles multiple spaces between words`() {
    val result = DrawerContentLogic.formatUserName("john    doe")

    assertEquals("John Doe", result)
  }

  @Test
  fun `formatUserName handles special characters`() {
    val result = DrawerContentLogic.formatUserName("josé maría")

    assertEquals("José María", result)
  }

  @Test
  fun `formatUserName handles unicode characters`() {
    val result = DrawerContentLogic.formatUserName("张伟")

    assertEquals("张伟", result)
  }

  @Test
  fun `formatUserName handles mixed case correctly`() {
    val result = DrawerContentLogic.formatUserName("JOHN DOE")

    assertEquals("John Doe", result)
  }

  @Test
  fun `formatConversationTitle returns title when not blank`() {
    val result = DrawerContentLogic.formatConversationTitle("My Conversation")

    assertEquals("My Conversation", result)
  }

  @Test
  fun `formatConversationTitle returns Untitled for empty string`() {
    val result = DrawerContentLogic.formatConversationTitle("")

    assertEquals("Untitled", result)
  }

  @Test
  fun `formatConversationTitle returns Untitled for whitespace`() {
    val result = DrawerContentLogic.formatConversationTitle("   ")

    assertEquals("Untitled", result)
  }

  @Test
  fun `shouldShowInRecentList returns true for valid index`() {
    assertTrue(DrawerContentLogic.shouldShowInRecentList(0))
    assertTrue(DrawerContentLogic.shouldShowInRecentList(5))
    assertTrue(DrawerContentLogic.shouldShowInRecentList(11))
  }

  @Test
  fun `shouldShowInRecentList returns false for index at limit`() {
    assertFalse(DrawerContentLogic.shouldShowInRecentList(12))
  }

  @Test
  fun `shouldShowInRecentList returns false for index beyond limit`() {
    assertFalse(DrawerContentLogic.shouldShowInRecentList(13))
    assertFalse(DrawerContentLogic.shouldShowInRecentList(100))
  }

  @Test
  fun `shouldShowInRecentList respects custom maxRecent`() {
    assertTrue(DrawerContentLogic.shouldShowInRecentList(4, maxRecent = 5))
    assertFalse(DrawerContentLogic.shouldShowInRecentList(5, maxRecent = 5))
  }
}
