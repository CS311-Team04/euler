package com.android.sample.Chat

import com.android.sample.settings.Localization

/**
 * Helper functions for Moodle file display logic. Keeps UI components pure by moving business logic
 * out of composables.
 */

/**
 * Returns a localization key for a Moodle file type. This logic belongs outside the composable for
 * better testability.
 */
private fun getMoodleFileTypeKey(fileType: String?): String {
  return when (fileType) {
    "lecture" -> "moodle_file_type_lecture"
    "homework" -> "moodle_file_type_homework"
    "homework_solution" -> "moodle_file_type_homework_solution"
    else -> "moodle_file_type_file"
  }
}

/** Formats a file type with optional number for display. */
fun formatMoodleFileTypeWithNumber(fileType: String?, fileNumber: String?): String {
  val typeKey = getMoodleFileTypeKey(fileType)
  val typeDisplay = Localization.t(typeKey)
  return if (fileNumber != null) {
    "$typeDisplay $fileNumber"
  } else {
    typeDisplay
  }
}
