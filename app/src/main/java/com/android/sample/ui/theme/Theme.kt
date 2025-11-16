package com.android.sample.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFFF0000), // Red
        secondary = PurpleGrey80,
        tertiary = Pink80,
        background = Color(0xFF000000), // Black
        surface = Color(0xFF1E1E1E), // Dark gray for cards/surfaces
        onPrimary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFBDBDBD) // Light gray for secondary text
        )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFFFF0000), // Red
        secondary = PurpleGrey40,
        tertiary = Pink40,
        background = Color(0xFF000000), // Black
        surface = Color(0xFF1E1E1E), // Dark gray for cards/surfaces
        onPrimary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFBDBDBD) // Light gray for secondary text
        )

@Composable
fun SampleAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      // Force a black status bar with light icons for consistent appearance
      window.statusBarColor = androidx.compose.ui.graphics.Color.Black.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
