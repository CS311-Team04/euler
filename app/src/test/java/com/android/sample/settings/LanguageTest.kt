package com.android.sample.settings

import org.junit.Assert.*
import org.junit.Test

class LanguageTest {

  @Test
  fun language_enum_has_correct_number_of_values() {
    assertEquals(7, Language.entries.size)
  }

  @Test
  fun language_EN_has_correct_values() {
    assertEquals("EN", Language.EN.code)
    assertEquals("English", Language.EN.displayName)
  }

  @Test
  fun language_FR_has_correct_values() {
    assertEquals("FR", Language.FR.code)
    assertEquals("Français", Language.FR.displayName)
  }

  @Test
  fun language_DE_has_correct_values() {
    assertEquals("DE", Language.DE.code)
    assertEquals("Deutsch", Language.DE.displayName)
  }

  @Test
  fun language_ES_has_correct_values() {
    assertEquals("ES", Language.ES.code)
    assertEquals("Español", Language.ES.displayName)
  }

  @Test
  fun language_IT_has_correct_values() {
    assertEquals("IT", Language.IT.code)
    assertEquals("Italiano", Language.IT.displayName)
  }

  @Test
  fun language_PT_has_correct_values() {
    assertEquals("PT", Language.PT.code)
    assertEquals("Português", Language.PT.displayName)
  }

  @Test
  fun language_ZH_has_correct_values() {
    assertEquals("ZH", Language.ZH.code)
    assertEquals("中文", Language.ZH.displayName)
  }

  @Test
  fun fromCode_returns_correct_language_for_EN() {
    assertEquals(Language.EN, Language.fromCode("EN"))
  }

  @Test
  fun fromCode_returns_correct_language_for_FR() {
    assertEquals(Language.FR, Language.fromCode("FR"))
  }

  @Test
  fun fromCode_returns_correct_language_for_DE() {
    assertEquals(Language.DE, Language.fromCode("DE"))
  }

  @Test
  fun fromCode_returns_correct_language_for_ES() {
    assertEquals(Language.ES, Language.fromCode("ES"))
  }

  @Test
  fun fromCode_returns_correct_language_for_IT() {
    assertEquals(Language.IT, Language.fromCode("IT"))
  }

  @Test
  fun fromCode_returns_correct_language_for_PT() {
    assertEquals(Language.PT, Language.fromCode("PT"))
  }

  @Test
  fun fromCode_returns_correct_language_for_ZH() {
    assertEquals(Language.ZH, Language.fromCode("ZH"))
  }

  @Test
  fun fromCode_is_case_insensitive() {
    assertEquals(Language.EN, Language.fromCode("en"))
    assertEquals(Language.FR, Language.fromCode("fr"))
    assertEquals(Language.DE, Language.fromCode("de"))
    assertEquals(Language.ES, Language.fromCode("es"))
    assertEquals(Language.IT, Language.fromCode("it"))
    assertEquals(Language.PT, Language.fromCode("pt"))
    assertEquals(Language.ZH, Language.fromCode("zh"))
  }

  @Test
  fun fromCode_handles_mixed_case() {
    assertEquals(Language.EN, Language.fromCode("En"))
    assertEquals(Language.FR, Language.fromCode("Fr"))
    assertEquals(Language.DE, Language.fromCode("De"))
  }

  @Test
  fun fromCode_returns_EN_for_unknown_code() {
    assertEquals(Language.EN, Language.fromCode("XX"))
    assertEquals(Language.EN, Language.fromCode("INVALID"))
    assertEquals(Language.EN, Language.fromCode(""))
  }

  @Test
  fun fromCode_returns_EN_as_default() {
    val result = Language.fromCode("nonexistent")
    assertNotNull(result)
    assertEquals(Language.EN, result)
  }

  @Test
  fun all_language_codes_are_uppercase() {
    Language.entries.forEach { language ->
      assertEquals(
          "Code should be uppercase: ${language.code}", language.code.uppercase(), language.code)
    }
  }

