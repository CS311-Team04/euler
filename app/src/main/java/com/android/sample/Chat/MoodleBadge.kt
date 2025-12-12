package com.android.sample.Chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.android.sample.ui.theme.Dimensions
import com.android.sample.ui.theme.MoodleOrangeDark
import com.android.sample.ui.theme.MoodleOrangeLight

@Composable
fun MoodleSourceBadge(metadata: MoodleMetadata, modifier: Modifier = Modifier) {
  val badgeColor = MoodleOrangeLight
  val onBadgeColor = MoodleOrangeDark
  val secondaryColor = MoodleOrangeDark.copy(alpha = 0.85f)

  Surface(
      shape = RoundedCornerShape(Dimensions.BadgeCornerRadius),
      color = badgeColor,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.BadgeContentSpacing),
            modifier =
                Modifier.padding(
                    horizontal = Dimensions.BadgePaddingHorizontal,
                    vertical = Dimensions.BadgePaddingVertical)) {
              Icon(
                  imageVector = Icons.Outlined.School,
                  contentDescription = "Moodle Overview",
                  tint = onBadgeColor)

              Column(verticalArrangement = Arrangement.spacedBy(Dimensions.TextVerticalSpacing)) {
                Text(
                    text = "Moodle Overview • ${metadata.weekLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = onBadgeColor)
                Text(
                    text = "Updated: ${formatMoodleUpdatedDate(metadata.lastUpdated)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryColor)
              }
            }
      }
  Spacer(modifier = Modifier.height(Dimensions.BadgeSpacerHeight))
}
