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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val MoodleOrangeLight = Color(0xFFFFF3E0) // Light Orange background
private val MoodleOrangeDark = Color(0xFFEF6C00) // Dark Orange for text/icon

@Composable
fun MoodleSourceBadge(metadata: MoodleMetadata, modifier: Modifier = Modifier) {
  val badgeColor = MoodleOrangeLight
  val onBadgeColor = MoodleOrangeDark
  val secondaryColor = MoodleOrangeDark.copy(alpha = 0.85f)

  Surface(
      shape = RoundedCornerShape(12.dp),
      color = badgeColor,
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
              Icon(
                  imageVector = Icons.Outlined.School,
                  contentDescription = "Moodle Overview",
                  tint = onBadgeColor)

              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
  Spacer(modifier = Modifier.height(4.dp))
}
