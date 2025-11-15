package com.android.sample.logic

import java.util.Locale

/**
 * Pure Kotlin logic for DrawerContent business logic. Extracted from DrawerContent for testability.
 */
object DrawerContentLogic {

  /**
   * Formats a user name for display in the drawer.
   * - Trims whitespace
   * - Returns "Student" if empty
   * - Capitalizes first letter of each word
   */
  fun formatUserName(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "Student"
    return trimmed.split("\\s+".toRegex()).joinToString(" ") { word ->
      word.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
      }
    }
  }

  /** Determines the display title for a conversation. Returns "Untitled" if the title is blank. */
  fun formatConversationTitle(title: String): String {
    return title.ifBlank { "Untitled" }
  }

  /**
   * Checks if a conversation should be shown in the recent list. Limits to first N conversations.
   */
  fun shouldShowInRecentList(index: Int, maxRecent: Int = 12): Boolean {
    return index < maxRecent
  }
}
