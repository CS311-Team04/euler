package com.android.sample.settings

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationAppearanceLabelTest {

  @After
  fun tearDown() {
    AppSettings.setLanguage(Language.EN)
  }

  @Test
  fun appearanceLabel_returns_english_strings_by_default() {
    assertEquals("System default", Localization.appearanceLabel(AppearanceMode.SYSTEM))
    assertEquals("Light", Localization.appearanceLabel(AppearanceMode.LIGHT))
    assertEquals("Dark", Localization.appearanceLabel(AppearanceMode.DARK))
  }

  @Test
  fun appearanceLabel_responds_to_language_changes() {
    AppSettings.setLanguage(Language.FR)

    assertEquals("Défaut système", Localization.appearanceLabel(AppearanceMode.SYSTEM))
    assertEquals("Clair", Localization.appearanceLabel(AppearanceMode.LIGHT))
    assertEquals("Sombre", Localization.appearanceLabel(AppearanceMode.DARK))
  }
}
