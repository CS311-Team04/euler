package com.android.sample.settings.connectors

import com.android.sample.settings.AppSettings
import com.android.sample.settings.Language
import com.android.sample.settings.Localization
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConnectorsLocalizationTest {

  @Before
  fun setup() {
    AppSettings.setLanguage(Language.EN)
  }

  // Expected translations for all languages
  private val expectedTranslations =
      mapOf(
          Language.EN to
              mapOf(
                  "connectors" to "Connectors",
                  "Connect_your_academic_services" to "Connect your academic services",
                  "by_epfl" to "BY EPFL",
                  "connected" to "Connected",
                  "not_connected" to "Not connected",
                  "connect" to "Connect",
                  "disconnect" to "Disconnect",
                  "disconnect_confirm_title" to "Disconnect?",
                  "disconnect_confirm_message" to "Are you sure you want to disconnect %s?",
                  "cancel" to "Cancel",
                  "close" to "Close"),
          Language.FR to
              mapOf(
                  "connectors" to "Connecteurs",
                  "Connect_your_academic_services" to "Connectez vos services académiques",
                  "by_epfl" to "PAR EPFL",
                  "connected" to "Connecté",
                  "not_connected" to "Non connecté",
                  "connect" to "Connecter",
                  "disconnect" to "Déconnecter",
                  "disconnect_confirm_title" to "Déconnecter?",
                  "disconnect_confirm_message" to "Êtes-vous sûr de vouloir déconnecter %s?",
                  "cancel" to "Annuler",
                  "close" to "Fermer"),
          Language.DE to
              mapOf(
                  "connectors" to "Konnektoren",
                  "Connect_your_academic_services" to "Verbinden Sie Ihre akademischen Dienste",
                  "by_epfl" to "VON EPFL",
                  "connected" to "Verbunden",
                  "not_connected" to "Nicht verbunden",
                  "connect" to "Verbinden",
                  "disconnect" to "Trennen",
                  "disconnect_confirm_title" to "Trennen?",
                  "disconnect_confirm_message" to "Möchten Sie %s wirklich trennen?",
                  "cancel" to "Abbrechen",
                  "close" to "Schließen"),
          Language.ES to
              mapOf(
                  "connectors" to "Conectores",
                  "Connect_your_academic_services" to "Conecta tus servicios académicos",
                  "by_epfl" to "POR EPFL",
                  "connected" to "Conectado",
                  "not_connected" to "No conectado",
                  "connect" to "Conectar",
                  "disconnect" to "Desconectar",
                  "disconnect_confirm_title" to "¿Desconectar?",
                  "disconnect_confirm_message" to "¿Está seguro de que desea desconectar %s?",
                  "cancel" to "Cancelar",
                  "close" to "Cerrar"),
          Language.IT to
              mapOf(
                  "connectors" to "Connettori",
                  "Connect_your_academic_services" to "Connetti i tuoi servizi accademici",
                  "by_epfl" to "DA EPFL",
                  "connected" to "Connesso",
                  "not_connected" to "Non connesso",
                  "connect" to "Connetti",
                  "disconnect" to "Disconnetti",
                  "disconnect_confirm_title" to "Disconnettere?",
                  "disconnect_confirm_message" to "Sei sicuro di voler disconnettere %s?",
                  "cancel" to "Annulla",
                  "close" to "Chiudi"),
          Language.PT to
              mapOf(
                  "connectors" to "Conectores",
                  "Connect_your_academic_services" to "Conecte seus serviços acadêmicos",
                  "by_epfl" to "POR EPFL",
                  "connected" to "Conectado",
                  "not_connected" to "Não conectado",
                  "connect" to "Conectar",
                  "disconnect" to "Desconectar",
                  "disconnect_confirm_title" to "Desconectar?",
                  "disconnect_confirm_message" to "Tem certeza de que deseja desconectar %s?",
                  "cancel" to "Cancelar",
                  "close" to "Fechar"),
          Language.ZH to
              mapOf(
                  "connectors" to "连接器",
                  "Connect_your_academic_services" to "连接您的学术服务",
                  "by_epfl" to "由 EPFL 提供",
                  "connected" to "已连接",
                  "not_connected" to "未连接",
                  "connect" to "连接",
                  "disconnect" to "断开连接",
                  "disconnect_confirm_title" to "断开连接?",
                  "disconnect_confirm_message" to "您确定要断开 %s 的连接吗?",
                  "cancel" to "取消",
                  "close" to "关闭"))

  // Keywords to check in disconnect_confirm_message for each language
  private val messageKeywords =
      mapOf(
          Language.EN to "Are you sure",
          Language.FR to "déconnecter",
          Language.DE to "trennen",
          Language.ES to "desconectar",
          Language.IT to "disconnettere",
          Language.PT to "desconectar",
          Language.ZH to "断开")

  @Test
  fun `all connector keys have correct translations in all languages`() {
    expectedTranslations.forEach { (language, translations) ->
      AppSettings.setLanguage(language)
      translations.forEach { (key, expectedValue) ->
        val actualValue = Localization.t(key)
        assertEquals("Translation for $key in $language", expectedValue, actualValue)
      }
    }
  }

  @Test
  fun `disconnect_confirm_message contains placeholder in all languages`() {
    Language.entries.forEach { language ->
      AppSettings.setLanguage(language)
      val message = Localization.t("disconnect_confirm_message")
      assertTrue("Message should contain %s placeholder in $language", message.contains("%s"))
    }
  }

  @Test
  fun `disconnect_confirm_message contains expected keywords in all languages`() {
    messageKeywords.forEach { (language, keyword) ->
      AppSettings.setLanguage(language)
      val message = Localization.t("disconnect_confirm_message")
      assertTrue("Message should contain '$keyword' in $language", message.contains(keyword))
    }
  }

  @Test
  fun `all connector localization keys exist and are not empty in all languages`() {
    val connectorKeys =
        listOf(
            "connectors",
            "Connect_your_academic_services",
            "by_epfl",
            "connected",
            "not_connected",
            "connect",
            "disconnect",
            "disconnect_confirm_title",
            "disconnect_confirm_message",
            "cancel",
            "close")

    Language.entries.forEach { language ->
      AppSettings.setLanguage(language)
      connectorKeys.forEach { key ->
        val translation = Localization.t(key)
        assertNotEquals("Key should have translation for $language: $key", key, translation)
        assertTrue("Translation should not be empty for $language: $key", translation.isNotEmpty())
      }
    }
  }
}
