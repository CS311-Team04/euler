package com.android.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.android.sample.ui.theme.EdPostCardBackground
import com.android.sample.ui.theme.EdPostDimensions

/**
 * Reusable gradient frame wrapper for ED post components. Provides a consistent visual style with
 * gradient border and dark background.
 *
 * @param gradient The gradient brush for the border
 * @param content The content to display inside the card
 */
@Composable
internal fun EdPostGradientFrame(
    gradient: Brush,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
  // Gradient frame
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .border(
                  BorderStroke(EdPostDimensions.GradientFrameBorderWidth, gradient),
                  RoundedCornerShape(EdPostDimensions.GradientFrameOuterCornerRadius))
              .background(
                  EdPostCardBackground,
                  RoundedCornerShape(EdPostDimensions.GradientFrameOuterCornerRadius))) {
        // Main card container
        Card(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(EdPostDimensions.GradientFrameInnerPadding)
                    .background(
                        EdPostCardBackground,
                        RoundedCornerShape(EdPostDimensions.GradientFrameInnerCornerRadius)),
            colors = CardDefaults.cardColors(containerColor = EdPostCardBackground),
            shape = RoundedCornerShape(EdPostDimensions.GradientFrameInnerCornerRadius),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = EdPostDimensions.GradientFrameElevation)) {
              content()
            }
      }
}
