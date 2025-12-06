package com.android.sample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.android.sample.home.EdPostCard
import com.android.sample.home.EdPostStatus
import com.android.sample.ui.theme.EdPostDimensions
import com.android.sample.ui.theme.EdPostStatusCancelled
import com.android.sample.ui.theme.EdPostStatusPublished
import com.android.sample.ui.theme.EdPostTextPrimary
import com.android.sample.ui.theme.ed1
import com.android.sample.ui.theme.ed2

@Composable
fun EdPostedCard(card: EdPostCard, modifier: Modifier = Modifier) {
  val statusColor =
      when (card.status) {
        EdPostStatus.Published -> EdPostStatusPublished
        EdPostStatus.Cancelled -> EdPostStatusCancelled
      }
  val gradient = Brush.horizontalGradient(listOf(ed1, ed2))

  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(
                  horizontal = EdPostDimensions.ContainerHorizontalPadding,
                  vertical = EdPostDimensions.ContainerVerticalPadding),
      verticalArrangement = Arrangement.spacedBy(EdPostDimensions.ContainerVerticalSpacing)) {
        // Gradient frame
        EdPostGradientFrame(gradient = gradient) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(EdPostDimensions.ContentHorizontalPadding),
              verticalArrangement = Arrangement.spacedBy(EdPostDimensions.ContentVerticalSpacing)) {
                // Title and status row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(EdPostDimensions.CardRowHorizontalSpacing)) {
                      Text(
                          text = card.title.ifBlank { "ED post" },
                          color = EdPostTextPrimary,
                          fontWeight = FontWeight.Bold,
                          style =
                              MaterialTheme.typography.titleLarge.copy(
                                  fontSize = EdPostDimensions.CardTitleFontSize))
                      Spacer(Modifier.weight(1f))
                      Box(
                          modifier =
                              Modifier.background(
                                      statusColor.copy(alpha = EdPostDimensions.StatusBadgeAlpha),
                                      RoundedCornerShape(EdPostDimensions.StatusBadgeCornerRadius))
                                  .padding(
                                      horizontal = EdPostDimensions.StatusBadgeHorizontalPadding,
                                      vertical = EdPostDimensions.StatusBadgeVerticalPadding)) {
                            Text(
                                text =
                                    if (card.status == EdPostStatus.Published) "Published"
                                    else "Cancelled",
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelMedium)
                          }
                    }

                // Ed Discussion footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(EdPostDimensions.CardFooterHorizontalSpacing)) {
                      Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = null,
                          tint = statusColor,
                          modifier = Modifier.size(EdPostDimensions.IconStatusSize))
                      Text(
                          text = "Ed Discussion",
                          color = statusColor,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Medium)
                    }
              }
        }
      }
}
