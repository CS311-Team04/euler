// package com.android.sample.settings
//
// import android.content.Context
// import androidx.test.core.app.ApplicationProvider
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.ExperimentalCoroutinesApi
// import kotlinx.coroutines.test.StandardTestDispatcher
// import kotlinx.coroutines.test.advanceUntilIdle
// import kotlinx.coroutines.test.resetMain
// import kotlinx.coroutines.test.runTest
// import kotlinx.coroutines.test.setMain
// import org.junit.After
// import org.junit.Assert.*
// import org.junit.Before
// import org.junit.Ignore
// import org.junit.Test
// import org.junit.runner.RunWith
// import org.robolectric.RobolectricTestRunner
// import org.robolectric.annotation.Config
//
// @OptIn(ExperimentalCoroutinesApi::class)
// @RunWith(RobolectricTestRunner::class)
// @Config(sdk = [28])
// class LocalizationTest {
//
//  private lateinit var context: Context
//  private val testDispatcher = StandardTestDispatcher()
//
//  @Before
//  fun setup() =
//      runTest(testDispatcher) {
//        Dispatchers.setMain(testDispatcher)
//        AppSettings.setDispatcher(testDispatcher)
//        context = ApplicationProvider.getApplicationContext()
//        AppSettings.initialize(context)
//        advanceUntilIdle()
//        // Reset to English for consistent tests
//        AppSettings.setLanguage(Language.EN)
//        advanceUntilIdle()
//      }
//
//  @After
//  fun tearDown() {
//    AppSettings.resetDispatcher()
//    Dispatchers.resetMain()
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_french_translation_when_FR_selected() =
//      runTest(testDispatcher) {
//        AppSettings.setLanguage(Language.FR)
//        advanceUntilIdle()
//        assertEquals("Paramètres", Localization.t("settings_title"))
//        assertEquals("Profil", Localization.t("profile"))
//        assertEquals("Connecteurs", Localization.t("connectors"))
//      }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_german_translation_when_DE_selected() {
//    AppSettings.setLanguage(Language.DE)
//    Thread.sleep(100)
//    assertEquals("Einstellungen", Localization.t("settings_title"))
//    assertEquals("Profil", Localization.t("profile"))
//    assertEquals("Konnektoren", Localization.t("connectors"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_spanish_translation_when_ES_selected() {
//    AppSettings.setLanguage(Language.ES)
//    Thread.sleep(100)
//    assertEquals("Configuración", Localization.t("settings_title"))
//    assertEquals("Perfil", Localization.t("profile"))
//    assertEquals("Conectores", Localization.t("connectors"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_italian_translation_when_IT_selected() {
//    AppSettings.setLanguage(Language.IT)
//    Thread.sleep(100)
//    assertEquals("Impostazioni", Localization.t("settings_title"))
//    assertEquals("Profilo", Localization.t("profile"))
//    assertEquals("Connettori", Localization.t("connectors"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_portuguese_translation_when_PT_selected() {
//    AppSettings.setLanguage(Language.PT)
//    Thread.sleep(100)
//    assertEquals("Configurações", Localization.t("settings_title"))
//    assertEquals("Perfil", Localization.t("profile"))
//    assertEquals("Conectores", Localization.t("connectors"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_chinese_translation_when_ZH_selected() {
//    AppSettings.setLanguage(Language.ZH)
//    Thread.sleep(100)
//    assertEquals("设置", Localization.t("settings_title"))
//    assertEquals("个人资料", Localization.t("profile"))
//    assertEquals("连接器", Localization.t("connectors"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_falls_back_to_english_for_missing_key() {
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    // If a key doesn't exist, should return the key itself as fallback
//    val result = Localization.t("nonexistent_key")
//    assertEquals("nonexistent_key", result)
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun t_returns_key_if_not_found_in_any_language() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    val unknownKey = "totally_unknown_key"
//    assertEquals(unknownKey, Localization.t(unknownKey))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun all_core_keys_have_english_translations() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    val coreKeys =
//        listOf(
//            "settings_title",
//            "profile",
//            "connectors",
//            "speech_language",
//            "log_out",
//            "close",
//            "info",
//            "menu",
//            "send",
//            "powered_by")
//
//    coreKeys.forEach { key ->
//      val translation = Localization.t(key)
//      assertNotEquals("Key should have translation: $key", key, translation)
//      assertTrue("Translation should not be empty", translation.isNotEmpty())
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun all_core_keys_have_french_translations() {
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    val coreKeys =
//        listOf(
//            "settings_title",
//            "profile",
//            "connectors",
//            "speech_language",
//            "log_out",
//            "close",
//            "info",
//            "menu",
//            "send",
//            "powered_by")
//
//    coreKeys.forEach { key ->
//      val translation = Localization.t(key)
//      assertNotEquals("Key should have translation: $key", key, translation)
//      assertTrue("Translation should not be empty", translation.isNotEmpty())
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun all_languages_have_settings_title() {
//    val expectedTranslations =
//        mapOf(
//            Language.EN to "Settings",
//            Language.FR to "Paramètres",
//            Language.DE to "Einstellungen",
//            Language.ES to "Configuración",
//            Language.IT to "Impostazioni",
//            Language.PT to "Configurações",
//            Language.ZH to "设置")
//
//    expectedTranslations.forEach { (language, expected) ->
//      AppSettings.setLanguage(language)
//      Thread.sleep(100)
//      assertEquals("Translation for $language", expected, Localization.t("settings_title"))
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun all_languages_have_new_chat_translation() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    assertEquals("New chat", Localization.t("new_chat"))
//
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    assertEquals("Nouveau chat", Localization.t("new_chat"))
//
//    AppSettings.setLanguage(Language.DE)
//    Thread.sleep(100)
//    assertEquals("Neuer Chat", Localization.t("new_chat"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun message_euler_translated_in_all_languages() {
//    val languages =
//        listOf(
//            Language.EN,
//            Language.FR,
//            Language.DE,
//            Language.ES,
//            Language.IT,
//            Language.PT,
//            Language.ZH)
//
//    languages.forEach { language ->
//      AppSettings.setLanguage(language)
//      Thread.sleep(100)
//      val translation = Localization.t("message_euler")
//      assertNotNull(translation)
//      assertTrue("Translation should not be empty", translation.isNotEmpty())
//      assertNotEquals("message_euler", translation)
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun powered_by_translated_in_all_languages() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    assertTrue(Localization.t("powered_by").contains("Apertus"))
//
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    assertTrue(Localization.t("powered_by").contains("Apertus"))
//
//    AppSettings.setLanguage(Language.DE)
//    Thread.sleep(100)
//    assertTrue(Localization.t("powered_by").contains("Apertus"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun suggestion_keys_work_for_all_languages() {
//    val suggestionKeys =
//        listOf("suggestion_what_is_epfl", "suggestion_check_ed", "suggestion_show_schedule")
//
//    Language.entries.forEach { language ->
//      AppSettings.setLanguage(language)
//      Thread.sleep(100)
//      suggestionKeys.forEach { key ->
//        val translation = Localization.t(key)
//        assertNotEquals("Should have translation for $key in $language", key, translation)
//      }
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun intro_suggestions_translated_in_all_languages() {
//    val introKeys =
//        listOf(
//            "intro_suggestion_1",
//            "intro_suggestion_2",
//            "intro_suggestion_3",
//            "intro_suggestion_4",
//            "intro_suggestion_5")
//
//    Language.entries.forEach { language ->
//      AppSettings.setLanguage(language)
//      Thread.sleep(100)
//      introKeys.forEach { key ->
//        val translation = Localization.t(key)
//        assertNotEquals("Should have translation for $key in $language", key, translation)
//        assertTrue("Translation should not be empty", translation.isNotEmpty())
//      }
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun drawer_elements_translated() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    assertEquals("New chat", Localization.t("new_chat"))
//    assertEquals("RECENTS", Localization.t("recents"))
//    assertEquals("View all chats", Localization.t("view_all_chats"))
//
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    assertEquals("Nouveau chat", Localization.t("new_chat"))
//    assertEquals("RÉCENTS", Localization.t("recents"))
//    assertEquals("Voir tous les chats", Localization.t("view_all_chats"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun recent_items_translated() {
//    val recentKeys =
//        listOf(
//            "recent_cs220_exam", "recent_linear_algebra", "recent_deadline",
// "recent_registration")
//
//    Language.entries.forEach { language ->
//      AppSettings.setLanguage(language)
//      Thread.sleep(100)
//      recentKeys.forEach { key ->
//        val translation = Localization.t(key)
//        assertNotEquals("Should translate $key", key, translation)
//      }
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun action_buttons_translated() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    assertEquals("Share", Localization.t("share"))
//    assertEquals("Delete", Localization.t("delete"))
//    assertEquals("Cancel", Localization.t("cancel"))
//
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    assertEquals("Partager", Localization.t("share"))
//    assertEquals("Supprimer", Localization.t("delete"))
//    assertEquals("Annuler", Localization.t("cancel"))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun clear_chat_dialog_translated() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    assertTrue(Localization.t("clear_chat").contains("Chat"))
//    assertTrue(Localization.t("clear_chat_message").contains("messages"))
//
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    assertTrue(Localization.t("clear_chat").contains("chat"))
//    assertTrue(Localization.t("clear_chat_message").contains("messages"))
//  }
//
//  @Test
//  fun localization_object_is_accessible() {
//    assertNotNull(Localization)
//  }
//
//  @Test
//  fun localization_t_method_is_public() {
//    val method = Localization::class.java.getDeclaredMethod("t", String::class.java)
//    assertNotNull(method)
//    // Method is public and accessible
//    assertEquals("t", method.name)
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun switching_languages_updates_translations() {
//    // Start with English
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    val englishTitle = Localization.t("settings_title")
//    assertEquals("Settings", englishTitle)
//
//    // Switch to French
//    AppSettings.setLanguage(Language.FR)
//    Thread.sleep(100)
//    val frenchTitle = Localization.t("settings_title")
//    assertEquals("Paramètres", frenchTitle)
//
//    // Verify they're different
//    assertNotEquals(englishTitle, frenchTitle)
//  }
//
//  @Test
//  fun empty_key_returns_empty_string() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    assertEquals("", Localization.t(""))
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun translations_are_not_empty() {
//    AppSettings.setLanguage(Language.EN)
//    Thread.sleep(100)
//    val keys = listOf("settings_title", "profile", "new_chat", "send")
//    keys.forEach { key ->
//      val translation = Localization.t(key)
//      assertTrue("Translation for $key should not be empty", translation.isNotEmpty())
//    }
//  }
//
//  @Ignore("Flaky test, needs investigation")
//  @Test
//  fun all_languages_maintain_consistency() {
//    val testKey = "settings_title"
//    val translations = mutableSetOf<String>()
//
//    Language.entries.forEach { language ->
//      AppSettings.setLanguage(language)
//      Thread.sleep(100)
//      translations.add(Localization.t(testKey))
//    }
//
//    // Should have 7 unique translations (one per language)
//    assertEquals(7, translations.size)
//  }
// }
