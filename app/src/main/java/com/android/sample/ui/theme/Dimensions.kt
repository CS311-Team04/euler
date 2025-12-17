package com.android.sample.ui.theme

import androidx.compose.ui.unit.dp

/** Global spacing and sizing constants to avoid magic numbers in UI components. */
object Dimensions {
  val BadgeCornerRadius = 12.dp
  val BadgePaddingHorizontal = 12.dp
  val BadgePaddingVertical = 10.dp
  val BadgeContentSpacing = 12.dp
  val TextVerticalSpacing = 2.dp
  val BadgeSpacerHeight = 4.dp
  val InputHorizontal = 16.dp
  val InputHeight = 52.dp

  // ChatInputBar button dimensions
  val ChatInputButtonSpacing = 8.dp
  val ChatInputButtonRowPaddingEnd = 10.dp
  val ChatInputMicButtonSize = 40.dp
  val ChatInputMicIconSize = 22.dp
  val ChatInputMicBorderWidth = 2.dp
  val ChatInputVoiceSendButtonSize = 43.dp
  val ChatInputVoiceSendButtonOffsetY = (-1).dp
  val ChatInputVoiceSendIconSize = 18.dp
  val ChatInputVoiceModeButtonAlpha = 0.2f
  val ChatInputProgressIndicatorStrokeWidth = 2.dp

  // Listening view dimensions
  val ChatInputListeningViewHeight = 55.dp
  val ChatInputListeningViewPaddingStart = 16.dp
  val ChatInputListeningViewPaddingEnd = 4.dp
  val ChatInputVoiceBarSpacing = 4.dp
  val ChatInputVoiceBarWidth = 5.dp
  val ChatInputVoiceBarMaxHeight = 22.dp
  val ChatInputVoiceBarMinHeight = 6.dp
  val ChatInputVoiceBarColorAlpha = 0.8f
  val ChatInputVoiceBarSpacerWidth = 12.dp

  // Voice bar animation values
  val ChatInputVoiceBarAnimationInitialScale = 0.3f
  val ChatInputVoiceBarAnimationTargetScale = 1.0f
  val ChatInputVoiceBarAnimationDurationMillis = 500
  val ChatInputVoiceBarAnimationDelays = listOf(0, 150, 300, 450, 600) // milliseconds

  // Text field dimensions
  val ChatInputCornerRadius = 50.dp
  val ChatInputMaxLines = 5
  // RAG Source Badge dimensions
  val SourceBadgeCornerRadius = 20.dp
  val SourceBadgePaddingHorizontal = 8.dp
  val SourceBadgePaddingVertical = 5.dp
  val SourceBadgeContentSpacing = 6.dp
  val SourceBadgeLogoSize = 18.dp
  val SourceBadgeLogoCornerRadius = 3.dp

  // Compact indicator dimensions
  val CompactIndicatorCornerRadius = 8.dp
  val CompactIndicatorPaddingHorizontal = 10.dp
  val CompactIndicatorPaddingVertical = 6.dp
  val CompactIndicatorIconSize = 12.dp
  val CompactIndicatorSpacing = 6.dp
}
