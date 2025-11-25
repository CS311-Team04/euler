package com.android.sample.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceModeTest {

  @Test
  fun fromPreference_recognizes_all_modes_case_insensitively() {
    assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromPreference("system"))
    assertEquals(AppearanceMode.LIGHT, AppearanceMode.fromPreference("LIGHT"))
    assertEquals(AppearanceMode.DARK, AppearanceMode.fromPreference("Dark"))
  }

  @Test
  fun fromPreference_defaults_to_system_on_unknown_values() {
    assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromPreference(null))
    assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromPreference("unexpected"))
  }
}
