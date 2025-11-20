package com.android.sample.settings

/** Supported appearance options for the app theme. */
enum class AppearanceMode(val prefValue: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromPreference(value: String?): AppearanceMode {
      return entries.firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: SYSTEM
    }
  }
}
