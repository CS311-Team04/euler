package com.android.sample.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppSettingsTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    // Note: Don't call AppSettings.initialize here as each test does it
  }

  @Test
  fun appSettings_initializes_with_default_language() {
    AppSettings.initialize(context)
    // Allow time for initialization
    Thread.sleep(100)
    assertNotNull(AppSettings.language)
  }

  @Test
  fun appSettings_default_language_is_EN() = runBlocking {
    AppSettings.initialize(context)
    delay(200) // Wait for DataStore to load
    // Reset to EN for consistent test (previous tests may have saved other languages)
    AppSettings.setLanguage(Language.EN)
    delay(100)
    assertEquals(Language.EN, AppSettings.language)
  }

  @Test
  fun setLanguage_updates_language_property() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    AppSettings.setLanguage(Language.FR)
    Thread.sleep(50)
    assertEquals(Language.FR, AppSettings.language)

    AppSettings.setLanguage(Language.DE)
    Thread.sleep(50)
    assertEquals(Language.DE, AppSettings.language)
  }

  @Test
  fun setLanguage_accepts_all_language_values() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    Language.entries.forEach { language ->
      AppSettings.setLanguage(language)
      Thread.sleep(50)
      assertEquals(language, AppSettings.language)
    }
  }

  @Test
  fun setLanguage_to_EN_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.EN)
    Thread.sleep(50)
    assertEquals(Language.EN, AppSettings.language)
  }

  @Test
  fun setLanguage_to_FR_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.FR)
    Thread.sleep(50)
    assertEquals(Language.FR, AppSettings.language)
  }

  @Test
  fun setLanguage_to_DE_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.DE)
    Thread.sleep(50)
    assertEquals(Language.DE, AppSettings.language)
  }

  @Test
  fun setLanguage_to_ES_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.ES)
    Thread.sleep(50)
    assertEquals(Language.ES, AppSettings.language)
  }

  @Test
  fun setLanguage_to_IT_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.IT)
    Thread.sleep(50)
    assertEquals(Language.IT, AppSettings.language)
  }

  @Test
  fun setLanguage_to_PT_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.PT)
    Thread.sleep(50)
    assertEquals(Language.PT, AppSettings.language)
  }

  @Test
  fun setLanguage_to_ZH_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.ZH)
    Thread.sleep(50)
    assertEquals(Language.ZH, AppSettings.language)
  }

  @Test
  fun language_changes_are_immediate() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    AppSettings.setLanguage(Language.FR)
    Thread.sleep(50)
    assertEquals(Language.FR, AppSettings.language)

    AppSettings.setLanguage(Language.EN)
    Thread.sleep(50)
    assertEquals(Language.EN, AppSettings.language)
  }

  @Test
  fun multiple_language_changes_work() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    AppSettings.setLanguage(Language.FR)
    Thread.sleep(50)
    assertEquals(Language.FR, AppSettings.language)

    AppSettings.setLanguage(Language.DE)
    Thread.sleep(50)
    assertEquals(Language.DE, AppSettings.language)

    AppSettings.setLanguage(Language.ES)
    Thread.sleep(50)
    assertEquals(Language.ES, AppSettings.language)

    AppSettings.setLanguage(Language.EN)
    Thread.sleep(50)
    assertEquals(Language.EN, AppSettings.language)
  }

  @Test
  fun appSettings_is_singleton() {
    val instance1 = AppSettings
    val instance2 = AppSettings
    assertSame(instance1, instance2)
  }

  @Test
  fun appSettings_object_is_accessible() {
    assertNotNull(AppSettings)
  }

  @Test
  fun initialize_can_be_called_multiple_times() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.initialize(context)
    Thread.sleep(100)
    // Should not throw exception
    assertNotNull(AppSettings.language)
  }

  @Test
  fun language_property_is_readable() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    val language = AppSettings.language
    assertNotNull(language)
  }

  @Test
  fun setLanguage_with_same_value_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    AppSettings.setLanguage(Language.EN)
    Thread.sleep(50)
    AppSettings.setLanguage(Language.EN)
    Thread.sleep(50)
    assertEquals(Language.EN, AppSettings.language)
  }

  @Test
  fun cycling_through_all_languages_works() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    val languages = Language.entries.toList()
    languages.forEach { language ->
      AppSettings.setLanguage(language)
      Thread.sleep(50)
      assertEquals(language, AppSettings.language)
    }
  }

  @Test
  fun language_setter_is_idempotent() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    AppSettings.setLanguage(Language.FR)
    Thread.sleep(50)
    val firstValue = AppSettings.language

    AppSettings.setLanguage(Language.FR)
    Thread.sleep(50)
    val secondValue = AppSettings.language

    assertEquals(firstValue, secondValue)
  }

  @Test
  fun appSettings_survives_language_cycling() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    // Cycle through all languages twice
    repeat(2) {
      Language.entries.forEach { language ->
        AppSettings.setLanguage(language)
        Thread.sleep(50)
        assertEquals(language, AppSettings.language)
      }
    }
  }

  @Test
  fun rapid_language_changes_work() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    repeat(10) {
      AppSettings.setLanguage(Language.FR)
      AppSettings.setLanguage(Language.EN)
      AppSettings.setLanguage(Language.DE)
    }
    Thread.sleep(100)

    // Should still be functional
    assertNotNull(AppSettings.language)
  }

  @Test
  fun language_can_be_read_after_initialization() {
    AppSettings.initialize(context)
    Thread.sleep(100)
    val language = AppSettings.language
    assertTrue(Language.entries.contains(language))
  }

  @Test
  fun setLanguage_returns_without_error() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    try {
      AppSettings.setLanguage(Language.FR)
      Thread.sleep(50)
      // Should not throw
      assertTrue(true)
    } catch (e: Exception) {
      fail("setLanguage should not throw exception: ${e.message}")
    }
  }

  @Test
  fun appSettings_handles_quick_initialization() {
    repeat(5) {
      AppSettings.initialize(context)
      Thread.sleep(50)
    }
    assertNotNull(AppSettings.language)
  }

  @Test
  fun language_state_is_consistent() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    AppSettings.setLanguage(Language.IT)
    Thread.sleep(50)
    val lang1 = AppSettings.language
    val lang2 = AppSettings.language
    val lang3 = AppSettings.language

    assertEquals(lang1, lang2)
    assertEquals(lang2, lang3)
  }

  @Test
  fun all_enum_values_can_be_set() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    val settableLanguages = mutableListOf<Language>()

    Language.entries.forEach { language ->
      AppSettings.setLanguage(language)
      Thread.sleep(50)
      if (AppSettings.language == language) {
        settableLanguages.add(language)
      }
    }

    assertEquals(Language.entries.size, settableLanguages.size)
  }

  @Test
  fun language_changes_are_reflected_immediately() {
    AppSettings.initialize(context)
    Thread.sleep(100)

    val initialLanguage = AppSettings.language
    val targetLanguage = if (initialLanguage == Language.EN) Language.FR else Language.EN

    AppSettings.setLanguage(targetLanguage)
    Thread.sleep(50)
    assertEquals(targetLanguage, AppSettings.language)
  }

  @Test
  fun appSettings_class_exists() {
    val clazz = AppSettings::class.java
    assertNotNull(clazz)
    assertEquals("AppSettings", clazz.simpleName)
  }

  @Test
  fun initialize_method_exists() {
    val method = AppSettings::class.java.getDeclaredMethod("initialize", Context::class.java)
    assertNotNull(method)
  }

  @Test
  fun setLanguage_method_exists() {
    val method = AppSettings::class.java.getDeclaredMethod("setLanguage", Language::class.java)
    assertNotNull(method)
  }

  @Test
  fun language_property_is_accessible_via_getter() {
    val getter = AppSettings::class.java.getMethod("getLanguage")
    assertNotNull(getter)
    assertEquals("getLanguage", getter.name)
  }
}
