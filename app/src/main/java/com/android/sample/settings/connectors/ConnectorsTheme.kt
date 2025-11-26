package com.android.sample.settings.connectors

import androidx.compose.ui.graphics.Color
import com.android.sample.ui.theme.ConnectorsBackground
import com.android.sample.ui.theme.ConnectorsDarkGlassBackground
import com.android.sample.ui.theme.ConnectorsDarkGlassBorder
import com.android.sample.ui.theme.ConnectorsDarkTextSecondary
import com.android.sample.ui.theme.ConnectorsDarkTextSecondary50
import com.android.sample.ui.theme.ConnectorsLightBackground
import com.android.sample.ui.theme.ConnectorsLightGlassBackground
import com.android.sample.ui.theme.ConnectorsLightGlassBorder
import com.android.sample.ui.theme.ConnectorsLightOnPrimary
import com.android.sample.ui.theme.ConnectorsLightTextPrimary
import com.android.sample.ui.theme.ConnectorsLightTextSecondary
import com.android.sample.ui.theme.ConnectorsLightTextSecondary50
import com.android.sample.ui.theme.DarkOnBackground
import com.android.sample.ui.theme.EulerAccentRed
import com.android.sample.ui.theme.EulerGreen
import com.android.sample.ui.theme.EulerGreenTransparent
import com.android.sample.ui.theme.EulerShadowAmbient
import com.android.sample.ui.theme.EulerShadowSpot
import com.android.sample.ui.theme.LightBackground
import com.android.sample.ui.theme.LightShadowAmbient
import com.android.sample.ui.theme.LightShadowSpot

/** Color scheme for the Connectors screen */
data class ConnectorsColors(
    val background: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSecondary50: Color,
    val glassBackground: Color,
    val glassBorder: Color,
    val shadowSpot: Color,
    val shadowAmbient: Color,
    val onPrimaryColor: Color,
    val connectedGreen: Color,
    val connectedGreenBackground: Color,
    val accentRed: Color
)

/** Theme object for Connectors screen */
object ConnectorsTheme {
  /** Get colors based on dark/light mode */
  fun colors(isDark: Boolean): ConnectorsColors {
    return if (isDark) {
      ConnectorsColors(
          background = ConnectorsBackground,
          textPrimary = DarkOnBackground,
          textSecondary = ConnectorsDarkTextSecondary,
          textSecondary50 = ConnectorsDarkTextSecondary50,
          glassBackground = ConnectorsDarkGlassBackground,
          glassBorder = ConnectorsDarkGlassBorder,
          shadowSpot = EulerShadowSpot,
          shadowAmbient = EulerShadowAmbient,
          onPrimaryColor = LightBackground,
          connectedGreen = EulerGreen,
          connectedGreenBackground = EulerGreenTransparent,
          accentRed = EulerAccentRed)
    } else {
      ConnectorsColors(
          background = ConnectorsLightBackground,
          textPrimary = ConnectorsLightTextPrimary,
          textSecondary = ConnectorsLightTextSecondary,
          textSecondary50 = ConnectorsLightTextSecondary50,
          glassBackground = ConnectorsLightGlassBackground,
          glassBorder = ConnectorsLightGlassBorder,
          shadowSpot = LightShadowSpot,
          shadowAmbient = LightShadowAmbient,
          onPrimaryColor = ConnectorsLightOnPrimary,
          connectedGreen = EulerGreen,
          connectedGreenBackground = EulerGreenTransparent,
          accentRed = EulerAccentRed)
    }
  }
}
