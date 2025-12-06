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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
              .border(BorderStroke(2.dp, gradient), RoundedCornerShape(18.dp))
              .background(Color(0xFF0F0F0F), RoundedCornerShape(18.dp))) {
        // Main card container
        Card(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(2.dp) // keep gradient visible
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
              content()
            }
      }
}
