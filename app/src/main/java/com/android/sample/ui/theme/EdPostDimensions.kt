package com.android.sample.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized dimensions and spacing constants for ED Post components. This eliminates magic values
 * and improves maintainability.
 */
object EdPostDimensions {

  // ========== Gradient Frame Dimensions ==========
  val GradientFrameBorderWidth = 2.dp
  val GradientFrameOuterCornerRadius = 18.dp
  val GradientFrameInnerPadding = 2.dp // keep gradient visible
  val GradientFrameInnerCornerRadius = 16.dp
  val GradientFrameElevation = 0.dp

  // ========== Container Spacing ==========
  val ContainerHorizontalPadding = 8.dp
  val ContainerVerticalPadding = 8.dp
  val ContainerVerticalSpacing = 10.dp
  val ContentHorizontalPadding = 16.dp
  val ContentVerticalSpacing = 12.dp

  // ========== Text Field Dimensions ==========
  val TextFieldTitleCornerRadius = 12.dp
  val TextFieldBodyCornerRadius = 14.dp
  val TextFieldPlaceholderFontSize = 15.sp
  val TextFieldTitleFontSize = 18.sp
  val TextFieldBodyFontSize = 16.sp
  val TextFieldBodyLineHeight = 23.sp
  val TextFieldBodyMinHeight = 200.dp
  val TextFieldBodyMaxHeight = 360.dp
  val TextFieldBodyMaxLines = 14

  // ========== Icon Dimensions ==========
  val IconEditSize = 18.dp
  val IconSendSize = 18.dp
  val IconStatusSize = 16.dp
  val IconLoadingSpinnerSize = 20.dp
  val IconLoadingSpinnerStrokeWidth = 2.dp

  // ========== Button Dimensions ==========
  val ButtonCancelCornerRadius = 10.dp
  val ButtonPostCornerRadius = 12.dp
  val ButtonGradientCornerRadius = 12.dp
  val ButtonVerticalPadding = 12.dp
  val ButtonTextFontSize = 14.sp
  val ButtonBorderWidth = 1.dp
  val ButtonSpacing = 12.dp
  val ButtonIconSpacerWidth = 10.dp

  // ========== Status Badge Dimensions ==========
  val StatusBadgeCornerRadius = 10.dp
  val StatusBadgeHorizontalPadding = 10.dp
  val StatusBadgeVerticalPadding = 6.dp
  val StatusBadgeAlpha = 0.14f

  // ========== Card Layout Dimensions ==========
  val CardTitleFontSize = 18.sp
  val CardRowHorizontalSpacing = 8.dp
  val CardFooterHorizontalSpacing = 6.dp

  // ========== Result Card Dimensions ==========
  val ResultCardElevation = 8.dp
  val ResultCardCornerRadius = 16.dp
  val ResultCardPadding = 16.dp
  val ResultCardRowSpacing = 12.dp
}
