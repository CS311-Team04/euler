package com.android.sample.settings.connectors

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions and spacing constants for Connectors UI components. This eliminates magic
 * values and improves maintainability.
 */
object ConnectorsDimensions {

  // ========== Card Dimensions ==========
  val CardAspectRatio = 0.95f
  val CardCornerRadius = 22.dp
  val CardPadding = 24.dp
  val CardElevationDark = 0.dp
  val CardElevationLight = 2.dp
  val CardBorderWidth = 1.dp

  // ========== Card Spacing ==========
  val CardContentSpacing = 0.dp
  val CardLogoHeight = 40.dp
  val CardLogoSpacer = 22.dp
  val CardTitleSubtitleSpacer = 8.dp
  val CardStatusButtonSpacer = 16.dp

  // ========== Card Typography ==========
  val CardTitleFontSize = 18.sp
  val CardTitleLetterSpacing = (-0.3).sp
  val CardSubtitleFontSize = 13.sp
  val CardSubtitleLineHeight = 18.sp
  val CardSubtitlePadding = 4.dp
  val CardStatusFontSize = 12.sp

  // ========== Button Dimensions ==========
  val ButtonCornerRadius = 12.dp
  val ButtonVerticalPadding = 13.dp
  val ButtonTextFontSize = 14.sp
  val ButtonTextLetterSpacing = 0.1.sp

  // ========== Logo Dimensions ==========
  val LogoSize = 40.dp
  val LogoCornerRadius = 8.dp
  val LogoPadding = 8.dp

  // ========== Moodle Logo ==========
  val MoodleGraduationCapWidth = 14.dp
  val MoodleGraduationCapHeight = 6.dp
  val MoodleGraduationCapCornerRadius = 2.dp
  val MoodleGraduationCapOffsetY = (-3).dp
  val MoodleTasselWidth = 1.5.dp
  val MoodleTasselHeight = 8.dp
  val MoodleTasselOffsetX = 0.dp
  val MoodleTasselOffsetY = 6.dp
  val MoodleTextFontSize = 24.sp
  val MoodleTextOffsetY = (-6).dp

  // ========== Ed Logo ==========
  val EdTextFontSize = 24.sp
  val EdTextLetterSpacing = (-3).sp

  // ========== IS Academia Logo ==========
  val ISAcademiaHeight = 28.dp
  val ISAcademiaPadding = 6.dp
  val ISAcademiaTextFontSize = 12.sp
  val ISAcademiaUnderlineWidth = 35.dp
  val ISAcademiaUnderlineHeight = 1.dp
  val ISAcademiaUnderlineOffsetY = (-1).dp
  val ScreenHorizontalPadding = 24.dp
  val ScreenTopPadding = 28.dp
  val ScreenBottomPadding = 28.dp
  val ScreenContentSpacing = 4.dp
  val TopBarIconButtonSize = 40.dp
  val TopBarIconSize = 22.dp
  val TopBarSpacerWidth = 16.dp
  val TopBarTitleFontSize = 28.sp
  val TopBarTitleLetterSpacing = (-0.5).sp
  val TopBarSubtitleSpacer = 6.dp
  val TopBarSubtitleFontSize = 14.sp
  val GridSpacing = 18.dp
  val GridBottomPadding = 32.dp
  val FooterFontSize = 11.sp
  val FooterLetterSpacing = 1.sp
  val DialogCornerRadius = 20.dp
  val DialogButtonCornerRadius = 10.dp
  val DialogTitleFontSize = 18.sp
  val DialogTextFontSize = 14.sp
  val DialogTextLineHeight = 20.sp
}
