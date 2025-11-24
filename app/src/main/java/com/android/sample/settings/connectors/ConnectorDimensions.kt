package com.android.sample.settings.connectors

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions and spacing constants for Connectors UI components. This eliminates magic
 * values and improves maintainability.
 */
object ConnectorsDimensions {
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
}
