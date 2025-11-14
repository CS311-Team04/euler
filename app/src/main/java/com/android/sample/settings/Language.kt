package com.android.sample.settings

/** Supported languages for the app interface and speech synthesis. */
enum class Language(val code: String, val displayName: String) {
  EN("EN", "English"),
  FR("FR", "Français"),
  DE("DE", "Deutsch"),
  ES("ES", "Español"),
  IT("IT", "Italiano"),
  PT("PT", "Português"),
  ZH("ZH", "中文");

  companion object {
    /** Get Language enum from code string. Returns EN as default if not found. */
    fun fromCode(code: String): Language {
      return entries.find { it.code.equals(code, ignoreCase = true) } ?: EN
    }
  }
}