  @Test
  fun all_language_codes_are_two_letters() {
    Language.entries.forEach { language ->
      assertEquals("Code should be 2 characters: ${language.code}", 2, language.code.length)
    }
  }

  @Test
  fun all_display_names_are_not_empty() {
    Language.entries.forEach { language ->
      assertTrue("Display name should not be empty", language.displayName.isNotEmpty())
    }
  }

  @Test
  fun all_display_names_are_not_blank() {
    Language.entries.forEach { language ->
      assertTrue("Display name should not be blank", language.displayName.isNotBlank())
    }
  }

  @Test
  fun all_language_codes_are_unique() {
    val codes = Language.entries.map { it.code }
    assertEquals("All codes should be unique", codes.size, codes.distinct().size)
  }

  @Test
  fun all_display_names_are_unique() {
    val displayNames = Language.entries.map { it.displayName }
    assertEquals(
        "All display names should be unique", displayNames.size, displayNames.distinct().size)
  }

  @Test
  fun language_enum_values_are_accessible() {
    assertNotNull(Language.EN)
    assertNotNull(Language.FR)
    assertNotNull(Language.DE)
    assertNotNull(Language.ES)
    assertNotNull(Language.IT)
    assertNotNull(Language.PT)
    assertNotNull(Language.ZH)
  }

  @Test
  fun language_enum_order_is_stable() {
    val languages = Language.entries.toList()
    assertEquals(Language.EN, languages[0])
    assertEquals(Language.FR, languages[1])
    assertEquals(Language.DE, languages[2])
    assertEquals(Language.ES, languages[3])
    assertEquals(Language.IT, languages[4])
    assertEquals(Language.PT, languages[5])
    assertEquals(Language.ZH, languages[6])
  }

  @Test
  fun fromCode_handles_null_or_empty_strings() {
    assertEquals(Language.EN, Language.fromCode(""))
    assertEquals(Language.EN, Language.fromCode("   "))
  }

  @Test
  fun language_companion_object_exists() {
    assertNotNull(Language.Companion)
  }

  @Test
  fun language_toString_works() {
    val langString = Language.EN.toString()
    assertTrue(langString.contains("EN"))
  }

  @Test
  fun all_languages_can_be_compared() {
    assertTrue(Language.EN == Language.EN)
    assertFalse(Language.EN == Language.FR)
    assertNotEquals(Language.FR, Language.DE)
  }

  @Test
  fun language_hashCode_is_consistent() {
    val hash1 = Language.EN.hashCode()
    val hash2 = Language.EN.hashCode()
    assertEquals(hash1, hash2)
  }

  @Test
  fun different_languages_have_different_hashCodes() {
    val allHashCodes = Language.entries.map { it.hashCode() }
    // Most should be different (not all required to be unique but likely)
    assertTrue("Most hash codes should differ", allHashCodes.distinct().size >= 5)
  }

  @Test
  fun fromCode_is_idempotent() {
    val lang = Language.fromCode("FR")
    assertEquals(Language.FR, lang)
    assertEquals(Language.FR, Language.fromCode(lang.code))
  }

  @Test
  fun language_entries_contains_all_values() {
    val entries = Language.entries
    assertTrue(entries.contains(Language.EN))
    assertTrue(entries.contains(Language.FR))
    assertTrue(entries.contains(Language.DE))
    assertTrue(entries.contains(Language.ES))
    assertTrue(entries.contains(Language.IT))
    assertTrue(entries.contains(Language.PT))
    assertTrue(entries.contains(Language.ZH))
  }

  @Test
  fun code_property_matches_enum_name_pattern() {
    Language.entries.forEach { language ->
      // Code should match the enum constant name
      assertEquals("Code should match enum name", language.name, language.code)
    }
  }

  @Test
  fun fromCode_performance_is_reasonable() {
    // Test that fromCode doesn't take too long (basic performance check)
    val startTime = System.currentTimeMillis()
    repeat(1000) { Language.fromCode("FR") }
    val endTime = System.currentTimeMillis()
    assertTrue("fromCode should be fast", endTime - startTime < 1000)
  }
}
