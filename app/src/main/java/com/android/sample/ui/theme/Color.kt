package com.android.sample.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Euler brand colors
val EulerRed = Color(0xFFEB5757)
val DarkBackground = Color(0xFF0F0F0F)
val DarkSurface = Color(0xFF1A1A1A)
val DarkSurfaceVariant = Color(0xFF242424)
val DarkOnBackground = Color(0xFFF3F3F3)
val DarkOnSurfaceVariant = Color(0xFFBDBDBD)
val DarkOutline = Color(0xFF2F2F2F)

val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF7F7F7)
val LightSurfaceVariant = Color(0xFFEDEDED)
val LightOnBackground = Color(0xFF1A1A1A)
val LightOnSurfaceVariant = Color(0xFF5C5C5C)
val LightOutline = Color(0xFFE0E0E0)
// Drawer colors
val EulerDrawerBackground = Color(0xFF121212)
val EulerNewChatCircleRed = Color(0xFFE53935)
val EulerNewChatTextRed = Color(0xFFFF6E6E)
val EulerDrawerMutedIcon = Color(0xFFB0B0B0)
val EulerDrawerSectionLabel = Color(0xFF8A8A8A)
val EulerDrawerEmptyText = Color(0xFF9E9E9E)
val EulerDrawerDivider = Color(0x22FFFFFF)
val EulerDrawerAvatarBackground = Color(0xFF2A2A2A)
val EulerRecentRowSelectedBg = Color(0x22FFFFFF)
val EulerRecentRowIconBackground = Color(0xFF2A2A2A)

// Suggestion chip colors
val EulerSuggestionChipBackground = Color(0xFFD0BCFF)
val EulerSuggestionChipText = Color.White

// Chat message colors
val EulerUserBubbleBg: Color = Color(0xFF2B2B2B)
val EulerUserBubbleText: Color = Color.White
val EulerAiText: Color = Color(0xFFEDEDED)

// Audio playback button colors
val EulerAudioButtonTint: Color = Color.White
val EulerAudioButtonTintSemiTransparent: Color = Color.White.copy(alpha = 0.75f)
val EulerAudioButtonLoadingColor: Color = Color.LightGray
val EulerThinkingCursorColor: Color = Color.White

// Connectors Screen colors

// Premium background
val ConnectorsBackground = DarkBackground // #0F0F0F (Remplace #0D0D0D)

// Light mode colors for Connectors
val ConnectorsLightBackground = LightBackground // White background
val ConnectorsLightTextPrimary = LightOnBackground // Dark text
val ConnectorsLightTextSecondary = LightOnSurfaceVariant.copy(alpha = 0.7f) // Gray text 70%
val ConnectorsLightTextSecondary50 = LightOnSurfaceVariant.copy(alpha = 0.5f) // Gray text 50%
val ConnectorsLightGlassBackground = LightSurface // Light gray surface
val ConnectorsLightGlassBorder = LightOutline.copy(alpha = 0.2f) // Light border
val ConnectorsLightOnPrimary = Color.White // White text on colored buttons
val ConnectorsLightSurface = LightSurface // Light surface

// Dark mode colors for Connectors (glassmorphism)
val ConnectorsDarkGlassBackground = LightBackground.copy(alpha = 0.04f)
val ConnectorsDarkGlassBorder = LightBackground.copy(alpha = 0.08f)
val ConnectorsDarkTextSecondary = LightBackground.copy(alpha = 0.70f)
val ConnectorsDarkTextSecondary50 = LightBackground.copy(alpha = 0.50f)

// Status colors
val EulerGreen = Color(0xFF39D98A)
val EulerGreenTransparent = EulerGreen.copy(alpha = 0.20f)

// Accent
val EulerAccentRed = EulerRed // alias pour Connectors UI

// Muted grays (brand-safe)
val EulerGrayDark = Color(0XFF424242) // ex #757575
val EulerGrayLight = LightOnSurfaceVariant
val textConnectors = Color.Gray.copy(alpha = 0.3f) // ex #9E9E9E
val textConnectorsLight = Color.Gray.copy(alpha = 0.5f)

// EPFL grayscale logos backgrounds (optional utility)
val ConnectorLogoBackground = LightBackground.copy(alpha = 0.03f)

// Shadow colors
val EulerShadowSpot = Color.Black.copy(alpha = 0.15f)
val EulerShadowAmbient = Color.Black.copy(alpha = 0.08f)

// Light mode shadow colors (lighter shadows for better visibility)
val LightShadowSpot = Color.Black.copy(alpha = 0.08f)
val LightShadowAmbient = Color.Black.copy(alpha = 0.04f)

// Moodle brand colors
val MoodleOrange = Color(0xFFFF9800)
val MoodleYellow = Color(0xFFFFC107)
// Moodle grayscale for disconnected state
val MoodleGray1 = Color(0xFF757575)
val MoodleGray2 = Color(0xFF9E9E9E)

// Moodle Login UI colors (matching official Moodle login page)
val MoodleLoginBackground = Color(0xFFECECEC) // Light gray background
val MoodleLoginCardBackground = Color(0xFFFFFFFF) // White card
val MoodleLoginTitleBlue = Color(0xFF1D3557) // "Connexion" title blue
val MoodleLoginButtonBlue = Color(0xFF2E6DA4) // Blue button
val MoodleLoginButtonHover = Color(0xFF255C8A) // Darker blue for press
val MoodleLoginInputBorder = Color(0xFFCED4DA) // Input border color
val MoodleLoginInputBorderFocused = Color(0xFF80BDFF) // Input border focused
val MoodleLoginPlaceholder = Color(0xFF6C757D) // Placeholder text
val MoodleLoginText = Color(0xFF333333) // Main text color
val MoodleLoginLink = Color(0xFF2E6DA4) // Link color (same as button)
val MoodleLoginError = Color(0xFFDC3545) // Error text color (Bootstrap red)

// ED logo
val ed1 = Color(0xFF6B46C1)
val ed2 = Color(0xFFEC4899)

// ED Post component colors
val EdPostCardBackground = DarkBackground // #0F0F0F - background for gradient frame and card
val EdPostTextFieldContainer = Color(0xFF16161A) // container color for text fields in modal
val EdPostStatusPublished = Color(0xFF2ECC71) // green for published status
val EdPostStatusCancelled = Color(0xFFE74C3C) // red for cancelled status
val EdPostTextPrimary = Color.White // primary text color
val EdPostIconSecondary = Color.White.copy(alpha = 0.7f) // secondary icon color (70% opacity)
val EdPostBorderSecondary = Color.White.copy(alpha = 0.4f) // secondary border color (40% opacity)
val EdPostTransparent = Color.Transparent // transparent color for borders and buttons

// IS-Academia brand colors
val isAcademiaR = Color(0xFFC62828)

// Container Color for connectors Screen
val containerColor = Color.Transparent
// preview BackGround color
const val previewBgColor: Long = 0xFF0D0D0D // long because preview takes Long type for color
