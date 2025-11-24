package com.android.sample.settings.connectors

import com.android.sample.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class ConnectorsThemeTest {

  @Test
  fun `colors returns dark theme colors when isDark is true`() {
    val colors = ConnectorsTheme.colors(isDark = true)

    assertEquals(ConnectorsBackground, colors.background)
    assertEquals(DarkOnBackground, colors.textPrimary)
    assertEquals(ConnectorsDarkTextSecondary, colors.textSecondary)
    assertEquals(ConnectorsDarkTextSecondary50, colors.textSecondary50)
    assertEquals(ConnectorsDarkGlassBackground, colors.glassBackground)
    assertEquals(ConnectorsDarkGlassBorder, colors.glassBorder)
    assertEquals(EulerShadowSpot, colors.shadowSpot)
    assertEquals(EulerShadowAmbient, colors.shadowAmbient)
    assertEquals(LightBackground, colors.onPrimaryColor)
    assertEquals(EulerGreen, colors.connectedGreen)
    assertEquals(EulerGreenTransparent, colors.connectedGreenBackground)
    assertEquals(EulerAccentRed, colors.accentRed)
  }

  @Test
  fun `colors returns light theme colors when isDark is false`() {
    val colors = ConnectorsTheme.colors(isDark = false)

    assertEquals(ConnectorsLightBackground, colors.background)
    assertEquals(ConnectorsLightTextPrimary, colors.textPrimary)
    assertEquals(ConnectorsLightTextSecondary, colors.textSecondary)
    assertEquals(ConnectorsLightTextSecondary50, colors.textSecondary50)
    assertEquals(ConnectorsLightGlassBackground, colors.glassBackground)
    assertEquals(ConnectorsLightGlassBorder, colors.glassBorder)
    assertEquals(LightShadowSpot, colors.shadowSpot)
    assertEquals(LightShadowAmbient, colors.shadowAmbient)
    assertEquals(ConnectorsLightOnPrimary, colors.onPrimaryColor)
    assertEquals(EulerGreen, colors.connectedGreen)
    assertEquals(EulerGreenTransparent, colors.connectedGreenBackground)
    assertEquals(EulerAccentRed, colors.accentRed)
  }

  @Test
  fun `colors returns consistent values for same isDark parameter`() {
    val colors1 = ConnectorsTheme.colors(isDark = true)
    val colors2 = ConnectorsTheme.colors(isDark = true)

    assertEquals(colors1.background, colors2.background)
    assertEquals(colors1.textPrimary, colors2.textPrimary)
    assertEquals(colors1.textSecondary, colors2.textSecondary)
    assertEquals(colors1.glassBackground, colors2.glassBackground)
    assertEquals(colors1.glassBorder, colors2.glassBorder)
  }

  @Test
  fun `colors returns different values for different isDark parameters`() {
    val darkColors = ConnectorsTheme.colors(isDark = true)
    val lightColors = ConnectorsTheme.colors(isDark = false)

    // Most colors should be different
    assertNotEquals(darkColors.background, lightColors.background)
    assertNotEquals(darkColors.textPrimary, lightColors.textPrimary)
    assertNotEquals(darkColors.textSecondary, lightColors.textSecondary)
    assertNotEquals(darkColors.glassBackground, lightColors.glassBackground)
    assertNotEquals(darkColors.glassBorder, lightColors.glassBorder)
    assertNotEquals(darkColors.shadowSpot, lightColors.shadowSpot)
    assertNotEquals(darkColors.shadowAmbient, lightColors.shadowAmbient)
    // onPrimaryColor might be the same (both white) - that's OK
    // These should be the same in both themes
    assertEquals(darkColors.connectedGreen, lightColors.connectedGreen)
    assertEquals(darkColors.accentRed, lightColors.accentRed)
    assertEquals(darkColors.connectedGreenBackground, lightColors.connectedGreenBackground)
  }

  @Test
  fun `connectedGreen and accentRed are same in both themes`() {
    val darkColors = ConnectorsTheme.colors(isDark = true)
    val lightColors = ConnectorsTheme.colors(isDark = false)

    assertEquals(darkColors.connectedGreen, lightColors.connectedGreen)
    assertEquals(darkColors.accentRed, lightColors.accentRed)
    assertEquals(darkColors.connectedGreenBackground, lightColors.connectedGreenBackground)
  }

  @Test
  fun `ConnectorsColors data class has all required properties`() {
    val colors = ConnectorsTheme.colors(isDark = true)

    assertNotNull(colors.background)
    assertNotNull(colors.textPrimary)
    assertNotNull(colors.textSecondary)
    assertNotNull(colors.textSecondary50)
    assertNotNull(colors.glassBackground)
    assertNotNull(colors.glassBorder)
    assertNotNull(colors.shadowSpot)
    assertNotNull(colors.shadowAmbient)
    assertNotNull(colors.onPrimaryColor)
    assertNotNull(colors.connectedGreen)
    assertNotNull(colors.connectedGreenBackground)
    assertNotNull(colors.accentRed)
  }
}
