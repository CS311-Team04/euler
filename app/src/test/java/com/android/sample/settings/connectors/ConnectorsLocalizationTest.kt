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

  @Test
  fun `connectors key returns correct English translation`() {
    assertEquals("Connectors", Localization.t("connectors"))
  }

  @Test
  fun `Connect_your_academic_services key returns correct English translation`() {
    assertEquals("Connect your academic services", Localization.t("Connect_your_academic_services"))
  }

  @Test
  fun `by_epfl key returns correct English translation`() {
    assertEquals("BY EPFL", Localization.t("by_epfl"))
  }

  @Test
  fun `connected key returns correct English translation`() {
    assertEquals("Connected", Localization.t("connected"))
  }

  @Test
  fun `not_connected key returns correct English translation`() {
    assertEquals("Not connected", Localization.t("not_connected"))
  }

  @Test
  fun `connect key returns correct English translation`() {
    assertEquals("Connect", Localization.t("connect"))
  }

  @Test
  fun `disconnect key returns correct English translation`() {
    assertEquals("Disconnect", Localization.t("disconnect"))
  }

  @Test
  fun `disconnect_confirm_title key returns correct English translation`() {
    assertEquals("Disconnect?", Localization.t("disconnect_confirm_title"))
  }

  @Test
  fun `disconnect_confirm_message key returns correct English translation with placeholder`() {
    val message = Localization.t("disconnect_confirm_message")
    assertTrue(message.contains("%s"))
    assertTrue(message.contains("Are you sure"))
  }

  @Test
  fun `cancel key returns correct English translation`() {
    assertEquals("Cancel", Localization.t("cancel"))
  }

  @Test
  fun `close key returns correct English translation`() {
    assertEquals("Close", Localization.t("close"))
  }

  @Test
  fun `all connector localization keys exist in English`() {
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

    connectorKeys.forEach { key ->
      val translation = Localization.t(key)
      assertNotEquals("Key should have translation: $key", key, translation)
      assertTrue("Translation should not be empty for: $key", translation.isNotEmpty())
    }
  }
}
